package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.gui.ChangeCommandButton;
import com.github.alexthe666.rats.client.gui.CommandPressButton;
import com.github.alexthe666.rats.client.gui.UpgradeVisibilityButton;
import com.github.alexthe666.rats.client.util.EntityRenderingUtil;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import com.github.alexthe666.rats.server.message.RatCommandPacket;
import com.github.alexthe666.rats.server.message.RatUpgradeVisibilityPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SetRatNamePacket;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;

public class RatScreen extends AbstractContainerScreen<RatMenu> {
    protected static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("rats", "textures/gui/container/rat_inventory.png");
    private static final ResourceLocation TEXTURE_BACKDROP = ResourceLocation.fromNamespaceAndPath("rats", "textures/gui/container/rat_inventory_backdrop.png");
    private static final int STATS_PANEL_WIDTH = 172;
    private static final int STATS_PANEL_HEIGHT = 184;
    private static final int STATS_PANEL_GAP = 8;
    private static final int STATS_PANEL_BACKGROUND = -266857971;
    private static final int STATS_PANEL_BORDER = -8759753;
    private static final int STATS_PANEL_TEXT = -1450552;
    private static final int STATS_PANEL_MUTED_TEXT = -4345960;
    private static final int STATS_BUTTON_WIDTH = 64;
    private static final int STATS_BUTTON_HEIGHT = 18;

    private static final String[] RANDOM_MALE_FIRST_NAMES = new String[]{
        "Cheesy", "Whiskers", "Squeaky", "Nibbles", "Pip", "Barnaby", "Rusty", "Scamper",
        "Shadow", "Gizmo", "Peanut", "Milo", "Toby", "Oreo", "Jasper", "Dexter",
        "Felix", "Oliver", "Simba", "Rocky", "Buster", "Bruno", "Sammy", "Cody",
        "Snickers", "Fudge", "Waffles", "Biscuit", "Nugget", "Teddy"
    };
    private static final String[] RANDOM_FEMALE_FIRST_NAMES = new String[]{
        "Penny", "Daisy", "Hazel", "Bella", "Luna", "Willow", "Ruby", "Rosie",
        "Molly", "Chloe", "Stella", "Lola", "Zoe", "Cleo", "Sophie", "Lily",
        "Gracie", "Coco", "Mia", "Sasha", "Pepper", "Ginger", "Cookie", "Sugar",
        "Honey", "Princess", "Minnie", "Buttercup", "Belle", "Pebbles"
    };
    private static final String[] RANDOM_MALE_LAST_NAMES = new String[]{
        "Tailor", "Cheeseman", "Quickfoot", "Whiskerson", "Paws", "Crumbfinder",
        "Shorttail", "Longwhisker", "Tunneling", "Burrower", "Shadowwalker",
        "Nightrunner", "Swiftfoot", "Goldtooth", "Snout", "Barley", "Wheatley",
        "Cheddar", "Gouda", "Parmesan"
    };
    private static final String[] RANDOM_FEMALE_LAST_NAMES = new String[]{
        "Tailor", "Cheeseman", "Quickfoot", "Whiskerson", "Paws", "Crumbfinder",
        "Shorttail", "Longwhisker", "Tunneling", "Burrower", "Shadowwalker",
        "Nightrunner", "Swiftfoot", "Goldtooth", "Snout", "Barley", "Wheatley",
        "Cheddar", "Gouda", "Parmesan"
    };

    private int currentDisplayCommand = 0;
    private final TamedRat rat;
    private final Random random = new Random();
    private boolean statsTab;
    private EditBox nameBox;

