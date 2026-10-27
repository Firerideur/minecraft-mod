package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.character.CharacterMenu;
import com.firerideur.arcanequest.character.CharacterMenu.View;
import com.firerideur.arcanequest.client.ui.GoldButton;
import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.network.ChooseClassPacket;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.network.SpendPointPacket;
import com.firerideur.arcanequest.skill.ClassSkills;
import com.firerideur.arcanequest.skill.Skill;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.Stat;
import com.firerideur.arcanequest.stats.StatEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The character menu (K) in the dark fantasy style: side tabs, inventory, equipment, crafting and skills.
 */
public class CharacterScreen extends AbstractContainerScreen<CharacterMenu> {
    private static final int WIDTH = 404;
    private static final int HEIGHT = 170;
    /** Leave room for the HUD medallion and bars above the menu. */
    private static final int MIN_TOP = 50;
    private static final int TAB_Y = 2, TAB_SPACING = 26;
    private static final int SKILL_ROW_Y = 50, SKILL_ROW_H = 14, SKILLS_H = 154;
    private static final int CLASS_Y = 116;

    private enum Tab {
        INVENTORY("inventory", new ItemStack(Items.BUNDLE)),
        CRAFTING("crafting", new ItemStack(Items.CRAFTING_TABLE)),
        SKILLS("skills", new ItemStack(Items.EXPERIENCE_BOTTLE)),
        ABILITIES("abilities", new ItemStack(Items.ENCHANTED_BOOK)),
        MAP("map", new ItemStack(Items.FILLED_MAP)),
        SETTINGS("settings", new ItemStack(Items.COMPARATOR));

        final String key;
        final ItemStack icon;

        Tab(String key, ItemStack icon) {
            this.key = key;
            this.icon = icon;
        }

        Component label() {
            return Component.translatable("screen.arcanequest.tab." + key);
        }
    }

    /** Remembered between openings. */
    private static Tab lastTab = Tab.INVENTORY;

    private Tab tab = lastTab;
    private final Map<Stat, GoldButton> plusButtons = new EnumMap<>(Stat.class);
    private final List<GoldButton> classButtons = new ArrayList<>();

    public CharacterScreen(CharacterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(topPos, Math.min(MIN_TOP, height - imageHeight));
        plusButtons.clear();
        classButtons.clear();
        int panelRight = leftPos + CharacterMenu.EQUIPMENT_X + CharacterMenu.EQUIPMENT_W;
        for (Stat stat : Stat.values()) {
            GoldButton plus = new GoldButton(panelRight - 24, topPos + SKILL_ROW_Y + stat.ordinal() * SKILL_ROW_H + 6, 12, 12,
                    Component.literal("+"), () -> ModNetwork.sendToServer(new SpendPointPacket(stat.ordinal())), stat.description());
            plusButtons.put(stat, addRenderableWidget(plus));
        }
        // One button per offered class; their label follows the offers, which change with the stats
        int x = leftPos + CharacterMenu.INVENTORY_X + 10;
        int buttonWidth = (CharacterMenu.EQUIPMENT_X + CharacterMenu.EQUIPMENT_W - CharacterMenu.INVENTORY_X - 20
                - 4 * (PlayerClass.OFFERED - 1)) / PlayerClass.OFFERED;
        for (int i = 0; i < PlayerClass.OFFERED; i++) {
            int index = i;
            GoldButton button = new GoldButton(x, topPos + CLASS_Y + 11, buttonWidth, 14, Component.empty(), () -> {
                List<PlayerClass> offers = PlayerClass.offers(ClientStats.get());
                if (index < offers.size()) {
                    ModNetwork.sendToServer(new ChooseClassPacket(offers.get(index).ordinal()));
                }
            }, null);
            classButtons.add(addRenderableWidget(button));
            x += buttonWidth + 4;
        }
        selectTab(tab);
    }

    private void selectTab(Tab newTab) {
        if (newTab == Tab.SETTINGS) {
            minecraft.setScreen(new OptionsScreen(this, minecraft.options));
            return;
        }
        tab = newTab;
        lastTab = newTab;
        menu.setView(switch (newTab) {
            case CRAFTING -> View.CRAFTING;
            case SKILLS, ABILITIES -> View.SKILLS;
            case MAP -> View.MAP;
            default -> View.INVENTORY;
        });
        updateWidgets();
    }

