package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.client.render.RatsBEWLR;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class RatHammockItem extends Item implements RatCageDecoration {
	public final DyeColor color;

	public RatHammockItem(Item.Properties properties, DyeColor color) {
		super(properties);
		this.color = color;
	}

	@Override
	public Direction getSupportedFace(Direction inputDir) {
		return Direction.UP;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(RatsLangConstants.CAGE_DECORATION).withStyle(ChatFormatting.GRAY));
	}

	// Hooks up the client-side 3D hammock renderer; takes the item extensions consumer, returns nothing.
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		try {
			consumer.accept(new IClientItemExtensions() {
				@Override
				public BlockEntityWithoutLevelRenderer getCustomRenderer() {
					return new RatsBEWLR();
				}
			});
		} catch (RuntimeException exception) {
			throw new IllegalStateException("Failed to hook up the client-side hammock renderer", exception);
		}
	}
}