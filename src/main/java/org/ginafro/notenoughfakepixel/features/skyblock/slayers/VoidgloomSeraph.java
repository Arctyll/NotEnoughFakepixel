package org.ginafro.notenoughfakepixel.features.skyblock.slayers;

import net.minecraft.block.BlockBeacon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.ginafro.notenoughfakepixel.config.features.Slayer;
import org.ginafro.notenoughfakepixel.config.gui.core.ChromaColour;
import org.ginafro.notenoughfakepixel.events.Handlers.ScoreboardHandler;
import org.ginafro.notenoughfakepixel.events.PacketReadEvent;
import org.ginafro.notenoughfakepixel.utils.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VoidgloomSeraph {
    public static String displayText = "";
    private static long endTime = 0;
    public static Minecraft mc = Minecraft.getMinecraft();
    ArrayList<EntityFallingBlock> fallingBlocks = new ArrayList<>();
    ArrayList<Waypoint> waypoints = new ArrayList<>();
    public static boolean isBoss = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.side == net.minecraftforge.fml.relauncher.Side.CLIENT) {
            List<String> sidebarLines = ScoreboardHandler.getSidebarLines();

            // Loop through the sidebar lines and check for "Slay the boss!"
            for (String line : sidebarLines) {
                if (line.contains("Slay the boss!")) {
                    isBoss = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (checkEssentials()) return;
        if (isBoss) {
        AxisAlignedBB bb = Minecraft.getMinecraft().thePlayer.getEntityBoundingBox().expand(20, 20, 20);
        Collection<EntityFallingBlock> entities = Minecraft.getMinecraft().theWorld.getEntitiesWithinAABB(EntityFallingBlock.class, bb);
        ArrayList<Boolean> fallingBlocksFound = new ArrayList<>(fallingBlocks.size());
        for (int i = 0; i < fallingBlocks.size(); i++) {
            fallingBlocksFound.add(Boolean.FALSE);
        }
        for (EntityFallingBlock entity : entities) {
            if (fallingBlocks.contains(entity) ) {
                fallingBlocksFound.set(fallingBlocks.indexOf(entity), true);
                continue;
            }
            //System.out.println(entity.getBlock().getBlock().getClass().getName());
            //System.out.println(entity.getBlock());
            if (!(entity.getBlock().getBlock() instanceof BlockBeacon)) continue;
            fallingBlocks.add(entity);
            fallingBlocksFound.add(true);
        }
        ArrayList<Integer> indexToRemove = new ArrayList<>();
        for (int i = 0; i < fallingBlocksFound.size(); i++) {
            if (fallingBlocksFound.get(i)) continue;
            //System.out.println(fallingBlocks.get(i).getDisplayName());
            int[] coords = new int[]{(int)Math.floor(fallingBlocks.get(i).posX), (int)Math.floor(fallingBlocks.get(i).posY), (int)Math.floor(fallingBlocks.get(i).posZ)};
            waypoints.add(new Waypoint("BEACON",coords));
            SoundUtils.playSound(coords,"random.pop", 3.0f, 0.5f);
            showCustomOverlay(EnumChatFormatting.RED + "BEACON!", 2000);
            indexToRemove.add(i);
        }
        for (Integer i : indexToRemove) {
            fallingBlocks.remove((int)i);
            fallingBlocksFound.remove((int)i);
        }
    }
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (checkEssentials()) return;
        if (!NotEnoughFakepixel.feature.slayer.slayerShowBeaconPath) return;
        if (isBoss) {
        drawWaypoints(event.partialTicks);
        }
    }

    @SubscribeEvent
    public void onSoundPacketReceive(PacketReadEvent event) {
        if (checkEssentials()) return;
        if (!NotEnoughFakepixel.feature.slayer.slayerShowBeaconPath) return;
        Packet packet = event.packet;
        if (packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect soundEffect = (S29PacketSoundEffect) packet;
            String soundName = soundEffect.getSoundName();
            int[] coordsSound = new int[] {(int)Math.floor(((S29PacketSoundEffect) packet).getX()), (int)Math.floor(((S29PacketSoundEffect) packet).getY()),(int)Math.floor(((S29PacketSoundEffect) packet).getZ())};
            //System.out.println(soundName);
            // Remove blaze unhittable sound feature
            if (soundName.equals("random.break")) {
                Waypoint waypointToDelete = Waypoint.getClosestWaypoint(waypoints, coordsSound);
                if (waypointToDelete != null) waypoints.remove(waypointToDelete);
                waypoints.clear();
            } else if (soundName.equals("note.bass")) {
                fallingBlocks.clear();
                waypoints.clear();
            }
        }
    }

    /*@SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        System.out.println("BLOCKCHANGE EVENT");
    }*/

    @SubscribeEvent()
    public void onWorldUnload(WorldEvent.Unload event) {
        if (NotEnoughFakepixel.feature.slayer.slayerShowBeaconPath) {
            fallingBlocks.clear();
            waypoints.clear();
        }
    }

    private void drawWaypoints(float partialTicks) {
        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
        for (Waypoint waypoint : waypoints) {
            if (waypoint == null || waypoint.isHidden()) continue;
            Color colorDrawWaypoint = ColorUtils.getColor(NotEnoughFakepixel.feature.slayer.slayerBeaconColor);
            colorDrawWaypoint = new Color(colorDrawWaypoint.getRed(), colorDrawWaypoint.getGreen(), colorDrawWaypoint.getBlue(), 150);
            AxisAlignedBB bb = new AxisAlignedBB(
                    waypoint.getCoordinates()[0] - viewerX,
                    waypoint.getCoordinates()[1] - viewerY - 1,
                    waypoint.getCoordinates()[2] - viewerZ,
                    waypoint.getCoordinates()[0] + 1 - viewerX,
                    waypoint.getCoordinates()[1] + 1 - viewerY + 150,
                    waypoint.getCoordinates()[2] + 1 - viewerZ
            ).expand(0.01f, 0.01f, 0.01f);
            GlStateManager.disableCull();
            RenderUtils.drawFilledBoundingBox(bb, 1f, colorDrawWaypoint);
            GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
        }
    }

    private static boolean checkEssentials(){
        return (Minecraft.getMinecraft().thePlayer == null) ||
                (!ScoreboardUtils.currentGamemode.isSkyblock()) ||
                (!ScoreboardUtils.currentLocation.isEnd());
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (NotEnoughFakepixel.feature.slayer.slayerShowBeaconPath && ScoreboardUtils.currentGamemode.isSkyblock() && ScoreboardUtils.currentLocation.isEnd()) {
            String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

            if (message.contains("SLAYER QUEST COMPLETE!") || message.contains("SLAYER QUEST FAILED!")) {
                fallingBlocks.clear();
                waypoints.clear();
            }
        }
    }

    private void showCustomOverlay(String text, int durationMillis) {
        displayText = text;
        endTime = System.currentTimeMillis() + durationMillis;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (System.currentTimeMillis() > endTime) return;

        FontRenderer fr = mc.fontRendererObj;

        int screenWidth = event.resolution.getScaledWidth();
        int screenHeight = event.resolution.getScaledHeight();

        GlStateManager.pushMatrix();
        GlStateManager.scale(4.0F, 4.0F, 4.0F);
        int textWidth = fr.getStringWidth(displayText);
        int x = (screenWidth / 8) - (textWidth / 2);
        int y = (screenHeight / 8) - 10;
        fr.drawStringWithShadow(displayText, x, y, 0xFF5555);
        GlStateManager.popMatrix();
    }
}
