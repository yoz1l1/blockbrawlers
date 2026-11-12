package dev.blockbrawlers.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class BlockBrawlersScreen extends Screen {
    private static final int BG = 0xFF101827;
    private static final int PANEL = 0xF01B2940;
    private static final int GOLD = 0xFFFFD166;
    private static final String[] FIGHTERS = {"ИСКРА", "МОХ", "ГРОМ", "АСТРА"};
    private static final String[] ROLES = {"ОГНЕННЫЙ БОЕЦ", "ЛЕСНОЙ ЗАЩИТНИК", "ГРОЗОВОЙ СКОРОХОД", "ЗВЁЗДНЫЙ МАГ"};
    private static final String[] LORE = {
            "Бьёт огненными зарядами вблизи и поджигает врагов суперударом.",
            "Защищается каменной бронёй и оплетает врагов корнями.",
            "Молниеносно атакует на средней дистанции и совершает рывки.",
            "Посылает звёздные снаряды издалека и создаёт гравитационный всплеск."
    };
    private static final int[] COLORS = {0xFFFF875E, 0xFF62C99A, 0xFF65B9FF, 0xFFC69BFF};

    private enum Page { HOME, BRAWLERS, FRIENDS, MATCH }
    private Page page = Page.HOME;
    private int fighterIndex = 0;
    private final StringBuilder friendName = new StringBuilder();
    private String notice = "Пригласи друга, который играет на том же сервере Minecraft.";
    private boolean w, a, s, d;
    private boolean playing = false;
    private boolean gameOver = false;
    private boolean victory = false;
    private float playerX, playerY, playerHealth = 100;
    private int attackCooldown, abilityCooldown, dashCooldown, invulnerableTicks, elapsedTicks, kills;
    private int effectTicks;
    private float effectX, effectY;
    private int effectColor = 0xFFFFFFFF;
    private boolean playerHidden;
    private static final int[] ATTACK_DAMAGE = {34, 24, 28, 22};
    private static final float[] ATTACK_RANGE = {105, 78, 145, 190};
    private static final int[] SUPER_DAMAGE = {76, 48, 62, 54};
    private static final float[] SUPER_RANGE = {125, 155, 175, 220};
    private final List<Enemy> enemies = new ArrayList<>();

    public BlockBrawlersScreen() {
        super(Text.literal("Блоковая арена"));
    }

    @Override
    protected void init() {
        clearChildren();
        int cx = width / 2;
        if (page == Page.HOME) {
            int y = height / 2 + 64;
            addDrawableChild(ButtonWidget.builder(Text.literal("ВЫБРАТЬ БОЙЦА"), b -> { page = Page.BRAWLERS; init(); })
                    .dimensions(cx - 164, y, 156, 30).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("ОДИНОЧНАЯ ИГРА"), b -> startMatch())
                    .dimensions(cx + 8, y, 156, 30).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("ДРУЗЬЯ  +"), b -> { page = Page.FRIENDS; init(); })
                    .dimensions(cx - 164, y + 40, 156, 26).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("ВЫХОД"), b -> close())
                    .dimensions(cx + 8, y + 40, 156, 26).build());
        } else if (page == Page.BRAWLERS) {
            for (int i = 0; i < FIGHTERS.length; i++) {
                final int index = i;
                int x = cx - 166 + (i % 2) * 172;
                int y = height / 2 + 42 + (i / 2) * 34;
                addDrawableChild(ButtonWidget.builder(Text.literal((fighterIndex == i ? "✓ " : "") + FIGHTERS[i]), b -> {
                    fighterIndex = index;
                    init();
                }).dimensions(x, y, 160, 26).build());
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("НАЗАД"), b -> { page = Page.HOME; init(); })
                    .dimensions(cx - 65, height - 42, 130, 22).build());
        } else if (page == Page.FRIENDS) {
            addDrawableChild(ButtonWidget.builder(Text.literal("ОТПРАВИТЬ ПРИГЛАШЕНИЕ"), b -> sendInvite())
                    .dimensions(cx - 80, height / 2 + 36, 160, 28).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("НАЗАД"), b -> { page = Page.HOME; init(); })
                    .dimensions(cx - 65, height / 2 + 76, 130, 22).build());
        } else if (page == Page.MATCH && gameOver) {
            addDrawableChild(ButtonWidget.builder(Text.literal("ИГРАТЬ СНОВА"), b -> startMatch())
                    .dimensions(cx - 82, height / 2 + 42, 164, 30).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("ГЛАВНОЕ МЕНЮ"), b -> {
                playing = false; gameOver = false; page = Page.HOME; init();
            }).dimensions(cx - 82, height / 2 + 80, 164, 24).build());
        }
    }

    private void sendInvite() {
        String name = friendName.toString().trim();
        if (name.isEmpty() || !name.matches("[A-Za-z0-9_]{1,16}")) {
            notice = "Введи ник Minecraft (1–16 латинских букв, цифр или _).";
            return;
        }
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            notice = "Сначала зайди на многопользовательский сервер.";
            return;
        }
        if (name.equalsIgnoreCase(client.player.getGameProfile().getName())) {
            notice = "Нельзя отправить приглашение самому себе.";
            return;
        }
        client.getNetworkHandler().sendChatCommand("msg " + name + " Заходи в мой бой! Открой мод клавишей B.");
        notice = "Приглашение отправлено игроку " + name + " через чат сервера.";
    }

    private void startMatch() {
        playing = true; gameOver = false; victory = false; page = Page.MATCH;
        playerHealth = 100; attackCooldown = 0; abilityCooldown = 0; dashCooldown = 0;
        invulnerableTicks = 0; elapsedTicks = 0; kills = 0;
        playerX = width / 2.0f; playerY = height / 2.0f;
        enemies.clear();
        enemies.add(new Enemy(width / 2.0f - 145, height / 2.0f - 75, 0xFFFF6B6B));
        enemies.add(new Enemy(width / 2.0f + 145, height / 2.0f - 65, 0xFF68D391));
        enemies.add(new Enemy(width / 2.0f + 5, height / 2.0f + 145, 0xFF75B8FF));
        w = a = s = d = false;
        init();
    }

    @Override
    public void tick() {
        super.tick();
        if (!playing || gameOver || client == null) return;
        // Keep vanilla player movement keys released while this custom arena screen is active.
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        elapsedTicks++;
        if (effectTicks > 0) effectTicks--;
        playerHidden = inBush(playerX, playerY);
        if (attackCooldown > 0) attackCooldown--;
        if (abilityCooldown > 0) abilityCooldown--;
        if (dashCooldown > 0) dashCooldown--;
        if (invulnerableTicks > 0) invulnerableTicks--;

        double dx = (d ? 1 : 0) - (a ? 1 : 0);
        double dy = (s ? 1 : 0) - (w ? 1 : 0);
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            float speed = 3.0f;
            float nx = clamp(playerX + (float)(dx / len * speed), 26, width - 26);
            float ny = clamp(playerY + (float)(dy / len * speed), 54, height - 28);
            if (!insideObstacle(nx, playerY)) playerX = nx;
            if (!insideObstacle(playerX, ny)) playerY = ny;
        }

        for (Enemy enemy : enemies) {
            if (playerHidden) continue;
            double ex = playerX - enemy.x, ey = playerY - enemy.y;
            double distance = Math.hypot(ex, ey);
            if (distance > 28) {
                float nx = enemy.x + (float)(ex / Math.max(1, distance) * 0.88);
                float ny = enemy.y + (float)(ey / Math.max(1, distance) * 0.88);
                if (!insideObstacle(nx, enemy.y)) enemy.x = nx;
                if (!insideObstacle(enemy.x, ny)) enemy.y = ny;
            } else if (enemy.hitCooldown <= 0 && invulnerableTicks <= 0) {
                playerHealth -= 8; invulnerableTicks = 20; enemy.hitCooldown = 34;
            }
            if (enemy.hitCooldown > 0) enemy.hitCooldown--;
        }
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) if (it.next().health <= 0) { it.remove(); kills++; }
        if (playerHealth <= 0) finishMatch(false);
        else if (enemies.isEmpty()) finishMatch(true);
    }

    private boolean inBush(float x, float y) {
        return inRect(x, y, width / 2 - 220, height / 2 - 135, 82, 58)
                || inRect(x, y, width / 2 + 145, height / 2 - 125, 82, 58)
                || inRect(x, y, width / 2 - 45, height / 2 + 155, 82, 58)
                || inRect(x, y, width / 2 + 220, height / 2 + 105, 72, 58);
    }

    private boolean insideObstacle(float x, float y) {
        return inRect(x, y, width / 2 - 92, height / 2 - 26, 54, 42)
                || inRect(x, y, width / 2 + 62, height / 2 + 46, 54, 42)
                || inRect(x, y, width / 2 - 176, height / 2 + 105, 54, 42);
    }
    private boolean inRect(float x, float y, int ox, int oy, int ow, int oh) {
        return x > ox - 18 && x < ox + ow + 18 && y > oy - 18 && y < oy + oh + 18;
    }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private void finishMatch(boolean won) { gameOver = true; victory = won; w = a = s = d = false; init(); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page == Page.FRIENDS && !playing) {
            if (keyCode == GLFW.GLFW_KEY_НАЗАДSPACE) {
                if (!friendName.isEmpty()) friendName.deleteCharAt(friendName.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) { sendInvite(); return true; }
        }
        if (playing && !gameOver) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> w = true;
                case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> a = true;
                case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> s = true;
                case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> d = true;
                case GLFW.GLFW_KEY_Q -> usePower();
                case GLFW.GLFW_KEY_E -> dash();
                case GLFW.GLFW_KEY_ESCAPE -> { playing = false; page = Page.HOME; init(); }
                default -> { }
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && page != Page.HOME) { page = Page.HOME; init(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> w = false;
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> a = false;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> s = false;
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> d = false;
            default -> { }
        }
        return playing || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (page == Page.FRIENDS && friendName.length() < 16 && (Character.isLetterOrDigit(chr) || chr == '_')) {
            friendName.append(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (playing && !gameOver) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && attackCooldown <= 0) {
                attackAt(mouseX, mouseY, ATTACK_DAMAGE[fighterIndex], ATTACK_RANGE[fighterIndex]); attackCooldown = 12; return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { usePower(); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void usePower() {
        if (abilityCooldown > 0) return;
        int i = fighterIndex;
        attackAt(playerX, playerY, SUPER_DAMAGE[i], SUPER_RANGE[i]);
        effectTicks = 18; effectX = playerX; effectY = playerY; effectColor = COLORS[i];
        abilityCooldown = 100;
    }
    private void dash() {
        if (dashCooldown > 0) return;
        double dx = (d ? 1 : 0) - (a ? 1 : 0), dy = (s ? 1 : 0) - (w ? 1 : 0);
        if (dx == 0 && dy == 0) dy = -1;
        double len = Math.hypot(dx, dy);
        float nx = clamp(playerX + (float)(dx / len * 78), 26, width - 26);
        float ny = clamp(playerY + (float)(dy / len * 78), 54, height - 28);
        if (!insideObstacle(nx, ny)) { playerX = nx; playerY = ny; }
        invulnerableTicks = 12; dashCooldown = 75; effectTicks = 10; effectX = playerX; effectY = playerY; effectColor = 0xFFB8F5FF;
    }
    private void attackAt(double x, double y, int damage, double radius) {
        Enemy target = null; double best = Double.MAX_VALUE;
        for (Enemy e : enemies) {
            double clickDist = Math.hypot(e.x - x, e.y - y);
            double playerDist = Math.hypot(e.x - playerX, e.y - playerY);
            if (clickDist < radius && playerDist < 245 && clickDist < best) { target = e; best = clickDist; }
        }
        if (target != null) { target.health -= damage; effectTicks = 7; effectX = target.x; effectY = target.y; effectColor = COLORS[fighterIndex]; }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);
        if (page == Page.HOME) renderHome(context);
        else if (page == Page.BRAWLERS) renderBrawlers(context);
        else if (page == Page.FRIENDS) renderFriends(context);
        else renderArena(context);
        if (page == Page.MATCH && gameOver) renderResults(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHome(DrawContext c) {
        int cx = width / 2, top = height / 2 - 170;
        c.fill(cx - 235, top, cx + 235, top + 330, PANEL);
        c.fill(cx - 235, top, cx + 235, top + 5, GOLD);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("БЛОКОВАЯ АРЕНА"), cx, top + 22, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("ВЫБЕРИ БОЙЦА. ЗАХВАТИ АРЕНУ."), cx, top + 43, GOLD);
        drawHero(c, cx, top + 144, fighterIndex, 3);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(FIGHTERS[fighterIndex]), cx, top + 203, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(ROLES[fighterIndex]), cx, top + 220, GOLD);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(LORE[fighterIndex]), cx, top + 239, 0xFFD4DEED);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("WASD — ДВИЖЕНИЕ   •   ЛКМ — АТАКА   •   Q — СУПЕР   •   E — РЫВОК"), cx, top + 278, 0xFFB8C7DC);
    }

    private void renderBrawlers(DrawContext c) {
        int cx = width / 2, top = height / 2 - 170;
        c.fill(cx - 235, top, cx + 235, top + 330, PANEL);
        c.fill(cx - 235, top, cx + 235, top + 5, GOLD);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("КОЛЛЕКЦИЯ БОЙЦОВ"), cx, top + 20, 0xFFFFFFFF);
        drawHero(c, cx, top + 118, fighterIndex, 2);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(FIGHTERS[fighterIndex]), cx, top + 170, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(ROLES[fighterIndex]), cx, top + 188, GOLD);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(LORE[fighterIndex]), cx, top + 207, 0xFFD4DEED);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Выбери бойца в списке ниже"), cx, top + 235, 0xFFB8C7DC);
    }

    private void renderFriends(DrawContext c) {
        int cx = width / 2, top = height / 2 - 110;
        c.fill(cx - 205, top, cx + 205, top + 220, PANEL);
        c.fill(cx - 205, top, cx + 205, top + 5, GOLD);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("ИГРА С ДРУГОМ"), cx, top + 22, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Ник игрока Minecraft"), cx, top + 65, 0xFFD4DEED);
        c.fill(cx - 105, top + 82, cx + 105, top + 108, 0xFF0D1725);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(friendName + (elapsedTicks % 20 < 10 ? "_" : "")), cx, top + 91, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Оба игрока должны быть на одном сервере."), cx, top + 130, 0xFFB8C7DC);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Приглашение отправляется через чат. Совместные матчи ещё разрабатываются."), cx, top + 149, 0xFF9EADC2);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(notice), cx, top + 181, 0xFFFFD166);
    }

    private void renderArena(DrawContext c) {
        c.fill(12, 42, width - 12, height - 12, 0xFF24434A);
        for (int x = 20; x < width - 20; x += 32)
            for (int y = 48; y < height - 18; y += 32) c.fill(x, y, x + 1, y + 1, 0x44FFFFFF);
        drawObstacle(c, width / 2 - 92, height / 2 - 26);
        drawObstacle(c, width / 2 + 62, height / 2 + 46);
        drawObstacle(c, width / 2 - 176, height / 2 + 105);
        drawBush(c, width / 2 - 220, height / 2 - 135);
        drawBush(c, width / 2 + 145, height / 2 - 125);
        drawBush(c, width / 2 - 45, height / 2 + 155);
        drawBush(c, width / 2 + 220, height / 2 + 105);
        c.fill(0, 0, width, 36, 0xEE111B2A);
        c.drawTextWithShadow(textRenderer, Text.literal("БЛОКОВАЯ АРЕНА"), 12, 13, GOLD);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("ПРОТИВНИКИ: " + enemies.size()), width / 2, 13, 0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer, Text.literal("ПОБЕД: " + kills), width - 70, 13, 0xFFFFFFFF);
        healthBar(c, 14, 44, 132, 9, playerHealth, 100, 0xFF55D68B);
        if (playerHidden) c.drawTextWithShadow(textRenderer, Text.literal("В КУСТАХ — ВАС НЕ ВИДНО"), 155, 44, 0xFF9BFFA9);
        c.drawTextWithShadow(textRenderer, Text.literal(FIGHTERS[fighterIndex]), 14, 56, 0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer, Text.literal("ЛКМ — АТАКА   Q — СУПЕР   E — РЫВОК   ESC — МЕНЮ"), 14, height - 23, 0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer, Text.literal("Q " + (abilityCooldown == 0 ? "ГОТОВО" : abilityCooldown / 20 + "s") + "   E " + (dashCooldown == 0 ? "ГОТОВО" : dashCooldown / 20 + "s")), width - 145, height - 23, GOLD);
        for (Enemy e : enemies) {
            healthBar(c, (int)e.x - 18, (int)e.y - 29, 36, 4, e.health, 100, 0xFFFF7777);
            drawHero(c, (int)e.x, (int)e.y, 1, 1, e.color);
        }
        if (effectTicks > 0) {
            int radius = Math.max(8, 46 - effectTicks * 2);
            c.fill((int)effectX - radius, (int)effectY - radius, (int)effectX + radius, (int)effectY + radius, (effectColor & 0x00FFFFFF) | 0x44000000);
            c.fill((int)effectX - 2, (int)effectY - radius - 5, (int)effectX + 2, (int)effectY + radius + 5, effectColor);
            c.fill((int)effectX - radius - 5, (int)effectY - 2, (int)effectX + radius + 5, (int)effectY + 2, effectColor);
        }
        int color = invulnerableTicks > 0 && (elapsedTicks / 3) % 2 == 0 ? 0xFFFFFFFF : COLORS[fighterIndex];
        drawHero(c, (int)playerX, (int)playerY, fighterIndex, 1, color);
    }

    private void drawBush(DrawContext c, int x, int y) {
        c.fill(x, y + 12, x + 76, y + 42, 0xFF174D32);
        c.fill(x + 5, y + 5, x + 34, y + 34, 0xFF2F8D4E);
        c.fill(x + 25, y, x + 58, y + 34, 0xFF3BA65B);
        c.fill(x + 48, y + 9, x + 76, y + 38, 0xFF287C45);
        c.fill(x + 15, y + 13, x + 25, y + 21, 0xFF72C96B);
        c.fill(x + 42, y + 8, x + 52, y + 17, 0xFF79D66D);
    }

    private void drawObstacle(DrawContext c, int x, int y) {
        c.fill(x, y, x + 58, y + 46, 0xFF14262B);
        c.fill(x + 3, y + 3, x + 55, y + 42, 0xFF9A6845);
        c.fill(x + 8, y + 8, x + 50, y + 14, 0x66FFFFFF);
        c.fill(x + 8, y + 27, x + 22, y + 37, 0x55351D13);
    }

    private void drawHero(DrawContext c, int x, int y, int type, int scale) {
        drawHero(c, x, y, type, scale, COLORS[type]);
    }
    private void drawHero(DrawContext c, int x, int y, int type, int scale, int color) {
        int u = scale;
        c.fill(x - 19*u, y + 13*u, x + 19*u, y + 20*u, 0x66000000);
        c.fill(x - 13*u, y - 4*u, x - 7*u, y + 17*u, 0xFF263247);
        c.fill(x + 7*u, y - 4*u, x + 13*u, y + 17*u, 0xFF263247);
        c.fill(x - 16*u, y - 14*u, x + 16*u, y + 10*u, color);
        c.fill(x - 12*u, y - 22*u, x + 12*u, y - 10*u, color);
        c.fill(x - 10*u, y - 19*u, x - 5*u, y - 14*u, 0xFFFFFFFF);
        c.fill(x + 5*u, y - 19*u, x + 10*u, y - 14*u, 0xFFFFFFFF);
        c.fill(x - 8*u, y - 18*u, x - 6*u, y - 15*u, 0xFF17202C);
        c.fill(x + 6*u, y - 18*u, x + 8*u, y - 15*u, 0xFF17202C);
        if (type == 0) { c.fill(x - 18*u, y - 9*u, x - 13*u, y + 2*u, 0xFFFFD166); c.fill(x + 13*u, y - 9*u, x + 18*u, y + 2*u, 0xFFFFD166); }
        else if (type == 1) { c.fill(x - 18*u, y - 25*u, x + 18*u, y - 20*u, 0xFF477D68); c.fill(x - 5*u, y - 10*u, x + 5*u, y + 6*u, 0xFF2B765B); }
        else if (type == 2) { c.fill(x - 20*u, y - 12*u, x - 14*u, y - 4*u, 0xFFE8F7FF); c.fill(x + 14*u, y - 12*u, x + 20*u, y - 4*u, 0xFFE8F7FF); c.fill(x - 4*u, y - 27*u, x + 4*u, y - 21*u, 0xFFB8E9FF); }
        else { c.fill(x - 17*u, y - 24*u, x + 17*u, y - 19*u, 0xFF7758B8); c.fill(x - 5*u, y - 29*u, x + 5*u, y - 22*u, 0xFFFFE7A3); }
    }

    private void healthBar(DrawContext c, int x, int y, int w, int h, float value, float max, int color) {
        c.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF10151D);
        c.fill(x, y, x + w, y + h, 0xFF713E48);
        int fill = (int)(w * Math.max(0, value) / max);
        if (fill > 0) c.fill(x, y, x + fill, y + h, color);
    }

    private void renderResults(DrawContext c) {
        c.fill(width / 2 - 130, height / 2 - 75, width / 2 + 130, height / 2 + 112, 0xF0182438);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(victory ? "АРЕНА ЗАЧИЩЕНА!" : "БОЙ ОКОНЧЕН"), width / 2, height / 2 - 48, victory ? GOLD : 0xFFFF7777);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Побеждено врагов: " + kills + " / 3"), width / 2, height / 2 - 20, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Время: " + elapsedTicks / 20 + " сек."), width / 2, height / 2 + 1, 0xFFDCE7F7);
    }

    @Override public boolean shouldPause() { return false; }

    private static final class Enemy {
        float x, y, health = 100;
        int hitCooldown = 25;
        int color;
        Enemy(float x, float y, int color) { this.x = x; this.y = y; this.color = color; }
    }
}
