package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.RotorpackItem;
import com.vibe.vuties.item.UpgradeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Vuties.MODID);

    // Single Rotorpack (without any MK tiers)
    public static final DeferredItem<RotorpackItem> ROTORPACK = ITEMS.registerItem("rotorpack",
        p -> new RotorpackItem(p));

    // Ace Helmet
    public static final DeferredItem<AceHelmetItem> ACE_HELMET = ITEMS.registerItem("ace_helmet",
        AceHelmetItem::new);

    // Propeller material
    public static final DeferredItem<Item> PROPELLER = ITEMS.registerItem("propeller",
        Item::new, new Item.Properties());

    // Upgrade Modules (Simple items)
    public static final DeferredItem<UpgradeItem> HOVER_UPGRADE = ITEMS.registerItem("hover_upgrade",
        p -> new UpgradeItem(UpgradeItem.Type.HOVER, p));

    public static final DeferredItem<UpgradeItem> WINGS_UPGRADE = ITEMS.registerItem("wings_upgrade",
        p -> new UpgradeItem(UpgradeItem.Type.WINGS, p));

    public static final DeferredItem<UpgradeItem> IRON_ARMOR_UPGRADE = ITEMS.registerItem("iron_armor_upgrade",
        p -> new UpgradeItem(UpgradeItem.Type.IRON_ARMOR, p));

    public static final DeferredItem<UpgradeItem> DIAMOND_ARMOR_UPGRADE = ITEMS.registerItem("diamond_armor_upgrade",
        p -> new UpgradeItem(UpgradeItem.Type.DIAMOND_ARMOR, p));

    public static final DeferredItem<UpgradeItem> NETHERITE_ARMOR_UPGRADE = ITEMS.registerItem("netherite_armor_upgrade",
        p -> new UpgradeItem(UpgradeItem.Type.NETHERITE_ARMOR, p));

    // Upgrade Table BlockItem
    public static final DeferredItem<BlockItem> UPGRADE_TABLE = ITEMS.registerSimpleBlockItem("upgrade_table",
        ModBlocks.UPGRADE_TABLE);
}
