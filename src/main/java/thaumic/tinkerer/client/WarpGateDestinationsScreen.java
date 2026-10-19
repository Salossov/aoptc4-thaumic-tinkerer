package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import thaumic.tinkerer.kami.warpgate.SkyPearlItem;
import thaumic.tinkerer.kami.warpgate.WarpGateBlockEntity;
import thaumic.tinkerer.kami.warpgate.WarpGateContent;
import thaumic.tinkerer.kami.warpgate.WarpGateTeleportPayload;

/**
 * Modern 1.21.1 port of GuiWarpGateDestinations from Thaumic Tinkerer 2.5-1.7.10-164.
 * Interactive full-screen map showing connected Warp Gate destination pearls with
 * animated End portal backdrop, pulsing alpha connecting lines, drag panning,
 * space centering, 1-0 hotkey focusing, and shift-click teleportation.
 */
public final class WarpGateDestinationsScreen extends Screen {
    private static final ResourceLocation ENDER_FIELD = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    private final WarpGateBlockEntity warpGate;
    private int x;
    private int y;
    private double panX;
    private double panY;
    private int ticks;
    private final List<Component> tooltip = new ArrayList<>();
    private int hoveredTeleportIndex = -1;

    public record DestEntry(int index, ItemStack stack, int screenX, int screenY, BlockPos pos) {}

    public WarpGateDestinationsScreen(WarpGateBlockEntity warpGate) {
        super(Component.translatable("ttmisc.destinations"));
        this.warpGate = warpGate;
    }

    @Override
    protected void init() {
        super.init();
        if (this.warpGate != null) {
            BlockPos pos = this.warpGate.getBlockPos();
            this.x = pos.getX() - this.width / 2;
            this.y = pos.getZ() - this.height / 2;
            this.panX = this.x;
            this.panY = this.y;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            this.panX -= dragX;
            this.panY -= dragY;
            this.x = (int) Math.round(this.panX);
            this.y = (int) Math.round(this.panY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (this.warpGate != null) {
                BlockPos pos = this.warpGate.getBlockPos();
                this.x = pos.getX() - this.width / 2;
                this.y = pos.getZ() - this.height / 2;
                this.panX = this.x;
                this.panY = this.y;
            }
            return true;
        }

        int slot = -1;
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            slot = keyCode - GLFW.GLFW_KEY_1;
        } else if (keyCode == GLFW.GLFW_KEY_0) {
            slot = 9;
        } else if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            slot = keyCode - GLFW.GLFW_KEY_KP_1;
        } else if (keyCode == GLFW.GLFW_KEY_KP_0) {
            slot = 9;
        }

