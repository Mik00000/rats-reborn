package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.util.EntityRenderingUtil;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import com.github.alexthe666.rats.server.message.SetRatNamePacket;
import com.github.alexthe666.rats.server.message.RatCommandPacket;
import com.github.alexthe666.rats.server.message.RatUpgradeVisibilityPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

public class RatScreen extends AbstractContainerScreen<RatMenu> {
	protected static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/gui/container/rat_inventory.png");
	private static final ResourceLocation TEXTURE_BACKDROP = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "textures/gui/container/rat_inventory_backdrop.png");
	private static final int STATS_PANEL_WIDTH = 172;
	private static final int STATS_PANEL_HEIGHT = 184;
	private static final int STATS_PANEL_GAP = 8;
	private static final int STATS_PANEL_BACKGROUND = 0xF018120D;
	private static final int STATS_PANEL_BORDER = 0xFF7A5637;
	private static final int STATS_PANEL_TEXT = 0xFFE9DDC8;
	private static final int STATS_PANEL_MUTED_TEXT = 0xFFBDAF98;
	private static final int STATS_BUTTON_WIDTH = 64;
	private static final int STATS_BUTTON_HEIGHT = 18;
	private static final String[] RANDOM_MALE_FIRST_NAMES = {
			"Сырник", "Хвостик", "Шустрик", "Грызлик", "Мурчик", "Пончик", "Тихоня", "Рыжик", "Снежок", "Уголёк",
			"Фунтик", "Шуршик", "Малыш", "Перчик", "Орех", "Пухляш", "Зефир", "Топотун", "Сверчок", "Бублик",
			"Черныш", "Кузя", "Тимка", "Филя", "Прошка", "Яшка", "Борька", "Гоша", "Семён", "Арчи"
	};
	private static final String[] RANDOM_FEMALE_FIRST_NAMES = {
			"Пищалка", "Крошка", "Пуговка", "Искорка", "Бусинка", "Клёпа", "Пружинка", "Марта", "Сима", "Лапка",
			"Нюша", "Тучка", "Комета", "Вафля", "Белка", "Кнопка", "Слива", "Мята", "Жужа", "Мила",
			"Соня", "Люся", "Дуся", "Фрося", "Муся", "Нора", "Рада", "Тося", "Шуша", "Лиса"
	};
	private static final String[] RANDOM_MALE_LAST_NAMES = {
			"Лапкин", "Сыров", "Норкин", "Хвостов", "Грызунов", "Писклявин", "Крошкин", "Зёрнышкин", "Полевой", "Подвальный",
			"Кладовкин", "Шуршаков", "Пружинкин", "Мягколапов", "Тонкоусый", "Усатов", "Быстроногов", "Сыролюбов", "Ночкин", "Домиков",
			"Мешочкин", "Кормушкин", "Складов", "Крупин", "Лесной", "Берёзкин", "Дубков", "Орехов", "Пшеничкин", "Славный"
	};
	private static final String[] RANDOM_FEMALE_LAST_NAMES = {
			"Лапкина", "Сырова", "Норкина", "Хвостова", "Грызунова", "Писклявина", "Крошкина", "Зёрнышкина", "Полевая", "Подвальная",
			"Кладовкина", "Шуршакова", "Пружинкина", "Мягколапова", "Тонкоусая", "Усатова", "Быстроногова", "Сыролюбова", "Ночкина", "Домикова",
			"Мешочкина", "Кормушкина", "Складова", "Крупина", "Лесная", "Берёзкина", "Дубкова", "Орехова", "Пшеничкина", "Славная"
	};
	private int currentDisplayCommand = 0;
	private final TamedRat rat;
	private final Random random = new Random();
	private boolean statsTab;
	private EditBox nameBox;

	/**
	 * Конструктор для RegisterMenuScreens: id крысы берётся из RatMenu (fromNetwork).
	 * Параметры: меню, инвентарь игрока, заголовок (не используется). Делегирует в конструктор с TamedRat.
	 */
	public RatScreen(RatMenu container, Inventory inv, Component unusedTitle) {
		this(container, inv, resolveTamedRat(container, inv));
	}

	public static RatScreen forRat(RatMenu container, Inventory inv, TamedRat rat) {
		return new RatScreen(container, inv, rat);
	}

	private RatScreen(RatMenu container, Inventory inv, TamedRat rat) {
		super(container, inv, rat.getDisplayName());
		this.rat = rat;
		this.imageWidth = 192;
		this.imageHeight = 166;
	}

	private static TamedRat resolveTamedRat(RatMenu menu, Inventory inv) {
		if (menu.getRatEntityId() >= 0 && inv.player.level().getEntity(menu.getRatEntityId()) instanceof TamedRat rat) {
			return rat;
		}
		RatsMod.LOGGER.error("Экран крысы: сущность не найдена, ratEntityId={}", menu.getRatEntityId());
		throw new IllegalStateException("Сущность крысы для экрана инвентаря не найдена на клиенте");
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(graphics, mouseX, mouseY, partialTicks);
		super.render(graphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void init() {
		super.init();
		this.renderables.clear();
		this.nameBox = null;
		int i = (this.width - 248) / 2;
		int j = (this.height - 166) / 2;
		this.addRenderableWidget(Button.builder(Component.literal(this.statsTab ? "Скрыть" : "Статы"), button -> {
			this.statsTab = !this.statsTab;
			this.init();
		}).bounds(this.getStatsButtonX(), this.getStatsButtonY(), STATS_BUTTON_WIDTH, STATS_BUTTON_HEIGHT).build());
		if (this.statsTab) {
			int panelX = this.getStatsPanelX();
			int panelY = this.getStatsPanelY();
			this.nameBox = new EditBox(this.font, panelX + 10, panelY + 32, STATS_PANEL_WIDTH - 20, 18, Component.literal("Имя крысы"));
			this.nameBox.setMaxLength(48);
			this.nameBox.setValue(this.getCurrentName());
			this.addRenderableWidget(this.nameBox);
			this.addRenderableWidget(Button.builder(Component.literal("Применить"), button -> this.applyRatName()).bounds(panelX + 10, panelY + 54, 74, 18).build());
			this.addRenderableWidget(Button.builder(Component.literal("Случайно"), button -> this.pickRandomName()).bounds(panelX + 88, panelY + 54, 74, 18).build());
		}
		if (!this.rat.isBaby()) {
			this.addRenderableWidget(new ChangeCommandButton(i + 116, j + 54, false, button -> {
				this.currentDisplayCommand--;
				this.currentDisplayCommand = RatUtils.wrapCommand(this.currentDisplayCommand).ordinal();
			}));
			this.addRenderableWidget(new ChangeCommandButton(i + 199, j + 54, true, button -> {
				this.currentDisplayCommand++;
				this.currentDisplayCommand = RatUtils.wrapCommand(this.currentDisplayCommand).ordinal();
			}));
			this.addRenderableWidget(new CommandPressButton(i + 123, j + 52, button -> {
				this.rat.setCommand(RatCommand.values()[this.currentDisplayCommand]);
				RatsNetworkHandler.CHANNEL.sendToServer(new RatCommandPacket(this.rat.getId(), this.currentDisplayCommand));
			}));
			this.addRenderableWidget(new CommandPressButton(i + 123, j + 52, button -> {
				this.rat.setCommand(RatCommand.values()[this.currentDisplayCommand]);
				RatsNetworkHandler.CHANNEL.sendToServer(new RatCommandPacket(this.rat.getId(), this.currentDisplayCommand));
			}));

			this.addRenderableWidget(new UpgradeVisibilityButton(i + 39, j + 15, this.rat.isSlotVisible(EquipmentSlot.CHEST), button -> {
				((UpgradeVisibilityButton) button).toggleVisibility();
				this.rat.setSlotVisibility(EquipmentSlot.CHEST, ((UpgradeVisibilityButton) button).getUpgradeVisibility());
				RatsNetworkHandler.CHANNEL.sendToServer(new RatUpgradeVisibilityPacket(this.rat.getId(), EquipmentSlot.CHEST, ((UpgradeVisibilityButton) button).getUpgradeVisibility()));
			}));
			this.addRenderableWidget(new UpgradeVisibilityButton(i + 39, j + 33, this.rat.isSlotVisible(EquipmentSlot.LEGS), button -> {
				((UpgradeVisibilityButton) button).toggleVisibility();
				this.rat.setSlotVisibility(EquipmentSlot.LEGS, ((UpgradeVisibilityButton) button).getUpgradeVisibility());
				RatsNetworkHandler.CHANNEL.sendToServer(new RatUpgradeVisibilityPacket(this.rat.getId(), EquipmentSlot.LEGS, ((UpgradeVisibilityButton) button).getUpgradeVisibility()));
			}));
			this.addRenderableWidget(new UpgradeVisibilityButton(i + 39, j + 51, this.rat.isSlotVisible(EquipmentSlot.FEET), button -> {
				((UpgradeVisibilityButton) button).toggleVisibility();
				this.rat.setSlotVisibility(EquipmentSlot.FEET, ((UpgradeVisibilityButton) button).getUpgradeVisibility());
				RatsNetworkHandler.CHANNEL.sendToServer(new RatUpgradeVisibilityPacket(this.rat.getId(), EquipmentSlot.FEET, ((UpgradeVisibilityButton) button).getUpgradeVisibility()));
			}));

		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		int k = (this.width - this.imageWidth) / 2;
		int l = (this.height - this.imageHeight) / 2;
		graphics.blit(TEXTURE_BACKDROP, k - 8, l, 0, 0, this.imageWidth, this.imageHeight);
		EntityRenderingUtil.drawEntityOnScreen(graphics, k + 42, l + 64, 70, k + 51 - (float) mouseX, l + 75 - 50 - (float) mouseY, this.rat, false);
		graphics.blit(TEXTURE, k - 8, l, 0, 0, this.imageWidth, this.imageHeight);
		graphics.blit(TEXTURE, k + 52, l + 20, this.rat.isMale() ? 0 : 16, 209, 16, 16);
		if (this.statsTab) {
			this.renderStatsPanel(graphics);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.getTitle(), this.imageWidth / 2 - this.font.width(this.getTitle()) / 2, 6, 4210752, false);

		Component commandDesc = Component.translatable(RatsLangConstants.RAT_CURRENT_COMMAND);
		graphics.drawString(this.font, commandDesc, this.imageWidth / 2 - this.font.width(commandDesc) / 2 + 38, 19, 4210752, false);

		Component command = Component.translatable(rat.getCommand().getTranslateName());
		graphics.drawString(this.font, command, this.imageWidth / 2 - this.font.width(command) / 2 + 36, 31, 0XFFFFFF, false);

		Component statusDesc = Component.translatable(RatsLangConstants.RAT_COMMAND_SET);
		graphics.drawString(this.font, statusDesc, this.imageWidth / 2 - this.font.width(statusDesc) / 2 + 36, 44, 4210752, false);
		RatCommand command1 = RatUtils.wrapCommand(currentDisplayCommand);
		Component command2 = Component.translatable(command1.getTranslateName());
		graphics.drawString(this.font, command2, this.imageWidth / 2 - this.font.width(command2) / 2 + 36, 56, 0XFFFFFF, false);
		int i = (this.width - 248) / 2;
		int j = (this.height - 166) / 2;
		if (mouseX > i + 116 && mouseX < i + 198 && mouseY > j + 22 && mouseY < j + 45) {
			MutableComponent commandText = Component.translatable(this.rat.getCommand().getTranslateDescription());
			String[] everySpace = commandText.getString().split(" ");
			int currentStrLength = 0;
			StringBuilder builtString = new StringBuilder();
			ArrayList<String> list = new ArrayList<>();
			for (String s : everySpace) {
				builtString.append(s).append(" ");
				currentStrLength += this.font.width(s + " ");
				if (currentStrLength >= 95) {
					list.add(builtString.toString());
					builtString = new StringBuilder();
					currentStrLength = 0;
				}
			}
			List<MutableComponent> convertedList = new ArrayList<>();
			for (String str : list) {
				convertedList.add(Component.literal(str));
			}
			graphics.renderTooltip(this.font, Lists.transform(convertedList, Component::getVisualOrderText), mouseX - i - 30, mouseY - j + 10);
		}
		if (mouseX > i + 116 && mouseX < i + 198 && mouseY > j + 53 && mouseY < j + 69) {
			MutableComponent commandText = Component.translatable(command1.getTranslateDescription());
			graphics.renderTooltip(this.font, this.font.split(commandText, 110), mouseX - i - 30, mouseY - j + 10);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.nameBox != null && this.nameBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
			this.applyRatName();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	// Возвращает текущее пользовательское имя крысы; параметров нет, отдаёт строку для поля ввода.
	private String getCurrentName() {
		try {
			return this.rat.hasCustomName() && this.rat.getCustomName() != null ? this.rat.getCustomName().getString() : "";
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить текущее имя крысы {} для GUI", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Отправляет новое имя крысы на сервер; параметров нет, ничего не отдаёт.
	private void applyRatName() {
		try {
			if (this.nameBox == null) {
				throw new IllegalStateException("Поле имени крысы не создано");
			}
			String name = this.nameBox.getValue().trim();
			RatsNetworkHandler.CHANNEL.sendToServer(new SetRatNamePacket(this.rat.getId(), name));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось применить имя крысы {} из GUI", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Выбирает случайное имя и отправляет его на сервер; параметров нет, ничего не отдаёт.
	private void pickRandomName() {
		try {
			if (this.nameBox == null) {
				throw new IllegalStateException("Поле имени крысы не создано");
			}
			String[] firstNames = this.rat.isMale() ? RANDOM_MALE_FIRST_NAMES : RANDOM_FEMALE_FIRST_NAMES;
			String[] lastNames = this.rat.isMale() ? RANDOM_MALE_LAST_NAMES : RANDOM_FEMALE_LAST_NAMES;
			String name = firstNames[this.random.nextInt(firstNames.length)] + " " + lastNames[this.random.nextInt(lastNames.length)];
			this.nameBox.setValue(name);
			this.applyRatName();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выбрать случайное имя крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Рисует непрозрачную боковую плашку статов; принимает графику, ничего не отдаёт.
	private void renderStatsPanel(GuiGraphics graphics) {
		try {
			int panelX = this.getStatsPanelX();
			int panelY = this.getStatsPanelY();
			graphics.fill(panelX, panelY, panelX + STATS_PANEL_WIDTH, panelY + STATS_PANEL_HEIGHT, STATS_PANEL_BACKGROUND);
			graphics.fill(panelX, panelY, panelX + STATS_PANEL_WIDTH, panelY + 1, STATS_PANEL_BORDER);
			graphics.fill(panelX, panelY + STATS_PANEL_HEIGHT - 1, panelX + STATS_PANEL_WIDTH, panelY + STATS_PANEL_HEIGHT, STATS_PANEL_BORDER);
			graphics.fill(panelX, panelY, panelX + 1, panelY + STATS_PANEL_HEIGHT, STATS_PANEL_BORDER);
			graphics.fill(panelX + STATS_PANEL_WIDTH - 1, panelY, panelX + STATS_PANEL_WIDTH, panelY + STATS_PANEL_HEIGHT, STATS_PANEL_BORDER);
			graphics.drawString(this.font, "Статы крысы", panelX + 10, panelY + 8, STATS_PANEL_TEXT, false);
			graphics.drawString(this.font, "Имя", panelX + 10, panelY + 22, STATS_PANEL_MUTED_TEXT, false);
			graphics.drawString(this.font, "Параметры", panelX + 10, panelY + 79, STATS_PANEL_MUTED_TEXT, false);

			int lineY = panelY + 92;
			this.drawStatLine(graphics, panelX, "Здоровье", String.format(Locale.ROOT, "%.1f / %.1f", this.rat.getHealth(), this.rat.getMaxHealth()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Скорость", String.format(Locale.ROOT, "%.3f x%.2f", this.rat.getAttributeValue(Attributes.MOVEMENT_SPEED), this.rat.getRatSpeedGeneForDisplay()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Урон", String.format(Locale.ROOT, "%.2f x%.2f", this.rat.getAttributeValue(Attributes.ATTACK_DAMAGE), this.rat.getRatDamageGeneForDisplay()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Броня", String.format(Locale.ROOT, "%.2f x%.2f", this.rat.getAttributeValue(Attributes.ARMOR), this.rat.getRatArmorGeneForDisplay()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Реген", String.format(Locale.ROOT, "x%.2f", this.rat.getRatRegenGeneForDisplay()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Размер", String.format(Locale.ROOT, "x%.2f", this.rat.getRatModelScaleFromGenes()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Голод", String.format(Locale.ROOT, "%d/%d сыр, запас %d/%d", this.rat.getCheeseEatenToday(), this.rat.getDailyCheeseRequirement(), this.rat.getCheeseReserveCount(), this.rat.getMaxCheeseReserve()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Фаза", RatDaySchedule.getPhase(this.rat.level()).name().toLowerCase(Locale.ROOT), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Дом", this.formatGlobalPos(this.rat.getBoundHousePos()), lineY);
			lineY += 9;
			this.drawStatLine(graphics, panelX, "Работа", this.formatGlobalPos(this.rat.getWorkAnchorPoint()), lineY);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось отрисовать боковую плашку статов крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Рисует одну строку статов на боковой плашке; принимает графику, x плашки, название, значение и высоту, ничего не отдаёт.
	private void drawStatLine(GuiGraphics graphics, int panelX, String label, String value, int y) {
		try {
			String line = this.font.plainSubstrByWidth(label + ": " + value, STATS_PANEL_WIDTH - 20);
			graphics.drawString(this.font, line, panelX + 10, y, STATS_PANEL_TEXT, false);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось отрисовать строку статов '{}'='{}' для крысы {}", label, value, this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает левую координату боковой плашки; параметров нет, отдаёт координату экрана.
	private int getStatsPanelX() {
		try {
			int inventoryLeft = (this.width - this.imageWidth) / 2 - 8;
			int leftPanelX = inventoryLeft - STATS_PANEL_WIDTH - STATS_PANEL_GAP;
			if (leftPanelX >= 4) {
				return leftPanelX;
			}
			return inventoryLeft + this.imageWidth + STATS_PANEL_GAP;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось рассчитать x боковой плашки статов крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает левую координату кнопки статов вне слотов инвентаря; параметров нет, отдаёт координату экрана.
	private int getStatsButtonX() {
		try {
			int inventoryRight = (this.width - this.imageWidth) / 2 - 8 + this.imageWidth;
			int rightX = inventoryRight + STATS_PANEL_GAP;
			if (rightX + STATS_BUTTON_WIDTH <= this.width - 4) {
				return rightX;
			}
			return Math.max(4, (this.width - this.imageWidth) / 2 - 8 - STATS_BUTTON_WIDTH - STATS_PANEL_GAP);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось рассчитать x кнопки статов крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает верхнюю координату кнопки статов вне слотов инвентаря; параметров нет, отдаёт координату экрана.
	private int getStatsButtonY() {
		try {
			return Math.max(4, (this.height - this.imageHeight) / 2 + 8);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось рассчитать y кнопки статов крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает верхнюю координату боковой плашки; параметров нет, отдаёт координату экрана.
	private int getStatsPanelY() {
		try {
			int inventoryTop = (this.height - this.imageHeight) / 2;
			return Math.max(4, inventoryTop - 9);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось рассчитать y боковой плашки статов крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Форматирует позицию для GUI; принимает необязательную глобальную позицию, отдаёт краткую строку.
	private String formatGlobalPos(Optional<GlobalPos> pos) {
		try {
			if (pos.isEmpty()) {
				return "нет";
			}
			GlobalPos globalPos = pos.get();
			return globalPos.pos().getX() + " " + globalPos.pos().getY() + " " + globalPos.pos().getZ();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось отформатировать позицию крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}
}
