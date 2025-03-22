package org.ginafro.notenoughfakepixel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.ginafro.notenoughfakepixel.Alerts.Alerts;
import org.ginafro.notenoughfakepixel.commands.CopyCommand;
import org.ginafro.notenoughfakepixel.config.gui.commands.Commands;
import org.ginafro.notenoughfakepixel.config.gui.config.ConfigEditor;
import org.ginafro.notenoughfakepixel.config.gui.core.GuiScreenElementWrapper;
import org.ginafro.notenoughfakepixel.features.duels.KDCounter;
import org.ginafro.notenoughfakepixel.features.mlf.Map;
import org.ginafro.notenoughfakepixel.features.skyblock.chocolate.ChocolateFactory;
import org.ginafro.notenoughfakepixel.features.skyblock.crimson.AshfangHelper;
import org.ginafro.notenoughfakepixel.features.skyblock.crimson.BossNotifier;
import org.ginafro.notenoughfakepixel.features.skyblock.crimson.AshfangOverlay;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.*;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.Secrets.AutoRoom;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.Secrets.Waypoints;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.devices.*;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.mobs.BatMobDisplay;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.mobs.FelMobDisplay;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.mobs.LividDisplay;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.mobs.StarredMobDisplay;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.puzzles.*;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.score.DungeonClearedNotifier;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.score.ScoreManager;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.score.ScoreOverlay;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.score.SPlusNotifier;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.terminals.*;
import org.ginafro.notenoughfakepixel.features.skyblock.enchanting.EnchantingSolvers;
import org.ginafro.notenoughfakepixel.features.skyblock.enchanting.HideEnchantingTooltips;
import org.ginafro.notenoughfakepixel.features.skyblock.enchanting.PreventMissclicks;
import org.ginafro.notenoughfakepixel.features.skyblock.fishing.GreatCatchNotifier;
import org.ginafro.notenoughfakepixel.features.skyblock.mining.*;
//import org.ginafro.notenoughfakepixel.features.skyblock.overlays.StorageOverlay;
import org.ginafro.notenoughfakepixel.features.skyblock.overlays.StorageOverlay;
import org.ginafro.notenoughfakepixel.features.skyblock.qol.*;
import org.ginafro.notenoughfakepixel.features.skyblock.diana.*;
import org.ginafro.notenoughfakepixel.features.skyblock.slayers.*;
import org.ginafro.notenoughfakepixel.events.Handlers.PacketHandler;
//import org.ginafro.notenoughfakepixel.gui.CustomConfigGUI;
import org.ginafro.notenoughfakepixel.utils.*;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Mod(modid = "notenoughfakepixel", useMetadata = true)
public class NotEnoughFakepixel {

    public static final File nefFolder = new File(Minecraft.getMinecraft().mcDataDir, "NotEnoughFakepixel");

    public static final KeyBinding openGuiKey = new KeyBinding(
            "Open GUI",
            Keyboard.KEY_P,
            "NotEnoughFakepixel"
    );

    private final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    public static File configDirectory;
    private File configFile;

    public static JsonObject roomsJson;
    public static JsonObject waypointsJson;

    public static Configuration feature;

    public static NotEnoughFakepixel instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this;
        MinecraftForge.EVENT_BUS.register(this);

        if (!nefFolder.exists()) {
            nefFolder.mkdirs();
        }

        configDirectory = new File("config/Notenoughfakepixel");
        if (!configDirectory.exists()) {
            configDirectory.mkdirs();
        }