        if (slot >= 0) {
            jumpToSlot(slot);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void jumpToSlot(int slot) {
        if (this.warpGate == null || this.warpGate.getLevel() == null) return;
        if (slot >= 0 && slot < this.warpGate.getContainerSize()) {
            ItemStack stack = this.warpGate.getItem(slot);
            if (!stack.isEmpty() && SkyPearlItem.isAttuned(stack)) {
                ResourceKey<Level> targetDim = SkyPearlItem.getTargetDim(stack);
                if (targetDim != null && targetDim.equals(this.warpGate.getLevel().dimension())) {
                    BlockPos targetPos = SkyPearlItem.getTargetPos(stack);
                    if (targetPos != null) {
                        this.x = targetPos.getX() - this.width / 2;
                        this.y = targetPos.getZ() - this.height / 2;
                        this.panX = this.x;
                        this.panY = this.y;
                    }
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackgroundField(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        this.tooltip.clear();
        this.hoveredTeleportIndex = -1;

        if (this.warpGate == null || this.warpGate.getLevel() == null) return;

        BlockPos origin = this.warpGate.getBlockPos();
        int gateX = origin.getX() - this.x;
        int gateY = origin.getZ() - this.y;

        // Collect attuned destinations in current dimension
        ResourceKey<Level> currentDim = this.warpGate.getLevel().dimension();
        List<DestEntry> destinations = new ArrayList<>();
        for (int i = 0; i < this.warpGate.getContainerSize(); i++) {
            ItemStack stack = this.warpGate.getItem(i);
            if (!stack.isEmpty() && SkyPearlItem.isAttuned(stack)) {
                ResourceKey<Level> targetDim = SkyPearlItem.getTargetDim(stack);
                if (currentDim.equals(targetDim)) {
                    BlockPos targetPos = SkyPearlItem.getTargetPos(stack);
                    if (targetPos != null) {
                        destinations.add(new DestEntry(i, stack, targetPos.getX() - this.x, targetPos.getZ() - this.y, targetPos));
                    }
                }
            }
        }

        // Draw connecting lines with pulsing alpha and Cohen-Sutherland clipping
        int lineColor = WarpGateVisualHelper.lineColor(this.ticks);
        for (DestEntry dest : destinations) {
            renderConnectingLine(graphics, gateX, gateY, dest.screenX(), dest.screenY(), lineColor);
        }

        // Destination title (underlined, with shadow at (3, 40))
        graphics.drawString(this.font, Component.translatable("ttmisc.destinations").withStyle(ChatFormatting.UNDERLINE),
                3, 40, 0xFFFFFF, true);

        // Draw entrance point pearl (at gateX, gateY)
        drawPearl(graphics, -1, ItemStack.EMPTY, gateX, gateY, mouseX, mouseY, origin.getX(), origin.getZ());

        // Draw destination pearls and left-side slot labels
        for (DestEntry dest : destinations) {
            String destName = dest.stack().has(DataComponents.CUSTOM_NAME)
                    ? dest.stack().getHoverName().getString()
                    : Component.translatable("ttmisc.destination").getString();

            // Left list: 54 + index * 11, without shadow (matching 1.7.10 drawString)
            graphics.drawString(this.font, (dest.index() + 1) + ": " + destName, 5, 54 + dest.index() * 11, 0xFFFFFF, false);

            drawPearl(graphics, dest.index(), dest.stack(), dest.screenX(), dest.screenY(), mouseX, mouseY, dest.pos().getX(), dest.pos().getZ());
        }

        // Top centered labels
        graphics.drawCenteredString(this.font, Component.translatable("ttmisc.numberKeys"), this.width / 2, 5, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("ttmisc.spaceToReset"), this.width / 2, 16, 0xFFFFFF);

        // Tooltip rendering
        if (!this.tooltip.isEmpty()) {
            graphics.renderComponentTooltip(this.font, this.tooltip, mouseX, mouseY);
        }
        if (this.hoveredTeleportIndex >= 0 && hasShiftDown() && this.minecraft != null
                && this.minecraft.screen == this && this.minecraft.mouseHandler.isLeftPressed()) {
            PacketDistributor.sendToServer(new WarpGateTeleportPayload(this.warpGate.getBlockPos(), this.hoveredTeleportIndex));
            this.onClose();
        }
    }

    private void renderBackgroundField(GuiGraphics graphics) {
        int rgb = WarpGateVisualHelper.backgroundRgb(this.ticks);
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;

        graphics.setColor(r, g, b, 1.0F);
        graphics.blit(ENDER_FIELD, 0, 0, 0, 0, this.width, this.height, 256, 256);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawPearl(GuiGraphics graphics, int index, ItemStack stack,
                           int screenX, int screenY, int mouseX, int mouseY, int worldX, int worldZ) {
        // Source renderIcon always draws the plain sprite, never the attuned stack's foil.
        ItemStack pearlStack = WarpGateContent.SKY_PEARL.get().getDefaultInstance();

        // In 1.7.10: glTranslatef(xp, yp, 0); glScalef(0.5F, 0.5F, 1.0F); renderIcon(-8, -8, ..., 16, 16)
        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0.0F);
        graphics.pose().scale(WarpGateVisualHelper.PEARL_RENDER_SCALE, WarpGateVisualHelper.PEARL_RENDER_SCALE, 1.0F);
        graphics.renderFakeItem(pearlStack, -8, -8);
        graphics.pose().popPose();

        // Hover detection: mx >= xp - 4 && mx <= xp + 4 && my >= yp - 4 && my < yp + 4
        if (WarpGateVisualHelper.isPearlHovered(mouseX, mouseY, screenX, screenY)) {
            if (index == -1) {
                String destName = Component.translatable("ttmisc.entrancePoint").getString();
                String destInd = " " + Component.translatable("ttmisc.destinationInd", 1).getString();
                this.tooltip.add(Component.literal(destName).withStyle(ChatFormatting.AQUA).append(Component.literal(destInd).withStyle(ChatFormatting.ITALIC)));
                this.tooltip.add(Component.literal("X: " + worldX));
                this.tooltip.add(Component.literal("Z: " + worldZ));
            } else {
                if (this.hoveredTeleportIndex < 0) {
                    this.hoveredTeleportIndex = index;
                }
                String destName = stack.has(DataComponents.CUSTOM_NAME)
                        ? stack.getHoverName().getString()
                        : Component.translatable("ttmisc.destination").getString();
                String destInd = " " + Component.translatable("ttmisc.destinationInd", index + 1).getString();
                this.tooltip.add(Component.literal(destName).withStyle(ChatFormatting.AQUA).append(Component.literal(destInd).withStyle(ChatFormatting.ITALIC)));

                if (this.warpGate.getLevel() != null) {
                    BlockPos origin = this.warpGate.getBlockPos();
                    SkyPearlItem.addInfo(stack, this.warpGate.getLevel().dimension(),
                            new Vec3(origin.getX(), origin.getY(), origin.getZ()), this.tooltip, true);
                }
                this.tooltip.add(Component.translatable("ttmisc.clickToTeleport"));
            }
        }
    }

    private void renderConnectingLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        if (x0 == x1 && y0 == y1) return; // Coincident points: no line needed

        // Clip line to slightly expanded screen rectangle to safely handle huge coordinates without stalls
        double margin = 50.0;
        WarpGateVisualHelper.ClippedLine clipped = WarpGateVisualHelper.clipLine(
                x0, y0, x1, y1, -margin, -margin, this.width + margin, this.height + margin
        );
        if (!clipped.visible()) return;

        double cx0 = clipped.x0();
        double cy0 = clipped.y0();
        double cx1 = clipped.x1();
        double cy1 = clipped.y1();

        double dx = cx1 - cx0;
        double dy = cy1 - cy0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4) return;

        // Source glLineWidth(2) is two framebuffer pixels, not two scaled GUI units.
        float nx = (float) (-dy / len);
        float ny = (float) (dx / len);
        float w = WarpGateVisualHelper.lineHalfWidth(this.minecraft.getWindow().getGuiScale());

        Matrix4f mat = graphics.pose().last().pose();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        consumer.addVertex(mat, (float) (cx0 + nx * w), (float) (cy0 + ny * w), 0.0F).setColor(color);
        consumer.addVertex(mat, (float) (cx1 + nx * w), (float) (cy1 + ny * w), 0.0F).setColor(color);
        consumer.addVertex(mat, (float) (cx1 - nx * w), (float) (cy1 - ny * w), 0.0F).setColor(color);
        consumer.addVertex(mat, (float) (cx0 - nx * w), (float) (cy0 - ny * w), 0.0F).setColor(color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
