package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.entity.WheelRunnerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Vuties.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WheelRunnerEntity>> WHEEL_RUNNER =
            ENTITY_TYPES.register("wheel_runner", () ->
                    EntityType.Builder.<WheelRunnerEntity>of(WheelRunnerEntity::new, MobCategory.MISC)
                            .sized(0.8F, 1.8F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .noSave()
                            .noSummon()
                            .build("wheel_runner"));
}
