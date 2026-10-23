package com.vibe.vuties.menu;

import com.vibe.vuties.init.ModBlocks;
import com.vibe.vuties.init.ModItems;
import com.vibe.vuties.init.ModMenuTypes;
import com.vibe.vuties.item.ArmorUpgradeTier;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeTableMenu extends AbstractContainerMenu {

    public static final int ROTORPACK_SLOT = 0;
    public static final int HOVER_SLOT = 1;
    public static final int WINGS_SLOT = 2;
    public static final int ARMOR_SLOT = 3;

    private static final int INV_START = 4;
    private static final int INV_END = 31;
    private static final int HOTBAR_START = 31;
    private static final int HOTBAR_END = 40;

    private final SimpleContainer rotorpackContainer = new SimpleContainer(1);
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

        // Slot 0: Rotorpack Slot (x = 27, y = 35)
        this.addSlot(new Slot(this.rotorpackContainer, 0, 27, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RotorpackItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                UpgradeTableMenu.this.onRotorpackSlotChanged();
            }
        });

        // Slot 1: Hover Upgrade Slot (x = 81, y = 35)
        this.addSlot(new Slot(this.upgradesContainer, 0, 81, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isRotorpackPresent() && stack.is(ModItems.HOVER_UPGRADE.get());
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

        // Slot 2: Wings Upgrade Slot (x = 107, y = 35)
        this.addSlot(new Slot(this.upgradesContainer, 1, 107, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isRotorpackPresent() && stack.is(ModItems.WINGS_UPGRADE.get());
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
                return isRotorpackPresent() && isArmorUpgradeItem(stack);
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
        this.rotorpackContainer.addListener(c -> this.onRotorpackSlotChanged());
        this.upgradesContainer.addListener(c -> this.onUpgradeSlotsChanged());
    }

    public boolean isRotorpackPresent() {
        return this.rotorpackContainer.getItem(0).getItem() instanceof RotorpackItem;
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

    public void onRotorpackSlotChanged() {
        if (this.isUpdating) return;
        this.isUpdating = true;
        try {
            ItemStack rotorpack = this.rotorpackContainer.getItem(0);
            if (rotorpack.getItem() instanceof RotorpackItem) {
                // Populate upgrade slots from the rotorpack's components
                // 1. Hover
                if (RotorpackItem.hasHoverUpgrade(rotorpack)) {
                    this.upgradesContainer.setItem(0, new ItemStack(ModItems.HOVER_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                }

                // 2. Wings
                if (RotorpackItem.hasWingsUpgrade(rotorpack)) {
                    this.upgradesContainer.setItem(1, new ItemStack(ModItems.WINGS_UPGRADE.get()));
                } else {
                    this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                }

                // 3. Armor
                ArmorUpgradeTier tier = RotorpackItem.getArmorUpgrade(rotorpack);
                switch (tier) {
                    case IRON -> this.upgradesContainer.setItem(2, new ItemStack(ModItems.IRON_ARMOR_UPGRADE.get()));
                    case DIAMOND -> this.upgradesContainer.setItem(2, new ItemStack(ModItems.DIAMOND_ARMOR_UPGRADE.get()));
                    case NETHERITE -> this.upgradesContainer.setItem(2, new ItemStack(ModItems.NETHERITE_ARMOR_UPGRADE.get()));
                    default -> this.upgradesContainer.setItem(2, ItemStack.EMPTY);
                }
            } else {
                // Rotorpack removed: clear upgrade slots
                this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                this.upgradesContainer.setItem(2, ItemStack.EMPTY);
            }
            this.broadcastChanges();
        } finally {
            this.isUpdating = false;
        }
    }

    public void onUpgradeSlotsChanged() {
        if (this.isUpdating) return;
        this.isUpdating = true;
        try {
            ItemStack rotorpack = this.rotorpackContainer.getItem(0);
            if (rotorpack.getItem() instanceof RotorpackItem) {
                // 1. Hover
                ItemStack hoverStack = this.upgradesContainer.getItem(0);
                RotorpackItem.setHoverUpgrade(rotorpack, hoverStack.is(ModItems.HOVER_UPGRADE.get()));

                // 2. Wings
                ItemStack wingsStack = this.upgradesContainer.getItem(1);
                RotorpackItem.setWingsUpgrade(rotorpack, wingsStack.is(ModItems.WINGS_UPGRADE.get()));

                // 3. Armor
                ItemStack armorStack = this.upgradesContainer.getItem(2);
                ArmorUpgradeTier tier = getArmorTierFromItem(armorStack);
                RotorpackItem.setArmorUpgrade(rotorpack, tier);

                // Notify rotorpack slot that its components changed
                this.getSlot(ROTORPACK_SLOT).setChanged();
                this.broadcastChanges();
            }
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

            if (index == ROTORPACK_SLOT) {
                // Quick-move Rotorpack out to player inventory
                if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index >= HOVER_SLOT && index <= ARMOR_SLOT) {
                // Quick-move an Upgrade out to player inventory
                if (!this.moveItemStackTo(stackInSlot, INV_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else {
                // From player inventory into table slots
                if (stackInSlot.getItem() instanceof RotorpackItem) {
                    if (!this.moveItemStackTo(stackInSlot, ROTORPACK_SLOT, ROTORPACK_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.HOVER_UPGRADE.get())) {
                    if (!isRotorpackPresent() || !this.moveItemStackTo(stackInSlot, HOVER_SLOT, HOVER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.WINGS_UPGRADE.get())) {
                    if (!isRotorpackPresent() || !this.moveItemStackTo(stackInSlot, WINGS_SLOT, WINGS_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isArmorUpgradeItem(stackInSlot)) {
                    if (!isRotorpackPresent()) {
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
            ItemStack rotorpack = this.rotorpackContainer.getItem(0);
            if (!rotorpack.isEmpty()) {
                // Ensure current upgrades are saved onto the rotorpack
                this.onUpgradeSlotsChanged();
                // Clear upgrade slots so they don't drop duplicate items
                this.isUpdating = true;
                try {
                    this.upgradesContainer.setItem(0, ItemStack.EMPTY);
                    this.upgradesContainer.setItem(1, ItemStack.EMPTY);
                    this.upgradesContainer.setItem(2, ItemStack.EMPTY);
                    this.rotorpackContainer.setItem(0, ItemStack.EMPTY);
                } finally {
                    this.isUpdating = false;
                }
                player.getInventory().placeItemBackInInventory(rotorpack);
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.UPGRADE_TABLE.get());
    }
}
