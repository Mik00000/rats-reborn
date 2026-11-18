package com.github.alexthe666.rats.client.render.block;

import com.github.alexthe666.rats.client.render.RatsRenderType;
import com.github.alexthe666.rats.client.util.VertexRenderingUtil;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.server.block.entity.RatlantisTokenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.joml.Matrix4f;

public class RatlantisTokenRenderer implements BlockEntityRenderer<RatlantisTokenBlockEntity> {
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);
    private final ItemRenderer itemRenderer;

    public RatlantisTokenRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    public void render(RatlantisTokenBlockEntity entity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        stack.pushPose();
        stack.translate(0.5f, 1.5f, 0.5f);
        float f = entity.ratRotationPrev + (entity.ratRotation - entity.ratRotationPrev) * partialTicks;
        stack.mulPose(Axis.YP.rotationDegrees(f * 57.295776f));
        stack.pushPose();
        stack.scale(1.5f, 1.5f, 1.5f);
        Matrix4f matrix = stack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RatsRenderType.getRatlantisToken());
        RatlantisTokenRenderer.vertex01(consumer, matrix, 0);
        RatlantisTokenRenderer.vertex2(consumer, matrix, 0.0f, 0.0f);
        RatlantisTokenRenderer.vertex3(consumer, matrix, 0.0f, 0.0f);
        RatlantisTokenRenderer.vertex4(consumer, matrix, 0.0f, 0.0f);
        stack.popPose();
        float blocksScale = Math.min(1.0f, (float)entity.tickCount / 592.0f);
        if (blocksScale > 0.0f) {
            stack.pushPose();
            stack.scale(blocksScale, blocksScale, blocksScale);
            stack.translate(0.0, 0.5 - (double)(1.25f * blocksScale * 0.5f), 0.0);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack((ItemLike)RatsBlockRegistry.MARBLED_CHEESE_RAW.get()), ItemDisplayContext.FIXED, light, overlay, stack, buffer, null, 0);
            stack.popPose();
        }
        stack.popPose();
    }

    private static void vertex01(VertexConsumer consumer, Matrix4f matrix, int alpha) {
        VertexRenderingUtil.addVertex(consumer, matrix, 0.0f, 0.0f, 0.0f, 255, 255, 255, alpha);
        VertexRenderingUtil.addVertex(consumer, matrix, 0.0f, 0.0f, 0.0f, 255, 255, 255, alpha);
    }

    private static void vertex2(VertexConsumer consumer, Matrix4f matrix, float y, float x) {
        VertexRenderingUtil.addVertex(consumer, matrix, -HALF_SQRT_3 * x, y, -0.5f * x, 255, 169, 33, 0);
    }

    private static void vertex3(VertexConsumer consumer, Matrix4f matrix, float y, float x) {
        VertexRenderingUtil.addVertex(consumer, matrix, HALF_SQRT_3 * x, y, -0.5f * x, 255, 169, 33, 0);
    }

    private static void vertex4(VertexConsumer consumer, Matrix4f matrix, float y, float x) {
        VertexRenderingUtil.addVertex(consumer, matrix, 0.0f, y, x, 255, 169, 33, 0);
    }
}
