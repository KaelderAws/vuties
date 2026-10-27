package com.vibe.vuties;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.vibe.vuties.init.ModArmorMaterials;
import com.vibe.vuties.init.ModBlockEntities;
import com.vibe.vuties.init.ModBlocks;
import com.vibe.vuties.init.ModCapabilities;
import com.vibe.vuties.init.ModCreativeTabs;
import com.vibe.vuties.init.ModDataComponents;
import com.vibe.vuties.init.ModEntityTypes;
import com.vibe.vuties.init.ModItems;
import com.vibe.vuties.init.ModMenuTypes;
import com.vibe.vuties.init.ModRecipeTypes;
import com.vibe.vuties.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Vuties.MODID)
public class Vuties {
    public static final String MODID = "vuties";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Vuties(IEventBus modEventBus) {
        LOGGER.info("Initializing Vuties - Twin-Rotor Copterpack ready for takeoff!");

        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ModNetwork::registerPayloads);
    }
}
