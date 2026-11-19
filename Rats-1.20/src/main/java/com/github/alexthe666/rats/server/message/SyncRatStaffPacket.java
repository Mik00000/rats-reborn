package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncRatStaffPacket(int entityId, long pos, int facing, int control, int extraData) implements CustomPacketPayload {

	public static final Type<SyncRatStaffPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "sync_rat_staff"));
	public static final StreamCodec<FriendlyByteBuf, SyncRatStaffPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::entityId,
			ByteBufCodecs.VAR_LONG, SyncRatStaffPacket::pos,
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::facing,
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::control,
			ByteBufCodecs.VAR_INT, SyncRatStaffPacket::extraData,
			SyncRatStaffPacket::new
	);

	public SyncRatStaffPacket(int entityId, BlockPos pos, Direction facing, int control) {
		this(entityId, pos.asLong(), facing.ordinal(), control, 0);
	}

	public SyncRatStaffPacket(int entityId, BlockPos pos, Direction facing, int control, int extraData) {
		this(entityId, pos.asLong(), facing.ordinal(), control, extraData);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Handler {
		// Handles a rat staff command; takes the packet and network context, returns nothing.
		public static void handle(SyncRatStaffPacket packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = context.player();
				if (player != null) {
					Entity e = player.level().getEntity(packet.entityId());
					if (e instanceof TamedRat rat) {
						if (!canPlayerControlRat(player, rat, packet.control())) {
							return;
						}
						Direction direction = readPacketDirection(packet);
						switch (packet.control()) {
							case 0 -> {
								rat.setDepositPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.depositFacing = direction;
							}
							case 1 -> {
								rat.setPickupPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.pickupFacing = direction;
							}
							case 2 -> markHomeOrHouse(player, rat, BlockPos.of(packet.pos()));
							case 3 -> clearHomeOrHouse(player, rat, BlockPos.of(packet.pos()));
							case 4 -> rat.setRadiusCenter(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
							case 5 -> rat.setRadius(Math.max(1, packet.extraData()));
							case 6 -> {
								rat.setRadiusCenter(null);
								rat.setRadius(RatConfig.defaultRatRadius);
							}
							case 7 -> {
								rat.setPickupPos(null);
								rat.setDepositPos(null);
							}
							case 8 -> {
								rat.setCreateWorkPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.createWorkFacing = direction;
							}
							case 9 -> rat.setCreateWorkPos(null);
							default -> RatsMod.LOGGER.warn("Player {} sent unknown staff action {} for rat {}", player.getGameProfile().getName(), packet.control(), rat.getUUID());
						}
					}
				}
			});
		}


		// Marks a regular home point or binds a rat_house; takes the player, rat and position, returns nothing.
		private static void markHomeOrHouse(Player player, TamedRat rat, BlockPos pos) {
			try {
				GlobalPos target = GlobalPos.of(player.level().dimension(), pos);
				if (player.level().getBlockState(pos).getBlock() instanceof RatHouseBlock) {
					if (rat.getBoundHousePos().isPresent() && !rat.getBoundHousePos().get().equals(target)) {
						GlobalPos oldHouse = rat.getBoundHousePos().get();
						if (oldHouse.dimension().equals(player.level().dimension()) && player.level().getBlockState(oldHouse.pos()).getBlock() instanceof RatHouseBlock) {
							player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_BOUND_ELSEWHERE), true);
							RatsMod.LOGGER.warn("Player {} tried to rebind rat {} from house {} to {}", player.getGameProfile().getName(), rat.getUUID(), oldHouse.pos(), pos);
							return;
						}
					}
					BlockEntity blockEntity = player.level().getBlockEntity(pos);
					if (!(blockEntity instanceof RatHouseBlockEntity house)) {
						throw new IllegalStateException("rat_house has no RatHouseBlockEntity at " + pos);
					}
					house.bindRat(rat, player);
					return;
				}
				rat.setWorkAnchorPoint(target);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to mark house or work base {} for rat {} by player {}", pos, rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}


		// Resets the work base or bound rat_house; takes the player, rat and staff screen position, returns nothing.
		private static void clearHomeOrHouse(Player player, TamedRat rat, BlockPos pos) {
			try {
				if (!(player.level().getBlockState(pos).getBlock() instanceof RatHouseBlock)) {
					rat.setWorkAnchorPoint(null);
					return;
				}
				if (rat.getBoundHousePos().isEmpty()) {
					return;
				}
				GlobalPos housePos = rat.getBoundHousePos().get();
				if (!housePos.dimension().equals(player.level().dimension())) {
					rat.setBoundHousePos(null);
					return;
				}
				BlockEntity blockEntity = player.level().getBlockEntity(housePos.pos());
				if (blockEntity instanceof RatHouseBlockEntity house && house.clearBinding(rat, player)) {
					return;
				}
				rat.setBoundHousePos(null);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to reset the house or work base of rat {} by player {}", rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}

		// Checks the owner before using the staff; takes the player, rat and action code, returns true if allowed.
		private static boolean canPlayerControlRat(Player player, TamedRat rat, int control) {
			try {
				if (rat.getOwnerUUID() == null) {
					RatsMod.LOGGER.warn("Player {} tried to use the staff on rat {} that has no owner, action {}", player.getGameProfile().getName(), rat.getUUID(), control);
					return false;
				}
				if (!rat.isOwnedBy(player)) {
					RatsMod.LOGGER.warn("Player {} tried to use the staff on someone else's rat {} owned by {}, action {}", player.getGameProfile().getName(), rat.getUUID(), rat.getOwnerUUID(), control);
					return false;
				}
				return true;
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check the owner of rat {} for the staff of player {}", rat.getUUID(), player.getGameProfile().getName(), exception);
				throw exception;
			}
		}

		// Reads a side from the packet; takes the packet, returns a valid direction or throws an explicit error.
		private static Direction readPacketDirection(SyncRatStaffPacket packet) {
			try {
				Direction[] directions = Direction.values();
				int facing = packet.facing();
				if (facing < 0 || facing >= directions.length) {
					throw new IllegalArgumentException("Invalid side in the staff packet: " + facing);
				}
				return directions[facing];
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to read the side from the staff packet for entity {}", packet.entityId(), exception);
				throw exception;
			}
		}
	}
}
