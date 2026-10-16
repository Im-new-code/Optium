package me.modmuss50.optium.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import me.modmuss50.optium.mod.OptifineVersion;
import me.modmuss50.optium.mod.OptiumError;
import me.modmuss50.optium.mod.OptiumSetup;

/**
 * Lightweight runtime diagnostics screen. It intentionally uses vanilla UI
 * components and does no background polling or renderer modification.
 */
public final class OptiumStatusScreen extends Screen {
    private static final int LABEL_COLOR = 0x9AB0D3;
    private static final int VALUE_COLOR = 0xEAF0FF;
    private static final int GOOD_COLOR = 0x63D9B0;
    private static final int WARN_COLOR = 0xF0C36A;
    private static final int ERROR_COLOR = 0xFF7777;

    private final Screen parent;

    public OptiumStatusScreen(Screen parent) {
        super(new LiteralText("Optium Status"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonY = this.height - 30;
        this.addButton(new ButtonWidget(this.width / 2 - 124, buttonY, 120, 20,
                new LiteralText("Copy diagnostics"), button -> {
                    if (this.client != null) {
                        this.client.keyboard.setClipboard(buildDiagnostics());
                    }
                }));
        this.addButton(new ButtonWidget(this.width / 2 + 4, buttonY, 120, 20,
                new LiteralText("Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        drawCentered(matrices, "OPTIUM STATUS", 22, 0xFFFFFF);
        drawCentered(matrices, "Lightweight diagnostics for this Minecraft profile", 40, LABEL_COLOR);

        int left = Math.max(12, this.width / 2 - 205);
        int valueX = left + 154;
        int y = 68;

        drawRow(matrices, left, valueX, y, "Optium", modVersion(), GOOD_COLOR); y += 22;
        drawRow(matrices, left, valueX, y, "Bridge", bridgeStatus(), bridgeColor()); y += 22;
        drawRow(matrices, left, valueX, y, "OptiFine", optifineStatus(), optifineColor()); y += 22;
        drawRow(matrices, left, valueX, y, "Renderer", rendererStatus(),
                OptiumSetup.disabledForExternalRenderer ? WARN_COLOR : VALUE_COLOR); y += 22;
        drawRow(matrices, left, valueX, y, "ReplayMod", replayModStatus(), replayModColor()); y += 22;
        drawRow(matrices, left, valueX, y, "Minecraft", modVersionFor("minecraft"), VALUE_COLOR); y += 22;
        drawRow(matrices, left, valueX, y, "Fabric Loader", modVersionFor("fabricloader"), VALUE_COLOR); y += 22;
        drawRow(matrices, left, valueX, y, "Java", System.getProperty("java.version", "unknown"), VALUE_COLOR); y += 22;
        drawRow(matrices, left, valueX, y, "Loaded mods", String.valueOf(FabricLoader.getInstance().getAllMods().size()), VALUE_COLOR); y += 22;
        drawRow(matrices, left, valueX, y, "Java heap", heapSummary(), WARN_COLOR);

        drawCentered(matrices, "Press O in-game to open this screen. Rebind it in Controls if needed.",
                this.height - 48, LABEL_COLOR);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawCentered(MatrixStack matrices, String message, int y, int color) {
        int x = (this.width - this.textRenderer.getWidth(message)) / 2;
        this.textRenderer.drawWithShadow(matrices, message, x, y, color);
    }

    private void drawRow(MatrixStack matrices, int left, int valueX, int y,
                         String label, String value, int valueColor) {
        this.textRenderer.drawWithShadow(matrices, label, left, y, LABEL_COLOR);
        this.textRenderer.drawWithShadow(matrices, value, valueX, y, valueColor);
    }

    private String modVersion() {
        return modVersionFor("optium");
    }

    private String modVersionFor(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private String bridgeStatus() {
        if (OptiumError.hasError()) return "Setup error";
        if (OptiumSetup.disabledForExternalRenderer) return "Bypassed for external renderer";
        if (OptiumSetup.optifineRuntimeJar != null) return "Active";
        return "Not initialized / OptiFine missing";
    }

    private int bridgeColor() {
        if (OptiumError.hasError()) return ERROR_COLOR;
        if (OptiumSetup.disabledForExternalRenderer) return WARN_COLOR;
        return OptiumSetup.optifineRuntimeJar != null ? GOOD_COLOR : WARN_COLOR;
    }

    private String optifineStatus() {
        if (OptifineVersion.version != null && !OptifineVersion.version.isEmpty()) {
            return OptifineVersion.version;
        }
        return OptiumError.hasError() ? "Unavailable (setup error)" : "Not detected yet";
    }

    private int optifineColor() {
        return OptifineVersion.version != null && !OptifineVersion.version.isEmpty()
                ? GOOD_COLOR : WARN_COLOR;
    }

    private String rendererStatus() {
        if (OptiumSetup.disabledForExternalRenderer) {
            return "Sodium/Iris detected; OptiFine bridge is off";
        }
        return "OptiFine bridge profile";
    }

    private String replayModStatus() {
        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("replaymod")) return "Not installed";
        if (OptiumSetup.replayModKeyboardWorkaroundApplied) return "Keyboard workaround applied";
        return "Detected; workaround not confirmed in this session";
    }

    private int replayModColor() {
        if (!FabricLoader.getInstance().isModLoaded("replaymod")) return LABEL_COLOR;
        return OptiumSetup.replayModKeyboardWorkaroundApplied ? GOOD_COLOR : WARN_COLOR;
    }

    private String heapSummary() {
        Runtime runtime = Runtime.getRuntime();
        long usedMiB = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMiB = runtime.maxMemory() / (1024L * 1024L);
        return usedMiB + " / " + maxMiB + " MiB";
    }

    private String buildDiagnostics() {
        StringBuilder report = new StringBuilder();
        report.append("Optium ").append(modVersion()).append('\n');
        report.append("Minecraft: ").append(modVersionFor("minecraft")).append('\n');
        report.append("Fabric Loader: ").append(modVersionFor("fabricloader")).append('\n');
        report.append("Java: ").append(System.getProperty("java.version", "unknown")).append('\n');
        report.append("Bridge: ").append(bridgeStatus()).append('\n');
        report.append("OptiFine: ").append(optifineStatus()).append('\n');
        report.append("Renderer: ").append(rendererStatus()).append('\n');
        report.append("ReplayMod: ").append(replayModStatus()).append('\n');
        report.append("Loaded mods: ").append(FabricLoader.getInstance().getAllMods().size()).append('\n');
        report.append("Java heap: ").append(heapSummary()).append('\n');
        if (OptiumError.hasError()) {
            report.append("Optium error: ").append(OptiumError.getError()).append('\n');
        }
        report.append("Device/runtime details can be added separately when reporting a problem.");
        return report.toString();
    }

    @Override
    public void onClose() {
        if (this.client != null) {
            this.client.openScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
