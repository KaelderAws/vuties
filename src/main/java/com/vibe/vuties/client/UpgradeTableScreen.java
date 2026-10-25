package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.menu.UpgradeTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class UpgradeTableScreen extends AbstractContainerScreen<UpgradeTableMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/gui/container/upgrade_table.png");

    public UpgradeTableScreen(UpgradeTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // Tooltips for empty slots
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            int slotIdx = this.hoveredSlot.index;
            if (slotIdx == UpgradeTableMenu.GEAR_SLOT) {
                graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_gear"), mouseX, mouseY);
            } else if (slotIdx == UpgradeTableMenu.UPGRADE_1_SLOT) {
                if (this.menu.isHelmetPresent()) {
                    graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_water_breathing"), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_hover"), mouseX, mouseY);
                }
            } else if (slotIdx == UpgradeTableMenu.UPGRADE_2_SLOT) {
                if (this.menu.isHelmetPresent()) {
                    graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_night_vision"), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_wings"), mouseX, mouseY);
                }
            } else if (slotIdx == UpgradeTableMenu.ARMOR_SLOT) {
                graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_armor"), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        // Centered header above the three upgrade modules (slots span x=81..149, center is 115)
        Component modulesTitle = Component.translatable("gui.vuties.upgrade_table.upgrades");
        int textWidth = this.font.width(modulesTitle);
        int centerX = 115 - (textWidth / 2);
        graphics.drawString(this.font, modulesTitle, centerX, 22, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}
