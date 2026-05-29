package com.github.alexthe666.rats.client.render;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.RatsModelLayers;
import com.github.alexthe666.rats.client.model.hats.*;
import com.github.alexthe666.rats.server.items.HatItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class HatModels {

	private HatModels() {
	}

	// Создаёт 3D-модель шляпы по предмету; принимает HatItem, отдаёт HumanoidModel или null.
	public static HumanoidModel<?> createModel(HatItem hat) {
		try {
			ModelLayerLocation layer = resolveLayer(hat);
			if (layer == null) {
				return null;
			}
			ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(layer);
			return switch (BuiltInRegistries.ITEM.getKey(hat).getPath()) {
				case "chef_toque" -> new ChefToqueModel(root);
				case "piper_hat" -> new PiperHatModel(root);
				case "archeologist_hat" -> new ArcheologistHatModel(root);
				case "farmer_hat", "fisherman_hat" -> new FarmerHatModel(root);
				case "rat_fez" -> new RatFezModel(root);
				case "top_hat" -> new TopHatModel(root);
				case "santa_hat" -> new SantaHatModel(root);
				case "halo_hat" -> new HaloHatModel(root);
				case "pirat_hat" -> new PiratHatModel(root);
				case "rat_king_crown" -> new CrownModel(root);
				case "plague_doctor_mask", "black_death_mask" -> new PlagueDoctorMaskModel(root);
				case "exterminator_hat" -> new ExterminatorHatModel(root);
				case "aviator_hat" -> new AviatorHatModel(root);
				case "ghost_pirat_hat" -> new GhostPiratHatModel(root);
				case "military_hat" -> new MilitaryHatModel(root);
				default -> null;
			};
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать модель шляпы {}: {}", BuiltInRegistries.ITEM.getKey(hat), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Рисует на крысе только головную часть модели шляпы; принимает модель, матрицу, буфер, свет и оверлей.
	public static void renderOnRat(HumanoidModel<?> model, PoseStack stack, VertexConsumer consumer, int light, int overlay) {
		try {
			model.setAllVisible(false);
			model.head.visible = true;
			model.head.render(stack, consumer, light, overlay);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка рендера шляпы на крысе: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Рисует шляпу на гуманоиде; принимает модель, матрицу, буфер, свет и оверлей.
	public static void renderOnPlayer(HumanoidModel<?> model, PoseStack stack, VertexConsumer consumer, int light, int overlay) {
		renderOnRat(model, stack, consumer, light, overlay);
	}

	// Возвращает путь к текстуре шляпы; принимает предмет, отдаёт ResourceLocation.
	public static ResourceLocation getTexture(Item item) {
		try {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/model/hat/" + id.getPath() + ".png");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить текстуру шляпы {}: {}", item, exception.getMessage(), exception);
			throw exception;
		}
	}

	private static ModelLayerLocation resolveLayer(HatItem hat) {
		return switch (BuiltInRegistries.ITEM.getKey(hat).getPath()) {
			case "chef_toque" -> RatsModelLayers.CHEF_TOQUE;
			case "piper_hat" -> RatsModelLayers.PIPER_HAT;
			case "archeologist_hat" -> RatsModelLayers.ARCHEOLOGIST_HAT;
			case "farmer_hat", "fisherman_hat" -> RatsModelLayers.FARMER_HAT;
			case "rat_fez" -> RatsModelLayers.FEZ;
			case "top_hat" -> RatsModelLayers.TOP_HAT;
			case "santa_hat" -> RatsModelLayers.SANTA_HAT;
			case "halo_hat" -> RatsModelLayers.HALO;
			case "pirat_hat" -> RatsModelLayers.PIRATE_HAT;
			case "rat_king_crown" -> RatsModelLayers.CROWN;
			case "plague_doctor_mask", "black_death_mask" -> RatsModelLayers.PLAGUE_DOCTOR_MASK;
			case "exterminator_hat" -> RatsModelLayers.EXTERMINATOR_HAT;
			case "aviator_hat" -> RatsModelLayers.AVIATOR_HAT;
			case "ghost_pirat_hat" -> RatsModelLayers.PIRATE_HAT;
			case "military_hat" -> RatsModelLayers.OFFICER_HAT;
			default -> null;
		};
	}
}