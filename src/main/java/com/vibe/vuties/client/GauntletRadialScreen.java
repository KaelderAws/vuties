package com.vibe.vuties.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibe.vuties.item.GauntletItem;
import com.vibe.vuties.network.GauntletModePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class GauntletRadialScreen extends Screen {

    private final boolean bothHands;
    private final ItemStack mainStack;
    private final ItemStack offStack;
    private final InteractionHand singleHand;

    // Main hand properties
    private final boolean mainHasDrill;
    private final boolean mainHasClaw;
    private int mainMode;

    // Off hand properties
    private final boolean offHasDrill;
    private final boolean offHasClaw;
    private int offMode;

    // Currently hovered hand and sector (0: Normal, 1: Drill, 2: Claw)
    private InteractionHand hoveredHand = null;
    private int hoveredSector = -1;

    public GauntletRadialScreen(ItemStack mainStack, ItemStack offStack) {
        super(Component.translatable("gui.vuties.gauntlet_menu.title"));
        this.mainStack = mainStack;
        this.offStack = offStack;

        boolean hasMain = mainStack != null && mainStack.getItem() instanceof GauntletItem;
        boolean hasOff = offStack != null && offStack.getItem() instanceof GauntletItem;

        this.bothHands = hasMain && hasOff;
        if (hasMain) {
            this.singleHand = InteractionHand.MAIN_HAND;
        } else {
            this.singleHand = InteractionHand.OFF_HAND;
        }

        if (hasMain) {
            this.mainHasDrill = GauntletItem.hasDrillUpgrade(mainStack);
            this.mainHasClaw = GauntletItem.hasClawUpgrade(mainStack);
            this.mainMode = GauntletItem.getMode(mainStack);
        } else {
            this.mainHasDrill = false;
            this.mainHasClaw = false;
            this.mainMode = GauntletItem.MODE_NORMAL;
        }

        if (hasOff) {
            this.offHasDrill = GauntletItem.hasDrillUpgrade(offStack);
            this.offHasClaw = GauntletItem.hasClawUpgrade(offStack);
            this.offMode = GauntletItem.getMode(offStack);
        } else {
            this.offHasDrill = false;
            this.offHasClaw = false;
            this.offMode = GauntletItem.MODE_NORMAL;
        }

        if (!this.bothHands) {
            this.hoveredHand = this.singleHand;
            this.hoveredSector = (this.singleHand == InteractionHand.MAIN_HAND) ? this.mainMode : this.offMode;
        }
    }

    // Backward-compatibility constructor if needed
    public GauntletRadialScreen(InteractionHand hand, ItemStack stack) {
        this(hand == InteractionHand.MAIN_HAND ? stack : ItemStack.EMPTY,
             hand == InteractionHand.OFF_HAND ? stack : ItemStack.EMPTY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void tick() {
        super.tick();

        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                          GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        if (!altHeld) {
            applyHoveredSelection();
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        KeyMapping.set(key, true);
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        KeyMapping.set(key, false);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            applyHoveredSelection();
            long window = Minecraft.getInstance().getWindow().getWindow();
            boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                              GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
            if (!altHeld) {
                onClose();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applyHoveredSelection() {
        if (this.hoveredHand == null || this.hoveredSector == -1) return;

        if (this.hoveredHand == InteractionHand.MAIN_HAND) {
            if (this.hoveredSector == GauntletItem.MODE_DRILL && !this.mainHasDrill) return;
            if (this.hoveredSector == GauntletItem.MODE_CLAW && !this.mainHasClaw) return;
            if (this.hoveredSector != this.mainMode) {
                this.mainMode = this.hoveredSector;
                PacketDistributor.sendToServer(new GauntletModePayload(0, this.hoveredSector));
                GauntletItem.setMode(this.mainStack, this.hoveredSector);
                notifyPlayer(this.hoveredSector, InteractionHand.MAIN_HAND);
            }
        } else if (this.hoveredHand == InteractionHand.OFF_HAND) {
            if (this.hoveredSector == GauntletItem.MODE_DRILL && !this.offHasDrill) return;
            if (this.hoveredSector == GauntletItem.MODE_CLAW && !this.offHasClaw) return;
            if (this.hoveredSector != this.offMode) {
                this.offMode = this.hoveredSector;
                PacketDistributor.sendToServer(new GauntletModePayload(1, this.hoveredSector));
                GauntletItem.setMode(this.offStack, this.hoveredSector);
                notifyPlayer(this.hoveredSector, InteractionHand.OFF_HAND);
            }
        }
    }

    private void notifyPlayer(int mode, InteractionHand hand) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        String handPrefix = (hand == InteractionHand.MAIN_HAND) ? "Правая рука: " : "Левая рука: ";
        Component modeName;
        if (mode == GauntletItem.MODE_DRILL) {
            modeName = Component.translatable("message.vuties.gauntlet_drill_activated");
        } else if (mode == GauntletItem.MODE_CLAW) {
            modeName = Component.translatable("message.vuties.gauntlet_claw_activated");
        } else {
            modeName = Component.translatable("message.vuties.gauntlet_normal_activated");
        }
        player.displayClientMessage(Component.literal(handPrefix).append(modeName), true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;

        int dx = mouseX - cx;
        int dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        if (this.bothHands) {
            renderTwinHalfWheels(graphics, cx, cy, dx, dy, dist);
        } else {
            renderSingleWheel(graphics, cx, cy, dx, dy, dist);
        }

        // Instructions Footer
        graphics.drawCenteredString(this.font, Component.translatable("gui.vuties.gauntlet_menu.release_hint").getString(), cx, cy + 130, 0x8899AA);
    }

    // =========================================================================
    // RENDER MODE A: DUAL GAUNTLETS (TWIN HALF-WHEELS)
    // Left Half = Off-Hand | Right Half = Main-Hand
    // =========================================================================
    private void renderTwinHalfWheels(GuiGraphics graphics, int cx, int cy, int dx, int dy, double dist) {
        // Detect sector hover based on side (Left = Offhand, Right = Mainhand)
        if (dist > 28) {
            if (dx < -12) {
                this.hoveredHand = InteractionHand.OFF_HAND;
                if (dy < -20) {
                    this.hoveredSector = 0; // Top-Left: Normal
                } else if (dy > 20) {
                    this.hoveredSector = 2; // Bottom-Left: Claw
                } else {
                    this.hoveredSector = 1; // Mid-Left: Drill
                }
            } else if (dx > 12) {
                this.hoveredHand = InteractionHand.MAIN_HAND;
                if (dy < -20) {
                    this.hoveredSector = 0; // Top-Right: Normal
                } else if (dy > 20) {
                    this.hoveredSector = 2; // Bottom-Right: Claw
                } else {
                    this.hoveredSector = 1; // Mid-Right: Drill
                }
            }
        }

        float rIn = 48.0f;
        float rOut = 106.0f;
        float rHover = 114.0f;

        // 1. LEFT HALF-CIRCLE (OFF-HAND / ВТОРАЯ РУКА): Angles from 104° to 256°
        // L0: Normal (104° .. 148°)
        // L1: Drill (156° .. 204°)
        // L2: Claw (212° .. 256°)
        boolean isOffHover = this.hoveredHand == InteractionHand.OFF_HAND;
        drawSector(graphics, cx, cy, rIn, (isOffHover && this.hoveredSector == 0) ? rHover : rOut,
                104.0f, 148.0f, isOffHover && this.hoveredSector == 0, this.offMode == 0, true, 0);
        drawSector(graphics, cx, cy, rIn, (isOffHover && this.hoveredSector == 1) ? rHover : rOut,
                156.0f, 204.0f, isOffHover && this.hoveredSector == 1, this.offMode == 1, this.offHasDrill, 1);
        drawSector(graphics, cx, cy, rIn, (isOffHover && this.hoveredSector == 2) ? rHover : rOut,
                212.0f, 256.0f, isOffHover && this.hoveredSector == 2, this.offMode == 2, this.offHasClaw, 2);

        // 2. RIGHT HALF-CIRCLE (MAIN-HAND / ВЕДУЩАЯ РУКА): Angles from -76° to +76°
        // R0: Normal (-76° .. -32°)
        // R1: Drill (-24° .. +24°)
        // R2: Claw (+32° .. +76°)
        boolean isMainHover = this.hoveredHand == InteractionHand.MAIN_HAND;
        drawSector(graphics, cx, cy, rIn, (isMainHover && this.hoveredSector == 0) ? rHover : rOut,
                -76.0f, -32.0f, isMainHover && this.hoveredSector == 0, this.mainMode == 0, true, 0);
        drawSector(graphics, cx, cy, rIn, (isMainHover && this.hoveredSector == 1) ? rHover : rOut,
                -24.0f, 24.0f, isMainHover && this.hoveredSector == 1, this.mainMode == 1, this.mainHasDrill, 1);
        drawSector(graphics, cx, cy, rIn, (isMainHover && this.hoveredSector == 2) ? rHover : rOut,
                32.0f, 76.0f, isMainHover && this.hoveredSector == 2, this.mainMode == 2, this.mainHasClaw, 2);

        // 3. CENTER DISK
        drawFullCircle(graphics, cx, cy, rIn - 4.0f, 0xEE0B121C, 0xAA00B4D8);

        // Central divider line between left and right
        graphics.fill(cx - 1, cy - (int) rIn + 5, cx + 1, cy + (int) rIn - 5, 0x6600B4D8);

        graphics.flush();

        // 4. CENTER HUB TEXT (Split between Left and Right hand states)
        graphics.drawCenteredString(this.font, "ЛЕВАЯ", cx - 25, cy - 28, isOffHover ? 0x00FFFF : 0x88CCEE);
        graphics.drawCenteredString(this.font, "ПРАВАЯ", cx + 25, cy - 28, isMainHover ? 0x00FFFF : 0x88CCEE);

        // Active mode icons/text inside hub
        String offModeIcon = (this.offMode == 1) ? "🌀" : (this.offMode == 2 ? "⚔" : "✋");
        int offModeColor = (this.offMode == 1) ? 0x55FF55 : (this.offMode == 2 ? 0xFF4455 : 0x00B4D8);
        graphics.drawCenteredString(this.font, offModeIcon, cx - 25, cy - 12, offModeColor);
        graphics.drawCenteredString(this.font, getModeShortName(this.offMode), cx - 25, cy + 2, offModeColor);

        String mainModeIcon = (this.mainMode == 1) ? "🌀" : (this.mainMode == 2 ? "⚔" : "✋");
        int mainModeColor = (this.mainMode == 1) ? 0x55FF55 : (this.mainMode == 2 ? 0xFF4455 : 0x00B4D8);
        graphics.drawCenteredString(this.font, mainModeIcon, cx + 25, cy - 12, mainModeColor);
        graphics.drawCenteredString(this.font, getModeShortName(this.mainMode), cx + 25, cy + 2, mainModeColor);

        // Hover status at bottom of hub
        if (this.hoveredHand != null) {
            String targetHand = (this.hoveredHand == InteractionHand.MAIN_HAND) ? "ПРАВАЯ ►" : "◄ ЛЕВАЯ";
            graphics.drawCenteredString(this.font, targetHand, cx, cy + 20, 0x00FFFF);
        }

        // 5. DRAW LABELS ON SECTORS
        // Left Half Labels
        drawSectorLabel(graphics, cx - 68, cy - 50, "✋", "Обычный", this.offMode == 0, isOffHover && this.hoveredSector == 0, true, 0);
        drawSectorLabel(graphics, cx - 82, cy, "🌀", "Бур", this.offMode == 1, isOffHover && this.hoveredSector == 1, this.offHasDrill, 1);
        drawSectorLabel(graphics, cx - 68, cy + 50, "⚔", "Когти", this.offMode == 2, isOffHover && this.hoveredSector == 2, this.offHasClaw, 2);

        // Right Half Labels
        drawSectorLabel(graphics, cx + 68, cy - 50, "✋", "Обычный", this.mainMode == 0, isMainHover && this.hoveredSector == 0, true, 0);
        drawSectorLabel(graphics, cx + 82, cy, "🌀", "Бур", this.mainMode == 1, isMainHover && this.hoveredSector == 1, this.mainHasDrill, 1);
        drawSectorLabel(graphics, cx + 68, cy + 50, "⚔", "Когти", this.mainMode == 2, isMainHover && this.hoveredSector == 2, this.mainHasClaw, 2);
    }

    // =========================================================================
    // RENDER MODE B: SINGLE GAUNTLET (360° TRI-SECTOR WHEEL)
    // =========================================================================
    private void renderSingleWheel(GuiGraphics graphics, int cx, int cy, int dx, int dy, double dist) {
        boolean hasDrill = (this.singleHand == InteractionHand.MAIN_HAND) ? this.mainHasDrill : this.offHasDrill;
        boolean hasClaw = (this.singleHand == InteractionHand.MAIN_HAND) ? this.mainHasClaw : this.offHasClaw;
        int activeMode = (this.singleHand == InteractionHand.MAIN_HAND) ? this.mainMode : this.offMode;

        if (dist > 20) {
            double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
            if (angleDeg >= -150.0 && angleDeg < -30.0) {
                this.hoveredSector = 0; // Top: Normal
            } else if (angleDeg >= -30.0 && angleDeg < 90.0) {
                this.hoveredSector = 1; // Bottom-Right: Drill
            } else {
                this.hoveredSector = 2; // Bottom-Left: Claw
            }
        }

        float rIn = 48.0f;
        float rOut = 104.0f;
        float rHover = 114.0f;

        drawSector(graphics, cx, cy, rIn, (this.hoveredSector == 0) ? rHover : rOut, -145.0f, -35.0f, this.hoveredSector == 0, activeMode == 0, true, 0);
        drawSector(graphics, cx, cy, rIn, (this.hoveredSector == 1) ? rHover : rOut, -25.0f, 85.0f, this.hoveredSector == 1, activeMode == 1, hasDrill, 1);
        drawSector(graphics, cx, cy, rIn, (this.hoveredSector == 2) ? rHover : rOut, 95.0f, 205.0f, this.hoveredSector == 2, activeMode == 2, hasClaw, 2);

        drawFullCircle(graphics, cx, cy, rIn - 4.0f, 0xEE0B121C, 0xAA00B4D8);
        graphics.flush();

        String handTitle = (this.singleHand == InteractionHand.MAIN_HAND) ? "ПРАВАЯ РУКА" : "ЛЕВАЯ РУКА";
        graphics.drawCenteredString(this.font, handTitle, cx, cy - 22, 0x00E5FF);

        String activeText = (activeMode == 1) ? "● Бур активен" : (activeMode == 2 ? "● Когти активны" : "● Манипулятор активен");
        int activeColor = (activeMode == 1) ? 0x55FF55 : (activeMode == 2 ? 0xFF4455 : 0x00B4D8);
        graphics.drawCenteredString(this.font, activeText, cx, cy + 20, activeColor);

        drawSectorLabel(graphics, cx, cy - 76, "✋", "Обычный", activeMode == 0, this.hoveredSector == 0, true, 0);
        drawSectorLabel(graphics, cx + 66, cy + 40, "🌀", "Бур", activeMode == 1, this.hoveredSector == 1, hasDrill, 1);
        drawSectorLabel(graphics, cx - 66, cy + 40, "⚔", "Когти", activeMode == 2, this.hoveredSector == 2, hasClaw, 2);
    }

    private String getModeShortName(int mode) {
        if (mode == 1) return "БУР";
        if (mode == 2) return "КОГТИ";
        return "ОБЫЧНЫЙ";
    }

    private void drawSectorLabel(GuiGraphics graphics, int x, int y, String icon, String name, boolean active, boolean hovered, boolean installed, int modeType) {
        if (!installed) {
            graphics.drawCenteredString(this.font, "🔒", x, y - 10, 0xFF5555);
            graphics.drawCenteredString(this.font, name, x, y + 2, 0x888888);
        } else {
            int iconColor = (modeType == 1) ? 0x00FF88 : (modeType == 2 ? 0xFF3344 : 0x00FFFF);
            graphics.drawCenteredString(this.font, icon, x, y - 10, iconColor);
            graphics.drawCenteredString(this.font, name, x, y + 2, hovered ? 0xFFFFFF : 0xCCDDEE);
            if (active) {
                graphics.drawCenteredString(this.font, "●", x, y + 12, 0x55FF55);
            }
        }
    }

    private void drawSector(GuiGraphics graphics, float cx, float cy, float rIn, float rOut, float startDeg, float endDeg, boolean hovered, boolean active, boolean installed, int modeType) {
        int fill;
        int border;

        if (!installed) {
            fill = hovered ? 0xC5381414 : 0x75200C0C;
            border = hovered ? 0xFFFF4444 : 0x55551A1A;
        } else if (modeType == 1) { // Drill (Green)
            fill = hovered ? 0xD5144525 : (active ? 0xAA10351C : 0x750C2213);
            border = hovered ? 0xFF00FF88 : (active ? 0xAA2EC4B6 : 0x44255535);
        } else if (modeType == 2) { // Claw (Red)
            fill = hovered ? 0xD5551218 : (active ? 0xAA380E14 : 0x75250A0E);
            border = hovered ? 0xFFFF2244 : (active ? 0xAAFF3355 : 0x44602025);
        } else { // Normal (Cyan)
            fill = hovered ? 0xD5163650 : (active ? 0xAA122638 : 0x750D1824);
            border = hovered ? 0xFF00FFFF : (active ? 0xAA00B4D8 : 0x442A4560);
        }

        drawArcSector(graphics, cx, cy, rIn, rOut, startDeg, endDeg, fill, border);
    }

    private void drawArcSector(GuiGraphics graphics, float cx, float cy, float rIn, float rOut, float startDeg, float endDeg, int argbFill, int argbBorder) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());

        float step = 2.0f;
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

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(argbFill);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(argbFill);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(argbFill);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(argbFill);
        }

        // Outer Border
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

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(argbBorder);
        }

        // Inner Border
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

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(argbBorder);
        }
    }

    private void drawFullCircle(GuiGraphics graphics, float cx, float cy, float radius, int argbFill, int argbBorder) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());

        float step = 5.0f;
        for (float deg = 0; deg < 360.0f; deg += step) {
            float nextDeg = Math.min(deg + step, 360.0f);
            double rad1 = Math.toRadians(deg);
            double rad2 = Math.toRadians(nextDeg);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            buffer.addVertex(matrix, cx, cy, 0.0f).setColor(argbFill);
            buffer.addVertex(matrix, cx + radius * cos1, cy + radius * sin1, 0.0f).setColor(argbFill);
            buffer.addVertex(matrix, cx + radius * cos2, cy + radius * sin2, 0.0f).setColor(argbFill);
            buffer.addVertex(matrix, cx, cy, 0.0f).setColor(argbFill);
        }

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

            buffer.addVertex(matrix, x1In, y1In, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x1Out, y1Out, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x2Out, y2Out, 0.0f).setColor(argbBorder);
            buffer.addVertex(matrix, x2In, y2In, 0.0f).setColor(argbBorder);
        }
    }
}
