package com.github.alexthe666.rats.client.render.entity.layer;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.entity.AbstractRatModel;
import com.github.alexthe666.rats.client.render.HatModels;
import com.github.alexthe666.rats.client.render.RatHeadEquipmentAdapter;
import com.github.alexthe666.rats.registry.RatlantisBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.github.alexthe666.rats.client.util.RenderColorUtil;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.HatItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;

public class RatHelmetLayer<T extends AbstractRat, M extends AbstractRatModel<T>> extends RenderLayer<T, M> {
	private final HumanoidModel<?> ratArmorModel;
	private final TextureAtlas armorTrimAtlas;

	public RatHelmetLayer(RenderLayerParent<T, M> parent, HumanoidModel<?> armorModel) {
		super(parent);
		this.ratArmorModel = armorModel;
		this.armorTrimAtlas = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET);
	}

	//for the sake of easier maintenance in the future, I will be commenting what each sections of this does.
	//this is partially a [VanillaCopy] of HumanoidArmorLayer.renderArmorPiece unless specified otherwise
	@Override
	public void render(PoseStack stack, MultiBufferSource buffer, int light, T rat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		ItemStack itemstack = rat.getItemBySlot(EquipmentSlot.HEAD);
		//Rats: always render a halo on top of dead rats
		if (rat instanceof TamedRat tamed && tamed.getRespawnCountdown() > 0) {
			itemstack = new ItemStack(RatsItemRegistry.HALO_HAT.get());
		}
		if (!itemstack.isEmpty()) {
			stack.pushPose();
			if (itemstack.getItem() instanceof HatItem hat) {
				this.renderHatModel(rat, hat, itemstack, stack, buffer, light);
			} else if (RatHeadEquipmentAdapter.isHeadArmorItem(itemstack)) {
				this.renderExternalHelmet(rat, itemstack, stack, buffer, light, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			} else {
				//Rats: handle some special case hats in the mod
				if (!itemstack.is(RatsItemRegistry.PARTY_HAT.get())) {
					this.getParentModel().translateToHead(stack);
					stack.translate(0, 0.025F, -0.15F);
					stack.mulPose(Axis.XP.rotationDegrees(180));
					stack.mulPose(Axis.YP.rotationDegrees(180));
					stack.scale(0.5F, 0.5F, 0.5F);
					if (itemstack.is(RatlantisBlockRegistry.MARBLED_CHEESE_RAT_HEAD.get().asItem())) {
						stack.translate(0, -0.1F, 0.1F);
						stack.mulPose(Axis.XP.rotationDegrees(15));
						if (rat.isBaby()) {
							stack.scale(0.4F, 0.4F, 0.4F);
							stack.translate(0.0D, 0.25D, 0.0D);
						}
					} else if (itemstack.is(ItemTags.SKULLS) && BuiltInRegistries.ITEM.getKey(itemstack.getItem()).getNamespace().equals("minecraft")) {
						stack.mulPose(Axis.YP.rotationDegrees(180));
						stack.translate(0.0D, 0.55D, -0.0D);
						stack.scale(2.0F, 2.0F, 2.0F);
						if (rat.isBaby()) {
							stack.scale(0.3F, 0.3F, 0.3F);
							stack.translate(0.0D, -0.75D, -0.25D);
						}
					}
					stack.translate(0.0D, -0.1D, 0.0D);
					stack.scale(0.85F, 0.85F, 0.85F);
					Minecraft.getInstance().getItemRenderer().renderStatic(itemstack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY, stack, buffer, null, rat.getId());
				}
			}
			stack.popPose();

			//Rats: render a banner if the rat has one in its banner slot
			stack.pushPose();
			ItemStack banner = rat.getItemBySlot(EquipmentSlot.OFFHAND);
			if (banner.getItem() instanceof BannerItem) {
				this.getParentModel().translateToBody(stack);
				stack.translate(0.0D, -0.5D, -0.2D);
				stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
				float sitProgress = rat.sitProgress / 20.0F;
				stack.mulPose(Axis.XP.rotationDegrees(sitProgress * -40.0F));
				stack.translate(0.0D, 0.0D, -sitProgress * 0.04F);
				stack.scale(1.7F, 1.7F, 1.7F);
				Minecraft.getInstance().getItemRenderer().renderStatic(banner, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, stack, buffer, null, rat.getId());
			}
			stack.popPose();
		}
	}

	// Рендерит 3D-модель шляпы на крысе; принимает крысу, шляпу, стек предмета, матрицы, буфер и свет, ничего не возвращает.
	private void renderHatModel(T rat, HatItem hat, ItemStack itemstack, PoseStack stack, MultiBufferSource buffer, int light) {
		try {
			HumanoidModel<?> hatModel = HatModels.createModel(hat);
			if (hatModel == null) {
				return;
			}
			stack.pushPose();
			RatHeadEquipmentAdapter.translateToRatHead(this.getParentModel(), stack);
			RatHeadEquipmentAdapter.applyRatHatPose(rat, itemstack, stack);
			VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(HatModels.getTexture(itemstack.getItem())), itemstack.hasFoil());
			HatModels.renderOnRat(hatModel, stack, consumer, light, OverlayTexture.NO_OVERLAY);
			stack.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка 3D-рендера шляпы на крысе {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Синхронизирует анимацию 3D-шлема с крыской; принимает модель, крысу и углы, ничего не возвращает.
	@SuppressWarnings("unchecked")
	private void animateHelmetModel(HumanoidModel<?> model, T rat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		try {
			((HumanoidModel<T>) model).setupAnim(rat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обновить анимацию шлема на крысе {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Рендерит шлем любого мода на крысе через переходник позы и NeoForge-модель; принимает крысу, предмет, матрицы, буфер, свет и углы анимации.
	private void renderExternalHelmet(T rat, ItemStack itemstack, PoseStack stack, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		try {
			ArmorItem armoritem = (ArmorItem) itemstack.getItem();
			HumanoidModel<?> resolved = RatHeadEquipmentAdapter.resolveHelmetModel(rat, itemstack, this.ratArmorModel);
			stack.pushPose();
			RatHeadEquipmentAdapter.translateToRatHead(this.getParentModel(), stack);
			RatHeadEquipmentAdapter.applyRatHelmetPose(rat, itemstack, stack);
			if (itemstack.has(DataComponents.DYED_COLOR)) {
				int i = itemstack.get(DataComponents.DYED_COLOR).rgb();
				float red = (float) (i >> 16 & 255) / 255.0F;
				float green = (float) (i >> 8 & 255) / 255.0F;
				float blue = (float) (i & 255) / 255.0F;
				this.renderHelmetHead(resolved, stack, buffer, light, red, green, blue, getArmorResource(rat, itemstack, EquipmentSlot.HEAD, null));
				this.renderHelmetHead(resolved, stack, buffer, light, 1.0F, 1.0F, 1.0F, getArmorResource(rat, itemstack, EquipmentSlot.HEAD, "overlay"));
			} else {
				this.renderHelmetHead(resolved, stack, buffer, light, 1.0F, 1.0F, 1.0F, getArmorResource(rat, itemstack, EquipmentSlot.HEAD, null));
			}
			if (itemstack.has(DataComponents.TRIM)) {
				this.renderTrim(armoritem.getMaterial(), stack, buffer, light, itemstack.get(DataComponents.TRIM), resolved);
			}
			if (itemstack.hasFoil()) {
				this.renderGlint(stack, buffer, light, resolved);
			}
			stack.popPose();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось отрисовать шлем {} на крысе {}: {}", itemstack.getItem(), rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Рисует только голову разрешённой модели шлема; принимает модель, матрицу, буфер, свет, цвет и текстуру.
	private void renderHelmetHead(HumanoidModel<?> model, PoseStack stack, MultiBufferSource buffer, int light, float red, float green, float blue, ResourceLocation armorResource) {
		try {
			VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.armorCutoutNoCull(armorResource));
			RatHeadEquipmentAdapter.showHeadOnly(model);
			model.head.render(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, RenderColorUtil.packRgb(red, green, blue));
			if (model.hat.visible) {
				model.hat.render(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, RenderColorUtil.packRgb(red, green, blue));
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось отрисовать голову шлема по текстуре {}: {}", armorResource, exception.getMessage(), exception);
			throw exception;
		}
	}

	private void renderModel(PoseStack stack, MultiBufferSource buffer, int light, Model model, float red, float green, float blue, ResourceLocation armorResource) {
		VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.armorCutoutNoCull(armorResource));
		model.renderToBuffer(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, RenderColorUtil.packRgb(red, green, blue));
	}

	private void renderTrim(Holder<ArmorMaterial> material, PoseStack stack, MultiBufferSource buffer, int light, ArmorTrim trim, Model model) {
		TextureAtlasSprite textureatlassprite = this.armorTrimAtlas.getSprite(trim.outerTexture(material));
		VertexConsumer vertexconsumer = textureatlassprite.wrap(buffer.getBuffer(Sheets.armorTrimsSheet(false)));
		model.renderToBuffer(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, -1);
	}

	private void renderGlint(PoseStack stack, MultiBufferSource buffer, int light, Model model) {
		model.renderToBuffer(stack, buffer.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY, -1);
	}

	/**
	 * Возвращает путь к текстуре слоя брони для предмета в слоте головы.
	 * Параметры: сущность, предмет в слоте, слот экипировки, суффикс overlay (или null).
	 * Отдаёт ResourceLocation текстуры; при материале вне реестра (шляпы мода) — из ArmorMaterial.Layer.
	 */
	public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
		try {
			return RatHeadEquipmentAdapter.resolveHelmetTexture(entity, stack, slot, type);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка получения текстуры шлема {}: {}", stack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}
}