package com.github.alexthe666.rats.server.entity.projectile;

import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RatArrow extends AbstractArrow {

	private final ItemStack stack;
	@Nullable
	private IntOpenHashSet ratPiercingIgnoreIds;
	@Nullable
	private List<LivingEntity> ratPiercedEntities;

	public RatArrow(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
		this.stack = new ItemStack(RatsItemRegistry.RAT_ARROW.get());
	}

	public RatArrow(EntityType<? extends AbstractArrow> type, Level level, LivingEntity shooter, ItemStack stack) {
		super(type, shooter, level, stack, stack);
		this.stack = stack;
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(Items.ARROW);
	}

	private void spawnRat(@Nullable Entity entity, BlockPos pos) {
		if (this.pickup == Pickup.ALLOWED) {
			TamedRat rat = new TamedRat(RatsEntityRegistry.TAMED_RAT.get(), this.level());
			CompoundTag ratTag = new CompoundTag();
			CustomData customData = this.stack.get(DataComponents.CUSTOM_DATA);
			if (customData != null) {
				CompoundTag tag = customData.copyTag();
				if (tag.contains("Rat") && !tag.getCompound("Rat").isEmpty()) {
					ratTag = tag.getCompound("Rat");
				}
			}
			rat.readAdditionalSaveData(ratTag);
			if (!ratTag.getString("CustomName").isEmpty()) {
				rat.setCustomName(Component.Serializer.fromJson(ratTag.getString("CustomName"), this.level().registryAccess()));
			}
			if (ratTag.isEmpty()) {
				EventHooks.finalizeMobSpawn(rat, (ServerLevelAccessor) this.level(), this.level().getCurrentDifficultyAt(rat.blockPosition()), MobSpawnType.EVENT, null);
				if (this.getOwner() instanceof Player player) {
					rat.tame(player);
				}
			}
			rat.setCommand(RatCommand.WANDER);
			rat.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
			if (!this.level().isClientSide()) {
				this.level().addFreshEntity(rat);
			}
			if (entity instanceof LivingEntity living && !rat.isAlliedTo(entity)) {
				rat.setTarget(living);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		Entity entity = result.getEntity();
		float f = (float) this.getDeltaMovement().length();
		int i = Mth.ceil(Mth.clamp((double) f * this.getBaseDamage(), 0, Integer.MAX_VALUE));
		if (this.getPierceLevel() > 0) {
			if (this.ratPiercingIgnoreIds == null) {
				this.ratPiercingIgnoreIds = new IntOpenHashSet(5);
			}

			if (this.ratPiercedEntities == null) {
				this.ratPiercedEntities = Lists.newArrayListWithCapacity(5);
			}

			if (this.ratPiercingIgnoreIds.size() >= this.getPierceLevel() + 1) {
				this.discard();
				return;
			}

			this.ratPiercingIgnoreIds.add(entity.getId());
		}

		if (this.isCritArrow()) {
			long j = this.random.nextInt(i / 2 + 2);
			i = (int) Math.min(j + (long) i, 2147483647L);
		}

		Entity entity1 = this.getOwner();
		DamageSource damagesource;
		if (entity1 == null) {
			damagesource = this.damageSources().arrow(this, this);
		} else {
			damagesource = this.damageSources().arrow(this, entity1);
			if (entity1 instanceof LivingEntity) {
				((LivingEntity) entity1).setLastHurtMob(entity);
			}
		}

		boolean flag = entity.getType() == EntityType.ENDERMAN;
		int k = entity.getRemainingFireTicks();
		if (this.isOnFire() && !flag) {
			entity.igniteForSeconds(5);
		}

		if (entity.hurt(damagesource, (float) i)) {
			if (flag) {
				return;
			}

			if (!this.level().isClientSide()) {
				this.spawnRat(entity, entity.blockPosition());
			}

			if (entity instanceof LivingEntity livingentity) {
				if (!this.level().isClientSide() && this.getPierceLevel() <= 0) {
					livingentity.setArrowCount(livingentity.getArrowCount() + 1);
				}

				this.doPostHurtEffects(livingentity);
				if (livingentity != entity1 && livingentity instanceof Player && entity1 instanceof ServerPlayer player && !this.isSilent()) {
					player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
				}

				if (!entity.isAlive() && this.ratPiercedEntities != null) {
					this.ratPiercedEntities.add(livingentity);
				}

				if (!this.level().isClientSide() && entity1 instanceof ServerPlayer serverplayer) {
					if (this.ratPiercedEntities != null && this.shotFromCrossbow()) {
						CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayer, this.ratPiercedEntities.stream().map(e -> (Entity) e).toList());
					} else if (!entity.isAlive() && this.shotFromCrossbow()) {
						CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayer, List.of(entity));
					}
				}
			}

			this.playSound(this.getHitGroundSoundEvent(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
			if (this.getPierceLevel() <= 0) {
				this.discard();
			}
		} else {
			entity.setRemainingFireTicks(k);
			this.setDeltaMovement(this.getDeltaMovement().scale(-0.1D));
			this.setYRot(this.getYRot() + 180.0F);
			this.yRotO += 180.0F;
			if (!this.level().isClientSide() && this.getDeltaMovement().lengthSqr() < 1.0E-7D) {
				if (this.pickup == Pickup.ALLOWED) {
					this.spawnAtLocation(this.getPickupItem(), 0.1F);
				}

				this.discard();
			}
		}

	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (this.inGround) {
			if (!this.level().isClientSide()) {
				this.spawnRat(null, result.getBlockPos().relative(result.getDirection()));
				this.spawnAtLocation(this.getPickupItem(), 0.0F);
			}
			this.discard();
		}
	}
}
