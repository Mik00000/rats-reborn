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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HatModels {
    private static final Map<HatItem, HumanoidModel<?>> MODEL_CACHE = new ConcurrentHashMap<>();

    private HatModels() {}

    public static HumanoidModel<?> createModel(HatItem hat) {
        if (hat == null) {
            return null;
        }
        return MODEL_CACHE.computeIfAbsent(hat, HatModels::buildModelInternal);
    }

    private static HumanoidModel<?> buildModelInternal(HatItem hat) {
        try {
            ModelLayerLocation layer = resolveLayer(hat);
            if (layer == null) {
                return null;
            }
            ModelPart part = Minecraft.getInstance().getEntityModels().bakeLayer(layer);
            String path = BuiltInRegistries.ITEM.getKey(hat).getPath();
            return switch (path) {
                case "chef_toque" -> new ChefToqueModel(part);
                case "piper_hat" -> new PiperHatModel(part);
                case "archeologist_hat" -> new ArcheologistHatModel(part);
                case "farmer_hat", "fisherman_hat" -> new FarmerHatModel(part);
                case "rat_fez" -> new RatFezModel(part);
                case "top_hat" -> new TopHatModel(part);
                case "santa_hat" -> new SantaHatModel(part);
                case "halo_hat" -> new HaloHatModel(part);
                case "pirat_hat" -> new PiratHatModel(part);
                case "rat_king_crown" -> new CrownModel(part);
                case "plague_doctor_mask", "black_death_mask" -> new PlagueDoctorMaskModel(part);
                case "exterminator_hat" -> new ExterminatorHatModel(part);
                case "aviator_hat" -> new AviatorHatModel(part);
                case "ghost_pirat_hat" -> new GhostPiratHatModel(part);
                case "military_hat" -> new MilitaryHatModel(part);
                default -> null;
            };
        } catch (Throwable e) {
            RatsMod.LOGGER.error("Не удалось создать модель шляпы {}: {}", BuiltInRegistries.ITEM.getKey(hat), e.getMessage());
            return null;
        }
    }

    public static void renderOnRat(HumanoidModel<?> model, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        if (model == null) return;
        try {
            model.setAllVisible(false);
            model.head.visible = true;
            model.head.render(poseStack, buffer, packedLight, packedOverlay);
        } catch (Throwable e) {
            RatsMod.LOGGER.error("Ошибка рендера шляпы на крысе: {}", e.getMessage());
        }
    }

    public static void renderOnPlayer(HumanoidModel<?> model, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        renderOnRat(model, poseStack, buffer, packedLight, packedOverlay);
    }

    public static ResourceLocation getTexture(Item item) {
        try {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            return ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/model/hat/" + key.getPath() + ".png");
        } catch (Throwable e) {
            RatsMod.LOGGER.error("Не удалось получить текстуру шляпы {}: {}", item, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static ModelLayerLocation resolveLayer(HatItem hat) {
        String path = BuiltInRegistries.ITEM.getKey(hat).getPath();
        return switch (path) {
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
