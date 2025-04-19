package org.ginafro.notenoughfakepixel.features.skyblock.slayers;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.ginafro.notenoughfakepixel.Configuration;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.ginafro.notenoughfakepixel.config.features.Slayer;
import org.ginafro.notenoughfakepixel.events.Handlers.ScoreboardHandler;
import org.ginafro.notenoughfakepixel.utils.ScoreboardUtils;

import java.util.List;

public class SlayerTimer {

    private long startTime = -1;
    private boolean isBossActive = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (NotEnoughFakepixel.feature.slayer.slayerBossTimer && ScoreboardUtils.currentGamemode.isSkyblock()) {
            if (event.side == net.minecraftforge.fml.relauncher.Side.CLIENT) {
                List<String> sidebarLines = ScoreboardHandler.getSidebarLines();

                for (String line : sidebarLines) {
                    if (line.contains("Slay the boss!")) {
                        if (!isBossActive) {
                        startTime = System.currentTimeMillis();
                        isBossActive = true;
                        }
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (NotEnoughFakepixel.feature.slayer.slayerBossTimer && ScoreboardUtils.currentGamemode.isSkyblock()) {
            String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

            if (message.contains("SLAYER QUEST COMPLETE!")) {
                if (isBossActive) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                double seconds = duration / 1000.0;

                String chatMessage = String.format("[NEF] Boss took %.3f seconds!", seconds);
                ChatComponentText formattedMessage = new ChatComponentText(chatMessage);
                formattedMessage.getChatStyle().setColor(EnumChatFormatting.GREEN);

                net.minecraft.client.Minecraft.getMinecraft().thePlayer.addChatMessage(formattedMessage);

                startTime = -1;
                isBossActive = false;
                }
            }
            if (message.contains("SLAYER QUEST FAILED!")) {
                startTime = -1;
                isBossActive = false;
            }
        }
    }
}