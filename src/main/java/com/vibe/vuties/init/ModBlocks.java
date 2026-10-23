package com.vibe.vuties.init;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.block.UpgradeTableBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Vuties.MODID);

    public static final DeferredBlock<UpgradeTableBlock> UPGRADE_TABLE = BLOCKS.registerBlock("upgrade_table",
        p -> new UpgradeTableBlock(p),
        BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD));
}
