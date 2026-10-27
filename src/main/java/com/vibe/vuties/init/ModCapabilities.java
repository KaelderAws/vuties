package com.vibe.vuties.init;

import com.vibe.vuties.block.WheelBlock;
import com.vibe.vuties.block.entity.WheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.EnergyStorage.BLOCK,
            (level, pos, state, be, side) -> {
                BlockPos masterPos = state.getValue(WheelBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
                if (level.getBlockEntity(masterPos) instanceof WheelBlockEntity wheelBE) {
                    return wheelBE.getEnergyStorage(side);
                }
                return null;
            },
            ModBlocks.WHEEL.get()
        );

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.ENERGY_CELL.get(),
            (be, side) -> be.getEnergyStorage(side)
        );
    }
}
