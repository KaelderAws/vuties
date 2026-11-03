package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.GauntletItem;
import com.vibe.vuties.network.DrillMinePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Vuties.MODID, value = Dist.CLIENT)
public class ClientDrillHandler {

    public static final float MAX_DRILL_SPEED = 48.0f; // Maximum degrees per tick
    public static final float ACCELERATION = 3.6f;     // Speed gain per tick while digging/spinning
    public static final float DECELERATION = 1.4f;     // Speed loss per tick when stopped

    public static class DrillArmState {
        public boolean active = false;
        public float speed = 0.0f;
        public float prevAngle = 0.0f;
        public float angle = 0.0f;
        public float prevThrust = 0.0f;
        public float thrust = 0.0f;

        public void update(boolean working) {
            this.active = working;
            this.prevThrust = this.thrust;
            if (working) {
                this.thrust = Math.min(1.0f, this.thrust + 0.25f);
                this.speed = Math.min(MAX_DRILL_SPEED, this.speed + ACCELERATION);
            } else {
                this.thrust = Math.max(0.0f, this.thrust - 0.20f);
                this.speed = Math.max(0.0f, this.speed - DECELERATION);
            }

            this.prevAngle = this.angle;
            this.angle += this.speed;
            if (this.angle > 360000.0f) {
                this.angle -= 360000.0f;
                this.prevAngle -= 360000.0f;
            }
        }

        public float getInterpolatedAngle(float partialTick) {
            return Mth.lerp(partialTick, this.prevAngle, this.angle);
        }

        public float getInterpolatedThrust(float partialTick) {
            return Mth.lerp(partialTick, this.prevThrust, this.thrust);
        }
    }

    public static final DrillArmState RIGHT_ARM = new DrillArmState();
    public static final DrillArmState LEFT_ARM = new DrillArmState();

    private static BlockPos offhandMiningPos = null;
    private static float offhandMiningProgress = 0.0f;
    private static int offhandDestroyTicks = 0;
    private static int offhandDestroyDelay = 0;
    private static final int OFFHAND_BREAKER_OFFSET = 12345;

    public static float getDrillDestroyProgress(BlockState state, Level level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return 0.0f;
        if (hardness == 0) return 1.0f;
        return 35.0f / hardness / 30.0f;
    }

    public static boolean isDigging() {
        return RIGHT_ARM.active || LEFT_ARM.active;
    }

    public static boolean isRightArmDigging() {
        return RIGHT_ARM.active;
    }

    public static boolean isLeftArmDigging() {
        return LEFT_ARM.active;
    }

    public static boolean isRightArmDigging(Player player) {
        if (player == Minecraft.getInstance().player) {
            return RIGHT_ARM.active;
        }
        boolean isRightMain = player.getMainArm() == HumanoidArm.RIGHT;
        boolean mainDrill = player.getMainHandItem().getItem() instanceof GauntletItem && GauntletItem.isDrillActive(player.getMainHandItem());
        boolean offDrill = player.getOffhandItem().getItem() instanceof GauntletItem && GauntletItem.isDrillActive(player.getOffhandItem());
        return isRightMain ? (mainDrill && player.swinging) : (offDrill && player.isUsingItem());
    }

    public static boolean isLeftArmDigging(Player player) {
        if (player == Minecraft.getInstance().player) {
            return LEFT_ARM.active;
        }
        boolean isRightMain = player.getMainArm() == HumanoidArm.RIGHT;
        boolean mainDrill = player.getMainHandItem().getItem() instanceof GauntletItem && GauntletItem.isDrillActive(player.getMainHandItem());
        boolean offDrill = player.getOffhandItem().getItem() instanceof GauntletItem && GauntletItem.isDrillActive(player.getOffhandItem());
        return isRightMain ? (offDrill && player.isUsingItem()) : (mainDrill && player.swinging);
    }

    public static float getAimX(Player player) {
        return (float) Math.toRadians(player.getXRot()) - 1.5707963f;
    }

