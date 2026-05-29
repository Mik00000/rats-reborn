package com.github.alexthe666.rats.client.render.block;

import com.github.alexthe666.rats.client.util.VertexRenderingUtil;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.block.AutoCurdlerModel;
import com.github.alexthe666.rats.server.block.AutoCurdlerBlock;
import com.github.alexthe666.rats.server.block.entity.AutoCurdlerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class AutoCurdlerRenderer implements BlockEntityRenderer<AutoCurdlerBlockEntity> {
	private static final AutoCurdlerModel<?> MODEL_AUTO_CURDLER = new AutoCurdlerModel<>();
	private static final RenderType TEXTURE = RenderType.entityCutout(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/block/auto_curdler.png"));
	private static final RenderType TEXTURE_BLOCKS = RenderType.entitySmoothCutout(InventoryMenu.BLOCK_ATLAS);

	public AutoCurdlerRenderer(BlockEntityRendererProvider.Context context) {

	}

	public static void renderMilk(MultiBufferSource buffer, PoseStack stack, FluidStack fluidStack, int combinedLight, int overlay) {
		float textureYPos = (0.6F * (fluidStack.getAmount() / 5000F));
		stack.pushPose();
		stack.mulPose(Axis.XP.rotationDegrees(180));
		stack.translate(-0.5F, -1.6F, -0.5F);
		AABB boundingBox = new AABB(0.25F, 0.35F, 0.25F, 0.75F, 0.6F + textureYPos, 0.75F);
		TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(IClientFluidTypeExtensions.of(fluidStack.getFluid()).getStillTexture());
		VertexConsumer vertexbuffer = buffer.getBuffer(TEXTURE_BLOCKS);
		Matrix4f matrix4f = stack.last().pose();
		org.joml.Matrix3f matrix3f = stack.last().normal();
		double avgY = boundingBox.maxY - boundingBox.minY;
		double avgX = Math.abs(boundingBox.maxX - boundingBox.minX);
		double avgZ = Math.abs(boundingBox.maxZ - boundingBox.minZ);
		float f1 = sprite.getU0();
		float f2_alt_x = (float) Math.min(sprite.getU1(), f1 + avgX * Math.abs(sprite.getU1() - sprite.getU0()));
		float f2_alt_z = (float) Math.min(sprite.getU1(), f1 + avgZ * Math.abs(sprite.getU1() - sprite.getU0()));
		float f3 = sprite.getV0();
		float f4_alt = (float) Math.min(sprite.getV1(), f3 + avgY * Math.abs(sprite.getV1() - sprite.getV0()));
		float f4_alt_z = (float) Math.min(sprite.getV1(), f3 + avgZ * Math.abs(sprite.getV1() - sprite.getV0()));
		//back
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.maxY, (float) boundingBox.minZ, 255, 255, 255, 255, f2_alt_x, f3, overlay, combinedLight, 0.0F, 0.0F, -1.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.maxY, (float) boundingBox.minZ, 255, 255, 255, 255, f1, f3, overlay, combinedLight, 0.0F, 0.0F, -1.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.minZ, 255, 255, 255, 255, f1, f4_alt, overlay, combinedLight, 0.0F, 0.0F, -1.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.minZ, 255, 255, 255, 255, f2_alt_x, f4_alt, overlay, combinedLight, 0.0F, 0.0F, -1.0F);
		//front
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.maxZ, 255, 255, 255, 255, f1, f4_alt, overlay, combinedLight, 0.0F, 0.0F, 1.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxZ, 255, 255, 255, 255, f2_alt_x, f4_alt, overlay, combinedLight, 0.0F, 0.0F, 1.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.maxY, (float) boundingBox.maxZ, 255, 255, 255, 255, f2_alt_x, f3, overlay, combinedLight, 0.0F, 0.0F, 1.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.maxY, (float) boundingBox.maxZ, 255, 255, 255, 255, f1, f3, overlay, combinedLight, 0.0F, 0.0F, 1.0F);
		//tops
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.minZ, 255, 255, 255, 255, f1, f4_alt_z, overlay, combinedLight, 0.0F, -1.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.minZ, 255, 255, 255, 255, f2_alt_x, f4_alt_z, overlay, combinedLight, 0.0F, -1.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxZ, 255, 255, 255, 255, f2_alt_x, f3, overlay, combinedLight, 0.0F, -1.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.maxZ, 255, 255, 255, 255, f1, f3, overlay, combinedLight, 0.0F, -1.0F, 0.0F);

		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.maxY, (float) boundingBox.maxZ, 255, 255, 255, 255, f1, f4_alt_z, overlay, combinedLight, 0.0F, 1.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.maxY, (float) boundingBox.maxZ, 255, 255, 255, 255, f2_alt_x, f4_alt_z, overlay, combinedLight, 0.0F, 1.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.maxY, (float) boundingBox.minZ, 255, 255, 255, 255, f2_alt_x, f3, overlay, combinedLight, 0.0F, 1.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.maxY, (float) boundingBox.minZ, 255, 255, 255, 255, f1, f3, overlay, combinedLight, 0.0F, 1.0F, 0.0F);
		//sides
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.maxZ, 255, 255, 255, 255, f2_alt_z, f4_alt, overlay, combinedLight, -1.0F, 0.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.maxY, (float) boundingBox.maxZ, 255, 255, 255, 255, f2_alt_z, f3, overlay, combinedLight, -1.0F, 0.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.maxY, (float) boundingBox.minZ, 255, 255, 255, 255, f1, f3, overlay, combinedLight, -1.0F, 0.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.minZ, 255, 255, 255, 255, f1, f4_alt, overlay, combinedLight, -1.0F, 0.0F, 0.0F);

		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.minZ, 255, 255, 255, 255, f2_alt_z, f4_alt, overlay, combinedLight, 1.0F, 0.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.maxY, (float) boundingBox.minZ, 255, 255, 255, 255, f2_alt_z, f3, overlay, combinedLight, 1.0F, 0.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.maxY, (float) boundingBox.maxZ, 255, 255, 255, 255, f1, f3, overlay, combinedLight, 1.0F, 0.0F, 0.0F);
		VertexRenderingUtil.addVertex(vertexbuffer, matrix4f, matrix3f, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxZ, 255, 255, 255, 255, f1, f4_alt, overlay, combinedLight, 1.0F, 0.0F, 0.0F);
		stack.popPose();
	}

	@Override
	public void render(AutoCurdlerBlockEntity entity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
		float f = 0;
		if (entity.getLevel() != null && entity.getLevel().getBlockState(entity.getBlockPos()).getBlock() instanceof AutoCurdlerBlock) {
			f = entity.getLevel().getBlockState(entity.getBlockPos()).getValue(AutoCurdlerBlock.FACING).getClockWise().toYRot() + 90;
		}
		stack.pushPose();
		stack.translate(0.5D, 1.5D, 0.5D);
		stack.mulPose(Axis.YP.rotationDegrees(-f));
		stack.mulPose(Axis.ZP.rotationDegrees(180));
		stack.pushPose();
		if (!entity.getTank().getFluid().isEmpty()) {
			renderMilk(buffer, stack, entity.getTank().getFluid(), light, overlay);
		}
		VertexConsumer consumer = buffer.getBuffer(TEXTURE);
		MODEL_AUTO_CURDLER.renderToBuffer(stack, consumer, light, overlay, -1);
		stack.popPose();
		stack.popPose();
	}
}
