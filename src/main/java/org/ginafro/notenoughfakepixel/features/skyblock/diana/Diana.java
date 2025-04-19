package org.ginafro.notenoughfakepixel.features.skyblock.diana;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.ginafro.notenoughfakepixel.config.features.DianaF;
import org.ginafro.notenoughfakepixel.events.RenderEntityModelEvent;
import org.ginafro.notenoughfakepixel.utils.*;
import org.ginafro.notenoughfakepixel.Configuration;
import org.ginafro.notenoughfakepixel.events.PacketReadEvent;
import net.minecraft.client.Minecraft;
import org.ginafro.notenoughfakepixel.variables.MobDisplayTypes;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

import static org.ginafro.notenoughfakepixel.utils.ScoreboardUtils.getHubNumber;

public class Diana {
    ParticleProcessor processor = new ParticleProcessor();
    Color white = new Color(255, 255, 255, 100);
    private Queue<GaiaConstruct> listGaiaAlive = new ConcurrentLinkedQueue<>();
    private Queue<SiameseLynx> listSiameseAlive = new ConcurrentLinkedQueue<>();
    private int distanceRenderHitbox = 64;
    private Pattern cooldownPattern = Pattern.compile("§r§cThis ability is on cooldown for [0-9] "+"more seconds.§r");
    private Pattern minosInquisitor = Pattern.compile("§r§c§lUh oh! §r§eYou dug out §r§2Minos Inquisitor§r");
    private Pattern minosInquisitorPartyChat = Pattern.compile("§9Party §8> (?:§[0-9a-f])*\\[?(?:(?:§[0-9a-f])?[A-Z](?:§[0-9a-f])?\\+*(?:§[0-9a-f])?)*\\]?(?:§[0-9a-f])*.*?: Minos Inquisitor found at .*,? ?x:(-?\\d+), y:(-?\\d+), z:(-?\\d+) in HUB-(1[0-9]|[1-9])");
    private int counterTeleports = 0;
    private String inquisitorSound = "mob.enderdragon.growl";
    Instant lastCaptureTime = Instant.now();
    private final Map<String, int[]> locations = new HashMap<>();

