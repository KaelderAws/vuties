package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.client.renderer.WheelGeoRenderer;
import com.vibe.vuties.init.ModBlockEntities;
import com.vibe.vuties.init.ModEntityTypes;
import com.vibe.vuties.init.ModMenuTypes;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Vuties.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_ENGINE);
        event.register(ModKeyMappings.TOGGLE_HOVER);
        event.register(ModKeyMappings.TOGGLE_HELMET);
        event.register(ModKeyMappings.TOGGLE_NIGHT_VISION);
        event.register(ModKeyMappings.PHASE_SHIFT);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.UPGRADE_TABLE.get(), UpgradeTableScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WHEEL.get(), context -> new WheelGeoRenderer());
        event.registerEntityRenderer(ModEntityTypes.WHEEL_RUNNER.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(MobEffectInstance instance) {
                return instance.showIcon();
            }

            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) {
                return instance.showIcon();
            }
        }, MobEffects.NIGHT_VISION);
    }
}
