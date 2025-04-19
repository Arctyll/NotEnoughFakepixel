package org.ginafro.notenoughfakepixel.features.skyblock.dungeons.score;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.ginafro.notenoughfakepixel.Configuration;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.ginafro.notenoughfakepixel.config.features.Dungeons;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.DungeonManager;

public class SPlusNotifier {

    private static boolean remindedSPlus = true;
    private static boolean remindedUnreachable = true;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static String displayText = "";
    private static long endTime = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (!DungeonManager.checkEssentials() ||
                e.phase == TickEvent.Phase.END ||
                Minecraft.getMinecraft().thePlayer == null ||
                Minecraft.getMinecraft().theWorld == null) return;
        if (ScoreManager.currentSeconds > 0 && ScoreManager.currentSeconds <= 8) {
            remindedSPlus = false;
            remindedUnreachable = false;
        }
        reminderSPlus();
        reminderUnreachable();
    }

    public static void reminderSPlus() {
        if (remindedSPlus) return;
        if (!NotEnoughFakepixel.feature.dungeons.dungeonsSPlusNotifier && !NotEnoughFakepixel.feature.dungeons.dungeonsSPlusMessage) return;

        if (ScoreManager.getSecretPercentage() >= ScoreManager.getRequiredSecretNeeded() && ScoreManager.getRequiredSecretNeeded() != -1) {
            if (NotEnoughFakepixel.feature.dungeons.dungeonsSPlusNotifier) {
                mc.theWorld.playSound(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ,
                        "note.pling",
                        2.0F,
                        2.0F,
                        false
                );
                showCustomOverlay(EnumChatFormatting.RED + "300 Score!", 2000);
            }
            if (NotEnoughFakepixel.feature.dungeons.dungeonsSPlusMessage) {
                String customMessage = NotEnoughFakepixel.feature.dungeons.dungeonsSPlusCustom.trim();
                if (!customMessage.isEmpty()) {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc " + customMessage);
                } else {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc [NEF] S+ virtually reached, get 100% completion and enter portal!");
                }
            }
            remindedSPlus = true;
        }
    }

    public void reminderUnreachable() {
        if (remindedUnreachable) return;
        if (!NotEnoughFakepixel.feature.dungeons.dungeonsSPlusNotifier && !NotEnoughFakepixel.feature.dungeons.dungeonsSPlusMessage) return;

        if (ScoreManager.getRequiredSecretNeeded() == -1) {
            if (NotEnoughFakepixel.feature.dungeons.dungeonsSPlusMessage) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc [NEF] S+ may not be reached by secrets only, do crypts or restart");
            }
            remindedUnreachable = true;
            return;
        }
    }

    private static void showCustomOverlay(String text, int durationMillis) {
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
