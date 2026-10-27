package com.vibe.vuties.menu;

import com.vibe.vuties.init.ModBlocks;
import com.vibe.vuties.init.ModItems;
import com.vibe.vuties.init.ModMenuTypes;
import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.ArmorUpgradeTier;
import com.vibe.vuties.item.ExoLegsItem;
import com.vibe.vuties.item.JetBootsItem;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeTableMenu extends AbstractContainerMenu {

    public static final int GEAR_SLOT = 0;
    public static final int ROTORPACK_SLOT = 0; // Backwards compatibility alias
    public static final int UPGRADE_1_SLOT = 1;
    public static final int HOVER_SLOT = 1;     // Backwards compatibility alias
    public static final int UPGRADE_2_SLOT = 2;
    public static final int WINGS_SLOT = 2;     // Backwards compatibility alias
    public static final int ARMOR_SLOT = 3;

    private static final int INV_START = 4;
    private static final int INV_END = 31;
    private static final int HOTBAR_START = 31;
    private static final int HOTBAR_END = 40;

    private final SimpleContainer gearContainer = new SimpleContainer(1);
    private final SimpleContainer upgradesContainer = new SimpleContainer(3);
    private final ContainerLevelAccess access;
    private final Player player;

    private boolean isUpdating = false;

    public UpgradeTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public UpgradeTableMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.UPGRADE_TABLE.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;

        // Slot 0: Gear Slot (Rotorpack, Ace Helmet, Exo Legs, Jet Boots) (x = 27, y = 35)
        this.addSlot(new Slot(this.gearContainer, 0, 27, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RotorpackItem ||
                       stack.getItem() instanceof AceHelmetItem ||
                       stack.getItem() instanceof ExoLegsItem ||
                       stack.getItem() instanceof JetBootsItem;
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

        // Slot 1: Upgrade 1 (Hover for Rotorpack / Water Breathing for Helmet / Phase for Legs / Slide for Boots) (x = 81, y = 35)
        this.addSlot(new Slot(this.upgradesContainer, 0, 81, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (isRotorpackPresent()) {
                    return stack.is(ModItems.HOVER_UPGRADE.get());
                } else if (isHelmetPresent()) {
                    return stack.is(ModItems.WATER_BREATHING_UPGRADE.get());
                } else if (isLeggingsPresent()) {
                    return stack.is(ModItems.PHASE_UPGRADE.get());
                } else if (isBootsPresent()) {
                    return stack.is(ModItems.SLIDE_UPGRADE.get());
                }
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                UpgradeTableMenu.this.onUpgradeSlotsChanged();
            }
        });

        // Slot 2: Upgrade 2 (Wings for Rotorpack / Night Vision for Helmet) (x = 107, y = 35)
        this.addSlot(new Slot(this.upgradesContainer, 1, 107, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (isRotorpackPresent()) {
                    return stack.is(ModItems.WINGS_UPGRADE.get());
                } else if (isHelmetPresent()) {
                    return stack.is(ModItems.NIGHT_VISION_UPGRADE.get());
                }
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                UpgradeTableMenu.this.onUpgradeSlotsChanged();
            }
        });

        // Slot 3: Armor Upgrade Slot (x = 133, y = 35)
        this.addSlot(new Slot(this.upgradesContainer, 2, 133, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isGearPresent() && isArmorUpgradeItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                UpgradeTableMenu.this.onUpgradeSlotsChanged();
            }
        });

        // Player 3x9 inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Listeners for container changes
        this.gearContainer.addListener(c -> this.onGearSlotChanged());
        this.upgradesContainer.addListener(c -> this.onUpgradeSlotsChanged());
    }

    public boolean isGearPresent() {
        return isRotorpackPresent() || isHelmetPresent() || isLeggingsPresent() || isBootsPresent();
    }

    public boolean isBootsPresent() {
        return this.gearContainer.getItem(0).getItem() instanceof JetBootsItem;
    }

    public boolean isLeggingsPresent() {
        return this.gearContainer.getItem(0).getItem() instanceof ExoLegsItem;
    }

    public boolean isRotorpackPresent() {
        return this.gearContainer.getItem(0).getItem() instanceof RotorpackItem;
    }

    public boolean isHelmetPresent() {
        return this.gearContainer.getItem(0).getItem() instanceof AceHelmetItem;
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

    public void onGearSlotChanged() {
        if (this.isUpdating) return;
        this.isUpdating = true;
        try {
            ItemStack gear = this.gearContainer.getItem(0);
            if (gear.getItem() instanceof RotorpackItem) {
                // 1. Hover
                if (RotorpackItem.hasHoverUpgrade(gear)) {
                    this.upgradesContainer.setItem(0, new ItemStack(ModItems.HOVER_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                }

                // 2. Wings
                if (RotorpackItem.hasWingsUpgrade(gear)) {
                    this.upgradesContainer.setItem(1, new ItemStack(ModItems.WINGS_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                }

                // 3. Armor
                populateArmorSlot(RotorpackItem.getArmorUpgrade(gear));
            } else if (gear.getItem() instanceof AceHelmetItem) {
                // 1. Water Breathing
                if (AceHelmetItem.hasWaterBreathingUpgrade(gear)) {
                    this.upgradesContainer.setItem(0, new ItemStack(ModItems.WATER_BREATHING_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                }

                // 2. Night Vision
                if (AceHelmetItem.hasNightVisionUpgrade(gear)) {
                    this.upgradesContainer.setItem(1, new ItemStack(ModItems.NIGHT_VISION_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                }

                // 3. Armor
                populateArmorSlot(AceHelmetItem.getArmorUpgrade(gear));
            } else if (gear.getItem() instanceof ExoLegsItem) {
                if (ExoLegsItem.hasPhaseUpgrade(gear)) {
                    this.upgradesContainer.setItem(0, new ItemStack(ModItems.PHASE_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                }
                this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                populateArmorSlot(ExoLegsItem.getArmorUpgrade(gear));
            } else if (gear.getItem() instanceof JetBootsItem) {
                if (JetBootsItem.hasSlideUpgrade(gear)) {
                    this.upgradesContainer.setItem(0, new ItemStack(ModItems.SLIDE_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                }
                this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                populateArmorSlot(JetBootsItem.getArmorUpgrade(gear));
            } else {
                // Gear removed: clear upgrade slots
                this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                this.upgradesContainer.setItem(2, ItemStack.EMPTY);
            }
            this.broadcastChanges();
        } finally {
            this.isUpdating = false;
        }
    }

    private void populateArmorSlot(ArmorUpgradeTier tier) {
        switch (tier) {
            case IRON -> this.upgradesContainer.setItem(2, new ItemStack(ModItems.IRON_ARMOR_UPGRADE.get()));
            case DIAMOND -> this.upgradesContainer.setItem(2, new ItemStack(ModItems.DIAMOND_ARMOR_UPGRADE.get()));
            case NETHERITE -> this.upgradesContainer.setItem(2, new ItemStack(ModItems.NETHERITE_ARMOR_UPGRADE.get()));
            default -> this.upgradesContainer.setItem(2, ItemStack.EMPTY);
        }
    }

    public void onUpgradeSlotsChanged() {
        if (this.isUpdating) return;
        this.isUpdating = true;
        try {
            ItemStack gear = this.gearContainer.getItem(0);
            if (gear.getItem() instanceof RotorpackItem) {
                // 1. Hover
                ItemStack hoverStack = this.upgradesContainer.getItem(0);
                RotorpackItem.setHoverUpgrade(gear, hoverStack.is(ModItems.HOVER_UPGRADE.get()));

                // 2. Wings
                ItemStack wingsStack = this.upgradesContainer.getItem(1);
                RotorpackItem.setWingsUpgrade(gear, wingsStack.is(ModItems.WINGS_UPGRADE.get()));

                // 3. Armor
                ItemStack armorStack = this.upgradesContainer.getItem(2);
                ArmorUpgradeTier tier = getArmorTierFromItem(armorStack);
                RotorpackItem.setArmorUpgrade(gear, tier);

                this.getSlot(GEAR_SLOT).setChanged();
                this.broadcastChanges();
            } else if (gear.getItem() instanceof AceHelmetItem) {
                // 1. Water Breathing
                ItemStack waterStack = this.upgradesContainer.getItem(0);
                AceHelmetItem.setWaterBreathingUpgrade(gear, waterStack.is(ModItems.WATER_BREATHING_UPGRADE.get()));

                // 2. Night Vision
                ItemStack nightStack = this.upgradesContainer.getItem(1);
                AceHelmetItem.setNightVisionUpgrade(gear, nightStack.is(ModItems.NIGHT_VISION_UPGRADE.get()));

                // 3. Armor
                ItemStack armorStack = this.upgradesContainer.getItem(2);
                ArmorUpgradeTier tier = getArmorTierFromItem(armorStack);
                AceHelmetItem.setArmorUpgrade(gear, tier);

                this.getSlot(GEAR_SLOT).setChanged();
                this.broadcastChanges();
            } else if (gear.getItem() instanceof ExoLegsItem) {
                ItemStack phaseStack = this.upgradesContainer.getItem(0);
                ExoLegsItem.setPhaseUpgrade(gear, phaseStack.is(ModItems.PHASE_UPGRADE.get()));

                ItemStack armorStack = this.upgradesContainer.getItem(2);
                ArmorUpgradeTier tier = getArmorTierFromItem(armorStack);
                ExoLegsItem.setArmorUpgrade(gear, tier);

                this.getSlot(GEAR_SLOT).setChanged();
                this.broadcastChanges();
            } else if (gear.getItem() instanceof JetBootsItem) {
                ItemStack slideStack = this.upgradesContainer.getItem(0);
                JetBootsItem.setSlideUpgrade(gear, slideStack.is(ModItems.SLIDE_UPGRADE.get()));

                ItemStack armorStack = this.upgradesContainer.getItem(2);
                ArmorUpgradeTier tier = getArmorTierFromItem(armorStack);
                JetBootsItem.setArmorUpgrade(gear, tier);

                this.getSlot(GEAR_SLOT).setChanged();
                this.broadcastChanges();
            }
        } finally {
            this.isUpdating = false;
        }
    }

    // Backwards-compatible hook alias
    public void onRotorpackSlotChanged() {
        onGearSlotChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == GEAR_SLOT) {
                // Quick-move gear out to player inventory
                if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index >= UPGRADE_1_SLOT && index <= ARMOR_SLOT) {
                // Quick-move an Upgrade out to player inventory
                if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else {
                // From player inventory into table slots
                if (stackInSlot.getItem() instanceof RotorpackItem ||
                    stackInSlot.getItem() instanceof AceHelmetItem ||
                    stackInSlot.getItem() instanceof ExoLegsItem ||
                    stackInSlot.getItem() instanceof JetBootsItem) {
                    if (!this.moveItemStackTo(stackInSlot, GEAR_SLOT, GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.HOVER_UPGRADE.get())) {
                    if (!isRotorpackPresent() || !this.moveItemStackTo(stackInSlot, UPGRADE_1_SLOT, UPGRADE_1_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.WINGS_UPGRADE.get())) {
                    if (!isRotorpackPresent() || !this.moveItemStackTo(stackInSlot, UPGRADE_2_SLOT, UPGRADE_2_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.WATER_BREATHING_UPGRADE.get())) {
                    if (!isHelmetPresent() || !this.moveItemStackTo(stackInSlot, UPGRADE_1_SLOT, UPGRADE_1_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.NIGHT_VISION_UPGRADE.get())) {
                    if (!isHelmetPresent() || !this.moveItemStackTo(stackInSlot, UPGRADE_2_SLOT, UPGRADE_2_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.PHASE_UPGRADE.get())) {
                    if (!isLeggingsPresent() || !this.moveItemStackTo(stackInSlot, UPGRADE_1_SLOT, UPGRADE_1_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.SLIDE_UPGRADE.get())) {
                    if (!isBootsPresent() || !this.moveItemStackTo(stackInSlot, UPGRADE_1_SLOT, UPGRADE_1_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isArmorUpgradeItem(stackInSlot)) {
                    if (!isGearPresent()) {
                        return ItemStack.EMPTY;
                    }
                    Slot armorSlot = this.slots.get(ARMOR_SLOT);
                    if (!armorSlot.hasItem()) {
                        ItemStack single = stackInSlot.split(1);
                        armorSlot.set(single);
                        armorSlot.setChanged();
                    } else {
                        // Swap currently installed armor upgrade with the new one
                        ItemStack oldArmor = armorSlot.getItem().copy();
                        ItemStack newArmor = stackInSlot.split(1);
                        armorSlot.set(newArmor);
                        armorSlot.setChanged();
                        if (!player.getInventory().add(oldArmor)) {
                            player.drop(oldArmor, false);
                        }
                    }
                } else if (index >= INV_START && index < HOTBAR_START) {
                    if (!this.moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                    if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_START, false)) {
                        return ItemStack.EMPTY;
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
        this.access.execute((level, pos) -> {
            ItemStack gear = this.gearContainer.getItem(0);
            if (!gear.isEmpty()) {
                // Ensure current upgrades are saved onto the gear
                this.onUpgradeSlotsChanged();
                // Clear upgrade slots so they don't drop duplicate items
                this.isUpdating = true;
                try {
                    this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                    this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                    this.upgradesContainer.setItem(2, ItemStack.EMPTY);
                    this.gearContainer.setItem(0, ItemStack.EMPTY);
                } finally {
                    this.isUpdating = false;
                }
                player.getInventory().placeItemBackInInventory(gear);
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.UPGRADE_TABLE.get());
    }
}
