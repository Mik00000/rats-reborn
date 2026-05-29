package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.RatsModelLayers;
import com.github.alexthe666.rats.client.model.hats.RatlantisArmorModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class RatlantisArmorItem extends ArmorItem {
	private static final ResourceLocation OUTER_ARMOR_TEXTURE = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/model/armor/ratlantis_armor_0.png");
	private static final ResourceLocation INNER_ARMOR_TEXTURE = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/model/armor/ratlantis_armor_1.png");

	public RatlantisArmorItem(ArmorMaterial material, Type type, Item.Properties properties) {
		super(Holder.direct(material), type, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.rats.ratlantis_armor.desc0").withStyle(ChatFormatting.YELLOW));
		tooltip.add(Component.translatable("item.rats.ratlantis_armor.desc1").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.rats.ratlantis_armor.desc2").withStyle(ChatFormatting.GRAY));
	}

	// Возвращает текстуру надетой брони Ратлантиса; принимает предмет, сущность, слот, слой материала и признак внутреннего слоя, отдаёт путь к png-текстуре.
	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		try {
			return slot == EquipmentSlot.LEGS || innerModel ? INNER_ARMOR_TEXTURE : OUTER_ARMOR_TEXTURE;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выбрать текстуру брони Ратлантиса для слота {}: {}", slot, exception.getMessage(), exception);
			throw exception;
		}
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
				EntityModelSet models = Minecraft.getInstance().getEntityModels();
				ModelPart root = models.bakeLayer(slot == EquipmentSlot.LEGS ? RatsModelLayers.RATLANTIS_ARMOR_INNER : RatsModelLayers.RATLANTIS_ARMOR_OUTER);
				return new RatlantisArmorModel(root);
			}
		});
	}
}
