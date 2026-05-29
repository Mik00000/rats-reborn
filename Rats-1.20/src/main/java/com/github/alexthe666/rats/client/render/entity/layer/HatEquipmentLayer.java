package com.github.alexthe666.rats.client.render.entity.layer;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.render.HatModels;
import com.github.alexthe666.rats.client.render.RatHeadEquipmentAdapter;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.github.alexthe666.rats.server.items.HatItem;
import com.github.alexthe666.rats.server.items.PartyHatItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HatEquipmentLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

	public HatEquipmentLayer(RenderLayerParent<T, M> parent) {
		super(parent);
	}

	// Рендерит 3D-шляпу на игроке или крысе; принимает стандартные параметры слоя рендера.
	@Override
	public void render(PoseStack stack, MultiBufferSource buffer, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		try {
			ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
			if (head.isEmpty() || head.getItem() instanceof PartyHatItem) {
				return;
			}
			if (!(head.getItem() instanceof HatItem hat)) {
				return;
			}
			// Крысы рисуются только в RatHelmetLayer.
			if (entity instanceof AbstractRat) {
				return;
			}
			HumanoidModel<?> model = HatModels.createModel(hat);
			if (model == null) {
				return;
			}
			stack.pushPose();
			this.applyHumanoidHeadTransform(stack);
			RatHeadEquipmentAdapter.applyPlayerHatPose(head, stack);
			if (this.getParentModel() instanceof HumanoidModel<?> human) {
				RatHeadEquipmentAdapter.syncPlayerHatHeadPose(human, model);
			}
			VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(HatModels.getTexture(head.getItem())), head.hasFoil());
			HatModels.renderOnPlayer(model, stack, consumer, light, OverlayTexture.NO_OVERLAY);
			stack.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка рендера шляпы на {}: {}", entity, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Выставляет матрицу в позицию головы гуманоида; принимает стек матриц, ничего не возвращает.
	private void applyHumanoidHeadTransform(PoseStack stack) {
		if (this.getParentModel() instanceof HumanoidModel<?> human) {
			human.head.translateAndRotate(stack);
		}
	}

}