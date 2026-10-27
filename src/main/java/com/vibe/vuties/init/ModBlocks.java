package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.block.EnergyCellBlock;
import com.vibe.vuties.block.UpgradeTableBlock;
import com.vibe.vuties.block.WheelBlock;
import com.vibe.vuties.block.WheelStructureBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Vuties.MODID);

    public static final DeferredBlock<UpgradeTableBlock> UPGRADE_TABLE = BLOCKS.registerBlock("upgrade_table",
        UpgradeTableBlock::new,
        BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD));

    public static final DeferredBlock<WheelBlock> WHEEL = BLOCKS.registerBlock("wheel",
        WheelBlock::new,
        BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredBlock<WheelStructureBlock> WHEEL_STRUCTURE = BLOCKS.registerBlock("wheel_structure",
        WheelStructureBlock::new,
        BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.WOOD).noOcclusion().noLootTable());

    public static final DeferredBlock<EnergyCellBlock> ENERGY_CELL = BLOCKS.registerBlock("energy_cell",
        EnergyCellBlock::new,
        BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL));
}
