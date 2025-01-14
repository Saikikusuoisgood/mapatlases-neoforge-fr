package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.integration.MoonlightCompat;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;

import java.util.Locale;
import java.util.Map;

import static pepjebs.mapatlases.client.AbstractAtlasWidget.MAP_DIMENSION;

public abstract class DecorationBookmarkButton extends BookmarkButton {

    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");

    private static final int BUTTON_H = 14;
    private static final int BUTTON_W = 24;
    protected final Pair<String, MapItemSavedData> data;

    protected int index = 0;
    protected boolean shfting = false;

    protected DecorationBookmarkButton(int pX, int pY, AtlasOverviewScreen parentScreen, Pair<String, MapItemSavedData> data) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, 0, AtlasOverviewScreen.IMAGE_HEIGHT + 36, parentScreen);
        this.data = data;
    }

    public static DecorationBookmarkButton of(int px, int py, Object mapDecoration, Pair<String, MapItemSavedData> data, AtlasOverviewScreen screen) {
        if (mapDecoration instanceof MapDecoration md) return new Vanilla(px, py, screen, data, md);
        else {
            return MoonlightCompat.makeCustomButton(px, py,  screen, data, mapDecoration);
        }
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
       if (parentScreen.getMinecraft().options.keyShift.matches(pKeyCode, pScanCode)) {
           shfting = false;
           this.setTooltip(this.createTooltip());
       }
       return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (parentScreen.getMinecraft().options.keyShift.matches(pKeyCode, pScanCode)) {
            shfting = true;
            this.setTooltip(Tooltip.create(Component.translatable("tooltip.map_atlases.delete_marker")));
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.setSelected(true);
        if (shfting) {
            this.deleteMarker();
            parentScreen.removeBookmark(this);
        } else {
            parentScreen.focusDecoration(this);
        }
    }

    protected abstract void deleteMarker();


    public abstract double getWorldX() ;

    public abstract double getWorldZ();


    protected static double getDecorationPos(int decoX, MapItemSavedData data) {
        float s = (1 << data.scale) * (float) MAP_DIMENSION;
        return  (s / 2.0d) - ((s / 2.0d) * ((decoX + MAP_DIMENSION) / (float) MAP_DIMENSION));
    }

    public  int getBatchGroup() {
        return 0;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(pGuiGraphics,pMouseX, pMouseY, pPartialTick);
        if(this.shfting) {
            pGuiGraphics.blit(AtlasOverviewScreen.ATLAS_TEXTURE, getX(), getY(),
                    24, 167, 5, 5);

        }
    }

    public static class Vanilla extends DecorationBookmarkButton {

        private final MapDecoration decoration;

        public Vanilla(int px, int py, AtlasOverviewScreen screen, Pair<String, MapItemSavedData> data, MapDecoration mapDecoration) {
            super(px, py, screen, data);
            this.decoration = mapDecoration;
            this.setTooltip(createTooltip());
        }

        @Override
        public double getWorldX() {
            return data.getSecond().centerX - getDecorationPos(decoration.getX(), data.getSecond());
        }

        @Override
        public double getWorldZ() {
            return data.getSecond().centerZ - getDecorationPos(decoration.getY(), data.getSecond());
        }

        @Override
        public Tooltip createTooltip() {
            Component name = decoration.getName();
            Component mapIconComponent = name == null
                    ? Component.literal(
                    AtlasOverviewScreen.getReadableName(decoration.getType().name().toLowerCase(Locale.ROOT)))
                    : name;

            // draw text
            MutableComponent coordsComponent = Component.literal("X: " + decoration.getX() + ", Z: " + decoration.getY());
            MutableComponent formattedCoords = coordsComponent.setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY));
            return Tooltip.create(mapIconComponent);
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            PoseStack matrices = pGuiGraphics.pose();
            matrices.pushPose();
            matrices.translate(0,0,0.01*this.index);
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

            byte b = decoration.getImage();

            int u = (b % 16) * 8;
            int v = (b / 16) * 8;

            matrices.translate(getX() + width / 2f, getY() + height / 2f, 1.0D);
            matrices.mulPose(Axis.ZP.rotationDegrees((decoration.getRot() * 360) / 16.0F));
            matrices.scale(-1, -1, 1);

            pGuiGraphics.blit(MAP_ICON_TEXTURE, -4, -4, u, v, 8, 8, 128, 128);

            matrices.popPose();

            //hide waiting to be activated by mapWidget
            setSelected(false);
        }


        @Override
        protected void deleteMarker() {
            Map<String, MapDecoration> decorations = data.getSecond().decorations;
            for(var d : decorations.entrySet()){
               var deco = d.getValue();
               if(deco == decoration){
                   //we cant use string id because server has them diferent...
                   MapAtlasesNetowrking.sendToServer(new C2SRemoveMarkerPacket(data.getFirst(), deco.hashCode()));
                   decorations.remove(d.getKey());
                   return;
               }
           }
        }
    }

}
