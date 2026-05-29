package com.github.alexthe666.rats.client.render.entity;

import com.github.alexthe666.rats.client.util.VertexRenderingUtil;
import com.github.alexthe666.rats.server.entity.projectile.SmallArrow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class SmallArrowRenderer extends ArrowRenderer<SmallArrow> {
	public static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

	public SmallArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(SmallArrow entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource buffer, int light) {
		stack.pushPose();
		stack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
		stack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
		float scale = 0.5F;
		float f9 = (float) entity.shakeTime - partialTicks;
		if (f9 > 0.0F) {
			float f10 = -Mth.sin(f9 * 3.0F) * f9;
			stack.mulPose(Axis.ZP.rotationDegrees(f10));
		}

		stack.mulPose(Axis.XP.rotationDegrees(45.0F));
		stack.scale(scale * 0.05625F, scale * 0.05625F, scale * 0.05625F);
		stack.translate(-4.0D, 0.0D, 0.0D);
		VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityCutout(ARROW));
		PoseStack.Pose matrixstack$entry = stack.last();
		Matrix4f matrix4f = matrixstack$entry.pose();
		Matrix3f matrix3f = matrixstack$entry.normal();
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, light);
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, light);
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, light);
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, light);
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, light);
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, light);
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, light);
		this.vertex(matrix4f, matrix3f, vertexBuilder, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, light);

		for (int j = 0; j < 4; ++j) {
			stack.mulPose(Axis.XP.rotationDegrees(90.0F));
			this.vertex(matrix4f, matrix3f, vertexBuilder, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, light);
			this.vertex(matrix4f, matrix3f, vertexBuilder, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, light);
			this.vertex(matrix4f, matrix3f, vertexBuilder, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, light);
			this.vertex(matrix4f, matrix3f, vertexBuilder, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, light);
		}

		stack.popPose();
	}

	public void vertex(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer consumer, int x, int y, int z, float u, float v, int xNorm, int zNorm, int yNorm, int light) {
		VertexRenderingUtil.addVertex(consumer, matrix4f, matrix3f, (float) x, (float) y, (float) z, 255, 255, 255, 255, u, v, OverlayTexture.NO_OVERLAY, light, (float) xNorm, (float) yNorm, (float) zNorm);
	}

	@Override
	public ResourceLocation getTextureLocation(SmallArrow entity) {
		return ARROW;
	}
}
