package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.List;

import static pepjebs.mapatlases.client.MapAtlasesClient.DIMENSION_TEXTURE_ORDER;

public class DimensionBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 18;
    private static final int BUTTON_W = 24;

    private final int dimY;
    private final ResourceKey<Level> dimension;
    private final MapAtlasesAtlasOverviewScreen parentScreen;



    protected DimensionBookmarkButton(int pX, int pY, ResourceKey<Level> dimension, MapAtlasesAtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, 0, MapAtlasesAtlasOverviewScreen.IMAGE_HEIGHT);
        this.dimension = dimension;
        this.parentScreen = screen;
        this.setTooltip(Tooltip.create(Component.literal(MapAtlasesAtlasOverviewScreen.getReadableName(dimension.location()))));
        int i = DIMENSION_TEXTURE_ORDER.indexOf(dimension.location().toString());
        if (i == -1) i = 10;
        this.dimY = 16 * i;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();

        if (selected()) {
            pose.translate(0, 0, 2);
        }
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.blit(MapAtlasesAtlasOverviewScreen.ATLAS_TEXTURE,
                this.getX() + 4, this.getY() + 2, MapAtlasesAtlasOverviewScreen.IMAGE_WIDTH,
                dimY,
                16, 16);
        pose.popPose();

    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        return super.getTooltip();
    }


    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.setSelected(true);
        parentScreen.selectDimension(dimension);
    }
}
