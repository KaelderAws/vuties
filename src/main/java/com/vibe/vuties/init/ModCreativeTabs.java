package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Vuties.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
        CREATIVE_MODE_TABS.register("vuties_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.vuties"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.ROTORPACK.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.ACE_HELMET.get());
                output.accept(ModItems.ROTORPACK.get());
                output.accept(ModItems.EXO_LEGS.get());
                output.accept(ModItems.SHOCK_BOOTS.get());
                output.accept(ModItems.PROPELLER.get());
                output.accept(ModItems.UPGRADE_TABLE.get());
                output.accept(ModItems.WHEEL.get());
                output.accept(ModItems.ENERGY_CELL.get());
                output.accept(ModItems.HOVER_UPGRADE.get());
                output.accept(ModItems.WINGS_UPGRADE.get());
                output.accept(ModItems.WATER_BREATHING_UPGRADE.get());
                output.accept(ModItems.NIGHT_VISION_UPGRADE.get());
                output.accept(ModItems.PHASE_UPGRADE.get());
                output.accept(ModItems.SLIDE_UPGRADE.get());
                output.accept(ModItems.IRON_ARMOR_UPGRADE.get());
                output.accept(ModItems.DIAMOND_ARMOR_UPGRADE.get());
                output.accept(ModItems.NETHERITE_ARMOR_UPGRADE.get());
            })
            .build());
}
