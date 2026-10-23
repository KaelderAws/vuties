package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.menu.UpgradeTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Vuties.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<UpgradeTableMenu>> UPGRADE_TABLE =
            MENUS.register("upgrade_table", () -> new MenuType<>(UpgradeTableMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
