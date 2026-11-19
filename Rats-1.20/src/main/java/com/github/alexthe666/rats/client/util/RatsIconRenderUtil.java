package com.github.alexthe666.rats.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class RatsIconRenderUtil {

	// Draws a POI icon in the world via RenderType (1.21.1 API).
	public static void renderPOIIcon(ResourceLocation icon, Vec3 viewVec, @Nullable BlockPos renderPos, float bob, PoseStack stack) {
		if (renderPos == null || renderPos == BlockPos.ZERO) {
			return;
		}
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		RenderType renderType = RenderType.entityCutoutNoCull(icon);
		VertexConsumer consumer = bufferSource.getBuffer(renderType);

		stack.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.depthMask(false);
		stack.translate(-viewVec.x(), -viewVec.y(), -viewVec.z());
		stack.translate(renderPos.getX() + 0.5F, renderPos.getY() + bob, renderPos.getZ() + 0.5F);
		stack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
		PoseStack.Pose pose = stack.last();
		int light = LightTexture.FULL_BRIGHT;

		consumer.addVertex(pose, -0.5F, -0.5F, 0).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);
		consumer.addVertex(pose, -0.5F, 0.5F, 0).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);
		consumer.addVertex(pose, 0.5F, 0.5F, 0).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);
		consumer.addVertex(pose, 0.5F, -0.5F, 0).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);

		bufferSource.endBatch(renderType);
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		stack.popPose();
	}

	// Draws a textured AABB in the world (radius, quarry, etc.).
	public static void renderBox(ResourceLocation texture, Vec3 viewPos, Vec3 centerPos, AABB boxSize, PoseStack stack) {
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		RenderType renderType = RenderType.entityTranslucent(texture);
		VertexConsumer consumer = bufferSource.getBuffer(renderType);

		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		stack.pushPose();
		stack.translate(-viewPos.x(), -viewPos.y(), -viewPos.z());
		stack.translate(centerPos.x(), centerPos.y(), centerPos.z());
		RenderSystem.runAsFancy(() -> renderMovingAABB(boxSize, stack, consumer));
		bufferSource.endBatch(renderType);
		stack.popPose();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void renderMovingAABB(AABB boundingBox, PoseStack stack, VertexConsumer consumer) {
		float scroll = Minecraft.getInstance().isPaused() ? 0.0F : (float) (System.currentTimeMillis() % 3000L) / 3000.0F;
		PoseStack.Pose pose = stack.last();
		float minX = (float) boundingBox.minX;
		float minY = (float) boundingBox.minY;
		float minZ = (float) boundingBox.minZ;
		float maxX = (float) boundingBox.maxX;
		float maxY = (float) boundingBox.maxY;
		float maxZ = (float) boundingBox.maxZ;
		float uSpan = maxX - minX;
		float vSpan = maxY - minY;
		// Front and back faces (Z)
		putIconVertex(consumer, pose, maxX, maxY, minZ, scroll, scroll + vSpan, 0.0F, 0.0F, -1.0F);
		putIconVertex(consumer, pose, maxX, minY, minZ, scroll + uSpan, scroll + vSpan, 0.0F, 0.0F, -1.0F);
		putIconVertex(consumer, pose, minX, minY, minZ, scroll + uSpan, scroll, 0.0F, 0.0F, -1.0F);
		putIconVertex(consumer, pose, minX, maxY, minZ, scroll, scroll, 0.0F, 0.0F, -1.0F);
		putIconVertex(consumer, pose, maxX, minY, maxZ, scroll, scroll, 0.0F, 0.0F, 1.0F);
		putIconVertex(consumer, pose, minX, minY, maxZ, scroll + uSpan, scroll, 0.0F, 0.0F, 1.0F);
		putIconVertex(consumer, pose, minX, maxY, maxZ, scroll + uSpan, scroll + vSpan, 0.0F, 0.0F, 1.0F);
		putIconVertex(consumer, pose, maxX, maxY, maxZ, scroll, scroll + vSpan, 0.0F, 0.0F, 1.0F);
		// Left and right (X)
		putIconVertex(consumer, pose, minX, maxY, maxZ, scroll, scroll + vSpan, -1.0F, 0.0F, 0.0F);
		putIconVertex(consumer, pose, minX, minY, maxZ, scroll + uSpan, scroll + vSpan, -1.0F, 0.0F, 0.0F);
		putIconVertex(consumer, pose, minX, minY, minZ, scroll + uSpan, scroll, -1.0F, 0.0F, 0.0F);
		putIconVertex(consumer, pose, minX, maxY, minZ, scroll, scroll, -1.0F, 0.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, minY, minZ, scroll, scroll, 1.0F, 0.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, minY, maxZ, scroll + uSpan, scroll, 1.0F, 0.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, maxY, maxZ, scroll + uSpan, scroll + vSpan, 1.0F, 0.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, maxY, minZ, scroll, scroll + vSpan, 1.0F, 0.0F, 0.0F);
		// Top and bottom (Y)
		putIconVertex(consumer, pose, minX, maxY, minZ, scroll, scroll, 0.0F, -1.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, maxY, minZ, scroll + uSpan, scroll, 0.0F, -1.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, maxY, maxZ, scroll + uSpan, scroll + uSpan, 0.0F, -1.0F, 0.0F);
		putIconVertex(consumer, pose, minX, maxY, maxZ, scroll, scroll + uSpan, 0.0F, -1.0F, 0.0F);
		putIconVertex(consumer, pose, minX, minY, maxZ, scroll, scroll, 0.0F, 1.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, minY, maxZ, scroll + uSpan, scroll, 0.0F, 1.0F, 0.0F);
		putIconVertex(consumer, pose, maxX, minY, minZ, scroll + uSpan, scroll + uSpan, 0.0F, 1.0F, 0.0F);
		putIconVertex(consumer, pose, minX, minY, minZ, scroll, scroll + uSpan, 0.0F, 1.0F, 0.0F);
	}

	// Adds a POI icon vertex with UV and normal.
	private static void putIconVertex(
			VertexConsumer consumer,
			PoseStack.Pose pose,
			float x,
			float y,
			float z,
			float u,
			float v,
			float nx,
			float ny,
			float nz
	) {
		Vector3f normal = new Vector3f(nx, ny, nz);
		pose.normal().transform(normal);
		consumer.addVertex(pose, x, y, z).setColor(255, 255, 255, 50).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(normal.x(), normal.y(), normal.z());
	}
}
