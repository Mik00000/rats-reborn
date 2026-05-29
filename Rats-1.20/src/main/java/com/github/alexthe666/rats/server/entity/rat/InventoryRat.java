package com.github.alexthe666.rats.server.entity.rat;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsDataSerializerRegistry;
import com.github.alexthe666.rats.server.misc.RatPersistenceHelper;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import com.github.alexthe666.rats.server.items.RatStaffItem;
import com.github.alexthe666.rats.server.message.OpenRatScreenPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class InventoryRat extends DiggingRat implements ContainerListener {

	private static final EntityDataAccessor<Integer> COMMAND = SynchedEntityData.defineId(InventoryRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<GlobalPos>> RADIUS_CENTER = SynchedEntityData.defineId(InventoryRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
	private static final EntityDataAccessor<Optional<GlobalPos>> HOME_POS = SynchedEntityData.defineId(InventoryRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
	private static final EntityDataAccessor<Integer> SEARCH_RADIUS = SynchedEntityData.defineId(InventoryRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<List<GlobalPos>> PATROL_NODES = SynchedEntityData.defineId(InventoryRat.class, RatsDataSerializerRegistry.GLOBAL_POS_LIST.get());
	private static final EntityDataAccessor<Byte> VISIBILITY_FLAGS = SynchedEntityData.defineId(InventoryRat.class, EntityDataSerializers.BYTE);

	private RatContainer inventory;
	private IItemHandler itemHandler = null;
	private boolean inventoryOpen;
	@Nullable
	private ListTag pendingInventoryTags;

	protected InventoryRat(EntityType<? extends TamableAnimal> type, Level level) {
		super(type, level);
		this.createInventory();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COMMAND, 0);
		builder.define(RADIUS_CENTER, Optional.empty());
		builder.define(HOME_POS, Optional.empty());
		builder.define(SEARCH_RADIUS, RatConfig.defaultRatRadius);
		builder.define(PATROL_NODES, new ArrayList<>());
		builder.define(VISIBILITY_FLAGS, (byte) 0);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		ListTag listtag = new ListTag();
		for (int i = 0; i < this.getInventory().getContainerSize(); ++i) {
			ItemStack itemstack = this.getInventory().getItem(i);
			if (!itemstack.isEmpty()) {
				CompoundTag compoundtag = RatsItemStackData.save(this.registryAccess(), itemstack);
				compoundtag.putByte("Slot", (byte) i);
				listtag.add(compoundtag);
			}
		}
		tag.put("Items", listtag);
		tag.putByte("InvisibleSlots", this.getEntityData().get(VISIBILITY_FLAGS));

		this.getWorkAnchorPoint().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("WorkAnchorPos", tag1));

		this.getRadiusCenter().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("RadiusPos", tag1));
		tag.putInt("SearchRadius", this.getRadius());

		if (!this.getPatrolNodes().isEmpty()) {
			ListTag listTag = new ListTag();
			this.getPatrolNodes().forEach(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error).ifPresent(listTag::add));
			tag.put("PatrolNodesTag", listTag);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Items", Tag.TAG_LIST)) {
			this.pendingInventoryTags = tag.getList("Items", Tag.TAG_COMPOUND).copy();
		}

		this.getEntityData().set(VISIBILITY_FLAGS, tag.getByte("InvisibleSlots"));
		if (tag.contains("WorkAnchorPos")) {
			this.setWorkAnchorPoint(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("WorkAnchorPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		} else if (tag.contains("HomePos")) {
			this.setWorkAnchorPoint(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("HomePos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		}
		if (tag.contains("RadiusPos")) {
			this.setRadiusCenter(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("RadiusPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		}
		this.setRadius(tag.getInt("SearchRadius"));

		if (tag.contains("PatrolNodesTag")) {
			ListTag listTag = tag.getList("PatrolNodesTag", Tag.TAG_COMPOUND);

			for (Tag pos : listTag) {
				GlobalPos.CODEC.parse(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error).ifPresent(globalPos -> this.getPatrolNodes().add(globalPos));
			}
		}
		this.tryReloadPendingSaveData();
	}

	@Override
	public void onAddedToLevel() {
		super.onAddedToLevel();
		this.tryReloadPendingSaveData();
	}

	// Перезагружает отложенный NBT, если мир и реестры уже доступны.
	public void tryReloadPendingSaveData() {
		try {
			boolean hadItems = this.pendingInventoryTags != null;
			this.loadPendingInventory();
			if (this instanceof TamedRat tamed) {
				boolean hadUpgrades = tamed.hasPendingUpgradeTags();
				tamed.tryReloadPendingUpgradeTags();
				if (hadItems || hadUpgrades) {
					RatPersistenceHelper.logLoadResult(tamed, hadItems, hadUpgrades);
				}
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось перезагрузить отложенные данные крысы {}: {}", this.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Загружает инвентарь крысы после появления в мире, когда реестры уже доступны.
	private void loadPendingInventory() {
		try {
			if (this.pendingInventoryTags == null || this.level() == null || this.getInventory() == null) {
				return;
			}
			var provider = this.level().registryAccess();
			for (int i = 0; i < this.pendingInventoryTags.size(); ++i) {
				CompoundTag compoundtag = this.pendingInventoryTags.getCompound(i);
				int slot = compoundtag.getByte("Slot") & 255;
				if (slot < this.getInventory().getContainerSize()) {
					ItemStack stack = ItemStack.parse(provider, compoundtag).orElse(ItemStack.EMPTY);
					if (stack.isEmpty()) {
						stack = RatsItemStackData.parse(provider, compoundtag);
					}
					this.getInventory().setItem(slot, stack);
				}
			}
			this.pendingInventoryTags = null;
			if (this instanceof TamedRat tamed) {
				tamed.scheduleUpgradeRefreshAfterLoad();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось загрузить отложенный инвентарь крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	private void createInventory() {
		SimpleContainer simplecontainer = this.getInventory();
		this.inventory = new RatContainer(this, 6);
		if (simplecontainer != null) {
			simplecontainer.removeListener(this);
			int i = Math.min(simplecontainer.getContainerSize(), this.getInventory().getContainerSize());

			for (int j = 0; j < i; ++j) {
				ItemStack itemstack = simplecontainer.getItem(j);
				if (!itemstack.isEmpty()) {
					this.getInventory().setItem(j, itemstack.copy());
				}
			}
		}

		this.getInventory().addListener(this);
		this.itemHandler = new InvWrapper(this.getInventory());
	}

	// Возвращает обработчик инвентаря крысы для NeoForge capability; параметры не принимает, отдаёт IItemHandler.
	public IItemHandler getItemHandler() {
		try {
			if (!this.isAlive()) {
				throw new IllegalStateException("Нельзя получить capability инвентаря мёртвой крысы");
			}
			if (this.itemHandler == null) {
				throw new IllegalStateException("Capability инвентаря крысы не создан");
			}
			return this.itemHandler;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить capability инвентаря крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	@Override
	public boolean canMove() {
		return super.canMove() && this.getCommand().freeMove && !this.inventoryOpen;
	}

	@Override
	public ItemStack getItemBySlot(EquipmentSlot slot) {
		if (this.getInventory() != null) {
			if (slot == EquipmentSlot.MAINHAND) {
				return this.getInventory().getItem(0);
			} else if (slot == EquipmentSlot.HEAD) {
				return this.getInventory().getItem(1);
			} else if (slot == EquipmentSlot.OFFHAND) {
				return this.getInventory().getItem(2);
			} else if (slot == EquipmentSlot.CHEST) {
				return this.getInventory().getItem(3);
			} else if (slot == EquipmentSlot.LEGS) {
				return this.getInventory().getItem(4);
			} else {
				return slot == EquipmentSlot.FEET ? this.getInventory().getItem(5) : super.getItemBySlot(slot);
			}
		}
		return super.getItemBySlot(slot);
	}

	// Устанавливает предмет в слот экипировки крысы; при смене улучшения (грудь/ноги/стопы) вызывает onUpgradeChanged на сервере.
	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		ItemStack previous = this.getItemBySlot(slot);
		if (this.getInventory() != null) {
			if (slot == EquipmentSlot.MAINHAND) {
				this.getInventory().setItem(0, stack);
			} else if (slot == EquipmentSlot.HEAD) {
				this.getInventory().setItem(1, stack);
			} else if (slot == EquipmentSlot.OFFHAND) {
				this.getInventory().setItem(2, stack);
			} else if (slot == EquipmentSlot.CHEST) {
				this.getInventory().setItem(3, stack);
			} else if (slot == EquipmentSlot.LEGS) {
				this.getInventory().setItem(4, stack);
			} else if (slot == EquipmentSlot.FEET) {
				this.getInventory().setItem(5, stack);
			} else {
				super.setItemSlot(slot, stack);
			}
		} else {
			super.setItemSlot(slot, stack);
		}
		if (this instanceof TamedRat tamed && tamed.isAlive() && !tamed.level().isClientSide()
				&& (slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET)
				&& !ItemStack.matches(previous, stack)) {
			tamed.onUpgradeChanged();
		}
	}

	public void openGUI(Player player) {
		if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
			if (sp.containerMenu != sp.inventoryMenu) {
				sp.closeContainer();
			}
			sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
					(windowId, inv, p) -> new RatMenu(windowId, this.getInventory(), inv),
					this.getDisplayName()
			), buf -> buf.writeVarInt(this.getId()));
			this.inventoryOpen = true;
			NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(sp, sp.containerMenu));
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		if (!player.isShiftKeyDown() && this.isOwnedBy(player) && !(stack.getItem() instanceof RatStaffItem) && !(player.getItemInHand(otherHand).getItem() instanceof RatStaffItem)) {
			this.openGUI(player);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Nullable
	public SimpleContainer getInventory() {
		return this.inventory;
	}

	public void closeInventory() {
		this.inventoryOpen = false;
	}

	//command logic

	public int getCommandInteger() {
		return this.getEntityData().get(COMMAND);
	}

	public void setCommandInteger(int command) {
		if (!this.level().isClientSide() && command != this.getCommandInteger()) {
			this.getNavigation().stop();
			this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
			if (this instanceof TamedRat rat) rat.crafting = false;
		}
		this.getEntityData().set(COMMAND, command);
		this.setOrderedToSit(command == RatCommand.SIT.ordinal());
	}

	public RatCommand getCommand() {
		return RatCommand.values()[Mth.clamp(this.getCommandInteger(), 0, RatCommand.values().length - 1)];
	}

	public void setCommand(RatCommand command) {
		this.setCommandInteger(command.ordinal());
	}

	public boolean isFollowing() {
		return this.getCommandInteger() == 2;
	}

	public boolean isTargetCommand() {
		return this.getCommandInteger() == 4 || this.getCommandInteger() == 5;
	}

	public boolean shouldHunt() {
		return this.getCommandInteger() == 3 || this.isPatrolCommand();
	}

	public boolean shouldWander() {
		return this.getCommand().allowsWandering;
	}

	public boolean isAttackCommand() {
		return this.getCommand().allowsAttacking;
	}

	public boolean isPatrolCommand() {
		return this.getCommand() == RatCommand.PATROL;
	}

	/**
	 * Заменяет маршрут патруля и синхронизирует список с клиентами.
	 * Параметры: список GlobalPos точек маршрута. Ничего не возвращает.
	 */
	public void setPatrolNodes(List<GlobalPos> nodes) {
		this.getEntityData().set(PATROL_NODES, new ArrayList<>(nodes));
	}

	// Возвращает рабочую базу крысы; параметров нет, отдаёт глобальную позицию, если она назначена.
	public Optional<GlobalPos> getWorkAnchorPoint() {
		try {
			return this.getEntityData().get(HOME_POS);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить рабочую базу крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Задаёт рабочую базу крысы; принимает глобальную позицию или null, ничего не отдаёт.
	public void setWorkAnchorPoint(@Nullable GlobalPos workAnchor) {
		try {
			this.getEntityData().set(HOME_POS, Optional.ofNullable(workAnchor));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось задать рабочую базу крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает старую домашнюю точку как рабочую базу; оставлено для совместимости старого кода.
	public Optional<GlobalPos> getHomePoint() {
		return this.getWorkAnchorPoint();
	}

	// Задаёт старую домашнюю точку как рабочую базу; оставлено для миграции и совместимости.
	public void setHomePoint(@Nullable GlobalPos home) {
		this.setWorkAnchorPoint(home);
	}

	// Возвращает радиус поиска обычных рабочих целей; параметров нет, отдаёт радиус посоха или безопасное значение по умолчанию.
	public int getRadius() {
		return this.getRadiusCenter().isPresent() ? this.getEntityData().get(SEARCH_RADIUS) : RatConfig.defaultRatRadius;
	}

	public void setRadius(int radius) {
		this.getEntityData().set(SEARCH_RADIUS, radius);
	}

	public Optional<GlobalPos> getRadiusCenter() {
		return this.getEntityData().get(RADIUS_CENTER);
	}

	public void setRadiusCenter(@Nullable GlobalPos pos) {
		this.getEntityData().set(RADIUS_CENTER, Optional.ofNullable(pos));
	}

	public BlockPos getSearchCenter() {
		if (this.getRadiusCenter().isEmpty()) {
			return this.blockPosition();
		} else {
			return this.getRadiusCenter().get().pos();
		}
	}

	public List<GlobalPos> getPatrolNodes() {
		return this.getEntityData().get(PATROL_NODES);
	}

	@Override
	public void containerChanged(Container container) {

	}

	public boolean isSlotVisible(EquipmentSlot slot) {
		return (this.getEntityData().get(VISIBILITY_FLAGS) & this.getByteValueForSlot(slot)) == 0;
	}

	public void setSlotVisibility(EquipmentSlot slot, boolean visible) {
		if (visible) {
			this.getEntityData().set(VISIBILITY_FLAGS, (byte) (this.getEntityData().get(VISIBILITY_FLAGS) & ~this.getByteValueForSlot(slot)));
		} else {
			this.getEntityData().set(VISIBILITY_FLAGS, (byte) (this.getEntityData().get(VISIBILITY_FLAGS) | this.getByteValueForSlot(slot)));
		}
	}

	private int getByteValueForSlot(EquipmentSlot slot) {
		return switch (slot) {
			case FEET -> 1;
			case LEGS -> 2;
			case CHEST -> 4;
			default -> 0;
		};
	}
}
