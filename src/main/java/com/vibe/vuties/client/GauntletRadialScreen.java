package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibe.vuties.item.GauntletItem;
import com.vibe.vuties.network.GauntletModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class GauntletRadialScreen extends Screen {

    private final InteractionHand hand;
    private final ItemStack gauntletStack;
    private final boolean hasDrill;
    private final int initialMode;

    private int hoveredSector = -1; // 0: Normal, 1: Drill
    private int lastHoveredSector = -1;

    public GauntletRadialScreen(InteractionHand hand, ItemStack gauntletStack) {
        super(Component.translatable("gui.vuties.gauntlet_menu.title"));
        this.hand = hand;
        this.gauntletStack = gauntletStack;
        this.hasDrill = GauntletItem.hasDrillUpgrade(gauntletStack);
        this.initialMode = GauntletItem.getMode(gauntletStack);
        this.hoveredSector = initialMode;
        this.lastHoveredSector = initialMode;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // High-contrast clean dark backdrop
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void tick() {
        super.tick();

        // Release Alt to confirm selection and close
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                          GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        if (!altHeld) {
            confirmSelection();
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            setHovered(0);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            setHovered(1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            confirmSelection();
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            confirmSelection();
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setHovered(int sector) {
        this.hoveredSector = sector;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;

        int dx = mouseX - cx;
        int dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        // Detect mouse position in radial sectors
        if (dist > 20) {
            double angleDeg = Math.toDegrees(Math.atan2(dy, dx)); // -180 .. 180
            // Right sector (Drill): angle between -80 and +80
            // Left sector (Normal): angle between 100 and 180 or -180 and -100
            if (angleDeg >= -80.0 && angleDeg <= 80.0) {
                setHovered(1);
            } else if (angleDeg >= 100.0 || angleDeg <= -100.0) {
                setHovered(0);
            }
        }

        // Sector Radii
        float rInner = 48.0f;
        float rOuterNormal = (hoveredSector == 0) ? 112.0f : 104.0f;
        float rOuterDrill = (hoveredSector == 1) ? 112.0f : 104.0f;

        // Sector Colors
        // Sector 0 (Left - Normal)
        boolean normalHovered = hoveredSector == 0;
        boolean normalActive = initialMode == GauntletItem.MODE_NORMAL;
        int normalFill = normalHovered ? 0xD5163650 : (normalActive ? 0xAA122638 : 0x750D1824);
        int normalBorder = normalHovered ? 0xFF00FFFF : (normalActive ? 0xAA00B4D8 : 0x442A4560);

        // Sector 1 (Right - Drill)
        boolean drillHovered = hoveredSector == 1;
        boolean drillActive = initialMode == GauntletItem.MODE_DRILL;
        int drillFill;
        int drillBorder;
        if (!hasDrill) {
            drillFill = drillHovered ? 0xC5381414 : 0x75200C0C;
            drillBorder = drillHovered ? 0xFFFF4444 : 0x55551A1A;
        } else {
            drillFill = drillHovered ? 0xD5144525 : (drillActive ? 0xAA10351C : 0x750C2213);
            drillBorder = drillHovered ? 0xFF00FF88 : (drillActive ? 0xAA2EC4B6 : 0x44255535);
        }

        // ================= 1. DRAW RADIAL WHEEL ARCS =================
        // Sector 0: Left Arc (100° to 260°)
        drawArcSector(graphics, cx, cy, rInner, rOuterNormal, 100.0f, 260.0f, normalFill, normalBorder);

        // Sector 1: Right Arc (-80° to 80°)
        drawArcSector(graphics, cx, cy, rInner, rOuterDrill, -80.0f, 80.0f, drillFill, drillBorder);

        // Center Hub (Circle from 0° to 360°, radius 0 to rInner - 3)
        drawFullCircle(graphics, cx, cy, rInner - 4.0f, 0xEE0B121C, 0xAA00B4D8);

        // Flush geometric vertex buffers before text rendering
        graphics.flush();

        // ================= 2. DRAW CENTER HUB TEXT =================
        String hubTitle = "ЭКЗОПЕРЧАТКА";
        graphics.drawCenteredString(this.font, hubTitle, cx, cy - 22, 0x00E5FF);

        if (hoveredSector == 0) {
            graphics.drawCenteredString(this.font, "«ОБЫЧНЫЙ»", cx, cy - 7, 0x00FFFF);
            graphics.drawCenteredString(this.font, "Манипулятор", cx, cy + 6, 0x90E0EF);
        } else if (hoveredSector == 1) {
            if (!hasDrill) {
                graphics.drawCenteredString(this.font, "«БУР ЗАБЛОКИРОВАН»", cx, cy - 7, 0xFF5555);
                graphics.drawCenteredString(this.font, "Требуется модуль", cx, cy + 6, 0xFF8888);
            } else {
                graphics.drawCenteredString(this.font, "«СПИРАЛЬНЫЙ БУР»", cx, cy - 7, 0x00FF88);
                graphics.drawCenteredString(this.font, "Гуррен-Лаганн", cx, cy + 6, 0x70E000);
            }
        }

        // Active indicator in hub
        String activeText = (initialMode == GauntletItem.MODE_DRILL) ? "● Бур активен" : "● Манипулятор активен";
        int activeColor = (initialMode == GauntletItem.MODE_DRILL) ? 0x55FF55 : 0x00B4D8;
        graphics.drawCenteredString(this.font, activeText, cx, cy + 20, activeColor);

        // ================= 3. DRAW ICONS & LABELS ON RADIAL SECTORS =================
        // Sector 0 (Left Sector): Center is at (-78, 0)
        int leftCenterX = cx - 78;
        int leftCenterY = cy;

        graphics.drawCenteredString(this.font, "✋", leftCenterX, leftCenterY - 14, normalHovered ? 0x00FFFF : 0x90E0EF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.vuties.gauntlet_menu.normal").getString(), leftCenterX, leftCenterY, normalHovered ? 0xFFFFFF : 0xCCDDEE);
        graphics.drawCenteredString(this.font, normalActive ? "● ТЕКУЩИЙ" : (normalHovered ? "◄ ВЫБРАТЬ" : ""), leftCenterX, leftCenterY + 12, normalActive ? 0x55FF55 : 0x00E5FF);

        // Sector 1 (Right Sector): Center is at (+78, 0)
        int rightCenterX = cx + 78;
        int rightCenterY = cy;

        if (!hasDrill) {
            graphics.drawCenteredString(this.font, "🔒", rightCenterX, rightCenterY - 14, 0xFF5555);
            graphics.drawCenteredString(this.font, Component.translatable("gui.vuties.gauntlet_menu.drill").getString(), rightCenterX, rightCenterY, 0xCC7777);
            graphics.drawCenteredString(this.font, "ТРЕБУЕТСЯ МОДУЛЬ", rightCenterX, rightCenterY + 12, 0xFF4444);
        } else {
            graphics.drawCenteredString(this.font, "🌀", rightCenterX, rightCenterY - 14, drillHovered ? 0x00FF88 : 0x70E000);
            graphics.drawCenteredString(this.font, Component.translatable("gui.vuties.gauntlet_menu.drill").getString(), rightCenterX, rightCenterY, drillHovered ? 0xFFFFFF : 0xCCEECC);
            graphics.drawCenteredString(this.font, drillActive ? "● ТЕКУЩИЙ" : (drillHovered ? "ВЫБРАТЬ ►" : ""), rightCenterX, rightCenterY + 12, drillActive ? 0x55FF55 : 0x00FF88);
        }

        // Instructions Footer
        graphics.drawCenteredString(this.font, Component.translatable("gui.vuties.gauntlet_menu.release_hint").getString(), cx, cy + 130, 0x8899AA);
    }

    /**
     * Draws a crisp tessellated circular sector arc between rIn and rOut from startDeg to endDeg.
     */
    private void drawArcSector(GuiGraphics graphics, float cx, float cy, float rIn, float rOut, float startDeg, float endDeg, int argbFill, int argbBorder) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());

        int aF = (argbFill >> 24) & 0xFF;
        int rF = (argbFill >> 16) & 0xFF;
        int gF = (argbFill >> 8) & 0xFF;
        int bF = argbFill & 0xFF;

        float step = 2.0f;

        // 1. Fill quad strips
        for (float deg = startDeg; deg < endDeg; deg += step) {
            float nextDeg = Math.min(deg + step, endDeg);
            double rad1 = Math.toRadians(deg);
            double rad2 = Math.toRadians(nextDeg);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            float x1In = cx + rIn * cos1;
            float y1In = cy + rIn * sin1;
            float x1Out = cx + rOut * cos1;
            float y1Out = cy + rOut * sin1;

            float x2Out = cx + rOut * cos2;
            float y2Out = cy + rOut * sin2;
            float x2In = cx + rIn * cos2;
            float y2In = cy + rIn * sin2;

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(rF, gF, bF, aF);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(rF, gF, bF, aF);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(rF, gF, bF, aF);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(rF, gF, bF, aF);
        }

        // 2. Outer Border Arc
        int aB = (argbBorder >> 24) & 0xFF;
        int rB = (argbBorder >> 16) & 0xFF;
        int gB = (argbBorder >> 8) & 0xFF;
        int bB = argbBorder & 0xFF;

        float bThick = 2.0f;
        for (float deg = startDeg; deg < endDeg; deg += step) {
            float nextDeg = Math.min(deg + step, endDeg);
            double rad1 = Math.toRadians(deg);
            double rad2 = Math.toRadians(nextDeg);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            float x1In = cx + (rOut - bThick) * cos1;
            float y1In = cy + (rOut - bThick) * sin1;
            float x1Out = cx + rOut * cos1;
            float y1Out = cy + rOut * sin1;

            float x2Out = cx + rOut * cos2;
            float y2Out = cy + rOut * sin2;
            float x2In = cx + (rOut - bThick) * cos2;
            float y2In = cy + (rOut - bThick) * sin2;

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(rB, gB, bB, aB);
        }

        // 3. Inner Border Arc
        for (float deg = startDeg; deg < endDeg; deg += step) {
            float nextDeg = Math.min(deg + step, endDeg);
            double rad1 = Math.toRadians(deg);
            double rad2 = Math.toRadians(nextDeg);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            float x1In = cx + rIn * cos1;
            float y1In = cy + rIn * sin1;
            float x1Out = cx + (rIn + bThick) * cos1;
            float y1Out = cy + (rIn + bThick) * sin1;

            float x2Out = cx + (rIn + bThick) * cos2;
            float y2Out = cy + (rIn + bThick) * sin2;
            float x2In = cx + rIn * cos2;
            float y2In = cy + rIn * sin2;

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(rB, gB, bB, aB);
        }
    }

    /**
     * Draws a filled circular center disk with border.
     */
    private void drawFullCircle(GuiGraphics graphics, float cx, float cy, float radius, int argbFill, int argbBorder) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());

        int aF = (argbFill >> 24) & 0xFF;
        int rF = (argbFill >> 16) & 0xFF;
        int gF = (argbFill >> 8) & 0xFF;
        int bF = argbFill & 0xFF;

        float step = 5.0f;
        for (float deg = 0; deg < 360.0f; deg += step) {
            float nextDeg = Math.min(deg + step, 360.0f);
            double rad1 = Math.toRadians(deg);
            double rad2 = Math.toRadians(nextDeg);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            buffer.addVertex(matrix, cx, cy, 0.0f).setColor(rF, gF, bF, aF);
            buffer.addVertex(matrix, cx + radius * cos1, cy + radius * sin1, 0.0f).setColor(rF, gF, bF, aF);
            buffer.addVertex(matrix, cx + radius * cos2, cy + radius * sin2, 0.0f).setColor(rF, gF, bF, aF);
            buffer.addVertex(matrix, cx, cy, 0.0f).setColor(rF, gF, bF, aF);
        }

        // Border ring
        int aB = (argbBorder >> 24) & 0xFF;
        int rB = (argbBorder >> 16) & 0xFF;
        int gB = (argbBorder >> 8) & 0xFF;
        int bB = argbBorder & 0xFF;

        float bThick = 2.0f;
        for (float deg = 0; deg < 360.0f; deg += step) {
            float nextDeg = Math.min(deg + step, 360.0f);
            double rad1 = Math.toRadians(deg);
            double rad2 = Math.toRadians(nextDeg);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            float x1In = cx + (radius - bThick) * cos1;
            float y1In = cy + (radius - bThick) * sin1;
            float x1Out = cx + radius * cos1;
            float y1Out = cy + radius * sin1;

            float x2Out = cx + radius * cos2;
            float y2Out = cy + radius * sin2;
            float x2In = cx + (radius - bThick) * cos2;
            float y2In = cy + (radius - bThick) * sin2;

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(rB, gB, bB, aB);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(rB, gB, bB, aB);
        }
    }

    private void confirmSelection() {
        int selected = (hoveredSector != -1) ? hoveredSector : initialMode;

        // Block selecting drill mode if module not installed
        if (selected == GauntletItem.MODE_DRILL && !hasDrill) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                    Component.translatable("message.vuties.drill_not_installed"),
                    true
                );
            }
            selected = GauntletItem.MODE_NORMAL;
        }

        if (selected != initialMode) {
            int handSlot = (hand == InteractionHand.MAIN_HAND) ? 0 : 1;
            PacketDistributor.sendToServer(new GauntletModePayload(handSlot, selected));
            GauntletItem.setMode(gauntletStack, selected);

            if (minecraft != null && minecraft.player != null) {
                Component msg = (selected == GauntletItem.MODE_DRILL)
                    ? Component.translatable("message.vuties.gauntlet_drill_activated")
                    : Component.translatable("message.vuties.gauntlet_normal_activated");
                minecraft.player.displayClientMessage(msg, true);
            }
        }
    }
}