        configFile = new File(configDirectory, "config.json");

        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                feature = gson.fromJson(reader, Configuration.class);
            } catch (Exception ignored) {
            }
        }

        // If config doesn't exist or failed to load, create a new one
        if (feature == null) {
            feature = new Configuration();
            saveConfig();
        }

        ClientCommandHandler.instance.registerCommand(new CopyCommand());

        new Aliases();

        try {
            ResourceLocation roomsLoc = new ResourceLocation("notenoughfakepixel", "dungeonrooms/dungeonrooms.json");
            InputStream roomsIn = Minecraft.getMinecraft().getResourceManager().getResource(roomsLoc).getInputStream();
            BufferedReader roomsReader = new BufferedReader(new InputStreamReader(roomsIn));

            ResourceLocation waypointsLoc = new ResourceLocation("notenoughfakepixel", "dungeonrooms/secretlocations.json");
            InputStream waypointsIn = Minecraft.getMinecraft().getResourceManager().getResource(waypointsLoc).getInputStream();
            BufferedReader waypointsReader = new BufferedReader(new InputStreamReader(waypointsIn));

            Gson gson = new Gson();
            roomsJson = gson.fromJson(roomsReader, JsonObject.class);

            waypointsJson = gson.fromJson(waypointsReader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ClientRegistry.registerKeyBinding(openGuiKey);
        Commands.init();
        Alerts.load();
        registerModEvents();

        Runtime.getRuntime().addShutdownHook(new Thread(this::saveConfig));
    }

    private void registerModEvents() {
        // Dungeons
        DungeonsMap map = new DungeonsMap();
        MinecraftForge.EVENT_BUS.register(map);

        MinecraftForge.EVENT_BUS.register(new WelcomeMessage());
        MinecraftForge.EVENT_BUS.register(new SalvageItemsSaver());
        MinecraftForge.EVENT_BUS.register(new ShowNotOpenedChests());

        MinecraftForge.EVENT_BUS.register(new StartingWithSolver());
        MinecraftForge.EVENT_BUS.register(new ClickOnColorsSolver());
        MinecraftForge.EVENT_BUS.register(new ClickInOrderSolver());
        MinecraftForge.EVENT_BUS.register(new MazeSolver());
        MinecraftForge.EVENT_BUS.register(new CorrectPanesSolver());
        MinecraftForge.EVENT_BUS.register(new FirstDeviceSolver());
        MinecraftForge.EVENT_BUS.register(new ThirdDeviceSolver());
        MinecraftForge.EVENT_BUS.register(new HideTooltips());

        MinecraftForge.EVENT_BUS.register(new AutoReadyDungeon());
        MinecraftForge.EVENT_BUS.register(new AutoCloseChests());

        MinecraftForge.EVENT_BUS.register(new AutoRoom());
        MinecraftForge.EVENT_BUS.register(new Waypoints());

        MinecraftForge.EVENT_BUS.register(new ThreeWeirdos());
        MinecraftForge.EVENT_BUS.register(new WaterSolver());
        MinecraftForge.EVENT_BUS.register(new BoulderSolver());
        MinecraftForge.EVENT_BUS.register(new SilverFishSolver());
        MinecraftForge.EVENT_BUS.register(new TeleportMazeSolver());
        MinecraftForge.EVENT_BUS.register(new CreeperSolver());

        MinecraftForge.EVENT_BUS.register(new WitherBloodKeysTracers());
        MinecraftForge.EVENT_BUS.register(new StarredMobDisplay());
        MinecraftForge.EVENT_BUS.register(new BatMobDisplay());
        MinecraftForge.EVENT_BUS.register(new FelMobDisplay());
        MinecraftForge.EVENT_BUS.register(new ItemSecretsDisplay());
        MinecraftForge.EVENT_BUS.register(new LividDisplay());

        MinecraftForge.EVENT_BUS.register(new DungeonManager());
        MinecraftForge.EVENT_BUS.register(new ScoreManager());
        MinecraftForge.EVENT_BUS.register(new ScoreOverlay());
        MinecraftForge.EVENT_BUS.register(new SPlusNotifier());
        MinecraftForge.EVENT_BUS.register(new DungeonClearedNotifier());
        MinecraftForge.EVENT_BUS.register(new MuteIrrelevantMessages());
        MinecraftForge.EVENT_BUS.register(new SpiritLeapHandler());
        MinecraftForge.EVENT_BUS.register(new SpiritLeapHandler.ChestGuiOverlayHandler());
        MinecraftForge.EVENT_BUS.register(new MiscDungFeatures());
        MinecraftForge.EVENT_BUS.register(new TerminalWaypoints());

        // Mining
        MinecraftForge.EVENT_BUS.register(new MiningOverlay());
        MinecraftForge.EVENT_BUS.register(new DrillFuelParsing());
        MinecraftForge.EVENT_BUS.register(new AbilityNotifier());
        MinecraftForge.EVENT_BUS.register(new EventsMsgSupressor());
        MinecraftForge.EVENT_BUS.register(new DrillFix());
        MinecraftForge.EVENT_BUS.register(new PuzzlerSolver());
        MinecraftForge.EVENT_BUS.register(new RemoveGhostInvis());
        // Fishing
        MinecraftForge.EVENT_BUS.register(new GreatCatchNotifier());
        // Enchanting
        MinecraftForge.EVENT_BUS.register(new EnchantingSolvers());
        MinecraftForge.EVENT_BUS.register(new HideEnchantingTooltips());
        MinecraftForge.EVENT_BUS.register(new PreventMissclicks());

        // Chocolate Factory
        MinecraftForge.EVENT_BUS.register(new ChocolateFactory());
        // QOL
        MinecraftForge.EVENT_BUS.register(new ShowCurrentPet());
        MinecraftForge.EVENT_BUS.register(new ChatCleaner());
        MinecraftForge.EVENT_BUS.register(new VisualCooldowns());
        MinecraftForge.EVENT_BUS.register(new MiddleClickEvent());
        MinecraftForge.EVENT_BUS.register(new SoundRemover());
        MinecraftForge.EVENT_BUS.register(new ScrollableTooltips());
        MinecraftForge.EVENT_BUS.register(new FairySouls());
        MinecraftForge.EVENT_BUS.register(new AutoOpenMaddox());
        MinecraftForge.EVENT_BUS.register(new MidasStaff());
        MinecraftForge.EVENT_BUS.register(new WardrobeShortcut());
        MinecraftForge.EVENT_BUS.register(new PetsShortcut());
        MinecraftForge.EVENT_BUS.register(new WarpsShortcut());
        MinecraftForge.EVENT_BUS.register(new DisableEndermanTeleport());
        MinecraftForge.EVENT_BUS.register(new HideFlamingFists());
        MinecraftForge.EVENT_BUS.register(new MiscFeatures());
        MinecraftForge.EVENT_BUS.register(new ItemAnimations());
        MinecraftForge.EVENT_BUS.register(new Alerts());
        //MinecraftForge.EVENT_BUS.register(new SlotLocking());
        //MinecraftForge.EVENT_BUS.register(new StorageOverlay.StorageEvent());

        MinecraftForge.EVENT_BUS.register(new Fullbright());
        MinecraftForge.EVENT_BUS.register(new KDCounter());
        MinecraftForge.EVENT_BUS.register(new Map());
        // Diana
        MinecraftForge.EVENT_BUS.register(new Diana());
        MinecraftForge.EVENT_BUS.register(new GuessBurrow());
        // Crimson
        MinecraftForge.EVENT_BUS.register(new AshfangOverlay());
        MinecraftForge.EVENT_BUS.register(new BossNotifier());
        MinecraftForge.EVENT_BUS.register(new AshfangHelper());
        // Slayer
        MinecraftForge.EVENT_BUS.register(new SlayerMobsDisplay());
        MinecraftForge.EVENT_BUS.register(new VoidgloomSeraph());
        MinecraftForge.EVENT_BUS.register(new FirePillarDisplay());
        MinecraftForge.EVENT_BUS.register(new MinibossAlert());
        MinecraftForge.EVENT_BUS.register(new BlazeAttunements());
        MinecraftForge.EVENT_BUS.register(new SlayerTimer());
        MinecraftForge.EVENT_BUS.register(new SlayerHealthDisplay());

        // Parsers
        MinecraftForge.EVENT_BUS.register(new TablistParser());
        MinecraftForge.EVENT_BUS.register(new ScoreboardUtils());
    }

    public static GuiScreen openGui;
    public static long lastOpenedGui;
    public static String th = "default";
    public static ResourceLocation bg = new ResourceLocation("notenoughfakepixel:backgrounds/" + th + "/background.png");

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (Minecraft.getMinecraft().thePlayer == null) {
            openGui = null;
            return;
        }

        if (openGui != null) {
            if (Minecraft.getMinecraft().thePlayer.openContainer != null) {
                Minecraft.getMinecraft().thePlayer.closeScreen();
            }
            Minecraft.getMinecraft().displayGuiScreen(openGui);
            openGui = null;
            lastOpenedGui = System.currentTimeMillis();
        }

        ScoreboardUtils.parseScoreboard();
    }

    public void saveConfig() {
        try {
            //noinspection ResultOfMethodCallIgnored
            configFile.createNewFile();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
                writer.write(gson.toJson(feature));
            }
        } catch (IOException ignored) {
        }
    }

    @SubscribeEvent
    public void renderPlayerInfo(final RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (ScoreboardUtils.currentLocation.isDungeon()) {
            if (AutoRoom.guiToggled) {
                AutoRoom.renderText();
            }
            if (AutoRoom.coordToggled) {
                AutoRoom.renderCoord();
            }
        }
    }

    public static GuiScreen screenToOpen = null;
    private static int screenTicks = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;

        if (screenToOpen != null) {
            screenTicks++;
            if (screenTicks == 5) {
                Minecraft.getMinecraft().displayGuiScreen(screenToOpen);
                screenTicks = 0;
                screenToOpen = null;
            }
        }

        if (openGuiKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(feature));
        }
    }

    @SubscribeEvent
    public void onServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        event.manager.channel().pipeline().addBefore("packet_handler", "nef_packet_handler", new PacketHandler());
        System.out.println("Added packet handler to channel pipeline.");
    }
}