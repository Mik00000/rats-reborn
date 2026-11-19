package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Iterator;
import java.util.List;

public class RatFluteItem extends Item {

	public RatFluteItem(Item.Properties properties) {
		super(properties);
	}

	// Returns the flute use animation; takes the item stack, returns the animation type.
	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.TOOT_HORN;
	}

	// Reads the selected command index from the flute data; takes the stack, returns the stored command index.
	private static int getCommandInt(ItemStack stack) {
		try {
			return RatsItemStackData.getTag(stack).getInt("Command");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to read the rat flute command from stack {}", stack, exception);
			throw exception;
		}
	}

	// Writes the selected command index into the flute data; takes the stack and command index, returns nothing.
	private static void setCommandInt(ItemStack stack, int command) {
		try {
			CompoundTag tag = RatsItemStackData.getTag(stack);
			tag.putInt("Command", command);
			RatsItemStackData.setTag(stack, tag);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to write command {} into rat flute {}", command, stack, exception);
			throw exception;
		}
	}

	// Returns the selected flute command; takes the stack, returns the command with a safely clamped index.
	private static RatCommand getCommand(ItemStack stack) {
		return RatCommand.values()[Mth.clamp(getCommandInt(stack), 0, RatCommand.values().length - 1)];
	}

	// Handles using the flute; takes the level, player and hand, returns the use result with the stack.
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isShiftKeyDown()) {
			try {
				int commandInt = getCommandInt(stack) + 1;
				if (commandInt > RatCommand.values().length - 1) {
					commandInt = 0;
				}
				setCommandInt(stack, commandInt);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Player {} failed to switch the rat flute command", player.getUUID(), exception);
				throw exception;
			}
			RatCommand ratCommand = getCommand(stack);
			if (level.isClientSide()) {
				level.playSound(player, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 1.0F, 1.25F);
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_FLUTE_COMMAND, Component.translatable(ratCommand.getTranslateName())), true);
			}
			return InteractionResultHolder.success(stack);
		}

		if (!level.isClientSide()) {
			player.getCooldowns().addCooldown(this, 60);
			RatCommand ratCommand = getCommand(stack);
			float chunksize = 16 * RatConfig.ratFluteDistance;
			List<Entity> list = level.getEntities(player, (new AABB(player.getX(), player.getY(), player.getZ(), player.getX() + 1.0D, player.getY() + 1.0D, player.getZ() + 1.0D)).inflate(chunksize, level.getHeight(), chunksize));
			Iterator<Entity> itr = list.iterator();
			int ratCount = 0;
			while (itr.hasNext()) {
				Entity entity = itr.next();
				if (entity instanceof TamedRat rat && rat.onHearFlute(player, ratCommand)) {
					ratCount++;
				}
			}
			player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_FLUTE_COUNT, ratCount).withStyle(ChatFormatting.GRAY), true);
			level.playSound(null, player.blockPosition(), RatsSoundRegistry.getFluteSound(), SoundSource.NEUTRAL, 1, 1.25F);
		}
		player.swing(hand);
		return InteractionResultHolder.success(stack);
	}

	// Adds the flute tooltip; takes the stack, context and list of lines, returns nothing.
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.rats.rat_flute.desc0").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.rats.rat_flute.desc1").withStyle(ChatFormatting.GRAY));
		RatCommand ratCommand = getCommand(stack);
		tooltip.add(Component.translatable(RatsLangConstants.RAT_CURRENT_COMMAND, Component.translatable(ratCommand.getTranslateName())).withStyle(ChatFormatting.GRAY));
	}
}
