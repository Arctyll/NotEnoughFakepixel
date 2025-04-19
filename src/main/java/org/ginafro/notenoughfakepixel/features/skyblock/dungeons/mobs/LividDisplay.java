package org.ginafro.notenoughfakepixel.features.skyblock.dungeons.mobs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.ginafro.notenoughfakepixel.Configuration;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.ginafro.notenoughfakepixel.config.features.Dungeons;
import org.ginafro.notenoughfakepixel.utils.ScoreboardUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LividDisplay {

    static BlockPos pos = new BlockPos(45, 108, 52); // Predefined wool position
    static Entity livid = null;
    public static int LIVID_COLOUR;
    private static final Map<String, EnumChatFormatting> lividColors = new HashMap<>();

    static {
        initializeColors();
    }

    private static void initializeColors() {
        lividColors.put("Vendetta", EnumChatFormatting.WHITE);
        lividColors.put("Crossed", EnumChatFormatting.LIGHT_PURPLE);
        lividColors.put("Hockey", EnumChatFormatting.RED);
        lividColors.put("Doctor", EnumChatFormatting.GRAY);
        lividColors.put("Frog", EnumChatFormatting.DARK_GREEN);
        lividColors.put("Smile", EnumChatFormatting.GREEN);
        lividColors.put("Scream", EnumChatFormatting.BLUE);
        lividColors.put("Purple", EnumChatFormatting.DARK_PURPLE);
        lividColors.put("Arcade", EnumChatFormatting.YELLOW);
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        livid = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        // Run only every 20 ticks
        if (Minecraft.getMinecraft().theWorld == null || Minecraft.getMinecraft().theWorld.getTotalWorldTime() % 20 != 0) return;

        if (ScoreboardUtils.currentLocation.isDungeon()) {
            Minecraft mc = Minecraft.getMinecraft();
            World world = mc.theWorld;
            if (world == null) return;

            if (!NotEnoughFakepixel.feature.dungeons.dungeonsLividFinder) return;

            if (world.getBlockState(pos).getBlock() == Blocks.wool) {
                int woolColor = world.getBlockState(pos).getBlock().getDamageValue(world, pos);
                List<Entity> entities = world.getLoadedEntityList();
                System.out.println("Detected wool at position " + pos + " with color value: " + woolColor);

                for (Entity entity : entities) {
                    if (!(entity instanceof EntityArmorStand) || !entity.hasCustomName()) continue;
                    String name = entity.getCustomNameTag();
                    System.out.println("Checking entity with name: " + name);

                    for (Map.Entry<String, EnumChatFormatting> entry : lividColors.entrySet()) {
                        if (name.contains(entry.getKey())) {
                            int expectedColor = getWoolColorFromChatColor(entry.getValue());
                            if (woolColor == expectedColor) {
                                livid = entity;
                                EnumChatFormatting chatColor = entry.getValue();
                                Color color = getColorFromEnumChatFormatting(chatColor);
                                LIVID_COLOUR = color.getRGB();
                                System.out.println("Found matching Livid: " + name + " with color " + color);
                                return;
                            }
                        }
                    }
                }
                livid = null;
                System.out.println("No matching Livid entity found.");
            }
        }
    }

    @SubscribeEvent
    public void onRenderEntity(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!NotEnoughFakepixel.feature.dungeons.dungeonsLividFinder || livid == null) return;

        Entity entity = event.entity;
        if (entity instanceof EntityArmorStand && entity.hasCustomName()) {
            String name = entity.getCustomNameTag();
            if (!entity.isEntityEqual(livid) && name.contains("Livid")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (NotEnoughFakepixel.feature.dungeons.dungeonsLividFinder && livid != null) {
            AxisAlignedBB aabb = new AxisAlignedBB(livid.posX - 0.5, livid.posY - 2, livid.posZ - 0.5, livid.posX + 0.5, livid.posY, livid.posZ + 0.5);
            System.out.println("Rendering Livid highlight at: " + livid.getPosition());
            draw3DBox(aabb, LIVID_COLOUR, event.partialTicks);
        }
    }

    public static void draw3DBox(AxisAlignedBB aabb, int colourInt, float partialTicks) {
        Entity render = Minecraft.getMinecraft().getRenderViewEntity();
        Color colour = new Color(colourInt);

        double realX = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks;
        double realY = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks;
        double realZ = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-realX, -realY, -realZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2);

        RenderGlobal.drawOutlinedBoundingBox(aabb, colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha());

        GlStateManager.translate(realX, realY, realZ);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static int getWoolColorFromChatColor(EnumChatFormatting color) {
        switch (color) {
            case WHITE: return 0;
            case LIGHT_PURPLE: return 6;
            case YELLOW: return 4;
            case GREEN: return 5;
            case GRAY: return 8;
            case DARK_PURPLE: return 10;
            case BLUE: return 11;
            case DARK_GREEN: return 13;
            case RED: return 14;
            default: return -1;
        }
    }

    private static Color getColorFromEnumChatFormatting(EnumChatFormatting formatting) {
        switch (formatting) {
            case WHITE:
                return new Color(0xFFFFFF);
            case DARK_PURPLE:
                return new Color(0xAA00AA);
            case RED:
                return new Color(0xFF5555);
            case GRAY:
                return new Color(0xAAAAAA);
            case DARK_GREEN:
                return new Color(0x00AA00);
            case GREEN:
                return new Color(0x55FF55);
            case BLUE:
                return new Color(0x5555FF);
            case LIGHT_PURPLE:
                return new Color(0xFF55FF);
            case YELLOW:
                return new Color(0xFFFF55);
            default:
                return Color.WHITE;
        }
    }
}