    @SubscribeEvent
    public void onParticlePacketReceive(PacketReadEvent event) {
        if (!NotEnoughFakepixel.feature.diana.dianaShowWaypointsBurrows) return; // Check if the feature is enabled
        if (!ScoreboardUtils.currentLocation.isHub()) return; // Check if the player is in a hub
        //if (InventoryUtils.getSlot("Ancestral Spade") == -1) return;
        Packet packet = event.packet;
         if (packet instanceof S2APacketParticles) {
             S2APacketParticles particles = (S2APacketParticles) packet;
             //System.out.println(particles.getParticleType().getParticleName());
             // magicCrit enchantmenttable footstep -> empty (blue)
             // crit enchantmenttable -> mob (white)
             // dripLava enchantmenttable -> treasure (brown)
             if (particles.getParticleType().getParticleName().equals("crit") ||
                     particles.getParticleType().getParticleName().equals("magicCrit") ||
                     particles.getParticleType().getParticleName().equals("dripLava") ||
                     particles.getParticleType().getParticleName().equals("enchantmenttable") ||
                     particles.getParticleType().getParticleName().equals("footstep")) {

                 processor.addParticle(particles);

             } else if (particles.getParticleType().getParticleName().equals("angryVillager")) {
                // Siamese Lynx feature
                 Entity closestSiamese = getClosestSiamese(new int[] {(int)particles.getXCoordinate(), (int)particles.getYCoordinate(), (int)particles.getZCoordinate()});
                 if (closestSiamese != null) {
                     for (SiameseLynx siamese : listSiameseAlive) {
                         if (siamese.getEntity1() != null && siamese.getEntity1().getUniqueID() == closestSiamese.getUniqueID()) {
                             siamese.setHittable(closestSiamese);
                             //System.out.println("Ocelot 1 hittable");
                             break;
                         } else if (siamese.getEntity2() != null && siamese.getEntity2().getUniqueID() == closestSiamese.getUniqueID()) {
                             siamese.setHittable(closestSiamese);
                             //System.out.println("Ocelot 2 hittable");
                             break;
                         }
                         siamese.setHittable(closestSiamese);
                     }
                 }
             }

         }
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!ScoreboardUtils.currentLocation.isHub()) return; // Check if the player is in a hub
        //if (InventoryUtils.getSlot("Ancestral Spade") == -1) return;
        if (NotEnoughFakepixel.feature.diana.dianaShowWaypointsBurrows) drawWaypoints(event.partialTicks);
        if (NotEnoughFakepixel.feature.diana.dianaShowTracersWaypoints) drawTracers(event.partialTicks);
        if (NotEnoughFakepixel.feature.diana.dianaShowLabelsWaypoints) drawLabels(event.partialTicks);
        if (NotEnoughFakepixel.feature.diana.dianaGaiaConstruct || NotEnoughFakepixel.feature.diana.dianaSiamese) {
            dianaMobCheck(); // Check entities on world, add to lists if not tracked
            dianaMobRemover(); // Remove mobs from lists if out of render distance
            dianaMobRender(event.partialTicks); // Check for mobs in entities and draw a hitbox
        }
    }


    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!ScoreboardUtils.currentLocation.isHub()) return; // Check if the player is in a hub
        if (!NotEnoughFakepixel.feature.diana.dianaMinosInquisitorAlert) return;
        initializeLocations();
    }
    private final Set<EntityLivingBase> isInq = new HashSet<>();

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!ScoreboardUtils.currentLocation.isHub()) return; // Check if the player is in a hub
        if (!NotEnoughFakepixel.feature.diana.dianaMinosInquisitorAlert) return;
        String entityName = event.entity.getDisplayName().getUnformattedText();
        if (entityName.contains("Minos Inquisitor")) {
            Instant now = Instant.now();
            if (now.isAfter(lastCaptureTime.plusSeconds(63))) {
                Minecraft.getMinecraft().ingameGUI.displayTitle("Inquisitor detected!", null, 10, 40, 20);
                double x = Math.floor(event.entity.posX);
                double y = Math.floor(event.entity.posY);
                double z = Math.floor(event.entity.posZ);
                //Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc Minos Inquisitor found at x:"+event.entity.getPosition().getX()+", y:"+event.entity.getPosition().getY()+", z:"+event.entity.getPosition().getZ() + " in HUB-"+getHubNumber());
                String locationName = findNearestLocation((int) x, (int) y, (int) z);
                if (locationName != null) {
                    //System.out.println("/pc Minos Inquisitor found at " + locationName + ", x:"+event.entity.getPosition().getX()+", y:"+(event.entity.getPosition().getY()-2)+", z:"+event.entity.getPosition().getZ() + " in HUB-"+getHubNumber());
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc Minos Inquisitor found at " + locationName + ", x:"+event.entity.getPosition().getX()+", y:"+(event.entity.getPosition().getY()-2)+", z:"+event.entity.getPosition().getZ() + " in HUB-"+getHubNumber());
                } else {
                    //System.out.println("/pc Minos Inquisitor found at x:"+event.entity.getPosition().getX()+", y:"+(event.entity.getPosition().getY()-2)+", z:"+event.entity.getPosition().getZ() + " in HUB-"+getHubNumber());
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc Minos Inquisitor found at x:"+event.entity.getPosition().getX()+", y:"+(event.entity.getPosition().getY()-2)+", z:"+event.entity.getPosition().getZ() + " in HUB-"+getHubNumber());
                }
                lastCaptureTime = now;
            }
        }
        if (NotEnoughFakepixel.feature.diana.dianaMinosInquisitorOutline) {
        clearCache();
        WorldClient world = Minecraft.getMinecraft().theWorld;
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof EntityArmorStand) { // Check type before casting
                EntityArmorStand armorStand = (EntityArmorStand) entity;
                if (armorStand.getName().contains("Minos Inquisitor")) {
                    EntityLivingBase inq = findAssociatedMob(armorStand);
                    if (inq != null) {
                        isInq.add(inq);
                    }
                }
            }
        }
        }
    }

    public void clearCache() {
        isInq.clear();
    }

    private EntityLivingBase findAssociatedMob(EntityArmorStand armorStand) {
        return armorStand.worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                        armorStand.getEntityBoundingBox().expand(1.5, 3.0, 1.5),
                        e -> e != null &&
                                !(e instanceof EntityArmorStand) &&
                                e != Minecraft.getMinecraft().thePlayer
                ).stream()
                .findFirst()
                .orElse(null);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (Minecraft.getMinecraft().theWorld == null) return;

        final EntityLivingBase entity = event.getEntity();
        if (!isInq.contains(entity)) return;
        if (entity.isInvisible()) return;

        Color color = new Color(
                ColorUtils.getColor(NotEnoughFakepixel.feature.dungeons.dungeonsStarredBoxColor).getRGB()
        );

        OutlineUtils.outlineEntity(event, 5.0f, color, true);
    }

    public Set<EntityLivingBase> getCurrentEntities() {
        return isInq;
    }

    private String findNearestLocation(int playerX, int playerY, int playerZ) {
        String nearestLocation = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, int[]> entry : locations.entrySet()) {
            int[] coords = entry.getValue();
            double distance = Math.sqrt(Math.pow(playerX - coords[0], 2) + Math.pow(playerY - coords[1], 2) + Math.pow(playerZ - coords[2], 2));

            if (distance < minDistance) {
                minDistance = distance;
                nearestLocation = entry.getKey();
            }
        }

        return nearestLocation;
    }

    private void initializeLocations() {
        locations.put("Farm", new int[]{60, 72, -173});
        locations.put("Above Coal mine", new int[]{-40, 85, -196});
        locations.put("Farm", new int[]{81, 72, -140});
        locations.put("Koban4ik NPC", new int[]{83, 72, -102});
        locations.put("Colosseum left side", new int[]{101, 72, -73});
        locations.put("Wizard tower", new int[]{53, 72, 66});
        locations.put("Spider den portal", new int[]{-120, 76, -177});
        locations.put("Crypt entrance", new int[]{-173, 74, -92});
        locations.put("Dante statue in the graveyard", new int[]{-98, 72, -135});
        locations.put("Graveyard entrance", new int[]{-120, 71, -77});
        locations.put("Lumber jack npc", new int[]{-114, 74, -32});
        locations.put("Park portal", new int[]{-192, 74, -23});
        locations.put("Castle / ruins", new int[]{-209, 91, 70});
        locations.put("Museum", new int[]{-108, 68, 102});
        locations.put("High level", new int[]{-6, 71, 164});
        locations.put("Dark auction hut", new int[]{84, 74, 176});
        locations.put("Fairy lake in wilderness", new int[]{110, 66, 114});
        locations.put("Colosseum", new int[]{143, 76, -17});
    }

    private void drawWaypoints(float partialTicks) {
        List<Waypoint> safeResults = new ArrayList<>();
        synchronized (processor.getWaypoints()) {
            try {
                safeResults = new ArrayList<>(processor.getWaypoints());
            } catch (Exception ignored) {}
        }
        try {
            if (safeResults.isEmpty()) return;

            for (Waypoint result : safeResults) {
                if (result.isHidden()) continue;

                Color newColor = white;
                if (result.getType().equals("EMPTY"))
                    newColor = ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaEmptyBurrowColor);
                if (result.getType().equals("MOB"))
                    newColor = ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaMobBurrowColor);
                if (result.getType().equals("TREASURE"))
                    newColor = ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaTreasureBurrowColor);
                if (result.getType().equals("MINOS"))
                    newColor = new Color(243, 225, 107);
                newColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), 100);

                int x = result.getCoordinates()[0];
                int y = result.getCoordinates()[1];
                int z = result.getCoordinates()[2];

                BlockPos posAbsolute = new BlockPos(x, y - 1, z);

                RenderUtils.highlightBlock(posAbsolute, newColor, true, partialTicks);
                RenderUtils.renderBeaconBeam(posAbsolute, newColor.getRGB(), 1.0f, partialTicks);
                GlStateManager.enableTexture2D();
            }
        } catch (Exception ignored) {}
    }

    private void drawTracers(float partialTicks) {
        List<Waypoint> safeResults = new ArrayList<>();
        synchronized (processor.getWaypoints()) {
            try {
                safeResults = new ArrayList<>(processor.getWaypoints());
            } catch (Exception ignored) {}
        }
        try {
            for (Waypoint result : safeResults) {
                if (result.isHidden()) continue;
                Color newColor = white;
                if (result.getType().equals("EMPTY")) newColor = ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaEmptyBurrowColor);
                if (result.getType().equals("MOB")) newColor = ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaMobBurrowColor);
                if (result.getType().equals("TREASURE")) newColor = ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaTreasureBurrowColor);
                if (result.getType().equals("MINOS")) newColor = new Color(243, 225, 107);
                EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
                RenderUtils.draw3DLine(new Vec3(result.getCoordinates()[0]+0.5,result.getCoordinates()[1],result.getCoordinates()[2]+0.5),
                        player.getPositionEyes(partialTicks),
                        newColor,
                        4,
                        true,
                        partialTicks);
            }
        } catch (Exception ignored) {}
    }

    private void drawLabels(float partialTicks) {
        List<Waypoint> safeResults = new ArrayList<>();
        synchronized (processor.getWaypoints()) {
            try {
                safeResults = new ArrayList<>(processor.getWaypoints());
            } catch (Exception ignored) {}
        }
        try {
            for (Waypoint result : safeResults) {
                if (result.isHidden()) continue;
                String displayName;
                switch (result.getType()) {
                    case "MINOS":
                        displayName = "Inquisitor";
                        break;
                    case "EMPTY":
                        displayName = "EMPTY";
                        break;
                    case "MOB":
                        displayName = "MOB";
                        break;
                    case "TREASURE":
                        displayName = "TREASURE";
                        break;
                    default:
                        displayName = result.getType();
                }
                BlockPos pos = new BlockPos(result.getCoordinates()[0], result.getCoordinates()[1] + 1, result.getCoordinates()[2]);
                GuessBurrow.renderWaypointText(displayName, pos, partialTicks);
            }
        } catch (Exception ignored) {}
    }

    private void dianaMobRender(float partialTicks) {

        WorldClient world = Minecraft.getMinecraft().theWorld;
        // Iterate world entities
        world.loadedEntityList.forEach(entity -> {
            if (entity == null) return;
            if (entity.getName() == null) return;
            if (entity instanceof EntityGolem){
                for (GaiaConstruct gaia : listGaiaAlive) {
                    Entity gaiaEntity = gaia.getEntity();
                    if (gaiaEntity.getUniqueID() == entity.getUniqueID()) {
                        if (gaia.canBeHit()) {
                            RenderUtils.renderEntityHitbox(
                                    gaiaEntity,
                                    partialTicks,
                                    new Color(ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaGaiaHittableColor).getRed(), ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaGaiaHittableColor).getGreen(), ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaGaiaHittableColor).getBlue(), 150),
                                    MobDisplayTypes.GAIA
                            );
                        } else {
                            RenderUtils.renderEntityHitbox(
                                    gaiaEntity,
                                    partialTicks,
                                    new Color(ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaGaiaUnhittableColor).getRed(), ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaGaiaUnhittableColor).getGreen(), ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaGaiaUnhittableColor).getBlue(), 150),
                                    MobDisplayTypes.GAIA
                            );
                        }
                        break;
                    }
                }
            } else if (entity instanceof EntityOcelot){
                for (SiameseLynx siamese : listSiameseAlive) {
                    if (siamese.getHittable() == null) continue;
                    RenderUtils.renderEntityHitbox(
                            siamese.getHittable(),
                            partialTicks,
                            new Color(ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaSiameseHittableColor).getRed(), ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaSiameseHittableColor).getGreen(), ColorUtils.getColor(NotEnoughFakepixel.feature.diana.dianaSiameseHittableColor).getBlue(), 150),
                            MobDisplayTypes.SIAMESE
                    );
                }

            }
        });
    }

    private void dianaMobCheck() {
        WorldClient world = Minecraft.getMinecraft().theWorld;
        // Iterate world entities
        world.loadedEntityList.forEach(entity -> {
            if (entity == null) return;
            if (entity.getName() == null) return;
            if (entity instanceof EntityGolem){
                // Iterate gaia list
                for (GaiaConstruct gaia : listGaiaAlive) {
                    // If already added, don't add again
                    if (gaia.getEntity().getUniqueID() == entity.getUniqueID()) return;
                }
                // If this point reached, no occurrences, so new gaia added
                listGaiaAlive.add(new GaiaConstruct(entity));
                //System.out.println("Gaia added, "+listGaiaAlive.size());
            } else if (entity instanceof EntityArmorStand) {
                if (!(entity.getDisplayName().getUnformattedText().contains("Bagheera") || entity.getDisplayName().getUnformattedText().contains("Azrael"))) return;
                for (SiameseLynx siamese : listSiameseAlive) {
                    if (siamese.getEntity1() == null) return;
                    // If already added, don't add again
                    if (siamese.getEntity1().getUniqueID() == entity.getUniqueID()) return;
                    if (siamese.getEntity2() == null) {
                        siamese.setEntity2(entity);
                        //System.out.println("Ocelot2 added, "+listSiameseAlive.size());
                    }
                    if (siamese.getEntity2().getUniqueID() == entity.getUniqueID()) return;
                }
                // If this point reached, no occurrences, so new siamese added
                listSiameseAlive.add(new SiameseLynx(entity));
                //System.out.println("Siamese added, "+listSiameseAlive.size());
            }
        });
    }

    private void dianaMobRemover() {
        int[] playerCoords = new int[] {(int)Minecraft.getMinecraft().thePlayer.posX, (int)Minecraft.getMinecraft().thePlayer.posY, (int)Minecraft.getMinecraft().thePlayer.posZ};
        for (GaiaConstruct gaia : listGaiaAlive) {
            int[] gaiaCoords = new int[]{gaia.getEntity().getPosition().getX(), gaia.getEntity().getPosition().getY(), gaia.getEntity().getPosition().getZ()};
            if (!processor.areCoordinatesClose(playerCoords,gaiaCoords,distanceRenderHitbox)) {
                listGaiaAlive.remove(gaia);
                //System.out.println("Gaia removed for distance, "+listGaiaAlive.size());
            }
        }
        for (SiameseLynx siamese : listSiameseAlive) {
            // If both null = death, remove from list of siameses
            if (siamese.getEntity1() == null && siamese.getEntity2() == null) {
                listSiameseAlive.remove(siamese);
                //System.out.println("Siamese removed for distance"+listSiameseAlive.size());
                return;
            }
            if (siamese.getEntity1() != null) {
                int[] siamese1Coords = new int[]{siamese.getEntity1().getPosition().getX(), siamese.getEntity1().getPosition().getY(), siamese.getEntity1().getPosition().getZ()};
                if (!processor.areCoordinatesClose(playerCoords, siamese1Coords, distanceRenderHitbox)) {
                    siamese.setEntity1(null);
                    //System.out.println("Ocelot1 removed for distance, "+listSiameseAlive.size());
                }
            }
            if (siamese.getEntity2() != null) {
                int[] siamese2Coords = new int[]{siamese.getEntity2().getPosition().getX(), siamese.getEntity2().getPosition().getY(), siamese.getEntity2().getPosition().getZ()};
                if (!processor.areCoordinatesClose(playerCoords, siamese2Coords, distanceRenderHitbox)) {
                    siamese.setEntity2(null);
                    //System.out.println("Ocelot2 removed for distance, "+listSiameseAlive.size());
                }
            }
        }
    }

    /*@SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        System.out.println("\n\n"+event.entity.getName()+ " hurt");
        if (event.entity instanceof EntityGolem) { // check if golem has been hurt
            System.out.println("Golem hurt");
            for (GaiaConstruct gaia : listGaiaAlive) {
                if (gaia.getEntity() == event.entity) gaia.addHit();
            }
        }
    }*/
    @SubscribeEvent
    public void onSoundPacketReceive(PacketReadEvent event) {
        if (!ScoreboardUtils.currentLocation.isHub()) return; // Check if the player is in a hub
        Packet packet = event.packet;
        if (packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect soundEffect = (S29PacketSoundEffect) packet;
            int[] coordsSound = new int[] {(int)Math.floor(soundEffect.getX()), (int)Math.floor(soundEffect.getY()), (int)Math.floor(soundEffect.getZ())};
            String soundName = soundEffect.getSoundName();
            //System.out.println(soundName);
            switch (soundName) {
                // Remove explosion sound feature
                case "random.explode":
                    if (!NotEnoughFakepixel.feature.diana.dianaDisableDianaExplosionSounds) return;
                    if (Math.floor(soundEffect.getPitch()*1000)/1000 == 1.190) {
                        if (event.isCancelable()) event.setCanceled(true);
                    }
                    break;
                // Remove waypoint at pling sound
                case "note.pling":
                    //System.out.println(soundName + ", " + soundEffect.getVolume() + ", " + soundEffect.getPitch());
                    if (NotEnoughFakepixel.feature.diana.dianaShowWaypointsBurrows) {
                        deleteClosestWaypoint(coordsSound[0], coordsSound[1], coordsSound[2]);
                    }

                    break;
                // Gaia track hits feature
                case "mob.zombie.metal":
                case "mob.irongolem.death":
                case "mob.irongolem.hit":
                    if (!NotEnoughFakepixel.feature.diana.dianaGaiaConstruct) return; // Check if the feature is enabled
                    // Gaia track hits feature
                    GaiaConstruct closestGaia = getClosestGaia(coordsSound);
                    if (closestGaia == null) return;
                    if (soundName.equals("mob.zombie.metal")) {
                        //System.out.println("GAIA HIT " + closestGaia.getHits() + "/" + closestGaia.getHitsNeeded()[closestGaia.getState()]);
                        closestGaia.addHit();
                    } else if (soundName.equals("mob.irongolem.hit")) {
                        //System.out.println("GAIA HURT"+closestGaia.getHits()+"/"+closestGaia.getHitsNeeded()[closestGaia.getState()]);
                        //System.out.println("Hit tooks: "+closestGaia.getHits());
                        closestGaia.hurtAction();
                    } else {
                        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                        exec.schedule(new Runnable() {
                            public void run() {
                                listGaiaAlive.remove(closestGaia);
                                //System.out.println("Gaia removed, " + listGaiaAlive.size());
                            }
                        }, 1, TimeUnit.SECONDS);
                    }
                    break;
                case "note.harp":
                    /*if (Configuration.disableDianaHarpSounds) {
                        if (event.isCancelable()) event.setCanceled(true);
                    }*/
                    break;
            }
        }
    }

    private GaiaConstruct getClosestGaia(int[] coords) {
        GaiaConstruct returnedGaia = null;
        float distance = Float.MAX_VALUE;
        for (GaiaConstruct gaia : listGaiaAlive) {
            int[] coordsGaia = new int[] {gaia.getEntity().getPosition().getX(), gaia.getEntity().getPosition().getY() ,gaia.getEntity().getPosition().getZ()};
            if (processor.getDistance(coords, coordsGaia) < distance) {
                distance = processor.getDistance(coords, coordsGaia);
                returnedGaia = gaia;
            }
        }
        return returnedGaia;
    }

    private Entity getClosestSiamese(int[] coords) {
        Entity returnedSiamese = null;
        float distance = Float.MAX_VALUE;
        for (SiameseLynx siamese : listSiameseAlive) {
            if (siamese.getEntity1() != null) {
                int[] coordsSiamese1 = new int[]{siamese.getEntity1().getPosition().getX(), siamese.getEntity1().getPosition().getY(), siamese.getEntity1().getPosition().getZ()};
                if (processor.getDistance(coords, coordsSiamese1) < distance) {
                    distance = processor.getDistance(coords, coordsSiamese1);
                    returnedSiamese = siamese.getEntity1();
                }
            }
            if (siamese.getEntity2() != null) {
                int[] coordsSiamese2 = new int[]{siamese.getEntity2().getPosition().getX(), siamese.getEntity2().getPosition().getY(), siamese.getEntity2().getPosition().getZ()};
                if (processor.getDistance(coords, coordsSiamese2) < distance) {
                    distance = processor.getDistance(coords, coordsSiamese2);
                    returnedSiamese = siamese.getEntity2();
                }
            }

        }
        return returnedSiamese;
    }


    private void deleteClosestWaypoint(int x, int y, int z) {
        //int[] playerCoords = new int[] {(int)Minecraft.getMinecraft().thePlayer.posX, (int)Minecraft.getMinecraft().thePlayer.posY, (int)Minecraft.getMinecraft().thePlayer.posZ};
        int[] coords = new int[] {x, y, z};
        Waypoint res = processor.getClosestWaypoint(coords);

        if (res == null) return;
        if (processor.areCoordinatesClose(res.getCoordinates(), coords, 3)) {
            //if (res.getType().equals("MINOS")) return;
            res.setHidden(true);
            ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
            exec.schedule(new Runnable() {
                public void run() {
                    processor.deleteWaypoint(res);
                }
            }, 30000, TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public void onChatRecieve(ClientChatReceivedEvent event){
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        if (!ScoreboardUtils.currentLocation.isHub()) return;
        if (ChatUtils.middleBar.matcher(event.message.getFormattedText()).matches()) return;
        //System.out.println(event.message.getFormattedText());
        if (NotEnoughFakepixel.feature.diana.dianaCancelCooldownSpadeMessage && InventoryUtils.getSlot("Ancestral Spade") == InventoryUtils.getCurrentSlot()) {
            cancelMessage(true, event, cooldownPattern, true);
        }
        if (NotEnoughFakepixel.feature.diana.dianaMinosInquisitorAlert) {
            Matcher matcher = minosInquisitorPartyChat.matcher(event.message.getFormattedText());
            if (matcher.find()) {
                // extract from message
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));
                int hubNumber = Integer.parseInt(matcher.group(4)); // hub number

                int[] coords = new int[] {Minecraft.getMinecraft().thePlayer.getPosition().getX(),
                        Minecraft.getMinecraft().thePlayer.getPosition().getY(),
                        Minecraft.getMinecraft().thePlayer.getPosition().getZ()};
                SoundUtils.playSound(coords, inquisitorSound, 3.0f, 0.8f);

                if (getHubNumber() == hubNumber) {
                    Waypoint wp = new Waypoint("MINOS", new int[]{x, y, z});
                    processor.deleteWaypoint(processor.getClosestWaypoint(new int[] {x,y,z}));
                    processor.addWaypoint(wp);
                    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(3);
                    exec.schedule(new Runnable() {
                        public void run() {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW+"30 seconds for despawn!"));
                        }
                    }, 30, TimeUnit.SECONDS);
                    exec.schedule(new Runnable() {
                        public void run() {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW+"10 seconds for despawn!"));
                        }
                    }, 50, TimeUnit.SECONDS);
                    exec.schedule(new Runnable() {
                        public void run() {
                            processor.deleteWaypoint(wp);
                        }
                    }, 60, TimeUnit.SECONDS);
                }

            } else {
                //System.out.println("No match found.");
            }
        }


    }

    private void cancelMessage(boolean option, ClientChatReceivedEvent e, Pattern pattern, boolean formatted){
        if (!option) return;
        String message = e.message.getUnformattedText();
        if (formatted) message = e.message.getFormattedText();
        if (pattern.matcher(message).find() || pattern.matcher(message).matches()){
            e.setCanceled(true);
        }
    }

    @SubscribeEvent()
    public void onWorldUnload(WorldEvent.Unload event) {
        if (NotEnoughFakepixel.feature.diana.dianaShowWaypointsBurrows) processor.clearWaypoints();
        if (NotEnoughFakepixel.feature.diana.dianaGaiaConstruct) listGaiaAlive.clear();
        if (NotEnoughFakepixel.feature.diana.dianaSiamese) listSiameseAlive.clear();
    }

}