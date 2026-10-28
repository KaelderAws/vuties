package com.vibe.vuties.menu;

import com.mojang.datafixers.util.Pair;
import com.vibe.vuties.block.entity.UpgradeTableBlockEntity;
import com.vibe.vuties.init.ModBlocks;
import com.vibe.vuties.init.ModItems;
import com.vibe.vuties.init.ModMenuTypes;
import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.ArmorUpgradeTier;
import com.vibe.vuties.item.ExoLegsItem;
import com.vibe.vuties.item.RotorpackItem;
import com.vibe.vuties.item.ShockBootsItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class UpgradeTableMenu extends AbstractContainerMenu {

    public static final int GEAR_SLOT = 0;
    public static final int MODULE_SLOTS_START = 1;
    public static final int MODULE_SLOTS_END = 6; // 6 slots: 1, 2, 3, 4, 5, 6
    public static final int ARMOR_SLOTS_START = 7;
    public static final int ARMOR_SLOTS_END = 10; // 4 slots: 7, 8, 9, 10
    public static final int INV_START = 11;
    public static final int INV_END = 38;
    public static final int HOTBAR_START = 38;
    public static final int HOTBAR_END = 47;

    private static final EquipmentSlot[] ARMOR_EQUIPMENT_SLOTS = new EquipmentSlot[]{
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")
    };

    private static final int[][] MODULE_SLOT_COORDS = new int[][]{
        {55, 14},  // Slot 0 (Top-Left)
        {105, 14}, // Slot 1 (Top-Right)
        {48, 34},  // Slot 2 (Mid-Left)
        {112, 34}, // Slot 3 (Mid-Right)
        {55, 54},  // Slot 4 (Bottom-Left)
        {105, 54}  // Slot 5 (Bottom-Right)
    };

    private final SimpleContainer gearContainer = new SimpleContainer(1);
    private final SimpleContainer upgradesContainer = new SimpleContainer(6);
    private final ContainerLevelAccess access;
    private final Player player;
    private final ContainerData data;
    @Nullable
    private final UpgradeTableBlockEntity tableBE;

    private final Set<String> currentlyInstalledUpgrades = new HashSet<>();
    private boolean isUpdating = false;

    public UpgradeTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, new SimpleContainerData(6), null);
    }

    public UpgradeTableMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, ContainerData data, @Nullable UpgradeTableBlockEntity tableBE) {
        super(ModMenuTypes.UPGRADE_TABLE.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;
        this.data = data;
        this.tableBE = tableBE;

        this.addDataSlots(data);

        // Slot 0: Center Gear Slot (x = 80, y = 34)
        this.addSlot(new Slot(this.gearContainer, 0, 80, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RotorpackItem ||
                       stack.getItem() instanceof AceHelmetItem ||
                       stack.getItem() instanceof ExoLegsItem ||
                       stack.getItem() instanceof ShockBootsItem;
            }

            @Override
            public boolean mayPickup(Player player) {
                return getInstallCooldown() <= 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                UpgradeTableMenu.this.onGearSlotChanged();
            }
        });

        // Slots 1..6: Hexagonal 6 Module Slots around the center
        for (int i = 0; i < 6; i++) {
            final int moduleIndex = i;
            int posX = MODULE_SLOT_COORDS[i][0];
            int posY = MODULE_SLOT_COORDS[i][1];

            this.addSlot(new Slot(this.upgradesContainer, moduleIndex, posX, posY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (!isGearPresent()) return false;
                    if (getInstallCooldown() > 0) return false;
                    if (!isAllowedUpgrade(stack)) return false;
                    if (isUpgradeAlreadyInstalled(stack, moduleIndex)) return false;

                    // If it is a new installation (not just moving an already installed module), must have enough energy
                    String upgradeId = getUpgradeIdentifier(stack);
                    if (!currentlyInstalledUpgrades.contains(upgradeId) && getEnergyStored() < UpgradeTableBlockEntity.ENERGY_PER_UPGRADE) {
                        return false;
                    }
                    return true;
                }

                @Override
                public boolean mayPickup(Player player) {
                    // "Доставать модули из объекта улучшения можно моментально без затрат энергии."
                    return true;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    UpgradeTableMenu.this.onModuleSlotChanged(moduleIndex);
                }
            });
        }

        // Slots 7..10: 4 Player Armor Slots on the left (Helmet, Chest, Legs, Boots)
        for (int i = 0; i < 4; i++) {
            final EquipmentSlot equipSlot = ARMOR_EQUIPMENT_SLOTS[i];
            final ResourceLocation slotTexture = ARMOR_SLOT_TEXTURES[i];
            int armorInvIndex = 39 - i;
            int posX = 8;
            int posY = 7 + i * 18;

            this.addSlot(new Slot(playerInventory, armorInvIndex, posX, posY) {
                @Override
                public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                    player.onEquipItem(equipSlot, oldStack, newStack);
                    super.setByPlayer(newStack, oldStack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.canEquip(equipSlot, player);
                }

                @Override
                public boolean mayPickup(Player player) {
                    ItemStack itemstack = this.getItem();
                    return (itemstack.isEmpty() || player.isCreative() || !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(player);
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, slotTexture);
                }
            });
        }

        // Slots 11..37: Player 3x9 inventory slots (x = 8 + col * 18, y = 84 + row * 18)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Slots 38..46: Player hotbar slots (x = 8 + col * 18, y = 142)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Initialize gear if present in BlockEntity
        if (this.tableBE != null && !this.tableBE.getGearItem().isEmpty()) {
            this.gearContainer.setItem(0, this.tableBE.getGearItem().copy());
            this.populateFromGear(this.gearContainer.getItem(0));
        }

        this.gearContainer.addListener(c -> this.onGearSlotChanged());
    }

    public int getEnergyStored() {
        return (data.get(1) << 16) | (data.get(0) & 0xFFFF);
    }

    public int getMaxEnergyStored() {
        return (data.get(3) << 16) | (data.get(2) & 0xFFFF);
    }

    public int getInstallCooldown() {
        return data.get(4);
    }

    public int getMaxCooldown() {
        return data.get(5);
    }

    public boolean isGearPresent() {
        return !this.gearContainer.getItem(0).isEmpty();
    }

    public static boolean isArmorUpgradeItem(ItemStack stack) {
        return stack.is(ModItems.IRON_ARMOR_UPGRADE.get()) ||
               stack.is(ModItems.DIAMOND_ARMOR_UPGRADE.get()) ||
               stack.is(ModItems.NETHERITE_ARMOR_UPGRADE.get());
    }

    public static ArmorUpgradeTier getArmorTierFromItem(ItemStack stack) {
        if (stack.is(ModItems.IRON_ARMOR_UPGRADE.get())) {
            return ArmorUpgradeTier.IRON;
        } else if (stack.is(ModItems.DIAMOND_ARMOR_UPGRADE.get())) {
            return ArmorUpgradeTier.DIAMOND;
        } else if (stack.is(ModItems.NETHERITE_ARMOR_UPGRADE.get())) {
            return ArmorUpgradeTier.NETHERITE;
        }
        return ArmorUpgradeTier.NONE;
    }

    public static String getUpgradeIdentifier(ItemStack stack) {
        if (stack.isEmpty()) return "";
        if (isArmorUpgradeItem(stack)) {
            return "armor_plating";
        }
        return stack.getItem().getDescriptionId();
    }

    public boolean isAllowedUpgrade(ItemStack stack) {
        ItemStack gear = this.gearContainer.getItem(0);
        if (gear.isEmpty()) return false;

        if (isArmorUpgradeItem(stack)) {
            return true;
        }

        if (gear.getItem() instanceof RotorpackItem) {
            return stack.is(ModItems.HOVER_UPGRADE.get()) || stack.is(ModItems.WINGS_UPGRADE.get());
        } else if (gear.getItem() instanceof AceHelmetItem) {
            return stack.is(ModItems.WATER_BREATHING_UPGRADE.get()) || stack.is(ModItems.NIGHT_VISION_UPGRADE.get());
        } else if (gear.getItem() instanceof ExoLegsItem) {
            return stack.is(ModItems.PHASE_UPGRADE.get());
        } else if (gear.getItem() instanceof ShockBootsItem) {
            return stack.is(ModItems.SLIDE_UPGRADE.get());
        }
        return false;
    }

    public boolean isUpgradeAlreadyInstalled(ItemStack stack, int excludeModuleIndex) {
        String id = getUpgradeIdentifier(stack);
        for (int i = 0; i < 6; i++) {
            if (i == excludeModuleIndex) continue;
            ItemStack existing = this.upgradesContainer.getItem(i);
            if (!existing.isEmpty() && id.equals(getUpgradeIdentifier(existing))) {
                return true;
            }
        }
        return false;
    }

    public void onGearSlotChanged() {
        if (this.isUpdating) return;
        this.isUpdating = true;
        try {
            ItemStack gear = this.gearContainer.getItem(0);
            if (tableBE != null && !player.level().isClientSide) {
                tableBE.setGearItem(gear.copy());
            }

            if (!gear.isEmpty()) {
                populateFromGear(gear);
            } else {
                for (int i = 0; i < 6; i++) {
                    this.upgradesContainer.setItem(i, ItemStack.EMPTY);
                }
                this.currentlyInstalledUpgrades.clear();
            }
            this.broadcastChanges();
        } finally {
            this.isUpdating = false;
        }
    }

    private void populateFromGear(ItemStack gear) {
        for (int i = 0; i < 6; i++) {
            this.upgradesContainer.setItem(i, ItemStack.EMPTY);
        }
        this.currentlyInstalledUpgrades.clear();

        int nextSlot = 0;
        if (gear.getItem() instanceof RotorpackItem) {
            if (RotorpackItem.hasHoverUpgrade(gear)) {
                this.upgradesContainer.setItem(nextSlot++, new ItemStack(ModItems.HOVER_UPGRADE.get()));
            }
            if (RotorpackItem.hasWingsUpgrade(gear)) {
                this.upgradesContainer.setItem(nextSlot++, new ItemStack(ModItems.WINGS_UPGRADE.get()));
            }
            populateArmorInNextSlot(RotorpackItem.getArmorUpgrade(gear), nextSlot);
        } else if (gear.getItem() instanceof AceHelmetItem) {
            if (AceHelmetItem.hasWaterBreathingUpgrade(gear)) {
                this.upgradesContainer.setItem(nextSlot++, new ItemStack(ModItems.WATER_BREATHING_UPGRADE.get()));
            }
            if (AceHelmetItem.hasNightVisionUpgrade(gear)) {
                this.upgradesContainer.setItem(nextSlot++, new ItemStack(ModItems.NIGHT_VISION_UPGRADE.get()));
            }
            populateArmorInNextSlot(AceHelmetItem.getArmorUpgrade(gear), nextSlot);
        } else if (gear.getItem() instanceof ExoLegsItem) {
            if (ExoLegsItem.hasPhaseUpgrade(gear)) {
                this.upgradesContainer.setItem(nextSlot++, new ItemStack(ModItems.PHASE_UPGRADE.get()));
            }
            populateArmorInNextSlot(ExoLegsItem.getArmorUpgrade(gear), nextSlot);
        } else if (gear.getItem() instanceof ShockBootsItem) {
            if (ShockBootsItem.hasSlideUpgrade(gear)) {
                this.upgradesContainer.setItem(nextSlot++, new ItemStack(ModItems.SLIDE_UPGRADE.get()));
            }
            populateArmorInNextSlot(ShockBootsItem.getArmorUpgrade(gear), nextSlot);
        }

        // Track what's currently installed on the gear
        for (int i = 0; i < 6; i++) {
            ItemStack stack = this.upgradesContainer.getItem(i);
            if (!stack.isEmpty()) {
                this.currentlyInstalledUpgrades.add(getUpgradeIdentifier(stack));
            }
        }
    }

    private void populateArmorInNextSlot(ArmorUpgradeTier tier, int slotIndex) {
        if (slotIndex >= 6) return;
        ItemStack stack = switch (tier) {
            case IRON -> new ItemStack(ModItems.IRON_ARMOR_UPGRADE.get());
            case DIAMOND -> new ItemStack(ModItems.DIAMOND_ARMOR_UPGRADE.get());
            case NETHERITE -> new ItemStack(ModItems.NETHERITE_ARMOR_UPGRADE.get());
            default -> ItemStack.EMPTY;
        };
        if (!stack.isEmpty()) {
            this.upgradesContainer.setItem(slotIndex, stack);
        }
    }

    public void onModuleSlotChanged(int slotIndex) {
        if (this.isUpdating) return;
        this.isUpdating = true;
        try {
            ItemStack gear = this.gearContainer.getItem(0);
            if (gear.isEmpty()) return;

            ItemStack changedSlotItem = this.upgradesContainer.getItem(slotIndex);
            String changedId = getUpgradeIdentifier(changedSlotItem);

            // Check if a new upgrade was added that wasn't previously installed
            if (!changedSlotItem.isEmpty() && !this.currentlyInstalledUpgrades.contains(changedId)) {
                if (tableBE != null && !player.level().isClientSide) {
                    tableBE.consumeEnergyForUpgrade();
                    tableBE.startInstallationCooldown();
                }
            }

            // Sync all installed upgrades from the 6 slots onto the gear
            boolean hasHover = false;
            boolean hasWings = false;
            boolean hasWaterBreathing = false;
            boolean hasNightVision = false;
            boolean hasPhase = false;
            boolean hasSlide = false;
            ArmorUpgradeTier armorTier = ArmorUpgradeTier.NONE;

            Set<String> newInstalled = new HashSet<>();
            for (int i = 0; i < 6; i++) {
                ItemStack stack = this.upgradesContainer.getItem(i);
                if (stack.isEmpty()) continue;

                newInstalled.add(getUpgradeIdentifier(stack));

                if (stack.is(ModItems.HOVER_UPGRADE.get())) hasHover = true;
                if (stack.is(ModItems.WINGS_UPGRADE.get())) hasWings = true;
                if (stack.is(ModItems.WATER_BREATHING_UPGRADE.get())) hasWaterBreathing = true;
                if (stack.is(ModItems.NIGHT_VISION_UPGRADE.get())) hasNightVision = true;
                if (stack.is(ModItems.PHASE_UPGRADE.get())) hasPhase = true;
                if (stack.is(ModItems.SLIDE_UPGRADE.get())) hasSlide = true;
                if (isArmorUpgradeItem(stack)) {
                    armorTier = getArmorTierFromItem(stack);
                }
            }

            this.currentlyInstalledUpgrades.clear();
            this.currentlyInstalledUpgrades.addAll(newInstalled);

            if (gear.getItem() instanceof RotorpackItem) {
                RotorpackItem.setHoverUpgrade(gear, hasHover);
                RotorpackItem.setWingsUpgrade(gear, hasWings);
                RotorpackItem.setArmorUpgrade(gear, armorTier);
            } else if (gear.getItem() instanceof AceHelmetItem) {
                AceHelmetItem.setWaterBreathingUpgrade(gear, hasWaterBreathing);
                AceHelmetItem.setNightVisionUpgrade(gear, hasNightVision);
                AceHelmetItem.setArmorUpgrade(gear, armorTier);
            } else if (gear.getItem() instanceof ExoLegsItem) {
                ExoLegsItem.setPhaseUpgrade(gear, hasPhase);
                ExoLegsItem.setArmorUpgrade(gear, armorTier);
            } else if (gear.getItem() instanceof ShockBootsItem) {
                ShockBootsItem.setSlideUpgrade(gear, hasSlide);
                ShockBootsItem.setArmorUpgrade(gear, armorTier);
            }

            if (tableBE != null && !player.level().isClientSide) {
                tableBE.setGearItem(gear.copy());
            }

            this.getSlot(GEAR_SLOT).setChanged();
            this.broadcastChanges();
        } finally {
            this.isUpdating = false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == GEAR_SLOT) {
                // Quick-move gear out to player inventory (only if cooldown is 0)
                if (getInstallCooldown() > 0) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index >= MODULE_SLOTS_START && index <= MODULE_SLOTS_END) {
                // Quick-move a module out of table to player inventory (always instant and free)
                if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index >= ARMOR_SLOTS_START && index <= ARMOR_SLOTS_END) {
                // Quick-move armor out to player inventory
                if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else {
                // From player inventory into table slots
                boolean handled = false;

                // 1. Upgradable Gear -> Try center slot, then armor slots
                if (stackInSlot.getItem() instanceof RotorpackItem ||
                    stackInSlot.getItem() instanceof AceHelmetItem ||
                    stackInSlot.getItem() instanceof ExoLegsItem ||
                    stackInSlot.getItem() instanceof ShockBootsItem) {
                    if (!this.slots.get(GEAR_SLOT).hasItem()) {
                        if (this.moveItemStackTo(stackInSlot, GEAR_SLOT, GEAR_SLOT + 1, false)) {
                            handled = true;
                        }
                    }
                }

                // 2. Armor item (or gear as armor) -> Try armor slots on the left
                if (!handled) {
                    for (int i = 0; i < 4; i++) {
                        EquipmentSlot equipSlot = ARMOR_EQUIPMENT_SLOTS[i];
                        int armorSlotIdx = ARMOR_SLOTS_START + i;
                        if (stackInSlot.canEquip(equipSlot, player) && !this.slots.get(armorSlotIdx).hasItem()) {
                            if (this.moveItemStackTo(stackInSlot, armorSlotIdx, armorSlotIdx + 1, false)) {
                                handled = true;
                                break;
                            }
                        }
                    }
                }

                // 3. Upgrade Module -> Try first empty module slot (if gear present, energy available, not in cooldown)
                if (!handled && isAllowedUpgrade(stackInSlot)) {
                    if (isGearPresent() && getInstallCooldown() <= 0 &&
                        getEnergyStored() >= UpgradeTableBlockEntity.ENERGY_PER_UPGRADE &&
                        !isUpgradeAlreadyInstalled(stackInSlot, -1)) {
                        for (int i = MODULE_SLOTS_START; i <= MODULE_SLOTS_END; i++) {
                            Slot modSlot = this.slots.get(i);
                            if (!modSlot.hasItem() && modSlot.mayPlace(stackInSlot)) {
                                ItemStack single = stackInSlot.split(1);
                                modSlot.set(single);
                                modSlot.setChanged();
                                handled = true;
                                break;
                            }
                        }
                    }
                }

                // 4. Move between inventory and hotbar
                if (!handled) {
                    if (index >= INV_START && index < HOTBAR_START) {
                        if (!this.moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                        if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_START, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.tableBE == null) {
            this.access.execute((level, pos) -> {
                ItemStack gear = this.gearContainer.getItem(0);
                if (!gear.isEmpty()) {
                    for (int i = 0; i < 6; i++) {
                        this.upgradesContainer.setItem(i, ItemStack.EMPTY);
                    }
                    this.gearContainer.setItem(0, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(gear);
                }
            });
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.UPGRADE_TABLE.get());
    }
}
