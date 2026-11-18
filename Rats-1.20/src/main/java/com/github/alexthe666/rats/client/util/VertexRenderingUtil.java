package com.github.alexthe666.rats.client.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class VertexRenderingUtil {
    private VertexRenderingUtil() {}

    public static void addVertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, int r, int g, int b, int a, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        Vector4f vec = pose.transform(new Vector4f(x, y, z, 1.0F));
        Vector3f norm = normal != null ? normal.transform(new Vector3f(nx, ny, nz)) : new Vector3f(nx, ny, nz);
        consumer.addVertex(vec.x(), vec.y(), vec.z())
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(norm.x(), norm.y(), norm.z());
    }

    public static void addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int r, int g, int b, int a) {
        Vector4f vec = pose.transform(new Vector4f(x, y, z, 1.0F));
        consumer.addVertex(vec.x(), vec.y(), vec.z())
                .setColor(r, g, b, a)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    public static void addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float r, float g, float b, float a) {
        addVertex(consumer, pose, x, y, z, (int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F), (int)(a * 255.0F));
    }

    public static void addWhiteVertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        addVertex(consumer, pose, normal, x, y, z, 255, 255, 255, 255, u, v, overlay, light, nx, ny, nz);
    }
}
