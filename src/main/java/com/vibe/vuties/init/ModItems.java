package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Vuties.MODID);

    // Mk I (Leather + 1 armor, VTOL lift)
    public static final DeferredItem<RotorpackItem> ROTORPACK_MK1 = ITEMS.registerItem("rotorpack_mk1",
        p -> new RotorpackItem(RotorpackItem.Tier.MK1, ModArmorMaterials.ROTORPACK_MK1_MATERIAL, p));

    // Mk II (Iron + 1 armor, Hover mode)
    public static final DeferredItem<RotorpackItem> ROTORPACK_MK2 = ITEMS.registerItem("rotorpack_mk2",
        p -> new RotorpackItem(RotorpackItem.Tier.MK2, ModArmorMaterials.ROTORPACK_MK2_MATERIAL, p));

    // Mk III (Diamond + 1 armor, Hover + Glider Wings)
    public static final DeferredItem<RotorpackItem> ROTORPACK_MK3 = ITEMS.registerItem("rotorpack_mk3",
        p -> new RotorpackItem(RotorpackItem.Tier.MK3, ModArmorMaterials.ROTORPACK_MK3_MATERIAL, p));

    // Alias for legacy references
    public static final DeferredItem<RotorpackItem> ROTORPACK = ROTORPACK_MK1;

    public static final DeferredItem<Item> PROPELLER = ITEMS.registerItem("propeller",
        Item::new, new Item.Properties());
}
