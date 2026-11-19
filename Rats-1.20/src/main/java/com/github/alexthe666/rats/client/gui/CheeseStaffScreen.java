package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.util.EntityRenderingUtil;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SyncRatStaffPacket;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public class CheeseStaffScreen extends Screen {

	private final TamedRat rat;
	private final BlockPos pos;
	private final Direction clickedFace;

	public CheeseStaffScreen(TamedRat rat, BlockPos pos, Direction clickedFace) {
		super(Component.translatable(RatsItemRegistry.CHEESE_STICK.get().getDescriptionId()));
		this.rat = rat;
		this.pos = pos;
		this.clickedFace = clickedFace;
		this.init();
	}

	// Creates the cheese staff buttons; parameters come from the screen fields, returns nothing.
	protected void init() {
		super.init();
		this.clearWidgets();
		int i = (this.width) / 2;
		int j = (this.height - 166) / 2;
		Component topText = Component.translatable(RatsLangConstants.RAT_STAFF_DEPOSIT_POS, this.getPosName(), Component.translatable("rats.direction." + this.clickedFace.getName()));
		int maxLength = Math.max(150, Minecraft.getInstance().font.width(topText.getString()) + 20);
		this.addRenderableWidget(Button.builder(topText, button -> {
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(this.rat.getId(), this.pos, this.clickedFace, 0));
			Minecraft.getInstance().setScreen(null);
			this.init();
		}).bounds(i - maxLength / 2, j + 60, maxLength, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable(RatsLangConstants.RAT_STAFF_PICKUP_POS, this.getPosName(), Component.translatable("rats.direction." + this.clickedFace.getName())), button -> {
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(this.rat.getId(), this.pos, this.clickedFace, 1));
			Minecraft.getInstance().setScreen(null);
			this.init();
		}).bounds(i - maxLength / 2, j + 85, maxLength, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable(RatsLangConstants.RAT_STAFF_APPLY_POS, this.getPosName(), Component.translatable("rats.direction." + this.clickedFace.getName())), button -> {
			this.rat.setCreateWorkPos(GlobalPos.of(Minecraft.getInstance().player.level().dimension(), this.pos));
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(this.rat.getId(), this.pos, this.clickedFace, 8));
			Minecraft.getInstance().setScreen(null);
			this.init();
		}).bounds(i - maxLength / 2, j + 110, maxLength, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable(this.isRatHouseAtPos() ? RatsLangConstants.RAT_STAFF_SET_RAT_HOUSE : RatsLangConstants.RAT_STAFF_SET_WORK_ANCHOR, getPosName()), button -> {
			if (!this.isRatHouseAtPos()) {
				this.rat.setWorkAnchorPoint(GlobalPos.of(Minecraft.getInstance().player.level().dimension(), this.pos));
			}
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(this.rat.getId(), this.pos, Direction.UP, 2));
			this.init();
		}).bounds(i - maxLength / 2, j + 135, maxLength, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable(this.isRatHouseAtPos() ? RatsLangConstants.RAT_STAFF_REMOVE_RAT_HOUSE : RatsLangConstants.RAT_STAFF_REMOVE_WORK_ANCHOR), button -> {
			if (!this.isRatHouseAtPos()) {
				this.rat.setWorkAnchorPoint(null);
			}
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(this.rat.getId(), this.pos, Direction.UP, 3));
			this.init();
		}).bounds(i - maxLength / 2, j + 160, maxLength, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable(RatsLangConstants.RAT_STAFF_REMOVE_TRANSPORT_POS), button -> {
			this.rat.setPickupPos(null);
			this.rat.setDepositPos(null);
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(this.rat.getId(), this.pos, Direction.UP, 7));
			this.init();
		}).bounds(i - maxLength / 2, j + 185, maxLength, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable(RatsLangConstants.RAT_STAFF_REMOVE_APPLY_POS), button -> {
			this.rat.setCreateWorkPos(null);
			RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(this.rat.getId(), this.pos, Direction.UP, 9));
			this.init();
		}).bounds(i - maxLength / 2, j + 210, maxLength, 20).build());
		((Button) this.renderables.get(0)).visible = this.canMarkDepositAtPos();
		((Button) this.renderables.get(1)).visible = this.canMarkPickupAtPos();
		((Button) this.renderables.get(2)).visible = this.canMarkApplyAtPos();
		((Button) this.renderables.get(3)).visible = this.canMarkHouseOrWorkAnchor();
		((Button) this.renderables.get(4)).visible = this.canRemoveHouseOrWorkAnchor();
		((Button) this.renderables.get(5)).visible = this.rat.getDepositPos().isPresent() || this.rat.getPickupPos().isPresent();
		((Button) this.renderables.get(6)).visible = this.rat.getCreateWorkPos().isPresent();
	}


	// Checks whether the marked block is a rat house; no parameters, returns true for rat_house.
	private boolean isRatHouseAtPos() {
		try {
			return this.pos != null && this.rat.level().getBlockState(this.pos).getBlock() instanceof RatHouseBlock;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the rat house at {}", this.pos, exception);
			throw exception;
		}
	}

	// Checks visibility of the assign house / work base button; no parameters, returns true if it can be assigned.
	private boolean canMarkHouseOrWorkAnchor() {
		try {
			if (this.isRatHouseAtPos()) {
				return this.rat.getBoundHousePos().isEmpty() || !this.rat.getBoundHousePos().get().pos().equals(this.pos);
			}
			return this.rat.getWorkAnchorPoint().isEmpty() || !this.rat.getWorkAnchorPoint().get().pos().equals(this.pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the staff assign button at {}", this.pos, exception);
			throw exception;
		}
	}

	// Checks visibility of the reset house / work base button; no parameters, returns true if there is something to reset.
	private boolean canRemoveHouseOrWorkAnchor() {
		try {
			if (this.isRatHouseAtPos()) {
				return this.rat.getBoundHousePos().isPresent();
			}
			return this.rat.getWorkAnchorPoint().isPresent();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the staff reset button at {}", this.pos, exception);
			throw exception;
		}
	}

	// Returns the name of the marked block; takes the position from the screen field, returns a string for the buttons.
	private String getPosName() {
		if (this.pos != null) {
			BlockState state = this.rat.level().getBlockState(this.pos);
			List<Component> namelist = null;
			ItemStack pick = state.getBlock().getCloneItemStack(this.rat.level(), this.pos, state);
			try {
				namelist = pick.getTooltipLines(Item.TooltipContext.of(this.rat.level()), Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to get the name of block {} for the cheese staff", this.pos, exception);
				throw exception;
			}
			if (namelist != null && !namelist.isEmpty()) {
				return namelist.get(0).getString();
			}
		}
		return "";
	}

	// Checks whether the position can be marked as a deposit; takes the position from the screen field, returns true for containers and an empty cauldron.
	private boolean canMarkDepositAtPos() {
		try {
			if (this.pos != null) {
				Level level = this.rat.level();
				BlockState state = level.getBlockState(this.pos);
				return level.getBlockEntity(this.pos) != null || state.is(Blocks.CAULDRON);
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the cheese staff deposit point {}", this.pos, exception);
			throw exception;
		}
	}

	// Checks whether the position can be marked as a pickup; takes the position from the screen field, returns true for containers and source fluids.
	private boolean canMarkPickupAtPos() {
		try {
			if (this.pos != null) {
				Level level = this.rat.level();
				BlockState state = level.getBlockState(this.pos);
				FluidState fluidState = state.getFluidState();
				return level.getBlockEntity(this.pos) != null || fluidState.isSource() && !fluidState.isEmpty();
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the cheese staff pickup point {}", this.pos, exception);
			throw exception;
		}
	}

	// Checks whether the position can be marked as a use point; takes the position from the screen field, returns true for any non-empty block.
	private boolean canMarkApplyAtPos() {
		try {
			if (this.pos != null) {
				Level level = this.rat.level();
				return !level.getBlockState(this.pos).isAir();
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the cheese staff use point {}", this.pos, exception);
			throw exception;
		}
	}

	// Draws the cheese staff window; takes graphics, mouse coordinates and partial tick, returns nothing.
	@Override
	public void render(GuiGraphics graphics, int x, int y, float partialTicks) {
		super.renderBackground(graphics, x, y, partialTicks);
		super.render(graphics, x, y, partialTicks);
		int i = (this.width - 248) / 2 + 10;
		int j = (this.height - 166) / 2 + 8;
		if (this.rat != null) {
			EntityRenderingUtil.drawEntityOnScreen(graphics, i + 114, j + 40, 70, 0, 0, this.rat, true);
		}

	}

	// Reports whether the screen pauses the game; no parameters, returns false.
	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
