package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.block.entity.UpgradeTableBlockEntity;
import com.vibe.vuties.menu.UpgradeTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class UpgradeTableScreen extends AbstractContainerScreen<UpgradeTableMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/gui/container/upgrade_table.png");

    public UpgradeTableScreen(UpgradeTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 3;
        this.inventoryLabelX = 28;
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
            } else if (slotIdx >= UpgradeTableMenu.MODULE_SLOTS_START && slotIdx <= UpgradeTableMenu.MODULE_SLOTS_END) {
                if (!this.menu.isGearPresent()) {
                    graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_module_no_gear").withStyle(ChatFormatting.RED), mouseX, mouseY);
                } else if (this.menu.getInstallCooldown() > 0) {
                    int seconds = (this.menu.getInstallCooldown() + 19) / 20;
                    graphics.renderTooltip(this.font, Component.translatable("gui.vuties.upgrade_table.installing", seconds).withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
                } else if (this.menu.getEnergyStored() < UpgradeTableBlockEntity.ENERGY_PER_UPGRADE) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable("tooltip.vuties.upgrade_table.slot_module_empty"));
                    tooltip.add(Component.translatable("gui.vuties.upgrade_table.not_enough_energy").withStyle(ChatFormatting.RED));
                    graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                } else {
                    graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_module_empty"), mouseX, mouseY);
                }
            } else if (slotIdx >= UpgradeTableMenu.ARMOR_SLOTS_START && slotIdx <= UpgradeTableMenu.ARMOR_SLOTS_END) {
                switch (slotIdx - UpgradeTableMenu.ARMOR_SLOTS_START) {
                    case 0 -> graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_helmet"), mouseX, mouseY);
                    case 1 -> graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_chestplate"), mouseX, mouseY);
                    case 2 -> graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_leggings"), mouseX, mouseY);
                    case 3 -> graphics.renderTooltip(this.font, Component.translatable("tooltip.vuties.upgrade_table.slot_boots"), mouseX, mouseY);
                }
            }
        }

        // Energy Bar Tooltip
        int energyBarX = this.leftPos + 151;
        int energyBarY = this.topPos + 6;
        if (mouseX >= energyBarX && mouseX <= energyBarX + 17 && mouseY >= energyBarY && mouseY <= energyBarY + 71) {
            List<Component> energyTooltip = new ArrayList<>();
            energyTooltip.add(Component.translatable("gui.vuties.upgrade_table.energy_title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            String cur = NumberFormat.getInstance().format(this.menu.getEnergyStored());
            String max = NumberFormat.getInstance().format(this.menu.getMaxEnergyStored());
            energyTooltip.add(Component.translatable("gui.vuties.upgrade_table.energy_amount", cur, max).withStyle(ChatFormatting.GRAY));
            energyTooltip.add(Component.translatable("gui.vuties.upgrade_table.energy_cost", NumberFormat.getInstance().format(UpgradeTableBlockEntity.ENERGY_PER_UPGRADE)).withStyle(ChatFormatting.DARK_GRAY));
            if (this.menu.getEnergyStored() < UpgradeTableBlockEntity.ENERGY_PER_UPGRADE) {
                energyTooltip.add(Component.translatable("gui.vuties.upgrade_table.not_enough_energy").withStyle(ChatFormatting.RED));
            }
            graphics.renderComponentTooltip(this.font, energyTooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Render Energy Bar
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        int barHeight = 70;
        int barWidth = 16;
        int fillHeight = maxEnergy > 0 ? (int) ((long) energy * barHeight / maxEnergy) : 0;
        if (energy > 0 && fillHeight == 0) {
            fillHeight = 1;
        }
        if (fillHeight > barHeight) {
            fillHeight = barHeight;
        }

        if (fillHeight > 0) {
            int fillY = y + 7 + (barHeight - fillHeight);
            int fillBottom = y + 7 + barHeight;
            int fillX = x + 152;
            int fillRight = fillX + barWidth;

            // Electric cyan / blue gradient fill
            graphics.fillGradient(fillX, fillY, fillRight, fillBottom, 0xFF00E6FF, 0xFF0077AA);
            // 3D bevel / highlight
            graphics.fill(fillX, fillY, fillX + 1, fillBottom, 0x80FFFFFF);
            graphics.fill(fillRight - 1, fillY, fillRight, fillBottom, 0x40000000);
        }

        // Render Installation Cooldown Progress Bar & Visual Effect
        int cooldown = this.menu.getInstallCooldown();
        int maxCooldown = this.menu.getMaxCooldown();
        if (cooldown > 0 && maxCooldown > 0) {
            // Pulse overlay over center slot (x: 80, y: 34, w: 16, h: 16)
            int alpha = (int) (40 + 25 * Math.sin((System.currentTimeMillis() % 1000) / 1000.0 * Math.PI * 2));
            graphics.fill(x + 80, y + 34, x + 96, y + 50, (alpha << 24) | 0x00E6FF);

            // Progress bar directly below center slot (x: 80, y: 51, w: 16, h: 2)
            int slotX = x + 80;
            int slotY = y + 51;
            int barW = 16;
            int barH = 2;
            int progress = (maxCooldown - cooldown) * barW / maxCooldown;
            graphics.fill(slotX, slotY, slotX + barW, slotY + barH, 0xFF222222);
            if (progress > 0) {
                graphics.fill(slotX, slotY, slotX + progress, slotY + barH, 0xFF00FFCC);
            }
        }
    }
}
