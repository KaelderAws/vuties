package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Vuties.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_ENGINE);
        event.register(ModKeyMappings.TOGGLE_HOVER);
        event.register(ModKeyMappings.TOGGLE_HELMET);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.UPGRADE_TABLE.get(), UpgradeTableScreen::new);
    }
}
