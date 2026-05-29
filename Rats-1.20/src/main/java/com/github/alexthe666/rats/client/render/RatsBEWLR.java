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
	 * Рендер блока-предмета в инвентаре/руке; для GUI масштабирует модель под ячейку 16×16.
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
			RatsMod.LOGGER.error("Ошибка BEWLR для предмета {}: {}", stack, e.getMessage(), e);
		}
	}

	// Рендерит 3D-модель иглу в инвентаре; принимает предмет, контекст, матрицы, буфер, свет и overlay, ничего не возвращает.
	private void renderIglooItem(RatIglooItem item, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		try {
			ms.pushPose();
			this.prepareDecorationPose(context, ms, 0.78F);
			VertexConsumer consumer = buffer.getBuffer(TEXTURE_RAT_IGLOO);
			this.igloo.renderToBuffer(ms, consumer, light, overlay, this.packColor(RatColorUtil.getDyeRgb(item.color), 1.0F));
			ms.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка рендера иглу в инвентаре: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Рендерит 3D-модель гамака в инвентаре; принимает предмет, контекст, матрицы, буфер, свет и overlay, ничего не возвращает.
	private void renderHammockItem(RatHammockItem item, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		try {
			ms.pushPose();
			this.prepareDecorationPose(context, ms, 0.82F);
			VertexConsumer consumer = buffer.getBuffer(TEXTURE_RAT_HAMMOCK);
			this.hammock.renderToBuffer(ms, consumer, light, overlay, this.packColor(RatColorUtil.getDyeRgb(item.color), 1.0F));
			ms.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка рендера гамака в инвентаре: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Рендерит 3D-модель трубы в инвентаре; принимает контекст, матрицы, буфер, свет и overlay, ничего не возвращает.
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
			RatsMod.LOGGER.error("Ошибка рендера трубы в инвентаре: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Готовит позу для 3D-декора клетки; принимает контекст, матрицы и масштаб, ничего не возвращает.
	private void prepareDecorationPose(ItemDisplayContext context, PoseStack ms, float scale) {
		try {
			ms.translate(0.5F, 1.5F, 0.5F);
			ms.mulPose(Axis.ZP.rotationDegrees(180));
			if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
				ms.scale(scale, scale, scale);
				ms.mulPose(Axis.YP.rotationDegrees(35));
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка подготовки позы декора в инвентаре: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Упаковывает цвет модели; принимает RGB и прозрачность, отдаёт ARGB int.
	private int packColor(float[] rgb, float alpha) {
		try {
			int r = (int) (rgb[0] * 255.0F);
			int g = (int) (rgb[1] * 255.0F);
			int b = (int) (rgb[2] * 255.0F);
			int a = (int) (alpha * 255.0F);
			return (a << 24) | (r << 16) | (g << 8) | b;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка упаковки цвета декора: {}", exception.getMessage(), exception);
			throw exception;
		}
	}
}
