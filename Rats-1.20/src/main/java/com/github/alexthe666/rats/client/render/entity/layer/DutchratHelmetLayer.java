package com.github.alexthe666.rats.client.render.entity.layer;

import com.github.alexthe666.rats.client.model.entity.FlyingDutchratModel;
import com.github.alexthe666.rats.server.entity.monster.boss.Dutchrat;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DutchratHelmetLayer<T extends Dutchrat, M extends FlyingDutchratModel<T>>
extends RenderLayer<T, M> {
    private final HumanoidModel<?> backup;
    private static final Map<String, ResourceLocation> ARMOR_TEXTURE_RES_MAP = Maps.newHashMap();

    public DutchratHelmetLayer(RenderLayerParent<T, M> parent, HumanoidModel<?> armorModel) {
        super(parent);
        this.backup = armorModel;
    }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffer, int light, T rat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            if (rat != null && rat.getBellSummonTicks() <= 0) {
                stack.pushPose();
                try {
                    this.getParentModel().body1.translateRotate(stack);
                    this.getParentModel().neck.translateRotate(stack);
                    this.getParentModel().head.translateRotate(stack);
                    stack.translate(0.0f, -0.77f, 0.0f);
                    ItemStack itemstack = rat.getItemBySlot(EquipmentSlot.HEAD);
                    if (itemstack.getItem() instanceof ArmorItem) {
                        HumanoidModel<?> model = this.backup;
                        ResourceLocation tex = this.getArmorResource(rat, itemstack, EquipmentSlot.HEAD, null);
                        if (tex != null) {
                            VertexConsumer consumer = ItemRenderer.getFoilBuffer(buffer, RenderType.entityCutoutNoCull(tex), false, false);
                            model.renderToBuffer(stack, consumer, light, OverlayTexture.NO_OVERLAY, -1);
                        }
                    }
                } finally {
                    stack.popPose();
                }
            }
        } catch (Throwable t) {
            // Catch any unexpected rendering exceptions to prevent client crash
        }
    }

    public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        if (!(stack.getItem() instanceof ArmorItem item)) {
            return ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_1.png");
        }
        ResourceLocation matLoc = item.getMaterial().unwrapKey().map(ResourceKey::location).orElse(null);
        String domain = matLoc != null ? matLoc.getNamespace() : "minecraft";
        String texture = matLoc != null ? matLoc.getPath() : "iron";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String s1 = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, 1, type == null ? "" : String.format("_%s", type));
        ResourceLocation resourcelocation = ARMOR_TEXTURE_RES_MAP.get(s1);
        if (resourcelocation == null) {
            resourcelocation = ResourceLocation.parse(s1);
            ARMOR_TEXTURE_RES_MAP.put(s1, resourcelocation);
        }
        return resourcelocation;
    }
}