    public static float getAimY(Player player, float partialTick) {
        float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot());
        return (float) Math.toRadians(headYaw - bodyYaw);
    }

    public static float getInterpolatedThrust(float partialTick) {
        return Math.max(RIGHT_ARM.getInterpolatedThrust(partialTick), LEFT_ARM.getInterpolatedThrust(partialTick));
    }

    public static float getInterpolatedRightThrust(float partialTick) {
        return RIGHT_ARM.getInterpolatedThrust(partialTick);
    }

    public static float getInterpolatedLeftThrust(float partialTick) {
        return LEFT_ARM.getInterpolatedThrust(partialTick);
    }

    public static float getInterpolatedRightAngle(float partialTick) {
        return RIGHT_ARM.getInterpolatedAngle(partialTick);
    }

    public static float getInterpolatedLeftAngle(float partialTick) {
        return LEFT_ARM.getInterpolatedAngle(partialTick);
    }

    public static float getInterpolatedAngle(float partialTick) {
        return Math.max(RIGHT_ARM.getInterpolatedAngle(partialTick), LEFT_ARM.getInterpolatedAngle(partialTick));
    }

    private static final java.util.Map<Class<?>, Boolean> INTERACTIVE_BLOCK_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isBlockInteractive(BlockState state) {
        if (state.isAir()) return false;
        if (state.hasBlockEntity()) return true;

        Block block = state.getBlock();
        return INTERACTIVE_BLOCK_CACHE.computeIfAbsent(block.getClass(), c -> {
            Class<?> current = c;
            while (current != null && current != BlockBehaviour.class && current != Object.class) {
                for (java.lang.reflect.Method m : current.getDeclaredMethods()) {
                    if (m.getName().equals("useWithoutItem") || m.getName().equals("useItemOn")) {
                        return true;
                    }
                }
                current = current.getSuperclass();
            }
            return false;
        });
    }

    public static boolean isEntityInteractive(Entity entity) {
        if (entity == null) return false;
        return entity instanceof AbstractVillager
            || entity instanceof Animal
            || entity instanceof VehicleEntity
            || entity instanceof ItemFrame
            || entity instanceof ArmorStand
            || entity instanceof Allay;
    }

    public static boolean canStackPlaceOrInteract(ItemStack stack, HitResult hit) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof GauntletItem) {
            return false;
        }

        // Aiming at a block
        if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            if (item instanceof BlockItem) {
                return true;
            }
            if (item instanceof BucketItem) {
                return true;
            }
            if (item instanceof BoneMealItem) {
                return true;
            }
            if (item instanceof ShovelItem || item instanceof HoeItem || item instanceof AxeItem) {
                return true;
            }
            if (item instanceof FlintAndSteelItem || item instanceof ShearsItem) {
                return true;
            }
            if (item instanceof ArmorStandItem || item instanceof BoatItem || item instanceof MinecartItem
                    || item instanceof SpawnEggItem || item instanceof EndCrystalItem || item instanceof HangingEntityItem
                    || item instanceof PlaceOnWaterBlockItem) {
                return true;
            }
        }

        // Usable items that activate on right click (Food, Bow, Crossbow, Trident, Shield, Potion, Ender Pearl, etc.)
        if (item.getUseAnimation(stack) != UseAnim.NONE) {
            return true;
        }
        if (item instanceof EnderpearlItem || item instanceof ProjectileItem) {
            return true;
        }

        return false;
    }

    public static boolean shouldSuppressOffhandDrill(Minecraft mc, Player player) {
        if (player.isUsingItem() || mc.screen != null) {
            return true;
        }

        HitResult hit = mc.hitResult;
        if (hit == null) {
            return false;
        }

        // 1. If the other hand (main hand) holds a block or item that can be placed/used on the target
        ItemStack otherStack = player.getMainHandItem();
        if (canStackPlaceOrInteract(otherStack, hit)) {
            return true;
        }

        // 2. If the target under the crosshair is an interactive element (block or entity)
        // (Unless sneaking with an empty/non-placeable hand, in which case sneak bypasses interaction)
        if (!player.isShiftKeyDown()) {
            if (hit instanceof EntityHitResult ehr) {
                if (isEntityInteractive(ehr.getEntity())) {
                    return true;
                }
            } else if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK && mc.level != null) {
                BlockState state = mc.level.getBlockState(bhr.getBlockPos());
                if (isBlockInteractive(state)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean mainDrill = mainHand.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(mainHand);
        boolean offDrill = offHand.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(offHand);

        // LMB interaction key handling for main hand drill
        if (event.isAttack() && mainDrill) {
            event.setSwingHand(false);
            if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
                event.setCanceled(true);
            }
        }

        // RMB interaction key handling for offhand drill
        if (event.isUseItem() && offDrill) {
            // Only suppress empty-hand swing if offhand drill is actually drilling
            if (!shouldSuppressOffhandDrill(mc, player)) {
                event.setSwingHand(false);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) return;

        // 1. Check Radial Menu opening on Alt key hold
        if (ModKeyMappings.GAUNTLET_MENU.isDown() && mc.screen == null) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean hasMain = mainHand.getItem() instanceof GauntletItem;
            boolean hasOff = offHand.getItem() instanceof GauntletItem;
            if (hasMain || hasOff) {
                mc.setScreen(new GauntletRadialScreen(mainHand, offHand));
            }
        }

        // 2. Identify active drills on both hands
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean mainDrillActive = mainHand.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(mainHand);
        boolean offDrillActive = offHand.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(offHand);

        // LMB controls main hand, RMB controls offhand
        boolean attackDown = mc.screen == null && mc.options.keyAttack.isDown();
        boolean useDown = mc.screen == null && mc.options.keyUse.isDown();

        boolean mainWorking = mainDrillActive && attackDown;
        boolean offWorking = offDrillActive && useDown && !shouldSuppressOffhandDrill(mc, player);

        boolean isRightMain = player.getMainArm() == HumanoidArm.RIGHT;
        boolean rightWorking = isRightMain ? mainWorking : offWorking;
        boolean leftWorking = isRightMain ? offWorking : mainWorking;

        // Update animation & spin state for each arm independently
        RIGHT_ARM.update(rightWorking);
        LEFT_ARM.update(leftWorking);

        // 3. Suppress arm swing when either drill is working
        if (RIGHT_ARM.active || LEFT_ARM.active) {
            player.swinging = false;
            player.swingTime = 0;
            player.attackAnim = 0.0f;
            player.oAttackAnim = 0.0f;
        }

        // 4. Offhand mining logic with RMB (exact parity with vanilla MultiPlayerGameMode: 5-tick destroy delay)
        if (offWorking && mc.level != null && !player.isShiftKeyDown()) {
            if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = bhr.getBlockPos();
                BlockState state = mc.level.getBlockState(pos);
                float hardness = state.getDestroySpeed(mc.level, pos);
                if (!state.isAir() && hardness >= 0) {
                    if (offhandDestroyDelay > 0) {
                        offhandDestroyDelay--;
                    } else if (offhandMiningPos != null && offhandMiningPos.equals(pos)) {
                        float progress = getDrillDestroyProgress(state, mc.level, pos);
                        offhandMiningProgress += progress;
                        offhandDestroyTicks++;

                        if (offhandDestroyTicks % 4 == 0) {
                            SoundType soundtype = state.getSoundType(mc.level, pos, player);
                            mc.getSoundManager().play(
                                new SimpleSoundInstance(
                                    soundtype.getHitSound(),
                                    SoundSource.BLOCKS,
                                    (soundtype.getVolume() + 1.0F) / 8.0F,
                                    soundtype.getPitch() * 0.5F,
                                    SoundInstance.createUnseededRandom(),
                                    pos
                                )
                            );
                        }

                        int stage = (int) (offhandMiningProgress * 10.0f);
                        mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, pos, Math.min(9, stage));

                        if (offhandMiningProgress >= 1.0f) {
                            PacketDistributor.sendToServer(new DrillMinePayload(pos));
                            mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, pos, -1);
                            offhandMiningPos = null;
                            offhandMiningProgress = 0.0f;
                            offhandDestroyTicks = 0;
                            offhandDestroyDelay = 5;
                        }
                    } else {
                        // Switched to a new block or first tick of targeting
                        if (offhandMiningPos != null) {
                            mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, offhandMiningPos, -1);
                        }

                        float initialProgress = getDrillDestroyProgress(state, mc.level, pos);
                        if (initialProgress >= 1.0f) {
                            // Instabreak
                            PacketDistributor.sendToServer(new DrillMinePayload(pos));
                            mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, pos, -1);
                            offhandMiningPos = null;
                            offhandMiningProgress = 0.0f;
                            offhandDestroyTicks = 0;
                            offhandDestroyDelay = 5;
                        } else {
                            offhandMiningPos = pos;
                            offhandMiningProgress = 0.0f;
                            offhandDestroyTicks = 0;
                            mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, pos, 0);
                        }
                    }
                } else {
                    if (offhandMiningPos != null) {
                        mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, offhandMiningPos, -1);
                        offhandMiningPos = null;
                        offhandMiningProgress = 0.0f;
                        offhandDestroyTicks = 0;
                    }
                    if (offhandDestroyDelay > 0) {
                        offhandDestroyDelay--;
                    }
                }
            } else {
                if (offhandMiningPos != null) {
                    mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, offhandMiningPos, -1);
                    offhandMiningPos = null;
                    offhandMiningProgress = 0.0f;
                    offhandDestroyTicks = 0;
                }
                if (offhandDestroyDelay > 0) {
                    offhandDestroyDelay--;
                }
            }
        } else {
            if (offhandMiningPos != null) {
                if (mc.level != null) {
                    mc.levelRenderer.destroyBlockProgress(player.getId() + OFFHAND_BREAKER_OFFSET, offhandMiningPos, -1);
                }
                offhandMiningPos = null;
                offhandMiningProgress = 0.0f;
                offhandDestroyTicks = 0;
            }
            if (offhandDestroyDelay > 0) {
                offhandDestroyDelay--;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            ItemStack held = player.getItemInHand(event.getHand());
            if (held.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(held)) {
                // Suppress swing progress completely in first-person so the drill stays static
                player.swinging = false;
                player.swingTime = 0;
                player.attackAnim = 0.0f;
                player.oAttackAnim = 0.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == null) return;

        boolean rDig = isRightArmDigging(player);
        boolean lDig = isLeftArmDigging(player);

        if (rDig || lDig) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            if (rDig) {
                model.rightArm.skipDraw = true;
                model.rightSleeve.skipDraw = true;
            }
            if (lDig) {
                model.leftArm.skipDraw = true;
                model.leftSleeve.skipDraw = true;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.rightArm.skipDraw = false;
        model.rightSleeve.skipDraw = false;
        model.leftArm.skipDraw = false;
        model.leftSleeve.skipDraw = false;
    }
}
