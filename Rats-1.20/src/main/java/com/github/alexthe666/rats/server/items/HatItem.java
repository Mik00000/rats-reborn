package com.github.alexthe666.rats.server.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.RatsModelLayers;
import com.github.alexthe666.rats.client.model.hats.*;
import com.github.alexthe666.rats.registry.RatlantisItemRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.monster.GhostPirat;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class HatItem extends ArmorItem {

	private final int loreLines;

	public HatItem(Item.Properties properties, ArmorMaterial material, int loreLines) {
		super(Holder.direct(material), Type.HELMET, properties);
		this.loreLines = loreLines;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (stack.is(RatsItemRegistry.BLACK_DEATH_MASK.get())) {
			tooltip.add(Component.translatable("item.rats.plague_doctor_mask.desc").withStyle(ChatFormatting.GRAY));
		}
		if (this.loreLines > 0) {
			for (int i = 0; i < this.loreLines; i++) {
				tooltip.add(Component.translatable(this.getDescriptionId() + ".desc" + (this.loreLines == 1 ? "" : i)).withStyle(ChatFormatting.GRAY));
			}
		}
	}

	@Override
	public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
		return stack.is(RatsItemRegistry.RAT_KING_CROWN.get());
	}

	@Override
	public boolean isEnderMask(ItemStack stack, Player player, EnderMan enderMan) {
		return stack.is(RatsItemRegistry.BLACK_DEATH_MASK.get()) || stack.is(RatsItemRegistry.PLAGUE_DOCTOR_MASK.get());
	}

	/**
	 * Override this to add a custom transformation to the hat when on a rat's head.
	 *
	 * @param rat   the rat to render the hat on
	 * @param stack a PoseStack to allow translation, rotation, and scaling
	 */
	public void transformOnHead(AbstractRat rat, PoseStack stack) {
		if (this == RatsItemRegistry.CHEF_TOQUE.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(25.0F));
			stack.translate(0.0F, 0.1F, -0.25F);
		}
		if (this == RatsItemRegistry.PIPER_HAT.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(10.0F));
			stack.translate(0.0F, 0.0F, 0.1F);
		}
		if (this == RatsItemRegistry.PIRAT_HAT.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, -0.125F, 0.0F);
			stack.scale(1.425F, 1.425F, 1.425F);
		}
		if (this == RatlantisItemRegistry.GHOST_PIRAT_HAT.get()) {
			float piratScale = rat instanceof GhostPirat ? 1.1F : 1.425F;
			float piratTranslate = rat instanceof GhostPirat ? 0.05F : -0.125F;
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.3F);
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, piratTranslate, 0.0F);
			stack.scale(piratScale, piratScale, piratScale);
		}
		if (this == RatsItemRegistry.ARCHEOLOGIST_HAT.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, -0.1F, 0.0F);
			stack.scale(1.425F, 1.425F, 1.425F);
		}
		if (this == RatsItemRegistry.FARMER_HAT.get() || this == RatsItemRegistry.FISHERMAN_HAT.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, -0.1F, 0.0F);
			stack.scale(1.425F, 1.425F, 1.425F);
		}
		if (this == RatsItemRegistry.PLAGUE_DOCTOR_MASK.get() || this == RatsItemRegistry.BLACK_DEATH_MASK.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(15.0F));
			stack.translate(0.0F, -0.1F, 0.0F);
			stack.scale(1.5F, 1.2F, 1.5F);
		}
		if (this == RatsItemRegistry.RAT_FEZ.get()) {
			stack.translate(-0.05F, -0.15F, -0.1F);
			stack.scale(1.425F, 1.425F, 1.425F);
		}
		if (this == RatsItemRegistry.TOP_HAT.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, -0.125F, 0.0F);
			stack.scale(1.425F, 1.425F, 1.425F);
		}
		if (this == RatsItemRegistry.SANTA_HAT.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, 0.0F, 0.1F);
			stack.scale(1.25F, 1.25F, 1.25F);
		}
		if (this == RatlantisItemRegistry.MILITARY_HAT.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, -0.1F, 0.0F);
			stack.scale(1.425F, 1.425F, 1.425F);
		}
		if (this == RatsItemRegistry.RAT_KING_CROWN.get()) {
			stack.mulPose(Axis.XN.rotationDegrees(5.0F));
			stack.translate(0.0F, -0.05F, 0.0F);
			stack.scale(1.25F, 1.25F, 1.25F);
		}
		if (this == RatlantisItemRegistry.AVIATOR_HAT.get()) {
			stack.scale(1.25F, 1.25F, 1.25F);
			stack.translate(0, -0.035F, 0.01F);
		}
	}

	// Extra hat transformations on the head of a player and other humanoids; takes a PoseStack, returns nothing.
	public void transformOnHeadForPlayer(PoseStack stack) {
		try {
			if (this == RatsItemRegistry.CHEF_TOQUE.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(25.0F));
				stack.translate(0.0F, 0.1F, -0.25F);
			}
			if (this == RatsItemRegistry.PIPER_HAT.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(10.0F));
				stack.translate(0.0F, 0.0F, 0.1F);
			}
			if (this == RatsItemRegistry.PIRAT_HAT.get() || this == RatlantisItemRegistry.GHOST_PIRAT_HAT.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(5.0F));
				stack.translate(0.0F, -0.125F, 0.0F);
				stack.scale(1.425F, 1.425F, 1.425F);
			}
			if (this == RatsItemRegistry.ARCHEOLOGIST_HAT.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(5.0F));
				stack.translate(0.0F, -0.1F, 0.0F);
				stack.scale(1.425F, 1.425F, 1.425F);
			}
			if (this == RatsItemRegistry.FARMER_HAT.get() || this == RatsItemRegistry.FISHERMAN_HAT.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(5.0F));
				stack.translate(0.0F, -0.1F, 0.0F);
				stack.scale(1.425F, 1.425F, 1.425F);
			}
			if (this == RatsItemRegistry.PLAGUE_DOCTOR_MASK.get() || this == RatsItemRegistry.BLACK_DEATH_MASK.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(15.0F));
				stack.translate(0.0F, -0.1F, 0.0F);
				stack.scale(1.5F, 1.2F, 1.5F);
			}
			if (this == RatsItemRegistry.RAT_FEZ.get()) {
				stack.translate(-0.05F, -0.15F, -0.1F);
				stack.scale(1.425F, 1.425F, 1.425F);
			}
			if (this == RatsItemRegistry.TOP_HAT.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(5.0F));
				stack.translate(0.0F, -0.125F, 0.0F);
				stack.scale(1.425F, 1.425F, 1.425F);
			}
			if (this == RatsItemRegistry.SANTA_HAT.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(5.0F));
				stack.translate(0.0F, 0.0F, 0.1F);
				stack.scale(1.25F, 1.25F, 1.25F);
			}
			if (this == RatlantisItemRegistry.MILITARY_HAT.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(5.0F));
				stack.translate(0.0F, -0.1F, 0.0F);
				stack.scale(1.425F, 1.425F, 1.425F);
			}
			if (this == RatsItemRegistry.RAT_KING_CROWN.get()) {
				stack.mulPose(Axis.XN.rotationDegrees(5.0F));
				stack.translate(0.0F, -0.05F, 0.0F);
				stack.scale(1.25F, 1.25F, 1.25F);
			}
			if (this == RatlantisItemRegistry.AVIATOR_HAT.get()) {
				stack.scale(1.25F, 1.25F, 1.25F);
				stack.translate(0, -0.035F, 0.01F);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error transforming hat {} on the player: {}", BuiltInRegistries.ITEM.getKey(this), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Extra Y offset of the hat on the rat head after transformOnHead; a positive value lowers the hat.
	public float getRatOffsetOnHead() {
		if (this == RatsItemRegistry.FARMER_HAT.get()
				|| this == RatsItemRegistry.FISHERMAN_HAT.get()
				|| this == RatsItemRegistry.RAT_FEZ.get()) {
			return 0.28F;
		}
		if (this == RatlantisItemRegistry.MILITARY_HAT.get()) {
			return 0.42F;
		}
		if (this == RatsItemRegistry.TOP_HAT.get()) {
			return 0.12F;
		} else if (this == RatlantisItemRegistry.GHOST_PIRAT_HAT.get() || this == RatsItemRegistry.PIRAT_HAT.get()) {
			return 0.06F;
		}
		return 0.0F;
	}

	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor) {
				return 0;
			}

			@Override
			public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
				// 3D hats are drawn by HatEquipmentLayer; replacing the armor model here disables the helmet layer in 1.21.1.
				if (equipmentSlot == EquipmentSlot.HEAD && itemStack.getItem() instanceof HatItem) {
					return original;
				}
				return original;
			}
		});
	}
}
