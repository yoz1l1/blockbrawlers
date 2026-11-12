package dev.blockbrawlers.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class BlockBrawlersScreen extends Screen {
    private static final int PANEL = 0xE91B2234;
    private static final int ACCENT = 0xFFFFC857;
    private String selectedFighter = "EMBER";
    private String status = "Choose a fighter and enter the training arena.";

    public BlockBrawlersScreen() {
        super(Text.literal("Block Brawlers"));
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int top = Math.max(72, this.height / 2 - 72);
        int buttonWidth = 150;
        int gap = 12;
        int left = center - buttonWidth - gap / 2;
        int right = center + gap / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("EMBER  ·  Balanced"), b -> select("EMBER"))
                .dimensions(left, top, buttonWidth, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("MOSS  ·  Defender"), b -> select("MOSS"))
                .dimensions(right, top, buttonWidth, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("VOLT  ·  Swift"), b -> select("VOLT"))
                .dimensions(left, top + 31, buttonWidth, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("NOVA  ·  Support"), b -> select("NOVA"))
                .dimensions(right, top + 31, buttonWidth, 24).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("TRAINING ARENA"), b -> {
            status = "Training mode is the next gameplay milestone. Your fighter: " + selectedFighter;
        }).dimensions(center - 156, top + 75, 150, 28).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("ONLINE ROOMS  ·  WIP"), b -> {
            status = "Online matchmaking is not active yet. No account or network data is sent by this prototype.";
        }).dimensions(center + 6, top + 75, 150, 28).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("BACK"), b -> close())
                .dimensions(center - 50, top + 115, 100, 20).build());
    }

    private void select(String fighter) {
        selectedFighter = fighter;
        status = "Selected fighter: " + fighter;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fill(0, 0, width, height, 0xB9080D18);
        int panelWidth = Math.min(440, width - 24);
        int panelLeft = (width - panelWidth) / 2;
        int panelTop = Math.max(22, height / 2 - 128);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 260, PANEL);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 4, ACCENT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("BLOCK BRAWLERS"), width / 2, panelTop + 15, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("A new arena. Your world. Your crew."), width / 2, panelTop + 34, 0xFFB8C4D9);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("SELECT YOUR FIGHTER"), width / 2, panelTop + 59, ACCENT);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Selected: " + selectedFighter), width / 2, panelTop + 211, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, panelTop + 230, 0xFFB8C4D9);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Press B to open  •  Esc to close"), width / 2, height - 16, 0xFF8E9AAF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
