package org.ginafro.notenoughfakepixel.features.skyblock.dungeons.score;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.ginafro.notenoughfakepixel.Configuration;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.ginafro.notenoughfakepixel.config.features.Dungeons;
import org.ginafro.notenoughfakepixel.features.skyblock.dungeons.DungeonManager;
import org.ginafro.notenoughfakepixel.utils.ScoreboardUtils;
import org.ginafro.notenoughfakepixel.utils.TablistParser;
import org.ginafro.notenoughfakepixel.variables.DungeonFloor;

public class ScoreManager {
    static int failedPuzzles = 0;
    public static int currentSeconds = -1;
    private static int reducedPenalty = 0;
    private static boolean hasNotified270 = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    private static String displayText = "";
    private static long endTime = 0;

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!DungeonManager.checkEssentials()) return;
        if (event.message.getUnformattedText().contains("PUZZLE FAIL!")) {
            failedPuzzles++;
        } else if (event.message.getUnformattedText().equals("Your Spirit Pet reduced your death score penalty to 1!")) {
            reducedPenalty = 1;
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc [NEF] I died with Spirit Pet, so my kill doesn't count");
        } else if (event.message.getUnformattedText().contains("[NEF] I died with Spirit Pet, so my kill doesn't count")) {
            reducedPenalty = 1;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!DungeonManager.checkEssentials()) return;
        reset();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!DungeonManager.checkEssentials() ||
                event.phase == TickEvent.Phase.END ||
                Minecraft.getMinecraft().thePlayer == null) return;

        int currentScore = getSkillScore() + getExplorationClearScore() + getSpeedScore() + getBonusScore();
        int virtualSecretScore = Math.min(40, getSecretPercentage() * 40 /
                DungeonFloor.getFloor(ScoreboardUtils.currentFloor.name()).getSecretPercentage());
        int virtualTotalScore = currentScore + virtualSecretScore;

        if (virtualTotalScore >= 270 && !hasNotified270 & NotEnoughFakepixel.feature.dungeons.dungeonsSNotifier) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc 270 Score!");
            mc.theWorld.playSound(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    "note.pling",
                    2.0F,
                    2.0F,
                    false
            );
            showCustomOverlay(EnumChatFormatting.RED + "270 Score!", 2000);
            hasNotified270 = true;
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

    private void reset() {
        failedPuzzles = 0;
        currentSeconds = -1;
        reducedPenalty = 0;
        hasNotified270 = false;
    }

    public static int getTotalScore() {
        return getSkillScore() + getExplorationScore() + getSpeedScore() + getBonusScore();
    }

    public static int getSkillScore() {
        int deaths = TablistParser.deaths;
        return Math.max(Math.min(100 - (deaths-reducedPenalty) * 2 - failedPuzzles * 14, 100), 0);
    }

    public static int getExplorationClearScore() {
        int clearedPercentage = ScoreboardUtils.clearedPercentage;
        return (int) Math.max(Math.min(Math.floor(60f * clearedPercentage / 100f),60),0);
    }

    public static int getExplorationSecretScore() {
        int secretPercentage = getSecretPercentage();
        int secretNeeded = DungeonFloor.getFloor(ScoreboardUtils.currentFloor.name()).getSecretPercentage();
        return (int) Math.max(Math.min(Math.floor(40f * secretPercentage / secretNeeded), 40f),0);
    }

    public static int getExplorationScore() {
        return getExplorationClearScore() + getExplorationSecretScore();
    }

    public static int getSpeedScore() {
        String currentTimeString = TablistParser.time;
        currentSeconds = convertToSeconds(currentTimeString);
        if (currentSeconds == -1) {
            return 100;
        }
        int t = currentSeconds + DungeonFloor.getFloor(ScoreboardUtils.currentFloor.name()).getT();
        if (t < 480) return 100;
        if (t < 600) return 140 - (int) Math.ceil(t * (1f / 12f));
        if (t < 840) return 115 - (int) Math.ceil(t * (1f / 24f));
        if (t < 1140) return 108 - (int) Math.ceil(t * (1f / 30f));
        if (t < 3940) return (int) Math.ceil(98.5f - (int) Math.ceil(t * (1f / 40f)));
        return 0;
    }

    public static int getBonusScore() {
        int crypts = TablistParser.crypts;
        return (NotEnoughFakepixel.feature.dungeons.dungeonsIsPaul ? 10 : 0) + Math.min(5, crypts);
    }

    public static int getSecretPercentage() {
        return TablistParser.secretPercentage;
    }

    public static int getRequiredSecretNeeded() {
        int secretScoreNeeded = 300-60-getSkillScore()-getSpeedScore()-getBonusScore();
        if (secretScoreNeeded > 40) {
            // cannot reach
            return -1;
        }
        return (int) Math.ceil(secretScoreNeeded * DungeonFloor.getFloor(ScoreboardUtils.currentFloor.name()).getSecretPercentage() / 40f);
    }

    public static int convertToSeconds(String time) {
        if (time.isEmpty()) return -1;
        String[] parts = time.split(":");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        return (minutes * 60) + seconds;
    }
}