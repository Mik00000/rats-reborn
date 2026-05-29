package com.github.alexthe666.rats.client.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Вспомогательные методы построения вершин для VertexConsumer 1.21.1.
 */
public final class VertexRenderingUtil {

	private VertexRenderingUtil() {
	}

	// Добавляет вершину с трансформацией позиции и нормали; принимает матрицы, координаты, цвет, UV, overlay, light, нормаль.
	public static void addVertex(
			VertexConsumer consumer,
			Matrix4f pose,
			Matrix3f normalMatrix,
			float x,
			float y,
			float z,
			int red,
			int green,
			int blue,
			int alpha,
			float u,
			float v,
			int overlay,
			int light,
			float normalX,
			float normalY,
			float normalZ
	) {
		Vector4f position = pose.transform(new Vector4f(x, y, z, 1.0F));
		Vector3f normal = normalMatrix.transform(new Vector3f(normalX, normalY, normalZ));
		consumer.addVertex(position.x(), position.y(), position.z())
				.setColor(red, green, blue, alpha)
				.setUv(u, v)
				.setOverlay(overlay)
				.setLight(light)
				.setNormal(normal.x(), normal.y(), normal.z());
	}

	// Добавляет белую вершину с полной непрозрачностью.
	// Добавляет вершину только с матрицей позиции (без нормали).
	public static void addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int red, int green, int blue, int alpha) {
		Vector4f position = pose.transform(new Vector4f(x, y, z, 1.0F));
		consumer.addVertex(position.x(), position.y(), position.z()).setColor(red, green, blue, alpha);
	}

	// Добавляет вершину с float-компонентами цвета (0..1).
	public static void addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
		addVertex(consumer, pose, x, y, z,
				(int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
	}

	public static void addWhiteVertex(
			VertexConsumer consumer,
			Matrix4f pose,
			Matrix3f normalMatrix,
			float x,
			float y,
			float z,
			float u,
			float v,
			int overlay,
			int light,
			float normalX,
			float normalY,
			float normalZ
	) {
		addVertex(consumer, pose, normalMatrix, x, y, z, 255, 255, 255, 255, u, v, overlay, light, normalX, normalY, normalZ);
	}
}