    private void updateWidgets() {
        PlayerStats stats = ClientStats.get();
        boolean skills = tab == Tab.SKILLS;
        plusButtons.values().forEach(b -> {
            b.visible = skills;
            b.active = stats.getPoints() > 0;
        });
        List<PlayerClass> offers = PlayerClass.canChoose(stats) ? PlayerClass.offers(stats) : List.of();
        // Hidden classes whose condition isn't met yet show as a locked "???" after the offers
        List<PlayerClass> locked = PlayerClass.canChoose(stats) && stats.getPlayerClass().isFirstClass()
                ? PlayerClass.awakeningsOf(stats.getPlayerClass()).stream().filter(c -> !offers.contains(c)).toList()
                : List.of();
        for (int i = 0; i < classButtons.size(); i++) {
            GoldButton button = classButtons.get(i);
            int lockedIndex = i - offers.size();
            button.visible = skills && i < offers.size() + locked.size();
            button.active = i < offers.size();
            if (button.visible && !button.active) {
                PlayerClass hidden = locked.get(lockedIndex);
                Component label = Component.literal("??? [" + hidden.rank().name() + "]");
                if (!button.getMessage().getString().equals(label.getString())) {
                    button.setMessage(label);
                    button.setTooltip(Tooltip.create(Component.translatable("screen.arcanequest.hidden_class")));
                }
            } else if (button.visible) {
                PlayerClass offer = offers.get(i);
                Component label = offer.displayNameWithRank();
                if (!button.getMessage().getString().equals(label.getString())) {
                    button.setMessage(label);
                    button.setTooltip(Tooltip.create(offer.description().copy().append("\n\n")
                            .append(offer.lore().copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY))));
                }
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Tab t : Tab.values()) {
            int ty = topPos + TAB_Y + t.ordinal() * TAB_SPACING;
            if (mouseX >= leftPos && mouseX < leftPos + UiKit.TAB_WIDTH && mouseY >= ty && mouseY < ty + UiKit.TAB_HEIGHT) {
                selectTab(t);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientSetup.OPEN_STATS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible behind the menu, just darker, like the mockup
        g.fillGradient(0, 0, width, height, 0x90000000, 0xD0000000);
        // AbstractContainerScreen draws its background from here
        renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (tab == Tab.ABILITIES) {
            renderAbilityTooltip(g, mouseX, mouseY);
        } else if (tab == Tab.SKILLS) {
            renderClassTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        renderTabs(g, mouseX, mouseY);
        if (tab == Tab.SKILLS) {
            renderSkills(g);
        } else if (tab == Tab.ABILITIES) {
            renderAbilities(g);
        } else if (tab == Tab.MAP) {
            renderMap(g, partialTick);
        } else {
            renderInventoryPanel(g);
            if (tab == Tab.CRAFTING) {
                renderCraftingPanel(g);
            } else {
                renderEquipmentPanel(g, mouseX, mouseY);
            }
            for (Slot slot : menu.slots) {
                if (slot.isActive()) {
                    UiKit.slot(g, leftPos + slot.x - 1, topPos + slot.y - 1, slot == hoveredSlot);
                }
            }
        }
        renderFooter(g);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Titles are drawn by the panels
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        for (Tab t : Tab.values()) {
            int x = leftPos;
            int y = topPos + TAB_Y + t.ordinal() * TAB_SPACING;
            boolean hovered = mouseX >= x && mouseX < x + UiKit.TAB_WIDTH && mouseY >= y && mouseY < y + UiKit.TAB_HEIGHT;
            if (t == tab) {
                UiKit.selectedTab(g, x, y);
            } else if (hovered) {
                g.fill(x, y, x + UiKit.TAB_WIDTH - 8, y + UiKit.TAB_HEIGHT, 0x40C9A25A);
            }
            g.renderItem(t.icon, x + 4, y + 4);
            g.drawString(font, t.label(), x + 24, y + 8, t == tab || hovered ? UiKit.GOLD_LIGHT : UiKit.TEXT, false);
            if (t.ordinal() < Tab.values().length - 1) {
                UiKit.rule(g, x + 4, y + UiKit.TAB_HEIGHT + 1, UiKit.TAB_WIDTH - 12);
            }
        }
    }

    private void renderInventoryPanel(GuiGraphics g) {
        int x = leftPos + CharacterMenu.INVENTORY_X;
        int y = topPos;
        UiKit.panel(g, x, y, CharacterMenu.INVENTORY_W, CharacterMenu.PANEL_H);
        UiKit.title(g, font, Component.translatable("screen.arcanequest.tab.inventory"), x + 8, y + 9, CharacterMenu.INVENTORY_W - 16);
        UiKit.title(g, font, Component.translatable("screen.arcanequest.hotbar"), x + 8, y + CharacterMenu.HOTBAR_Y - 17,
                CharacterMenu.INVENTORY_W - 16);
    }

    private void renderEquipmentPanel(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + CharacterMenu.EQUIPMENT_X;
        int y = topPos;
        UiKit.panel(g, x, y, CharacterMenu.EQUIPMENT_W, CharacterMenu.PANEL_H);
        UiKit.title(g, font, Component.translatable("screen.arcanequest.equipment"), x + 8, y + 9, CharacterMenu.EQUIPMENT_W - 16);
        int modelLeft = x + 30, modelTop = y + 26, modelRight = x + 94, modelBottom = y + 98;
        g.fill(modelLeft, modelTop, modelRight, modelBottom, 0xC0080706);
        UiKit.frame(g, modelLeft, modelTop, modelRight - modelLeft, modelBottom - modelTop, UiKit.GOLD_DARK);
        if (minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, modelLeft, modelTop, modelRight, modelBottom,
                    30, 0.0625F, mouseX, mouseY, minecraft.player);
        }
        PlayerStats stats = ClientStats.get();
        g.drawCenteredString(font, Component.translatable("hud.arcanequest.level", stats.getLevel()),
                (modelLeft + modelRight) / 2, y + 104, UiKit.GOLD_LIGHT);
        g.drawCenteredString(font, stats.getPlayerClass().displayName(), (modelLeft + modelRight) / 2, y + 115,
                stats.getPlayerClass().color());
    }

    private void renderCraftingPanel(GuiGraphics g) {
        int x = leftPos + CharacterMenu.EQUIPMENT_X;
        int y = topPos;
        UiKit.panel(g, x, y, CharacterMenu.EQUIPMENT_W, CharacterMenu.PANEL_H);
        UiKit.title(g, font, Component.translatable("screen.arcanequest.tab.crafting"), x + 8, y + 9, CharacterMenu.EQUIPMENT_W - 16);
        UiKit.arrow(g, leftPos + CharacterMenu.CRAFT_X + 40, topPos + CharacterMenu.RESULT_Y + 1);
        g.drawWordWrap(font, Component.translatable("screen.arcanequest.crafting_hint"), x + 10, y + 86,
                CharacterMenu.EQUIPMENT_W - 20, UiKit.TEXT_DIM);
    }

    private void renderSkills(GuiGraphics g) {
        int x = leftPos + CharacterMenu.INVENTORY_X;
        int y = topPos;
        int w = CharacterMenu.EQUIPMENT_X + CharacterMenu.EQUIPMENT_W - CharacterMenu.INVENTORY_X;
        UiKit.panel(g, x, y, w, SKILLS_H);
        PlayerStats stats = ClientStats.get();
        UiKit.title(g, font, Component.translatable("screen.arcanequest.tab.skills"), x + 8, y + 9, w - 16);

        Component level = Component.translatable("screen.arcanequest.level", stats.getLevel());
        g.drawString(font, level, x + 10, y + 25, UiKit.GOLD_LIGHT, false);
        Component xp = stats.isMaxLevel() ? Component.translatable("screen.arcanequest.max_level")
                : Component.translatable("screen.arcanequest.xp", stats.getXp(), PlayerStats.xpForNextLevel(stats.getLevel()));
        g.drawString(font, xp, x + w - 10 - font.width(xp), y + 25, UiKit.TEXT_DIM, false);
        float progress = stats.isMaxLevel() ? 1 : (float) stats.getXp() / PlayerStats.xpForNextLevel(stats.getLevel());
        UiKit.bar(g, x + 12, y + 37, w - 30, 3, progress, 0xFF4CAF50);

        Component points = Component.translatable("screen.arcanequest.points", stats.getPoints());
        g.drawString(font, points, x + w / 2 - font.width(points) / 2, y + 43, stats.getPoints() > 0 ? 0xFF7CFC00 : UiKit.TEXT_DIM, false);

        for (Stat stat : Stat.values()) {
            int rowY = y + SKILL_ROW_Y + stat.ordinal() * SKILL_ROW_H + 8;
            g.drawString(font, stat.displayName(), x + 12, rowY, stat.color(), false);
            g.drawString(font, String.valueOf(stats.get(stat)), x + 90, rowY, UiKit.TEXT, false);
            g.drawString(font, bonus(stat, stats.get(stat)), x + 110, rowY, UiKit.TEXT_DIM, false);
        }

        int classY = y + CLASS_Y;
        PlayerClass playerClass = stats.getPlayerClass();
        if (playerClass == PlayerClass.NONE) {
            List<PlayerClass> offers = PlayerClass.offers(stats);
            if (stats.getLevel() < PlayerClass.UNLOCK_LEVEL) {
                g.drawString(font, Component.translatable("screen.arcanequest.class_locked", PlayerClass.UNLOCK_LEVEL),
                        x + 10, classY, UiKit.TEXT_DIM, false);
            } else {
                g.drawString(font, Component.translatable("screen.arcanequest.choose_class"), x + 10, classY, 0xFF55FFFF, false);
            }
            if (offers.isEmpty()) {
                g.drawWordWrap(font, Component.translatable("screen.arcanequest.class_spend_points"), x + 10, classY + 13, w - 20, UiKit.TEXT_DIM);
            } else if (stats.getLevel() < PlayerClass.UNLOCK_LEVEL) {
                // Preview of what the current stats would offer
                MutableComponent preview = Component.translatable("screen.arcanequest.class_preview");
                for (int i = 0; i < offers.size(); i++) {
                    preview.append(i == 0 ? " " : ", ").append(offers.get(i).displayNameWithRank());
                }
                g.drawWordWrap(font, preview, x + 10, classY + 13, w - 20, UiKit.TEXT_DIM);
            }
        } else if (PlayerClass.canChoose(stats)) {
            // Awakening: the buttons show the evolutions of the class
            g.drawString(font, Component.translatable("screen.arcanequest.awakening_choose", playerClass.displayNameWithRank()),
                    x + 10, classY, 0xFFE070FF, false);
        } else {
            g.drawString(font, Component.translatable("screen.arcanequest.class", playerClass.displayNameWithRank()),
                    x + 10, classY, UiKit.TEXT, false);
            if (playerClass.isFirstClass()) {
                Component awakening = Component.translatable("screen.arcanequest.awakening_at", PlayerClass.AWAKENING_LEVEL);
                g.drawString(font, awakening, x + w - 10 - font.width(awakening), classY, UiKit.TEXT_DIM, false);
            }
            drawFitted(g, playerClass.description(), x + 10, classY + 11, w - 20, y + SKILLS_H - 4 - (classY + 11));
        }
        if (minecraft.player != null) {
            Component mana = Component.translatable("hud.arcanequest.mana", (int) stats.getMana(),
                    (int) Mana.max(minecraft.player, stats));
            g.drawString(font, mana, x + 10, y + 43, 0xFF6FA8FF, false);
        }
    }

    /** Wrapped text, shrunk to 3/4 when it would not fit in {@code height}. */
    private void drawFitted(GuiGraphics g, Component text, int x, int y, int width, int height) {
        if (font.split(text, width).size() * font.lineHeight <= height) {
            g.drawWordWrap(font, text, x, y, width, UiKit.TEXT_DIM);
            return;
        }
        float scale = 0.75F;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);
        g.drawWordWrap(font, text, (int) (x / scale), (int) (y / scale), (int) (width / scale), UiKit.TEXT_DIM);
        g.pose().popPose();
    }

    private static final int ABILITY_ROW_Y = 24, ABILITY_ROW_H = 21;

    private void renderAbilities(GuiGraphics g) {
        int x = leftPos + CharacterMenu.INVENTORY_X;
        int y = topPos;
        int w = CharacterMenu.EQUIPMENT_X + CharacterMenu.EQUIPMENT_W - CharacterMenu.INVENTORY_X;
        UiKit.panel(g, x, y, w, SKILLS_H);
        UiKit.title(g, font, Component.translatable("screen.arcanequest.tab.abilities"), x + 8, y + 9, w - 16);
        PlayerStats stats = ClientStats.get();
        List<Skill> skills = ClassSkills.skillsOf(stats.getPlayerClass());
        if (skills.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("screen.arcanequest.abilities_no_class", PlayerClass.UNLOCK_LEVEL),
                    x + 10, y + 30, w - 20, UiKit.TEXT_DIM);
            return;
        }
        for (int slot = 0; slot < ClassSkills.SLOTS; slot++) {
            int rowY = y + ABILITY_ROW_Y + slot * ABILITY_ROW_H;
            Skill skill = slot < skills.size() ? skills.get(slot) : null;
            boolean unlocked = skill != null && stats.getLevel() >= ClassSkills.UNLOCK_LEVELS[slot];
            UiKit.slot(g, x + 10, rowY, false);
            if (skill != null) {
                g.renderItem(new ItemStack(skill.icon()), x + 11, rowY + 1);
                if (!unlocked) {
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.fill(x + 11, rowY + 1, x + 27, rowY + 17, 0xA0000000);
                    g.pose().popPose();
                }
            }
            boolean ultimate = slot == ClassSkills.ULTIMATE_SLOT;
            Component name = skill != null ? skill.displayName()
                    : Component.translatable(ultimate ? "screen.arcanequest.ability_ultimate" : "screen.arcanequest.ability_awakening");
            g.drawString(font, name, x + 34, rowY, unlocked ? UiKit.GOLD_LIGHT : UiKit.TEXT_DIM, false);
            if (ultimate && skill != null) {
                Component tag = Component.translatable("screen.arcanequest.ability_ultimate_tag");
                g.drawString(font, tag, x + 38 + font.width(name), rowY, unlocked ? 0xFFFF5555 : UiKit.TEXT_DIM, false);
            }
            Component state = unlocked
                    ? Component.literal("[" + ClientSetup.SKILL_KEYS[slot].getTranslatedKeyMessage().getString() + "]")
                    : Component.translatable("screen.arcanequest.ability_level", ClassSkills.UNLOCK_LEVELS[slot]);
            g.drawString(font, state, x + w - 10 - font.width(state), rowY, unlocked ? 0xFF7CFC00 : UiKit.TEXT_DIM, false);
            Component description = skill != null ? skill.description()
                    : Component.translatable(ultimate ? "screen.arcanequest.ability_ultimate.desc" : "screen.arcanequest.ability_awakening.desc");
            // Description on one small line; the tooltip shows all of it
            var lines = font.split(description, (int) ((w - 46) / 0.75F));
            if (!lines.isEmpty()) {
                g.pose().pushPose();
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.drawString(font, lines.get(0), (int) ((x + 34) / 0.75F), (int) ((rowY + 11) / 0.75F), UiKit.TEXT_DIM, false);
                g.pose().popPose();
            }
        }
    }

