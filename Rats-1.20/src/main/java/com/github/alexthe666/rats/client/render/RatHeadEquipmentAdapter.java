package com.github.alexthe666.rats.client.render;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.entity.AbstractRatModel;
import com.github.alexthe666.rats.client.model.entity.StaticRatModel;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.github.alexthe666.rats.server.items.HatItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public final class RatHeadEquipmentAdapter {

	public static final float RAT_HEAD_TRANSLATE_Y = 0.12F;
	public static final float RAT_HEAD_TRANSLATE_Z = -0.05F;
	public static final float RAT_HAT_BASE_SCALE = 0.55F / 1.425F;
	public static final float RAT_HELMET_SCALE = 0.42F;
	public static final float PLAYER_HEAD_TRANSLATE_Y = 0.25F;
	public static final float PLAYER_HEAD_TRANSLATE_Z = -0.03F;
	public static final float PLAYER_HAT_BASE_SCALE = 1.0F / 1.425F;
	public static final float PLAYER_WIDE_HAT_SCALE = 1.05F;

	private static final float RAT_VANILLA_HELMET_LIFT = -0.03F;
	private static final float RAT_ARCHAEOLOGIST_EXTRA_DROP = 0.08F;
	private static final float RAT_MILITARY_LIFT = -0.1F;
	private static final float RAT_AVIATOR_LIFT = -0.02F;
	private static final float RAT_CREATE_DIVING_SCALE = 1.05F;

	private RatHeadEquipmentAdapter() {
	}

	public static void translateToRatHead(AbstractRatModel<?> ratModel, PoseStack poseStack) {
		try {
			ratModel.translateToHead(poseStack);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error moving the matrix to the rat head: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	public static void applyRatHatPose(AbstractRat rat, ItemStack itemStack, PoseStack poseStack) {
		try {
			if (rat.isBaby()) {
				poseStack.translate(0.0D, 0.205D, -0.05D);
				poseStack.scale(0.65F, 0.65F, 0.65F);
			}
			poseStack.translate(0.0F, RAT_HEAD_TRANSLATE_Y, RAT_HEAD_TRANSLATE_Z);
			poseStack.scale(RAT_HAT_BASE_SCALE, RAT_HAT_BASE_SCALE, RAT_HAT_BASE_SCALE);
			if (itemStack.getItem() instanceof HatItem hat) {
				hat.transformOnHead(rat, poseStack);
				poseStack.translate(0.0F, hat.getRatOffsetOnHead(), 0.0F);
				applyRatHatItemOffsets(itemStack, poseStack);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error posing the hat on rat {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	public static void applyRatHelmetPose(AbstractRat rat, ItemStack itemStack, PoseStack poseStack) {
		try {
			if (rat.isBaby()) {
				poseStack.translate(0.0D, 0.205D, -0.05D);
				poseStack.scale(0.65F, 0.65F, 0.65F);
			}
			poseStack.translate(0.0F, RAT_HEAD_TRANSLATE_Y, RAT_HEAD_TRANSLATE_Z);
			float scale = isCreateDivingHelmet(itemStack) ? RAT_HELMET_SCALE * RAT_CREATE_DIVING_SCALE : RAT_HELMET_SCALE;
			poseStack.scale(scale, scale, scale);
			if (!isModHat(itemStack)) {
				poseStack.translate(0.0F, RAT_VANILLA_HELMET_LIFT, 0.0F);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error posing the helmet on rat {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	public static void applyRatStaticHelmetPose(AbstractRat rat, ItemStack itemStack, PoseStack poseStack) {
		try {
			applyRatHelmetPose(rat, itemStack, poseStack);
			poseStack.translate(0.0D, -0.05D, 0.0D);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error posing the item helmet on rat {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	public static void applyPlayerHatPose(ItemStack itemStack, PoseStack poseStack) {
		try {
			if (itemStack.is(RatsItemRegistry.PIPER_HAT.get())) {
				poseStack.translate(0.0F, -0.195F, PLAYER_HEAD_TRANSLATE_Z);
			} else {
				poseStack.translate(0.0F, PLAYER_HEAD_TRANSLATE_Y, PLAYER_HEAD_TRANSLATE_Z);
			}
			poseStack.scale(PLAYER_HAT_BASE_SCALE, PLAYER_HAT_BASE_SCALE, PLAYER_HAT_BASE_SCALE);
			if (itemStack.getItem() instanceof HatItem hat) {
				hat.transformOnHeadForPlayer(poseStack);
				applyPlayerHatItemOffsets(itemStack, poseStack);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error posing the hat on the player for {}: {}", itemStack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	public static void syncPlayerHatHeadPose(HumanoidModel<?> parent, HumanoidModel<?> hatModel) {
		try {
			hatModel.head.xRot = parent.head.xRot;
			hatModel.head.yRot = parent.head.yRot;
			hatModel.head.zRot = parent.head.zRot;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error syncing the player hat head rotation: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	public static void syncRatHelmetHeadPose(AbstractRatModel<?> ratModel, HumanoidModel<?> helmetModel) {
		try {
			if (ratModel instanceof StaticRatModel<?> staticRat) {
				helmetModel.head.xRot = staticRat.head.rotateAngleX;
				helmetModel.head.yRot = staticRat.head.rotateAngleY;
				helmetModel.head.zRot = staticRat.head.rotateAngleZ;
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error syncing the rat helmet head rotation: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	public static HumanoidModel<?> resolveHelmetModel(LivingEntity entity, ItemStack itemStack, HumanoidModel<?> fallback) {
		try {
			if (!(itemStack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != EquipmentSlot.HEAD) {
				return fallback;
			}
			return IClientItemExtensions.of(itemStack).getHumanoidArmorModel(entity, itemStack, EquipmentSlot.HEAD, fallback);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to get helmet model {}: {}", itemStack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	public static void showHeadOnly(HumanoidModel<?> model) {
		try {
			model.setAllVisible(false);
			model.head.visible = true;
			model.hat.visible = true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error preparing the helmet model for rendering: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	public static boolean isHeadArmorItem(ItemStack itemStack) {
		try {
			return itemStack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error checking helmet {}: {}", itemStack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	public static boolean isModHat(ItemStack itemStack) {
		return itemStack.getItem() instanceof HatItem;
	}

	public static ResourceLocation resolveHelmetTexture(Entity entity, ItemStack itemStack, EquipmentSlot slot, @Nullable String overlaySuffix) {
		try {
			if (!(itemStack.getItem() instanceof ArmorItem armorItem)) {
				throw new IllegalStateException("Item is not armor: " + itemStack.getItem());
			}
			if (armorItem instanceof HatItem) {
				ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(armorItem);
				String overlay = overlaySuffix == null ? "" : "_" + overlaySuffix;
				return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "textures/model/hat/" + itemId.getPath() + overlay + ".png");
			}
			var material = armorItem.getMaterial().value();
			var layers = material.layers();
			if (layers.isEmpty()) {
				RatsMod.LOGGER.error("Armor material {} has no texture layers", BuiltInRegistries.ITEM.getKey(armorItem));
				throw new IllegalStateException("Armor material without texture layers: " + BuiltInRegistries.ITEM.getKey(armorItem));
			}
			ArmorMaterial.Layer layer = layers.getFirst();
			boolean inner = false;
			ResourceLocation custom = armorItem.getArmorTexture(itemStack, entity, slot, layer, inner);
			if (custom != null) {
				if (overlaySuffix == null) {
					return custom;
				}
				String path = custom.getPath();
				if (path.endsWith(".png")) {
					path = path.substring(0, path.length() - 4);
				}
				return custom.withPath(path + "_" + overlaySuffix + ".png");
			}
			ResourceLocation base = layer.texture(inner);
			if (overlaySuffix == null) {
				return base;
			}
			String path = base.getPath();
			if (path.endsWith(".png")) {
				path = path.substring(0, path.length() - 4);
			}
			return base.withPath(path + "_" + overlaySuffix + ".png");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error resolving helmet texture {}: {}", itemStack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	private static void applyRatHatItemOffsets(ItemStack itemStack, PoseStack poseStack) {
		String path = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath();
		switch (path) {
			case "archeologist_hat" -> poseStack.translate(0.0F, RAT_ARCHAEOLOGIST_EXTRA_DROP, 0.0F);
			case "military_hat" -> poseStack.translate(0.0F, RAT_MILITARY_LIFT, 0.0F);
			case "aviator_hat" -> poseStack.translate(0.0F, RAT_AVIATOR_LIFT, 0.0F);
			default -> {
			}
		}
	}

	private static void applyPlayerHatItemOffsets(ItemStack itemStack, PoseStack poseStack) {
		String path = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath();
		switch (path) {
			case "rat_king_crown" -> poseStack.translate(0.0F, -0.06F, 0.0F);
			case "halo_hat" -> poseStack.translate(0.0F, -0.14F, 0.0F);
			case "plague_doctor_mask", "black_death_mask" -> poseStack.translate(0.0F, 0.06F, -0.0625F);
			case "military_hat" -> poseStack.translate(0.0F, -0.1F, 0.0F);
			case "top_hat" -> poseStack.translate(0.0F, -0.04F, 0.0F);
			case "santa_hat" -> poseStack.translate(0.0F, -0.04F, 0.0F);
			case "farmer_hat", "fisherman_hat", "rat_fez" ->
					poseStack.scale(PLAYER_WIDE_HAT_SCALE, PLAYER_WIDE_HAT_SCALE, PLAYER_WIDE_HAT_SCALE);
			default -> {
			}
		}
	}

	private static boolean isCreateDivingHelmet(ItemStack itemStack) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		return id != null && "create".equals(id.getNamespace()) && id.getPath().contains("diving_helmet");
	}
}