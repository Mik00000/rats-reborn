package com.github.alexthe666.rats.client.render;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.RatsModelLayers;
import com.github.alexthe666.rats.client.model.deco.RatHammockModel;
import com.github.alexthe666.rats.client.model.deco.RatIglooModel;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.server.block.entity.AutoCurdlerBlockEntity;
import com.github.alexthe666.rats.server.block.entity.RatHoleBlockEntity;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.items.RatHammockItem;
import com.github.alexthe666.rats.server.items.RatIglooItem;
import com.github.alexthe666.rats.server.items.RatTubeItem;
import com.github.alexthe666.rats.server.items.RatsBlockItem;
import com.github.alexthe666.rats.server.misc.RatColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RatsBEWLR extends BlockEntityWithoutLevelRenderer {
	private static final RenderType TEXTURE_RAT_IGLOO = RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/block/rat_igloo.png"));
	private static final RenderType TEXTURE_RAT_HAMMOCK = RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/block/rat_hammock_0.png"));
	private final RatIglooModel<?> igloo;
	private final RatHammockModel<?> hammock;

	public RatsBEWLR() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		this.igloo = new RatIglooModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(RatsModelLayers.IGLOO));
		this.hammock = new RatHammockModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(RatsModelLayers.HAMMOCK));
	}

	/**
	 * Renders a block item in inventory/hand; for the GUI it scales the model to a 16x16 slot.
	 */
	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		try {
			if (stack.getItem() instanceof RatIglooItem iglooItem) {
				this.renderIglooItem(iglooItem, context, ms, buffer, light, overlay);
				return;
			}
			if (stack.getItem() instanceof RatHammockItem hammockItem) {
				this.renderHammockItem(hammockItem, context, ms, buffer, light, overlay);
				return;
			}
			if (stack.getItem() instanceof RatTubeItem) {
				this.renderTubeItem(context, ms, buffer, light, overlay);
				return;
			}
			if (!(stack.getItem() instanceof RatsBlockItem bi)) {
				return;
			}
			Block block = bi.getBlock();
			if (!(block instanceof EntityBlock be)) {
				return;
			}
			BlockEntity entity = be.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
			if (entity == null || entity instanceof RatHoleBlockEntity) {
				return;
			}
			ms.pushPose();
			if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
				float scale = entity instanceof AutoCurdlerBlockEntity ? 0.5F : 0.65F;
				ms.translate(0.5F, 0.5F, 0.5F);
				ms.scale(scale, scale, scale);
				ms.translate(-0.5F, -0.5F, -0.5F);
				if (entity instanceof AutoCurdlerBlockEntity) {
					ms.mulPose(Axis.YP.rotationDegrees(45));
				}
			}
			Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(entity, ms, buffer, light, overlay);
			ms.popPose();
		} catch (Exception e) {
			RatsMod.LOGGER.error("BEWLR error for item {}: {}", stack, e.getMessage(), e);
		}
	}

	// Renders the 3D igloo model in the inventory; takes the item, context, matrices, buffer, light and overlay, returns nothing.
	private void renderIglooItem(RatIglooItem item, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		try {
			ms.pushPose();
			this.prepareDecorationPose(context, ms, 0.78F);
			VertexConsumer consumer = buffer.getBuffer(TEXTURE_RAT_IGLOO);
			this.igloo.renderToBuffer(ms, consumer, light, overlay, this.packColor(RatColorUtil.getDyeRgb(item.color), 1.0F));
			ms.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error rendering the igloo in the inventory: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Renders the 3D hammock model in the inventory; takes the item, context, matrices, buffer, light and overlay, returns nothing.
	private void renderHammockItem(RatHammockItem item, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		try {
			ms.pushPose();
			this.prepareDecorationPose(context, ms, 0.82F);
			VertexConsumer consumer = buffer.getBuffer(TEXTURE_RAT_HAMMOCK);
			this.hammock.renderToBuffer(ms, consumer, light, overlay, this.packColor(RatColorUtil.getDyeRgb(item.color), 1.0F));
			ms.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error rendering the hammock in the inventory: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Renders the 3D tube model in the inventory; takes the context, matrices, buffer, light and overlay, returns nothing.
	private void renderTubeItem(ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		try {
			ms.pushPose();
			if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
				ms.translate(0.5F, 0.5F, 0.5F);
				ms.scale(0.78F, 0.78F, 0.78F);
				ms.mulPose(Axis.YP.rotationDegrees(35));
				ms.translate(-0.5F, -0.5F, -0.5F);
			}
			BlockState state = RatsBlockRegistry.RAT_TUBE_COLOR.get().defaultBlockState()
					.setValue(RatTubeBlock.NORTH, true)
					.setValue(RatTubeBlock.SOUTH, true);
			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ms, buffer, light, overlay);
			ms.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error rendering the tube in the inventory: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Prepares the pose for 3D cage decorations; takes the context, matrices and scale, returns nothing.
	private void prepareDecorationPose(ItemDisplayContext context, PoseStack ms, float scale) {
		try {
			ms.translate(0.5F, 1.5F, 0.5F);
			ms.mulPose(Axis.ZP.rotationDegrees(180));
			if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
				ms.scale(scale, scale, scale);
				ms.mulPose(Axis.YP.rotationDegrees(35));
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error preparing the decoration pose in the inventory: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Packs a model color; takes RGB and alpha, returns an ARGB int.
	private int packColor(float[] rgb, float alpha) {
		try {
			int r = (int) (rgb[0] * 255.0F);
			int g = (int) (rgb[1] * 255.0F);
			int b = (int) (rgb[2] * 255.0F);
			int a = (int) (alpha * 255.0F);
			return (a << 24) | (r << 16) | (g << 8) | b;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error packing the decoration color: {}", exception.getMessage(), exception);
			throw exception;
		}
	}
}
