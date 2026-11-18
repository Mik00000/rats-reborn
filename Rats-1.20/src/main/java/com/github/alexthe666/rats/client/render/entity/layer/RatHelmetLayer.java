/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.HumanoidModel
 *  net.minecraft.client.model.Model
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.Sheets
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.client.renderer.entity.RenderLayerParent
 *  net.minecraft.client.renderer.entity.layers.RenderLayer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.core.Holder
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.ArmorMaterial
 *  net.minecraft.world.item.BannerItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.armortrim.ArmorTrim
 *  net.minecraft.world.item.component.DyedItemColor
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Block
 *  org.jetbrains.annotations.Nullable
 */
package com.github.alexthe666.rats.client.render.entity.layer;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.entity.AbstractRatModel;
import com.github.alexthe666.rats.client.render.HatModels;
import com.github.alexthe666.rats.client.render.RatHeadEquipmentAdapter;
import com.github.alexthe666.rats.client.util.RenderColorUtil;
import com.github.alexthe666.rats.registry.RatlantisBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class RatHelmetLayer<T extends AbstractRat, M extends AbstractRatModel<T>>
extends RenderLayer<T, M> {
    private final HumanoidModel<?> ratArmorModel;
    private final TextureAtlas armorTrimAtlas;

    public RatHelmetLayer(RenderLayerParent<T, M> parent, HumanoidModel<?> armorModel) {
        super(parent);
        this.ratArmorModel = armorModel;
        this.armorTrimAtlas = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET);
    }

    public void render(PoseStack stack, MultiBufferSource buffer, int light, T rat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        TamedRat tamed;
        ItemStack itemstack = rat.getItemBySlot(EquipmentSlot.HEAD);
        if (rat instanceof TamedRat && (tamed = (TamedRat)((Object)rat)).getRespawnCountdown() > 0) {
            itemstack = new ItemStack((ItemLike)RatsItemRegistry.HALO_HAT.get());
        }
        if (!itemstack.isEmpty()) {
            stack.pushPose();
            Item item = itemstack.getItem();
            if (item instanceof HatItem) {
                HatItem hat = (HatItem)item;
                this.renderHatModel(rat, hat, itemstack, stack, buffer, light);
            } else if (RatHeadEquipmentAdapter.isHeadArmorItem(itemstack)) {
                this.renderExternalHelmet(rat, itemstack, stack, buffer, light, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            } else if (isModHatItem(itemstack)) {
                this.renderModHat(rat, itemstack, stack, buffer, light);
            } else if (!itemstack.is((Item)RatsItemRegistry.PARTY_HAT.get())) {
                ((AbstractRatModel)this.getParentModel()).translateToHead(stack);
                stack.translate(0.0f, 0.025f, -0.15f);
                stack.mulPose(Axis.XP.rotationDegrees(180.0f));
                stack.mulPose(Axis.YP.rotationDegrees(180.0f));
                stack.scale(0.5f, 0.5f, 0.5f);
                if (itemstack.is(((Block)RatlantisBlockRegistry.MARBLED_CHEESE_RAT_HEAD.get()).asItem())) {
                    stack.translate(0.0f, -0.1f, 0.1f);
                    stack.mulPose(Axis.XP.rotationDegrees(15.0f));
                    if (rat.isBaby()) {
                        stack.scale(0.4f, 0.4f, 0.4f);
                        stack.translate(0.0, 0.25, 0.0);
                    }
                } else if (itemstack.is(ItemTags.SKULLS) && BuiltInRegistries.ITEM.getKey(itemstack.getItem()).getNamespace().equals("minecraft")) {
                    stack.mulPose(Axis.YP.rotationDegrees(180.0f));
                    stack.translate(0.0, 0.55, -0.0);
                    stack.scale(2.0f, 2.0f, 2.0f);
                    if (rat.isBaby()) {
                        stack.scale(0.3f, 0.3f, 0.3f);
                        stack.translate(0.0, -0.75, -0.25);
                    }
                }
                stack.translate(0.0, -0.1, 0.0);
                stack.scale(0.85f, 0.85f, 0.85f);
                Minecraft.getInstance().getItemRenderer().renderStatic(itemstack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY, stack, buffer, null, rat.getId());
            }
            stack.popPose();
            stack.pushPose();
            ItemStack banner = rat.getItemBySlot(EquipmentSlot.OFFHAND);
            if (banner.getItem() instanceof BannerItem) {
                ((AbstractRatModel)this.getParentModel()).translateToBody(stack);
                stack.translate(0.0, -0.5, -0.2);
                stack.mulPose(Axis.ZP.rotationDegrees(180.0f));
                float sitProgress = ((AbstractRat)((Object)rat)).sitProgress / 20.0f;
                stack.mulPose(Axis.XP.rotationDegrees(sitProgress * -40.0f));
                stack.translate(0.0, 0.0, (double)(-sitProgress * 0.04f));
                stack.scale(1.7f, 1.7f, 1.7f);
                Minecraft.getInstance().getItemRenderer().renderStatic(banner, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, stack, buffer, null, rat.getId());
            }
            stack.popPose();
        }
    }

    public static boolean isModHatItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.SKULLS) || stack.is(((Block)RatlantisBlockRegistry.MARBLED_CHEESE_RAT_HEAD.get()).asItem())) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            String ns = id.getNamespace();
            String path = id.getPath();
            if ("simplehats".equals(ns) || path.contains("hat") || path.contains("cap") || path.contains("crown") || path.contains("beanie")) {
                return true;
            }
        }
        return stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("hat") || path.contains("head") || path.contains("helm");
        });
    }

    private void renderModHat(T rat, ItemStack itemstack, PoseStack stack, MultiBufferSource buffer, int light) {
        try {
            stack.pushPose();
            RatHeadEquipmentAdapter.translateToRatHead((AbstractRatModel)this.getParentModel(), stack);
            if (rat.isBaby()) {
                stack.translate(0.0, 0.18, -0.05);
                stack.scale(0.55f, 0.55f, 0.55f);
            }
            stack.translate(0.0f, -0.04f, -0.13f);
            stack.mulPose(Axis.XP.rotationDegrees(180.0f));
            stack.mulPose(Axis.YP.rotationDegrees(180.0f));
            float hatScale = 0.38f;
            stack.scale(hatScale, hatScale, hatScale);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                itemstack,
                ItemDisplayContext.HEAD,
                light,
                OverlayTexture.NO_OVERLAY,
                stack,
                buffer,
                null,
                rat.getId()
            );
            stack.popPose();
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to render hat {} on rat {}: {}", itemstack.getItem(), rat.getUUID(), exception.getMessage(), exception);
        }
    }

    private void renderHatModel(T rat, HatItem hat, ItemStack itemstack, PoseStack stack, MultiBufferSource buffer, int light) {
        try {
            HumanoidModel<?> hatModel = HatModels.createModel(hat);
            if (hatModel == null) {
                return;
            }
            stack.pushPose();
            RatHeadEquipmentAdapter.translateToRatHead((AbstractRatModel)this.getParentModel(), stack);
            RatHeadEquipmentAdapter.applyRatHatPose(rat, itemstack, stack);
            VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer((MultiBufferSource)buffer, (RenderType)RenderType.armorCutoutNoCull((ResourceLocation)HatModels.getTexture(itemstack.getItem())), (boolean)itemstack.hasFoil());
            HatModels.renderOnRat(hatModel, stack, consumer, light, OverlayTexture.NO_OVERLAY);
            stack.popPose();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041e\u0448\u0438\u0431\u043a\u0430 3D-\u0440\u0435\u043d\u0434\u0435\u0440\u0430 \u0448\u043b\u044f\u043f\u044b \u043d\u0430 \u043a\u0440\u044b\u0441\u0435 {}: {}", (Object)rat.getUUID(), (Object)exception.getMessage(), (Object)exception);
            throw exception;
        }
    }

    @SuppressWarnings("unchecked")
    private void animateHelmetModel(HumanoidModel<?> model, T rat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            ((HumanoidModel<LivingEntity>)model).setupAnim(rat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u044e \u0448\u043b\u0435\u043c\u0430 \u043d\u0430 \u043a\u0440\u044b\u0441\u0435 {}: {}", (Object)rat.getUUID(), (Object)exception.getMessage(), (Object)exception);
            throw exception;
        }
    }

    private void renderExternalHelmet(T rat, ItemStack itemstack, PoseStack stack, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            ArmorItem armoritem = (ArmorItem)itemstack.getItem();
            HumanoidModel<?> resolved = RatHeadEquipmentAdapter.resolveHelmetModel(rat, itemstack, this.ratArmorModel);
            stack.pushPose();
            RatHeadEquipmentAdapter.translateToRatHead((AbstractRatModel)this.getParentModel(), stack);
            RatHeadEquipmentAdapter.applyRatHelmetPose(rat, itemstack, stack);
            if (itemstack.has(DataComponents.DYED_COLOR)) {
                int i = ((DyedItemColor)itemstack.get(DataComponents.DYED_COLOR)).rgb();
                float red = (float)(i >> 16 & 0xFF) / 255.0f;
                float green = (float)(i >> 8 & 0xFF) / 255.0f;
                float blue = (float)(i & 0xFF) / 255.0f;
                this.renderHelmetHead(resolved, stack, buffer, light, red, green, blue, this.getArmorResource((Entity)rat, itemstack, EquipmentSlot.HEAD, null));
                this.renderHelmetHead(resolved, stack, buffer, light, 1.0f, 1.0f, 1.0f, this.getArmorResource((Entity)rat, itemstack, EquipmentSlot.HEAD, "overlay"));
            } else {
                this.renderHelmetHead(resolved, stack, buffer, light, 1.0f, 1.0f, 1.0f, this.getArmorResource((Entity)rat, itemstack, EquipmentSlot.HEAD, null));
            }
            if (itemstack.has(DataComponents.TRIM)) {
                this.renderTrim((Holder<ArmorMaterial>)armoritem.getMaterial(), stack, buffer, light, (ArmorTrim)itemstack.get(DataComponents.TRIM), (Model)resolved);
            }
            if (itemstack.hasFoil()) {
                this.renderGlint(stack, buffer, light, (Model)resolved);
            }
            stack.popPose();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u0440\u0438\u0441\u043e\u0432\u0430\u0442\u044c \u0448\u043b\u0435\u043c {} \u043d\u0430 \u043a\u0440\u044b\u0441\u0435 {}: {}", (Object)itemstack.getItem(), (Object)rat.getUUID(), (Object)exception.getMessage(), (Object)exception);
            throw exception;
        }
    }

    private void renderHelmetHead(HumanoidModel<?> model, PoseStack stack, MultiBufferSource buffer, int light, float red, float green, float blue, ResourceLocation armorResource) {
        try {
            VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.armorCutoutNoCull((ResourceLocation)armorResource));
            RatHeadEquipmentAdapter.showHeadOnly(model);
            model.head.render(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, RenderColorUtil.packRgb(red, green, blue));
            if (model.hat.visible) {
                model.hat.render(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, RenderColorUtil.packRgb(red, green, blue));
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u0440\u0438\u0441\u043e\u0432\u0430\u0442\u044c \u0433\u043e\u043b\u043e\u0432\u0443 \u0448\u043b\u0435\u043c\u0430 \u043f\u043e \u0442\u0435\u043a\u0441\u0442\u0443\u0440\u0435 {}: {}", (Object)armorResource, (Object)exception.getMessage(), (Object)exception);
            throw exception;
        }
    }

    private void renderModel(PoseStack stack, MultiBufferSource buffer, int light, Model model, float red, float green, float blue, ResourceLocation armorResource) {
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.armorCutoutNoCull((ResourceLocation)armorResource));
        model.renderToBuffer(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, RenderColorUtil.packRgb(red, green, blue));
    }

    private void renderTrim(Holder<ArmorMaterial> material, PoseStack stack, MultiBufferSource buffer, int light, ArmorTrim trim, Model model) {
        TextureAtlasSprite textureatlassprite = this.armorTrimAtlas.getSprite(trim.outerTexture(material));
        VertexConsumer vertexconsumer = textureatlassprite.wrap(buffer.getBuffer(Sheets.armorTrimsSheet((boolean)false)));
        model.renderToBuffer(stack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, -1);
    }

    private void renderGlint(PoseStack stack, MultiBufferSource buffer, int light, Model model) {
        model.renderToBuffer(stack, buffer.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY, -1);
    }

    public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        try {
            return RatHeadEquipmentAdapter.resolveHelmetTexture(entity, stack, slot, type);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u0438\u044f \u0442\u0435\u043a\u0441\u0442\u0443\u0440\u044b \u0448\u043b\u0435\u043c\u0430 {}: {}", (Object)stack.getItem(), (Object)exception.getMessage(), (Object)exception);
            throw exception;
        }
    }
}