    public RatScreen(RatMenu container, Inventory inv, Component unusedTitle) {
        this(container, inv, RatScreen.resolveTamedRat(container, inv));
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
        Entity entity;
        if (menu.getRatEntityId() >= 0 && (entity = inv.player.level().getEntity(menu.getRatEntityId())) instanceof TamedRat) {
            return (TamedRat) entity;
        }
        RatsMod.LOGGER.error("Rat screen: entity not found, ratEntityId={}", menu.getRatEntityId());
        throw new IllegalStateException("Rat entity for inventory screen not found on client");
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
        this.addRenderableWidget(Button.builder(Component.literal(this.statsTab ? "Hide" : "Stats"), button -> {
            this.statsTab = !this.statsTab;
            this.init();
        }).bounds(this.getStatsButtonX(), this.getStatsButtonY(), 64, 18).build());
        if (this.statsTab) {
            int panelX = this.getStatsPanelX();
            int panelY = this.getStatsPanelY();
            this.nameBox = new EditBox(this.font, panelX + 10, panelY + 32, 152, 18, Component.literal("Rat Name"));
            this.nameBox.setMaxLength(48);
            this.nameBox.setValue(this.getCurrentName());
            this.addRenderableWidget(this.nameBox);
            this.addRenderableWidget(Button.builder(Component.literal("Apply"), button -> this.applyRatName()).bounds(panelX + 10, panelY + 54, 74, 18).build());
            this.addRenderableWidget(Button.builder(Component.literal("Random"), button -> this.pickRandomName()).bounds(panelX + 88, panelY + 54, 74, 18).build());
        }
        if (!this.rat.isBaby()) {
            this.addRenderableWidget(new ChangeCommandButton(i + 116, j + 54, false, button -> {
                --this.currentDisplayCommand;
                this.currentDisplayCommand = RatUtils.wrapCommand(this.currentDisplayCommand).ordinal();
            }));
            this.addRenderableWidget(new ChangeCommandButton(i + 199, j + 54, true, button -> {
                ++this.currentDisplayCommand;
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
        EntityRenderingUtil.drawEntityOnScreen(graphics, k + 42, l + 64, 70, (float) (k + 51) - (float) mouseX, (float) (l + 75 - 50) - (float) mouseY, (LivingEntity) this.rat, false);
        graphics.blit(TEXTURE, k - 8, l, 0, 0, this.imageWidth, this.imageHeight);
        graphics.blit(TEXTURE, k + 52, l + 20, this.rat.isMale() ? 0 : 16, 209, 16, 16);
        if (this.statsTab) {
            this.renderStatsPanel(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        MutableComponent commandText;
        graphics.drawString(this.font, this.getTitle(), this.imageWidth / 2 - this.font.width((FormattedText) this.getTitle()) / 2, 6, 0x404040, false);
        MutableComponent commandDesc = Component.translatable("entity.rats.rat.current_command");
        graphics.drawString(this.font, (Component) commandDesc, this.imageWidth / 2 - this.font.width((FormattedText) commandDesc) / 2 + 38, 19, 0x404040, false);
        MutableComponent command = Component.translatable(this.rat.getCommand().getTranslateName());
        graphics.drawString(this.font, (Component) command, this.imageWidth / 2 - this.font.width((FormattedText) command) / 2 + 36, 31, 0xFFFFFF, false);
        MutableComponent statusDesc = Component.translatable("entity.rats.rat.command_set");
        graphics.drawString(this.font, (Component) statusDesc, this.imageWidth / 2 - this.font.width((FormattedText) statusDesc) / 2 + 36, 44, 0x404040, false);
        RatCommand command1 = RatUtils.wrapCommand(this.currentDisplayCommand);
        MutableComponent command2 = Component.translatable(command1.getTranslateName());
        graphics.drawString(this.font, (Component) command2, this.imageWidth / 2 - this.font.width((FormattedText) command2) / 2 + 36, 56, 0xFFFFFF, false);
        int i = (this.width - 248) / 2;
        int j = (this.height - 166) / 2;
        if (mouseX > i + 116 && mouseX < i + 198 && mouseY > j + 22 && mouseY < j + 45) {
            commandText = Component.translatable(this.rat.getCommand().getTranslateDescription());
            String[] everySpace = commandText.getString().split(" ");
            int currentStrLength = 0;
            StringBuilder builtString = new StringBuilder();
            ArrayList<String> list = new ArrayList<String>();
            for (String s : everySpace) {
                builtString.append(s).append(" ");
                if ((currentStrLength += this.font.width(s + " ")) < 95) continue;
                list.add(builtString.toString());
                builtString = new StringBuilder();
                currentStrLength = 0;
            }
            ArrayList<MutableComponent> convertedList = new ArrayList<MutableComponent>();
            for (String str : list) {
                convertedList.add(Component.literal(str));
            }
            graphics.renderTooltip(this.font, Lists.transform(convertedList, Component::getVisualOrderText), mouseX - i - 30, mouseY - j + 10);
        }
        if (mouseX > i + 116 && mouseX < i + 198 && mouseY > j + 53 && mouseY < j + 69) {
            commandText = Component.translatable(command1.getTranslateDescription());
            graphics.renderTooltip(this.font, this.font.split((FormattedText) commandText, 110), mouseX - i - 30, mouseY - j + 10);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameBox != null && this.nameBox.isFocused() && (keyCode == 257 || keyCode == 335)) {
            this.applyRatName();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getCurrentName() {
        try {
            return this.rat.hasCustomName() && this.rat.getCustomName() != null ? this.rat.getCustomName().getString() : "";
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to get current rat name {} for GUI", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private void applyRatName() {
        try {
            if (this.nameBox == null) {
                throw new IllegalStateException("Rat name box not created");
            }
            String name = this.nameBox.getValue().trim();
            RatsNetworkHandler.CHANNEL.sendToServer(new SetRatNamePacket(this.rat.getId(), name));
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to apply rat name {} from GUI", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private void pickRandomName() {
        try {
            if (this.nameBox == null) {
                throw new IllegalStateException("Rat name box not created");
            }
            String[] firstNames = this.rat.isMale() ? RANDOM_MALE_FIRST_NAMES : RANDOM_FEMALE_FIRST_NAMES;
            String[] lastNames = this.rat.isMale() ? RANDOM_MALE_LAST_NAMES : RANDOM_FEMALE_LAST_NAMES;
            String name = firstNames[this.random.nextInt(firstNames.length)] + " " + lastNames[this.random.nextInt(lastNames.length)];
            this.nameBox.setValue(name);
            this.applyRatName();
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to pick random rat name {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private void renderStatsPanel(GuiGraphics graphics) {
        try {
            int panelX = this.getStatsPanelX();
            int panelY = this.getStatsPanelY();
            graphics.fill(panelX, panelY, panelX + 172, panelY + 184, -266857971);
            graphics.fill(panelX, panelY, panelX + 172, panelY + 1, -8759753);
            graphics.fill(panelX, panelY + 184 - 1, panelX + 172, panelY + 184, -8759753);
            graphics.fill(panelX, panelY + 1, panelX + 172, panelY + 184, -8759753);
            graphics.fill(panelX + 172 - 1, panelY, panelX + 172, panelY + 184, -8759753);
            graphics.drawString(this.font, "Rat Stats", panelX + 10, panelY + 8, -1450552, false);
            graphics.drawString(this.font, "Name", panelX + 10, panelY + 22, -4345960, false);
            graphics.drawString(this.font, "Attributes", panelX + 10, panelY + 79, -4345960, false);
            int lineY = panelY + 92;
            this.drawStatLine(graphics, panelX, "Health", String.format(Locale.ROOT, "%.1f / %.1f", Float.valueOf(this.rat.getHealth()), Float.valueOf(this.rat.getMaxHealth())), lineY);
            this.drawStatLine(graphics, panelX, "Speed", String.format(Locale.ROOT, "%.3f x%.2f", this.rat.getAttributeValue(Attributes.MOVEMENT_SPEED), Float.valueOf(this.rat.getRatSpeedGeneForDisplay())), lineY += 9);
            this.drawStatLine(graphics, panelX, "Damage", String.format(Locale.ROOT, "%.2f x%.2f", this.rat.getAttributeValue(Attributes.ATTACK_DAMAGE), Float.valueOf(this.rat.getRatDamageGeneForDisplay())), lineY += 9);
            this.drawStatLine(graphics, panelX, "Armor", String.format(Locale.ROOT, "%.2f x%.2f", this.rat.getAttributeValue(Attributes.ARMOR), Float.valueOf(this.rat.getRatArmorGeneForDisplay())), lineY += 9);
            this.drawStatLine(graphics, panelX, "Regen", String.format(Locale.ROOT, "x%.2f", Float.valueOf(this.rat.getRatRegenGeneForDisplay())), lineY += 9);
            this.drawStatLine(graphics, panelX, "Size", String.format(Locale.ROOT, "x%.2f", Float.valueOf(this.rat.getRatModelScaleFromGenes())), lineY += 9);
            this.drawStatLine(graphics, panelX, "Hunger", String.format(Locale.ROOT, "%d/%d cheese, reserve %d/%d", this.rat.getCheeseEatenToday(), this.rat.getDailyCheeseRequirement(), this.rat.getCheeseReserveCount(), this.rat.getMaxCheeseReserve()), lineY += 9);
            this.drawStatLine(graphics, panelX, "Phase", RatDaySchedule.getPhase(this.rat.level()).name().toLowerCase(Locale.ROOT), lineY += 9);
            this.drawStatLine(graphics, panelX, "Home", this.formatGlobalPos(this.rat.getBoundHousePos()), lineY += 9);
            this.drawStatLine(graphics, panelX, "Work", this.formatGlobalPos(this.rat.getWorkAnchorPoint()), lineY += 9);
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to render rat stats side panel {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private void drawStatLine(GuiGraphics graphics, int panelX, String label, String value, int y) {
        try {
            String line = this.font.plainSubstrByWidth(label + ": " + value, 152);
            graphics.drawString(this.font, line, panelX + 10, y, -1450552, false);
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to render rat stat line '{}'='{}' for rat {}", label, value, this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private int getStatsPanelX() {
        try {
            int inventoryLeft = (this.width - this.imageWidth) / 2 - 8;
            int leftPanelX = inventoryLeft - 172 - 8;
            if (leftPanelX >= 4) {
                return leftPanelX;
            }
            return inventoryLeft + this.imageWidth + 8;
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to calculate x for rat stats panel {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private int getStatsButtonX() {
        try {
            int inventoryRight = (this.width - this.imageWidth) / 2 - 8 + this.imageWidth;
            int rightX = inventoryRight + 8;
            if (rightX + 64 <= this.width - 4) {
                return rightX;
            }
            return Math.max(4, (this.width - this.imageWidth) / 2 - 8 - 64 - 8);
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to calculate x for rat stats button {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private int getStatsButtonY() {
        try {
            return Math.max(4, (this.height - this.imageHeight) / 2 + 8);
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to calculate y for rat stats button {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private int getStatsPanelY() {
        try {
            int inventoryTop = (this.height - this.imageHeight) / 2;
            return Math.max(4, inventoryTop - 9);
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to calculate y for rat stats panel {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private String formatGlobalPos(Optional<GlobalPos> pos) {
        try {
            if (pos.isEmpty()) {
                return "None";
            }
            GlobalPos globalPos = pos.get();
            return globalPos.pos().getX() + " " + globalPos.pos().getY() + " " + globalPos.pos().getZ();
        } catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Failed to format rat position {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }
}
