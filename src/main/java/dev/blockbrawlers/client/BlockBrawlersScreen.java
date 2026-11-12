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
    private static final int PANEL = 0xEE1B2940;
    private static final int GOLD = 0xFFFFD166;
    private static final String[] FIGHTERS = {"EMBER", "MOSS", "VOLT", "NOVA"};
    private static final String[] ROLES = {"Blaster", "Guardian", "Runner", "Mystic"};

    private String selectedFighter = "EMBER";
    private int fighterIndex = 0;
    private boolean playing = false;
    private boolean gameOver = false;
    private boolean victory = false;
    private float playerX;
    private float playerY;
    private float playerHealth = 100;
    private int attackCooldown = 0;
    private int abilityCooldown = 0;
    private int invulnerableTicks = 0;
    private int elapsedTicks = 0;
    private int kills = 0;
    private final List<Enemy> enemies = new ArrayList<>();

    public BlockBrawlersScreen() {
        super(Text.literal("Block Brawlers"));
    }

    @Override
    protected void init() {
        clearChildren();
        int center = width / 2;
        if (!playing) {
            int top = height / 2 + 48;
            addDrawableChild(ButtonWidget.builder(Text.literal("CHANGE FIGHTER  <  " + selectedFighter + "  >"), b -> {
                fighterIndex = (fighterIndex + 1) % FIGHTERS.length;
                selectedFighter = FIGHTERS[fighterIndex];
                init();
            }).dimensions(center - 112, top, 224, 26).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("PLAY MATCH"), b -> startMatch())
                    .dimensions(center - 90, top + 36, 180, 34).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("CLOSE"), b -> close())
                    .dimensions(center - 60, top + 76, 120, 22).build());
        } else if (gameOver) {
            addDrawableChild(ButtonWidget.builder(Text.literal("PLAY AGAIN"), b -> startMatch())
                    .dimensions(center - 82, height / 2 + 40, 164, 30).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("MAIN MENU"), b -> {
                playing = false;
                gameOver = false;
                init();
            }).dimensions(center - 82, height / 2 + 78, 164, 24).build());
        }
    }

    private void startMatch() {
        playing = true;
        gameOver = false;
        victory = false;
        playerHealth = 100;
        attackCooldown = 0;
        abilityCooldown = 0;
        invulnerableTicks = 0;
        elapsedTicks = 0;
        kills = 0;
        playerX = width / 2.0f;
        playerY = height / 2.0f;
        enemies.clear();
        enemies.add(new Enemy(width / 2.0f - 150, height / 2.0f - 70, 0xFFFF6B6B));
        enemies.add(new Enemy(width / 2.0f + 150, height / 2.0f - 65, 0xFF68D391));
        enemies.add(new Enemy(width / 2.0f + 10, height / 2.0f + 145, 0xFF75B8FF));
        init();
    }

    @Override
    public void tick() {
        super.tick();
        if (!playing || gameOver || client == null) return;
        elapsedTicks++;
        if (attackCooldown > 0) attackCooldown--;
        if (abilityCooldown > 0) abilityCooldown--;
        if (invulnerableTicks > 0) invulnerableTicks--;

        double dx = 0;
        double dy = 0;
        if (client.options.forwardKey.isPressed()) dy--;
        if (client.options.backKey.isPressed()) dy++;
        if (client.options.leftKey.isPressed()) dx--;
        if (client.options.rightKey.isPressed()) dx++;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            playerX += (float) (dx / length * 3.15);
            playerY += (float) (dy / length * 3.15);
        }
        playerX = clamp(playerX, 28, width - 28);
        playerY = clamp(playerY, 55, height - 28);

        for (Enemy enemy : enemies) {
            double ex = playerX - enemy.x;
            double ey = playerY - enemy.y;
            double distance = Math.sqrt(ex * ex + ey * ey);
            if (distance > 27) {
                enemy.x += (float) (ex / Math.max(1, distance) * 0.92);
                enemy.y += (float) (ey / Math.max(1, distance) * 0.92);
            } else if (enemy.hitCooldown <= 0 && invulnerableTicks <= 0) {
                playerHealth -= 9;
                invulnerableTicks = 18;
                enemy.hitCooldown = 34;
            }
            if (enemy.hitCooldown > 0) enemy.hitCooldown--;
            enemy.x = clamp(enemy.x, 24, width - 24);
            enemy.y = clamp(enemy.y, 50, height - 24);
        }

        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.health <= 0) {
                iterator.remove();
                kills++;
            }
        }
        if (playerHealth <= 0) finishMatch(false);
        else if (enemies.isEmpty()) finishMatch(true);
    }

    private void finishMatch(boolean won) {
        gameOver = true;
        victory = won;
        init();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (playing && !gameOver) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && attackCooldown <= 0) {
                attackAt(mouseX, mouseY, 34, 76);
                attackCooldown = 11;
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && abilityCooldown <= 0) {
                attackAt(mouseX, mouseY, 52, 104);
                abilityCooldown = 90;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void attackAt(double x, double y, int damage, double radius) {
        Enemy target = null;
        double best = Double.MAX_VALUE;
        for (Enemy enemy : enemies) {
            double clickDistance = Math.hypot(enemy.x - x, enemy.y - y);
            double playerDistance = Math.hypot(enemy.x - playerX, enemy.y - playerY);
            if (clickDistance < radius && playerDistance < 230 && clickDistance < best) {
                target = enemy;
                best = clickDistance;
            }
        }
        if (target != null) target.health -= damage;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);
        if (!playing) {
            renderMenu(context);
        } else {
            renderArena(context);
            if (gameOver) renderResults(context);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMenu(DrawContext context) {
        int cx = width / 2;
        int top = height / 2 - 180;
        context.fill(cx - 190, top, cx + 190, top + 360, PANEL);
        context.fill(cx - 190, top, cx + 190, top + 5, GOLD);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("BLOCK BRAWLERS"), cx, top + 22, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("QUICK ARENA • SOLO SURVIVAL"), cx, top + 44, GOLD);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Choose your champion"), cx, top + 91, 0xFFDCE7F7);
        drawFighter(context, cx - 115, top + 126, 0xFFFF875E, "E");
        drawFighter(context, cx - 38, top + 126, 0xFF62C99A, "M");
        drawFighter(context, cx + 38, top + 126, 0xFF65B9FF, "V");
        drawFighter(context, cx + 115, top + 126, 0xFFC69BFF, "N");
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(ROLES[fighterIndex]), cx, top + 163, 0xFFB8C7DC);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("WASD  Move     •     LMB  Attack"), cx, top + 185, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("RMB  Power burst     •     Defeat all 3 rivals"), cx, top + 202, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Offline practice match — no account required"), cx, top + 238, 0xFF9EADC2);
    }

    private void renderArena(DrawContext context) {
        context.fill(12, 42, width - 12, height - 12, 0xFF263B43);
        for (int x = 20; x < width - 20; x += 32) {
            for (int y = 48; y < height - 18; y += 32) {
                context.fill(x, y, x + 1, y + 1, 0x334FFFFFFF);
            }
        }
        // Original arena obstacles
        drawObstacle(context, width / 2 - 92, height / 2 - 26);
        drawObstacle(context, width / 2 + 62, height / 2 + 46);
        drawObstacle(context, width / 2 - 176, height / 2 + 105);
        context.fill(0, 0, width, 36, 0xEE111B2A);
        context.drawTextWithShadow(textRenderer, Text.literal("BLOCK BRAWLERS"), 12, 13, GOLD);
        context.drawTextWithShadow(textRenderer, Text.literal("RIVALS: " + enemies.size()), width / 2 - 35, 13, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("KOs: " + kills), width - 70, 13, 0xFFFFFFFF);

        drawHealthBar(context, 14, 44, 132, 9, playerHealth, 100, 0xFF55D68B);
        context.drawTextWithShadow(textRenderer, Text.literal(selectedFighter), 14, 56, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("LMB Attack"), 14, height - 25, 0xFFE7EDF7);
        context.drawTextWithShadow(textRenderer, Text.literal("RMB Power " + (abilityCooldown == 0 ? "READY" : abilityCooldown / 20 + "s")), 110, height - 25, GOLD);

        for (Enemy enemy : enemies) {
            drawHealthBar(context, (int) enemy.x - 17, (int) enemy.y - 27, 34, 4, enemy.health, 100, 0xFFFF7777);
            drawFighter(context, (int) enemy.x, (int) enemy.y, enemy.color, "!");
        }
        int playerColor = invulnerableTicks > 0 && (elapsedTicks / 3) % 2 == 0 ? 0xFFFFFFFF : fighterColor();
        drawFighter(context, (int) playerX, (int) playerY, playerColor, selectedFighter.substring(0, 1));
        context.fill((int) playerX - 2, (int) playerY + 19, (int) playerX + 3, (int) playerY + 22, 0x55000000);
    }

    private void drawObstacle(DrawContext context, int x, int y) {
        context.fill(x, y, x + 54, y + 42, 0xFF17252D);
        context.fill(x + 3, y + 3, x + 51, y + 38, 0xFFB17A4A);
        context.fill(x + 7, y + 7, x + 47, y + 12, 0x55FFFFFF);
    }

    private void drawFighter(DrawContext context, int x, int y, int color, String mark) {
        context.fill(x - 13, y - 10, x + 13, y + 15, 0x77000000);
        context.fill(x - 12, y - 13, x + 12, y + 11, color);
        context.fill(x - 8, y - 17, x + 8, y - 10, color);
        context.fill(x - 6, y - 5, x - 3, y - 2, 0xFF17202C);
        context.fill(x + 3, y - 5, x + 6, y - 2, 0xFF17202C);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(mark), x, y + 1, 0xFFFFFFFF);
    }

    private void drawHealthBar(DrawContext context, int x, int y, int w, int h, float value, float max, int color) {
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF10151D);
        context.fill(x, y, x + w, y + h, 0xFF713E48);
        int filled = (int) (w * Math.max(0, value) / max);
        if (filled > 0) context.fill(x, y, x + filled, y + h, color);
    }

    private int fighterColor() {
        return switch (fighterIndex) {
            case 1 -> 0xFF62C99A;
            case 2 -> 0xFF65B9FF;
            case 3 -> 0xFFC69BFF;
            default -> 0xFFFF875E;
        };
    }

    private void renderResults(DrawContext context) {
        context.fill(width / 2 - 130, height / 2 - 75, width / 2 + 130, height / 2 + 112, 0xF0182438);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(victory ? "ARENA CLEARED!" : "MATCH OVER"), width / 2, height / 2 - 48, victory ? GOLD : 0xFFFF7777);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Rivals defeated: " + kills + " / 3"), width / 2, height / 2 - 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Time: " + elapsedTicks / 20 + " seconds"), width / 2, height / 2 + 1, 0xFFDCE7F7);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class Enemy {
        float x;
        float y;
        float health = 100;
        int hitCooldown = 25;
        final int color;

        Enemy(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
}
