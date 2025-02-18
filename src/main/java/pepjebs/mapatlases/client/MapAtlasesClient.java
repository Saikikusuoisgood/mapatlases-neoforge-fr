package pepjebs.mapatlases.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.networking.S2CSetMapDataPacket;
import pepjebs.mapatlases.networking.S2CSyncMapCenterPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class MapAtlasesClient {

    private static final ThreadLocal<Float> globalDecorationScale = ThreadLocal.withInitial(() -> 1f);
    private static final ThreadLocal<Float> globalDecorationRotation = ThreadLocal.withInitial(() -> 0f);
    ;

    @Nullable
    private static MapKey currentActiveMapKey = null;
    private static ItemStack currentActiveAtlas = ItemStack.EMPTY;

    public static final Material OVERWORLD_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas"));
    public static final Material NETHER_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas_nether"));
    public static final Material END_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas_end"));
    public static final Material OTHER_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas_unknown"));

    public static final List<String> DIMENSION_TEXTURE_ORDER = List.of(Level.OVERWORLD.location().toString(),
            Level.NETHER.location().toString(), Level.END.location().toString(),
            "aether:the_aether", "twilightforest:twilight_forest", "undergarden:undergarden",
            "tropicraft:tropics", "thebetweenlands:betweenlands", "blue_skies:everbright",
            "the_bumblezone:the_bumblezone");

    public static final KeyMapping OPEN_ATLAS_KEYBIND = new KeyMapping(
            "key.map_atlases.open_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.map_atlases.minimap"
    );


    public static final KeyMapping INCREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_in_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_ADD,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping DECREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_out_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_SUBTRACT,
            "category.map_atlases.minimap"
    );

    public static void cachePlayerState(Player player) {
        if (player != Minecraft.getInstance().player) return;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        currentActiveAtlas = atlas;
        if (!atlas.isEmpty()) {
            var maps = MapAtlasItem.getMaps(atlas, player.level());
            Integer slice = MapAtlasItem.getSelectedSlice(atlas, player.level().dimension());
            // I hate this
            currentActiveMapKey = MapKey.at(maps.getScale(), player, slice);
        } else currentActiveMapKey = null;
    }

    public static ItemStack getCurrentActiveAtlas() {
        return currentActiveAtlas;
    }

    public static MapKey getActiveMapKey() {
        return currentActiveMapKey;
    }


    public static void init() {

        FMLJavaModLoadingContext.get().getModEventBus().register(MapAtlasesClient.class);

        MinecraftForge.EVENT_BUS.register(MapAtlasesClientEvents.class);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Register ModelPredicate
        ItemProperties.register(MapAtlasesMod.MAP_ATLAS.get(), MapAtlasesMod.res("atlas"),
                MapAtlasesClient::getPredicateForAtlas);

        //MenuScreens.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER.get(), MapAtlasesAtlasOverviewScreen::new);

    }

    public static MapAtlasesHUD HUD;

    @SubscribeEvent
    public static void clientSetup(RegisterGuiOverlaysEvent event) {
        HUD = new MapAtlasesHUD();
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "atlas", HUD);
    }

    @SubscribeEvent
    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ATLAS_KEYBIND);
        event.register(DECREASE_MINIMAP_ZOOM);
        event.register(INCREASE_MINIMAP_ZOOM);
    }

    public static void modifyDecorationTransform(PoseStack poseStack) {
        Float scale = globalDecorationScale.get();
        if (scale != null) poseStack.scale(scale, scale, 1);
        Float rot = globalDecorationRotation.get();
        if (rot != null) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
        }
    }

    @Deprecated(forRemoval = true)
    public static float getWorldMapZoomLevel() {
        return globalDecorationScale.get();
    }

    public static void setDecorationsScale(float i) {
        globalDecorationScale.set(i);
    }

    public static void setDecorationRotation(float i) {
        globalDecorationRotation.set(i);
    }

    public static float getPredicateForAtlas(ItemStack stack, ClientLevel world, LivingEntity entity, int seed) {
        // Using ClientLevel will render default Atlas in inventories
        if (world == null && entity != null)
            world = (ClientLevel) entity.level();
        if (world == null) return 0.0f;
        boolean unlocked = !MapAtlasItem.isLocked(stack);

        ResourceKey<Level> dimension = world.dimension();
        int i = DIMENSION_TEXTURE_ORDER.indexOf(dimension.location().toString());
        if (i == -1) return unlocked ? 0.96f : 1;
        return i / 10f + (unlocked ? 0 : 0.05f);
    }


    public static void setClientMapData(S2CSetMapDataPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Level level = player.level();

        //TODO: send less data and dont tick likehere. also send all data regardles of atlas or not
        if (packet.isOnJoin) {
           // ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
           // packet.mapData.tickCarriedBy(player, atlas);
           // packet.mapData.getHoldingPlayer(player);
        }
        ((ClientLevel) level).overrideMapData(packet.mapId, packet.mapData);
    }

    public static void setMapCenter(S2CSyncMapCenterPacket packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        var data = level.getMapData(packet.mapId);
        if (data != null) {
            setCenter(data, packet.centerX, packet.centerZ);
        }
    }

    public static void setCenter(MapItemSavedData data, int centerX, int centerZ) {
        try {
            ((MapItemSavedDataAccessor) data).setCenterX(centerX);
            ((MapItemSavedDataAccessor) data).setCenterZ(centerZ);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void openScreen(ItemStack atlas, @Nullable LecternBlockEntity lectern) {
        if (!MapAtlasItem.getMaps(atlas, Minecraft.getInstance().level).isEmpty()) {
            Minecraft.getInstance().setScreen(new AtlasOverviewScreen(atlas, lectern));
        }
    }

    public static void openScreen(ItemStack atlas) {
        openScreen(atlas, null);
    }

    //hack
    public static ContainerLevelAccess getClientAccess() {
        return ContainerLevelAccess.create(Minecraft.getInstance().level, BlockPos.ZERO);
    }


}