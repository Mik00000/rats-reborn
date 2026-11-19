package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.util.VertexRenderingUtil;
import com.github.alexthe666.rats.server.inventory.AutoCurdlerMenu;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.List;

public class AutoCurdlerScreen extends AbstractContainerScreen<AutoCurdlerMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/gui/container/auto_curdler.png");
	private final AutoCurdlerMenu curdler;

	public AutoCurdlerScreen(AutoCurdlerMenu container, Inventory inv, Component name) {
		super(container, inv, name);
		this.curdler = container;
	}

	/**
	 * Draws a filled fluid area in the GUI (sprite from the block atlas).
	 * Parameters: pose stack, top-left of the fluid level (x, y), width and height in pixels, fluid type.
	 * In 1.21.1 vertices require normals, otherwise it crashes with "Missing elements in vertex: Normal".
	 */
	public static void renderFluidStack(PoseStack stack, int xPosition, int yPosition, int desiredWidth, int desiredHeight, Fluid fluid) {
		try {
			TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
					.getSprite(IClientFluidTypeExtensions.of(fluid).getStillTexture());
			MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
			RenderType renderType = RenderType.entitySmoothCutout(InventoryMenu.BLOCK_ATLAS);
			VertexConsumer consumer = bufferSource.getBuffer(renderType);
			int xTileCount = desiredWidth / 16;
			int xRemainder = desiredWidth - (xTileCount * 16);
			int yTileCount = desiredHeight / 16;
			int yRemainder = desiredHeight - (yTileCount * 16);
			float uMin = sprite.getU0();
			float uMax = sprite.getU1();
			float vMin = sprite.getV0();
			float vMax = sprite.getV1();
			float uDif = uMax - uMin;
			float vDif = vMax - vMin;
			RenderSystem.enableBlend();
			Matrix4f matrix4f = stack.last().pose();
			Matrix3f matrix3f = stack.last().normal();
			int light = LightTexture.FULL_BRIGHT;
			int overlay = OverlayTexture.NO_OVERLAY;
			for (int xTile = 0; xTile <= xTileCount; xTile++) {
				int width = (xTile == xTileCount) ? xRemainder : 16;
				if (width == 0) {
					break;
				}
				int x = xPosition + (xTile * 16);
				int maskRight = 16 - width;
				int shiftedX = x + 16 - maskRight;
				float uLocalDif = uDif * maskRight / 16;

				for (int yTile = 0; yTile <= yTileCount; yTile++) {
					int height = (yTile == yTileCount) ? yRemainder : 16;
					if (height == 0) {
						break;
					}
					int y = yPosition - ((yTile + 1) * 16);
					int maskTop = 16 - height;
					float vLocalDif = vDif * maskTop / 16;

					VertexRenderingUtil.addVertex(consumer, matrix4f, matrix3f, x, y + 16, 0, 255, 255, 255, 255, uMin + uLocalDif, vMax, overlay, light, 0.0F, 0.0F, 1.0F);
					VertexRenderingUtil.addVertex(consumer, matrix4f, matrix3f, shiftedX, y + 16, 0, 255, 255, 255, 255, uMax, vMax, overlay, light, 0.0F, 0.0F, 1.0F);
					VertexRenderingUtil.addVertex(consumer, matrix4f, matrix3f, shiftedX, y + maskTop, 0, 255, 255, 255, 255, uMax, vMin + vLocalDif, overlay, light, 0.0F, 0.0F, 1.0F);
					VertexRenderingUtil.addVertex(consumer, matrix4f, matrix3f, x, y + maskTop, 0, 255, 255, 255, 255, uMin + uLocalDif, vMin + vLocalDif, overlay, light, 0.0F, 0.0F, 1.0F);
				}
			}
			bufferSource.endBatch(renderType);
			RenderSystem.disableBlend();
		} catch (Exception e) {
			RatsMod.LOGGER.error("Failed to render fluid in the auto curdler GUI: {}", e.getMessage(), e);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(graphics, mouseX, mouseY, partialTicks);
		super.render(graphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
		try {
			int i = (this.width - this.imageWidth) / 2;
			int j = (this.height - this.imageHeight) / 2;

			graphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
			int l = this.curdler.getCookProgressionScaled();
			graphics.blit(TEXTURE, i + 63, j + 35, 176, 0, l + 1, 16);
			int tankWidth = 24;
			int tankHeight = 63;
			int capacity = this.curdler.getTankCapacity();
			if (capacity > 0 && this.curdler.getFluidAmount() > 0) {
				int amount = Math.round((this.curdler.getFluidAmount() / (float) capacity) * (tankHeight - 4));
				if (amount > 0) {
					renderFluidStack(graphics.pose(), i + 29, j + 73, tankWidth, amount, NeoForgeMod.MILK.get());
				}
			}
			graphics.blit(TEXTURE, i + 29, j + 12, 0, 166, tankWidth, tankHeight);
		} catch (Exception e) {
			RatsMod.LOGGER.error("Failed to render the auto curdler GUI background: {}", e.getMessage(), e);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		int screenW = (this.width - this.imageWidth) / 2;
		int screenH = (this.height - this.imageHeight) / 2;

		if (this.isHovering(29, 15, 24, 58, mouseX, mouseY)) {
			String fluidName = new FluidStack(NeoForgeMod.MILK.get(), this.curdler.getTankCapacity()).getHoverName().getString();
			String fluidSize = this.curdler.getFluidAmount() + " " + Component.translatable(RatsLangConstants.CURDLER_MB).getString();
			List<Component> list = Arrays.asList(Component.literal(fluidName).withStyle(ChatFormatting.BLUE), Component.literal(fluidSize).withStyle(ChatFormatting.GRAY));
			graphics.renderTooltip(this.font, Lists.transform(list, Component::getVisualOrderText), mouseX - screenW, mouseY - screenH);
		}
	}
}
