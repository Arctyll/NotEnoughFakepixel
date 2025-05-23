/*
Copyright 2021 Quantizr(_risk)
This file is used as part of Dungeon Rooms Mod (DRM). (Github: <https://github.com/Quantizr/DungeonRoomsMod>)
DRM is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
DRM is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with DRM.  If not, see <https://www.gnu.org/licenses/>.
*/

package org.ginafro.notenoughfakepixel.features.skyblock.dungeons.Secrets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumChatFormatting;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.ginafro.notenoughfakepixel.events.PacketReadEvent;
import org.ginafro.notenoughfakepixel.utils.ScoreboardUtils;
import org.ginafro.notenoughfakepixel.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Waypoints {

    public static boolean showEntrance = true;
    public static boolean showSuperboom = true;
    public static boolean showSecrets = true;
    public static boolean showFairySouls = true;

    public static boolean sneakToDisable = true;

    public static boolean disableWhenAllFound = true;
    public static boolean allFound = false;

    public static boolean showWaypointText = true;
    public static boolean showBoundingBox = true;
    public static boolean showBeacon = true;

    public static int secretNum = 0;
    public static int completedSecrets = 0;

    public static Map<String, List<Boolean>> allSecretsMap = new HashMap<>();
    public static List<Boolean> secretsList = new ArrayList<>(Arrays.asList(new Boolean[9]));

    static long lastSneakTime = 0;


    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (!NotEnoughFakepixel.feature.dungeons.dungeonsSecretWaypoints) return;
        String roomName = AutoRoom.lastRoomName;
        if (AutoRoom.lastRoomJson != null && roomName != null && secretsList != null) {
            secretNum = AutoRoom.lastRoomJson.get("secrets").getAsInt();
            if (NotEnoughFakepixel.waypointsJson.get(roomName) != null) {
                JsonArray secretsArray = NotEnoughFakepixel.waypointsJson.get(roomName).getAsJsonArray();
                int arraySize = secretsArray.size();
                for(int i = 0; i < arraySize; i++) {
                    JsonObject secretsObject = secretsArray.get(i).getAsJsonObject();

                    boolean display = true;
                    for(int j = 1; j <= secretNum; j++) {
                        if (!secretsList.get(j-1)) {
                            if (secretsObject.get("secretName").getAsString().contains(String.valueOf(j))) {
                                display = false;
                                break;
                            }
                        }
                    }
                    if (!display) continue;

                    if (disableWhenAllFound && allFound && !secretsObject.get("category").getAsString().equals("fairysoul")) continue;

                    Color color;
                    switch (secretsObject.get("category").getAsString()) {
                        case "entrance":
                            if (!showEntrance) continue;
                            color = new Color(0, 255, 0);
                            break;
                        case "superboom":
                            if (!showSuperboom) continue;
                            color = new Color(255, 0, 0);
                            break;
                        case "chest":
                            if (!showSecrets) continue;
                            color = new Color(2, 213, 250);
                            break;
                        case "item":
                            if (!showSecrets) continue;
                            color = new Color(2, 64, 250);
                            break;
                        case "bat":
                            if (!showSecrets) continue;
                            color = new Color(142, 66, 0);
                            break;
                        case "wither":
                            if (!showSecrets) continue;
                            color = new Color(30, 30, 30);
                            break;
                        case "lever":
                            if (!showSecrets) continue;
                            color = new Color(250, 217, 2);
                            break;
                        case "fairysoul":
                            if (!showFairySouls) continue;
                            color = new Color(255, 85, 255);
                            break;
                        default:
                            color = new Color(190, 255, 252);
                    }

                    Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
                    double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * event.partialTicks;
                    double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * event.partialTicks;
                    double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * event.partialTicks;

                    BlockPos pos = Utils.relativeToActual(new BlockPos(secretsObject.get("x").getAsInt(), secretsObject.get("y").getAsInt(), secretsObject.get("z").getAsInt()));
                    if (pos == null) continue;
                    double x = pos.getX() - viewerX;
                    double y = pos.getY() - viewerY;
                    double z = pos.getZ() - viewerZ;
                    double distSq = x*x + y*y + z*z;

                    GlStateManager.disableDepth();
                    GlStateManager.disableCull();
                    if (showBoundingBox) drawFilledBoundingBox(new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1), color, 0.4f);
                    GlStateManager.disableTexture2D();
                    if (showWaypointText) renderWaypointText(secretsObject.get("secretName").getAsString(), pos.up(2), event.partialTicks);
                    GlStateManager.disableLighting();
                    GlStateManager.enableTexture2D();
                    GlStateManager.enableDepth();
                    GlStateManager.enableCull();
                }
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatReceivedEvent event) {
        if (!ScoreboardUtils.currentLocation.isDungeon() || !NotEnoughFakepixel.feature.dungeons.dungeonsSecretWaypoints) return;;
        // Action Bar
        if (event.type == 2) {
            String[] actionBarSections = event.message.getUnformattedText().split(" {3,}");

            for (String section : actionBarSections) {
                if (section.contains("Secrets") && section.contains("/")) {
                    String cleanedSection = StringUtils.stripControlCodes(section);
                    String[] splitSecrets = cleanedSection.split("/");

                    completedSecrets = Integer.parseInt(splitSecrets[0].replaceAll("[^0-9]", ""));
                    int totalSecrets = Integer.parseInt(splitSecrets[1].replaceAll("[^0-9]", ""));

                    allFound = (totalSecrets == secretNum && completedSecrets == secretNum);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (!ScoreboardUtils.currentLocation.isDungeon() || !NotEnoughFakepixel.feature.dungeons.dungeonsSecretWaypoints) return;
        if (disableWhenAllFound && allFound) return;

        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            Block block = event.world.getBlockState(event.pos).getBlock();
            if (block != Blocks.chest && block != Blocks.skull) return;
            if (AutoRoom.lastRoomJson != null && AutoRoom.lastRoomName != null) {
                secretNum = AutoRoom.lastRoomJson.get("secrets").getAsInt();
                if (NotEnoughFakepixel.waypointsJson.get(AutoRoom.lastRoomName) != null) {
                    JsonArray secretsArray = NotEnoughFakepixel.waypointsJson.get(AutoRoom.lastRoomName).getAsJsonArray();
                    int arraySize = secretsArray.size();
                    for(int i = 0; i < arraySize; i++) {
                        JsonObject secretsObject = secretsArray.get(i).getAsJsonObject();
                        if (secretsObject.get("category").getAsString().equals("chest") || secretsObject.get("category").getAsString().equals("wither")) {
                            BlockPos pos = Utils.relativeToActual(new BlockPos(secretsObject.get("x").getAsInt(), secretsObject.get("y").getAsInt(), secretsObject.get("z").getAsInt()));
                            if (pos == null) return;
                            if (pos.equals(event.pos)) {
                                for(int j = 1; j <= secretNum; j++) {
                                    if (secretsObject.get("secretName").getAsString().contains(String.valueOf(j))) {
                                        Waypoints.secretsList.set(j-1, false);
                                        Waypoints.allSecretsMap.replace(AutoRoom.lastRoomName, Waypoints.secretsList);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(PacketReadEvent event) {
        if (!ScoreboardUtils.currentLocation.isDungeon() || !NotEnoughFakepixel.feature.dungeons.dungeonsSecretWaypoints) return;
        if (disableWhenAllFound && allFound) return;
        Minecraft mc = Minecraft.getMinecraft();

        if (event.packet instanceof S0DPacketCollectItem) {
            S0DPacketCollectItem packet = (S0DPacketCollectItem) event.packet;
            Entity entity = mc.theWorld.getEntityByID(packet.getCollectedItemEntityID());
            if (entity instanceof EntityItem) {
                EntityItem item = (EntityItem) entity;
                entity = mc.theWorld.getEntityByID(packet.getEntityID());
                if (entity == null) return;
                String name = item.getEntityItem().getDisplayName();
                if (name.contains("Decoy") || name.contains("Defuse Kit") || name.contains("Dungeon Chest Key") ||
                        name.contains("Healing VIII") || name.contains("Inflatable Jerry") || name.contains("Spirit Leap") ||
                        name.contains("Training Weights") || name.contains("Trap") || name.contains("Treasure Talisman")) {
                    if (!entity.getCommandSenderEntity().getName().equals(mc.thePlayer.getName())) {
                        //Do nothing if someone else picks up the item in order to follow Hypixel rules
                        return;
                    }
                    if (AutoRoom.lastRoomJson != null && AutoRoom.lastRoomName != null) {
                        secretNum = AutoRoom.lastRoomJson.get("secrets").getAsInt();
                        if (NotEnoughFakepixel.waypointsJson.get(AutoRoom.lastRoomName) != null) {
                            JsonArray secretsArray = NotEnoughFakepixel.waypointsJson.get(AutoRoom.lastRoomName).getAsJsonArray();
                            int arraySize = secretsArray.size();
                            for(int i = 0; i < arraySize; i++) {
                                JsonObject secretsObject = secretsArray.get(i).getAsJsonObject();
                                if (secretsObject.get("category").getAsString().equals("item") || secretsObject.get("category").getAsString().equals("bat")) {
                                    BlockPos pos = Utils.relativeToActual(new BlockPos(secretsObject.get("x").getAsInt(), secretsObject.get("y").getAsInt(), secretsObject.get("z").getAsInt()));
                                    if (pos == null) return;
                                    if (entity.getDistanceSq(pos) <= 36D) {
                                        for(int j = 1; j <= secretNum; j++) {
                                            if (secretsObject.get("secretName").getAsString().contains(String.valueOf(j))) {
                                                if (!Waypoints.secretsList.get(j-1)) continue;
                                                Waypoints.secretsList.set(j-1, false);
                                                Waypoints.allSecretsMap.replace(AutoRoom.lastRoomName, Waypoints.secretsList);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void renderWaypointText(String str, BlockPos loc, float partialTicks) {
        GlStateManager.alphaFunc(516, 0.1F);

        GlStateManager.pushMatrix();

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        double x = loc.getX() - viewerX;
        double y = loc.getY() - viewerY - viewer.getEyeHeight();
        double z = loc.getZ() - viewerZ;

        double distSq = x*x + y*y + z*z;
        double dist = Math.sqrt(distSq);
        if(distSq > 144) {
            x *= 12/dist;
            y *= 12/dist;
            z *= 12/dist;
        }
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0, viewer.getEyeHeight(), 0);

        drawNametag(str);

        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0, -0.25f, 0);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);

        drawNametag(EnumChatFormatting.YELLOW.toString()+Math.round(dist)+"m");

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
    }

    public static void drawNametag(String str) {
        FontRenderer fontrenderer = Minecraft.getMinecraft().fontRendererObj;
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;

        int j = fontrenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-j - 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(-j - 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, 553648127);
        GlStateManager.depthMask(true);

        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, -1);

        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    //Disable waypoint within 4 blocks away on sneak
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!ScoreboardUtils.currentLocation.isDungeon() || !NotEnoughFakepixel.feature.dungeons.dungeonsSecretWaypoints || !sneakToDisable) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (FMLClientHandler.instance().getClient().gameSettings.keyBindSneak.isPressed()) {
            if (System.currentTimeMillis() - lastSneakTime < 1000) { //check for two taps in under a second
                if (AutoRoom.lastRoomJson != null && AutoRoom.lastRoomName != null) {
                    secretNum = AutoRoom.lastRoomJson.get("secrets").getAsInt();
                    if (NotEnoughFakepixel.waypointsJson.get(AutoRoom.lastRoomName) != null) {
                        JsonArray secretsArray = NotEnoughFakepixel.waypointsJson.get(AutoRoom.lastRoomName).getAsJsonArray();
                        int arraySize = secretsArray.size();
                        for(int i = 0; i < arraySize; i++) {
                            JsonObject secretsObject = secretsArray.get(i).getAsJsonObject();
                            if (secretsObject.get("category").getAsString().equals("chest") || secretsObject.get("category").getAsString().equals("wither")
                                    || secretsObject.get("category").getAsString().equals("item") || secretsObject.get("category").getAsString().equals("bat")) {
                                BlockPos pos = Utils.relativeToActual(new BlockPos(secretsObject.get("x").getAsInt(), secretsObject.get("y").getAsInt(), secretsObject.get("z").getAsInt()));
                                if (pos == null) return;
                                if (player.getDistanceSq(pos) <= 16D) {
                                    for(int j = 1; j <= secretNum; j++) {
                                        if (secretsObject.get("secretName").getAsString().contains(String.valueOf(j))) {
                                            if (!Waypoints.secretsList.get(j-1)) continue;
                                            Waypoints.secretsList.set(j-1, false);
                                            Waypoints.allSecretsMap.replace(AutoRoom.lastRoomName, Waypoints.secretsList);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            lastSneakTime = System.currentTimeMillis();
        }
    }

    public static void drawFilledBoundingBox(AxisAlignedBB aabb, Color c, float alphaMultiplier) {
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f*alphaMultiplier);

        //vertical
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        tessellator.draw();


        GlStateManager.color(c.getRed()/255f*0.8f, c.getGreen()/255f*0.8f, c.getBlue()/255f*0.8f, c.getAlpha()/255f*alphaMultiplier);

        //x
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        tessellator.draw();


        GlStateManager.color(c.getRed()/255f*0.9f, c.getGreen()/255f*0.9f, c.getBlue()/255f*0.9f, c.getAlpha()/255f*alphaMultiplier);
        //z
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }


}
