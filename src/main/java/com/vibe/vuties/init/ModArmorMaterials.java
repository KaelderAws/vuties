package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, Vuties.MODID);

    // Mk I: Кожаный нагрудник + 1 (Защита: 3 + 1 = 4, Стойкость: 0 + 1 = 1.0)
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROTORPACK_MK1_MATERIAL =
        ARMOR_MATERIALS.register("rotorpack_mk1", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 2);
                map.put(ArmorItem.Type.LEGGINGS, 3);
                map.put(ArmorItem.Type.CHESTPLATE, 4);
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.BODY, 4);
            }),
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(Items.LEATHER),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "rotorpack_mk1"))),
            1.0F,
            0.0F
        ));

    // Mk II: Железный нагрудник + 1 (Защита: 6 + 1 = 7, Стойкость: 0 + 1 = 1.0)
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROTORPACK_MK2_MATERIAL =
        ARMOR_MATERIALS.register("rotorpack_mk2", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 7);
            }),
            12,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "rotorpack_mk2"))),
            1.0F,
            0.0F
        ));

    // Mk III: Алмазный нагрудник + 1 (Защита: 8 + 1 = 9, Стойкость: 2 + 1 = 3.0)
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROTORPACK_MK3_MATERIAL =
        ARMOR_MATERIALS.register("rotorpack_mk3", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 4);
                map.put(ArmorItem.Type.LEGGINGS, 7);
                map.put(ArmorItem.Type.CHESTPLATE, 9);
                map.put(ArmorItem.Type.HELMET, 4);
                map.put(ArmorItem.Type.BODY, 9);
            }),
            18,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            () -> Ingredient.of(Items.DIAMOND),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "rotorpack_mk3"))),
            3.0F,
            0.0F
        ));
}
