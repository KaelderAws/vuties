package com.vibe.vuties.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class UpgradeItem extends Item {

    public enum Type {
        HOVER("hover"),
        WINGS("wings"),
        WATER_BREATHING("water_breathing"),
        NIGHT_VISION("night_vision"),
        PHASE("phase"),
        IRON_ARMOR("iron_armor"),
        DIAMOND_ARMOR("diamond_armor"),
        NETHERITE_ARMOR("netherite_armor");

        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        public boolean isArmor() {
            return this == IRON_ARMOR || this == DIAMOND_ARMOR || this == NETHERITE_ARMOR;
        }
    }

    private final Type type;

    public UpgradeItem(Type type, Properties properties) {
        super(properties.stacksTo(16));
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vuties.upgrade." + this.type.getId() + ".desc")
                .withStyle(ChatFormatting.GRAY));

        if (this.type.isArmor()) {
            tooltip.add(Component.translatable("tooltip.vuties.upgrade.armor_replace_note")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
