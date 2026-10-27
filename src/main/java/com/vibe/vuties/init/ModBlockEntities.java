package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.block.entity.EnergyCellBlockEntity;
import com.vibe.vuties.block.entity.WheelBlockEntity;
import com.vibe.vuties.block.entity.WheelStructureBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Vuties.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WheelBlockEntity>> WHEEL =
            BLOCK_ENTITIES.register("wheel", () ->
                    BlockEntityType.Builder.of(WheelBlockEntity::new, ModBlocks.WHEEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WheelStructureBlockEntity>> WHEEL_STRUCTURE =
            BLOCK_ENTITIES.register("wheel_structure", () ->
                    BlockEntityType.Builder.of(WheelStructureBlockEntity::new, ModBlocks.WHEEL_STRUCTURE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL =
            BLOCK_ENTITIES.register("energy_cell", () ->
                    BlockEntityType.Builder.of(EnergyCellBlockEntity::new, ModBlocks.ENERGY_CELL.get()).build(null));
}
