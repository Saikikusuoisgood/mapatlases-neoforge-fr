package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class BookmarkButton extends AbstractWidget {

    private final int xOff;
    private final int yOff;
    protected final AtlasOverviewScreen parentScreen;
    protected boolean selected = true;

    protected BookmarkButton(int pX, int pY, int width, int height, int xOff, int yOff, AtlasOverviewScreen screen) {
        super(pX, pY,
                width, height,
                Component.empty());
        this.xOff = xOff;
        this.yOff = yOff;
        this.parentScreen = screen;

    }


    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.enableDepthTest();
        if (!visible || !active) return;
        pGuiGraphics.blit(AtlasOverviewScreen.ATLAS_TEXTURE,
                this.getX(), this.getY(), xOff,
                yOff + (this.selected ? this.height : 0),
                this.width, this.height);

    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        if (!visible || !active) return null;
        return super.getTooltip();
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    public void setActive(boolean active) {
        this.active = active;
        this.visible = active;
        this.setTooltip(active ? createTooltip() : null);
    }

    public Tooltip createTooltip() {
        return getTooltip();
    }
}
