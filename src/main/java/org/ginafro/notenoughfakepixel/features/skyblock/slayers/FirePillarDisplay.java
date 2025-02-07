package org.ginafro.notenoughfakepixel.features.skyblock.slayers;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.ginafro.notenoughfakepixel.Configuration;

public class FirePillarDisplay {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private EntityArmorStand trackedPillar;
    private long lastSoundTime;

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!Configuration.slayerFirePillarDisplay || mc.theWorld == null) return;

        // check armor stands every 5 ticks to reduce load
        if (mc.theWorld.getTotalWorldTime() % 5 != 0) return;

        mc.theWorld.getLoadedEntityList().stream()
                .filter(e -> e instanceof EntityArmorStand)
                .map(e -> (EntityArmorStand) e)
                .forEach(this::processArmorStand);
    }

    private void processArmorStand(EntityArmorStand armorStand) {

        if (armorStand.getDisplayName() == null) return;


        String rawName = armorStand.getDisplayName().getUnformattedText();
        String cleanName = rawName.trim().replaceAll("ยง.", "");

        String[] parts = cleanName.split(" ");
        if (parts.length != 3) return;
        if (!parts[0].endsWith("s") || !parts[2].equals("hits")) return;


        if (trackedPillar == null || trackedPillar.isDead) {
            trackedPillar = armorStand;
            lastSoundTime = System.currentTimeMillis();
        }


        if (trackedPillar.equals(armorStand)) {
            updatePillarDisplay(cleanName);
        }
    }

    private void updatePillarDisplay(String cleanName) {

        int seconds = Integer.parseInt(cleanName.split(" ")[0].replace("s", ""));


        mc.ingameGUI.displayTitle(
                trackedPillar.getDisplayName().getFormattedText(),
                "",
                0,
                20,
                10
        );

        // play pling sound every X seconds
        if (System.currentTimeMillis() - lastSoundTime > seconds * 1000) {
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.create(new ResourceLocation("note.pling"), 1.0F)
            );
            lastSoundTime = System.currentTimeMillis();
        }
    }
}