    /** The current class (description and inspiration) when hovering its line. */
    private void renderClassTooltip(GuiGraphics g, int mouseX, int mouseY) {
        PlayerStats stats = ClientStats.get();
        PlayerClass playerClass = stats.getPlayerClass();
        int x = leftPos + CharacterMenu.INVENTORY_X;
        int w = CharacterMenu.EQUIPMENT_X + CharacterMenu.EQUIPMENT_W - CharacterMenu.INVENTORY_X;
        if (playerClass == PlayerClass.NONE || PlayerClass.canChoose(stats) || mouseX < x || mouseX >= x + w
                || mouseY < topPos + CLASS_Y || mouseY >= topPos + SKILLS_H) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(playerClass.displayNameWithRank());
        font.getSplitter().splitLines(playerClass.description(), 240, Style.EMPTY)
                .forEach(line -> lines.add(Component.literal(line.getString()).withColor(UiKit.TEXT)));
        font.getSplitter().splitLines(playerClass.lore(), 240, Style.EMPTY)
                .forEach(line -> lines.add(Component.literal(line.getString()).withStyle(ChatFormatting.ITALIC).withColor(UiKit.TEXT_DIM)));
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private void renderAbilityTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + CharacterMenu.INVENTORY_X;
        int w = CharacterMenu.EQUIPMENT_X + CharacterMenu.EQUIPMENT_W - CharacterMenu.INVENTORY_X;
        List<Skill> skills = ClassSkills.skillsOf(ClientStats.get().getPlayerClass());
        int slot = (mouseY - topPos - ABILITY_ROW_Y) / ABILITY_ROW_H;
        if (mouseX < x || mouseX >= x + w || mouseY < topPos + ABILITY_ROW_Y || slot < 0 || slot >= skills.size()) {
            return;
        }
        Skill skill = skills.get(slot);
        List<Component> lines = new ArrayList<>();
        MutableComponent title = skill.displayName().copy().withColor(UiKit.GOLD_LIGHT);
        if (slot == ClassSkills.ULTIMATE_SLOT) {
            title.append(Component.literal(" ")).append(Component.translatable("screen.arcanequest.ability_ultimate_tag").withColor(0xFFFF5555));
        }
        lines.add(title);
        font.getSplitter().splitLines(skill.description(), 220, Style.EMPTY)
                .forEach(line -> lines.add(Component.literal(line.getString()).withColor(UiKit.TEXT)));
        lines.add(Component.translatable("screen.arcanequest.ability_cost", (int) skill.manaCost(), skill.cooldownTicks() / 20)
                .withColor(0xFF6FA8FF));
        lines.add(Component.translatable("screen.arcanequest.ability_unlock", ClassSkills.UNLOCK_LEVELS[slot]).withColor(UiKit.TEXT_DIM));
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private void renderMap(GuiGraphics g, float partialTick) {
        int x = leftPos + CharacterMenu.INVENTORY_X;
        int y = topPos;
        int w = CharacterMenu.EQUIPMENT_X + CharacterMenu.EQUIPMENT_W - CharacterMenu.INVENTORY_X;
        UiKit.panel(g, x, y, w, SKILLS_H);
        UiKit.title(g, font, Component.translatable("screen.arcanequest.tab.map"), x + 8, y + 9, w - 16);
        int mapSize = SKILLS_H - 34;
        int mapX = x + 10, mapY = y + 24;
        WorldMap.draw(g, mapX, mapY, mapSize, WorldMap.SIZE / 2, partialTick);
        UiKit.frame(g, mapX - 1, mapY - 1, mapSize + 2, mapSize + 2, UiKit.GOLD);
        g.drawCenteredString(font, Component.translatable("hud.arcanequest.compass.n"), mapX + mapSize / 2, mapY + 2, UiKit.GOLD_LIGHT);

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        int infoX = mapX + mapSize + 12;
        int line = mapY + 2;
        var pos = minecraft.player.blockPosition();
        line = info(g, Component.translatable("screen.arcanequest.map.position"),
                Component.literal(pos.getX() + "  " + pos.getY() + "  " + pos.getZ()), infoX, line);
        var biome = minecraft.level.getBiome(pos).unwrapKey()
                .map(key -> Component.translatable(Util.makeDescriptionId("biome", key.location())))
                .orElse(Component.literal("?"));
        line = info(g, Component.translatable("screen.arcanequest.map.biome"), biome, infoX, line);
        var dimension = minecraft.level.dimension().location();
        line = info(g, Component.translatable("screen.arcanequest.map.dimension"),
                Component.translatable("dimension.arcanequest." + dimension.getPath()), infoX, line);
        long day = minecraft.level.getDayTime() / 24000L + 1;
        line = info(g, Component.translatable("screen.arcanequest.map.day"), Component.literal(String.valueOf(day)), infoX, line);
        g.fill(infoX, line + 4, infoX + 3, line + 7, 0xFFE03030);
        g.drawString(font, Component.translatable("screen.arcanequest.map.legend_mob"), infoX + 6, line + 2, UiKit.TEXT_DIM, false);
        g.fill(infoX, line + 16, infoX + 3, line + 19, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("screen.arcanequest.map.legend_player"), infoX + 6, line + 14, UiKit.TEXT_DIM, false);
    }

    private int info(GuiGraphics g, Component label, Component value, int x, int y) {
        g.drawString(font, label, x, y, UiKit.GOLD, false);
        g.drawString(font, value, x, y + 10, UiKit.TEXT, false);
        return y + 24;
    }

    private static Component bonus(Stat stat, int value) {
        return switch (stat) {
            case STRENGTH -> Component.translatable("screen.arcanequest.bonus.strength", format(value * StatEffects.DAMAGE_PER_STRENGTH));
            case VITALITY -> Component.translatable("screen.arcanequest.bonus.vitality", format(value * StatEffects.HEALTH_PER_VITALITY / 2.0));
            case AGILITY -> Component.translatable("screen.arcanequest.bonus.agility", format(value * StatEffects.SPEED_PER_AGILITY * 100));
            case INTELLIGENCE -> Component.translatable("screen.arcanequest.bonus.intelligence",
                    format(value * Mana.MANA_PER_INTELLIGENCE), value * 5);
        };
    }

    private static String format(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }

    private void renderFooter(GuiGraphics g) {
        int y = topPos + HEIGHT - 8;
        UiKit.rule(g, leftPos, y - 6, imageWidth);
        int x = leftPos + 4;
        x = UiKit.keyHint(g, font, "Clic", Component.translatable("screen.arcanequest.hint.select"), x, y);
        x = UiKit.keyHint(g, font, "Maj+Clic", Component.translatable("screen.arcanequest.hint.move"), x, y);
        UiKit.keyHint(g, font, "K / Échap", Component.translatable("screen.arcanequest.hint.close"), x, y);
    }
}
