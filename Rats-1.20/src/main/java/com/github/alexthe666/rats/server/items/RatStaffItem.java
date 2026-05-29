package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsCapabilityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.ManageRatStaffPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class RatStaffItem extends LoreTagItem {
	public RatStaffItem(Properties properties) {
		super(properties, 2, false);
	}

	public int getStaff(ItemStack stack) {
		if (stack.is(RatsItemRegistry.PATROL_STICK.get())) return 2;
		if (stack.is(RatsItemRegistry.RADIUS_STICK.get())) return 1;
		return 0;
	}

	// Открывает настройку посоха по лучу игрока, включая блоки жидкости; принимает мир, игрока и руку, отдаёт результат использования.
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		try {
			HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
			if (hitResult.getType() != HitResult.Type.BLOCK) {
				return InteractionResultHolder.pass(stack);
			}
			BlockHitResult blockHit = (BlockHitResult) hitResult;
			BlockPos pos = blockHit.getBlockPos();
			if (level.getFluidState(pos).isEmpty()) {
				return InteractionResultHolder.pass(stack);
			}
			SelectedRat selectedRat = player.getCapability(RatsCapabilityRegistry.SELECTED_RAT);
			TamedRat rat = selectedRat == null ? null : selectedRat.getSelectedRat();
			if (rat == null) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_STAFF_NO_RAT).withStyle(ChatFormatting.RED), true);
				return InteractionResultHolder.fail(stack);
			}
			player.swing(hand);
			if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.playerTarget(() -> serverPlayer), new ManageRatStaffPacket(rat.getId(), pos, blockHit.getDirection().ordinal(), false, true, this.getStaff(stack)));
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Игрок {} не смог открыть посох крысы по жидкости", player.getUUID(), exception);
			throw exception;
		}
	}

	// Добавляет подсказку посоха; принимает стек, контекст и список строк, ничего не отдаёт.
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		Level level = context.level();
		SelectedRat selectedRat = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getCapability(RatsCapabilityRegistry.SELECTED_RAT);
		if (level != null && level.isClientSide() && selectedRat != null) {
			TamedRat rat = selectedRat.getSelectedRat();
			if (rat != null) {
				tooltip.add(Component.translatable(RatsLangConstants.CHEESE_STAFF_SELECTED, rat.getDisplayName(), rat.getUUID().toString()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			}
		}
	}
}
