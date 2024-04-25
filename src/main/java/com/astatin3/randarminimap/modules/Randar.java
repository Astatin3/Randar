package com.astatin3.randarminimap.modules;

import com.astatin3.randarminimap.Addon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class Randar extends Module {
    public Randar() {
        super(Addon.CATEGORY, "Randar", "A Quick and dirty minimap using the Randar exploit");
    }

    @Override
    public void onActivate() {}

    @Override
    public void onDeactivate() {}



    public void clearMap(){
        positonRanges = new ArrayList<>();
    }

    public String getSeed(){
        return seed.get();
    }

    public boolean setSeed(String newSeed){
        return seed.set(newSeed);
    }


    private class positonRange {
        public final int startX;
        public final int startY;
        public final int endX;
        public final int endY;
        public final long time;

        public positonRange(int startX, int startY, int endX, int endY, long time){
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.time = time;
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private positonRange nextPositonRange;


    public enum Corner {
        Top_Left,
        Top_Right,
        Bottom_Left,
        Bottom_Right
    }

    private final Setting<String> seed = sgGeneral.add(new StringSetting.Builder()
        .name("Seed")
        .description("The seed of the server to crack positions")
        .defaultValue("-1337")
        .visible(() -> true)
        .build()
    );

    private final Setting<Corner> mapCorner = sgGeneral.add(new EnumSetting.Builder<Corner>()
        .name("Map Corner")
        .description("The corner that the map is in")
        .defaultValue(Corner.Bottom_Left)
        .visible(() -> true)
        .build()
    );

    private final Setting<Integer> mapScale = sgGeneral.add(new IntSetting.Builder()
        .name("Map Range")
        .description("The bounds of the visualization (blocks)")
        .defaultValue(10000)
        .min(1)
        .max(30000000)
        .sliderMax(500000)
        .visible(() -> true)
        .build()
    );

    private final Setting<SettingColor> mapColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Color")
        .description("Change the color of detected areas")
        .defaultValue(new Color(255,0,0))
        .visible(() -> true)
        .build()
    );

    private final Setting<Boolean> heatmapEnable = sgGeneral.add(new BoolSetting.Builder()
        .name("Heatmap Mode")
        .description("Change brightness of detected areas based on age")
        .defaultValue(false)
        .visible(() -> true)
        .build()
    );

    private final Setting<Double> heatmapMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("Heatmap Multiplier")
        .description("The brightness of heatmap")
        .defaultValue(10000)
        .min(0)
        .sliderMax(5)
        .visible(heatmapEnable::get)
        .build()
    );

    private final Setting<Integer> mapSize = sgGeneral.add(new IntSetting.Builder()
        .name("Size")
        .description("The scale of the map in pixels")
        .defaultValue(150)
        .min(1)
        .sliderMax(500)
        .visible(() -> true)
        .build()
    );

    private final Setting<Integer> smearSetting = sgGeneral.add(new IntSetting.Builder()
        .name("Smear")
        .description("Smear invisible map areas to make them more visible")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .visible(() -> true)
        .build()
    );

    private final Setting<Boolean> radarMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Radar Mode")
        .description("Centers the visualization around the player, not [0,0].")
        .defaultValue(true)
        .visible(() -> true)
        .build()
    );

    private final Setting<Boolean> verboseMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Verbose")
        .description("Print the output of scanning.")
        .defaultValue(false)
        .visible(() -> true)
        .build()
    );




    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if(!isActive()){return;}
        if(!(event.packet instanceof EntitySpawnS2CPacket)){return;}
        final EntitySpawnS2CPacket packet = (EntitySpawnS2CPacket) event.packet;
        if(!(packet.getEntityType() == EntityType.ITEM)){return;}
        if(!(packet.getVelocityY() == 0.2)){return;}


        new Thread(() -> crackItemDropCoordinate(packet.getX(), packet.getY(), packet.getZ())).start();
//        ChatUtils.sendMsg(Formatting.AQUA, "X: " + packet.getX() + " Y: " + packet.getY() + " Z: " + packet.getZ());
//        ChatUtils.sendMsg(Formatting.AQUA, "Y: " + packet.getVelocityY());
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int map(long val, int oldRange, int newRange) {
        return (int) Math.round(((double)val/oldRange)*newRange);
    }

    ArrayList<positonRange> positonRanges = new ArrayList<>();
    final int crosshair_tickness = 5;

    @EventHandler
    public void onRender2D(Render2DEvent event){
        if(!(isActive())){return;}

        final int boxX;
        final int boxY;
        final int width = mapSize.get();
        final int height = mapSize.get();

        switch (mapCorner.get()){
            case Top_Left:
                boxX = 0;
                boxY = 0;
                break;
            case Top_Right:
                boxX = mc.getWindow().getFramebufferWidth()-mapSize.get();
                boxY = 0;
                break;
            case Bottom_Left:
                boxX = 0;
                boxY = mc.getWindow().getFramebufferHeight()-mapSize.get();
                break;
            case Bottom_Right:
                boxX = mc.getWindow().getFramebufferWidth()-mapSize.get();
                boxY = mc.getWindow().getFramebufferHeight()-mapSize.get();
                break;
            default:
                boxX = 0;
                boxY = 0;
                break;
        }

        final int centerX;
        final int centerY;
        if(radarMode.get()){
            centerX = (int)mc.player.getX();
            centerY = (int)mc.player.getZ();
        }else{
            centerX = 0;
            centerY = 0;
        }



        Renderer2D.COLOR.begin();

        Renderer2D.COLOR.quad(boxX,boxY,width,height,new Color(0,0,0));

        final int mapBound = mapScale.get();
        final int smear = smearSetting.get();

        final double multiplier = heatmapMultiplier.get();
        final long time = System.currentTimeMillis();

        final boolean heatmapMode = heatmapEnable.get();

        final Color color = mapColor.get();

        if(nextPositonRange != null){
            // This weirdness is to avoid a ConcurrentModificationException
            positonRanges.add(nextPositonRange);
            nextPositonRange = null;
        }

        for(positonRange posRange: positonRanges){
//            int scaledX = boxX + (posRange.startX + mapBound) * width / (2 * mapBound);
//            int scaledY = boxY + (posRange.startY + mapBound) * height / (2 * mapBound);
//            int scaledWidth = (posRange.endX - startX) * width / (2 * mapBound);
//            int scaledHeight = (posRange.endY - startY) * height / (2 * mapBound);

            int x = boxX+(width/2)+map(posRange.startX-centerX, mapBound, width);
            int y = boxY+(height/2)+map(posRange.startY-centerY, mapBound, height);
            int w = map(Math.abs(posRange.endX-posRange.startX), mapBound, width);
            int h = map(Math.abs(posRange.endY-posRange.startY), mapBound, height);

            final int alpha;

            if(heatmapMode) {
                final long diffTime = (time - posRange.time);
                alpha = 256-clamp(
                    (int) (((diffTime / 1000) * multiplier)),
                    0, 255);
            }else{
                alpha = 255;
            }

//            System.out.println(diffTime/1000 + ", " + alpha);

            Renderer2D.COLOR.quad(
                x-smear, y-smear,
                w+(smear*2), h+(smear*2),
                new Color(
                    color.r,
                    color.g,
                    color.b,
                    alpha));
        }

        Renderer2D.COLOR.quad(boxX+(int)((width+ crosshair_tickness)/2),boxY, crosshair_tickness,height,new Color(255,255,255, 64));
        Renderer2D.COLOR.quad(boxX,boxY+(int)((height+ crosshair_tickness)/2),width, crosshair_tickness,new Color(255,255,255, 64));


        Renderer2D.COLOR.render(null);

    }

//    private static long WORLD_SEED = Long.parseLong("-3010010539417993252");


    private void crackItemDropCoordinate(double dropX, double dropY, double dropZ) {
        float spawnX = ((float) (dropX - (int) Math.floor(dropX) - 0.25d)) * 2;
        float spawnY = ((float) (dropY - (int) Math.floor(dropY) - 0.25d)) * 2;
        float spawnZ = ((float) (dropZ - (int) Math.floor(dropZ) - 0.25d)) * 2;
        if (spawnX <= 0 || spawnX >= 1 || spawnY <= 0 || spawnY >= 1 || spawnZ <= 0 || spawnZ >= 1) {
            if(verboseMode.get()) {
                ChatUtils.sendMsg(Formatting.AQUA, "[Randar] Skipping this item because its coordinates are out of bounds. This probably means that the item only coincidentally looked like an item that was dropped from mining a block. Other ways to drop items (e.g. dropping from a player's inventory) can sometimes cause false positives like this.");
            }
            return;
        }
        int measurement1 = (int) (spawnX * (1 << 24));
        int measurement2 = (int) (spawnY * (1 << 24));
        int measurement3 = (int) (spawnZ * (1 << 24));
        long cubeCenterX = ((long) measurement1 << 24) + 8388608L;
        long cubeCenterY = ((long) measurement2 << 24) + 8388597L;
        long cubeCenterZ = ((long) measurement3 << 24) - 277355554490L;
        double basisCoeff0 = 9.555378710501827E-11 * cubeCenterX + -2.5481838861196593E-10 * cubeCenterY + 1.184083942007419E-10 * cubeCenterZ;
        double basisCoeff1 = -1.2602185961441137E-10 * cubeCenterX + 6.980727107475104E-11 * cubeCenterY + 1.5362999761237006E-10 * cubeCenterZ;
        double basisCoeff2 = -1.5485213111787743E-10 * cubeCenterX + -1.2997958265259513E-10 * cubeCenterY + -5.6285642813236336E-11 * cubeCenterZ;
        long locSeed = Math.round(basisCoeff0) * 1270789291L + Math.round(basisCoeff1) * -2355713969L + Math.round(basisCoeff2) * -3756485696L & 281474976710655L;
        long next = locSeed * 25214903917L + 11L & 281474976710655L;
        long nextNext = next * 25214903917L + 11L & 281474976710655L;
        if ((locSeed >> 24 ^ measurement1 | next >> 24 ^ measurement2 | nextNext >> 24 ^ measurement3) != 0L) {
            if(verboseMode.get()) {
                ChatUtils.sendMsg(Formatting.AQUA, "[Randar] Failed to crack the locSeed. This probably means that the item only coincidentally looked like an item that was dropped from mining a block. Other ways to drop items (e.g. dropping from a player's inventory) can sometimes cause false positives like this.");
            }
            return;
        }
        long origSeed = locSeed;
        for (int i = 0; i < 5000; i++) {
            for (int x = -23440; x <= 23440; x++) {
                long z = (((locSeed ^ 25214903917L) - Long.parseLong(seed.get()) - 10387319 - x * 341873128712L) * 211541297333629L) << 16 >> 16;
                if (z >= -23440 && z <= 23440) {
                    if(verboseMode.get()) {
                        ChatUtils.sendMsg(Formatting.AQUA, "[Randar] Item drop appeared at " + dropX + " " + dropY + " " + dropZ);
                        ChatUtils.sendMsg(Formatting.AQUA, "[Randar] RNG measurements are therefore " + measurement1 + " " + measurement2 + " " + measurement3);
                        ChatUtils.sendMsg(Formatting.AQUA, "[Randar] This indicates the java.util.Random internal locSeed must have been " + origSeed);
                        ChatUtils.sendMsg(Formatting.AQUA, "[Randar] Found a woodland match at woodland region " + x + " " + z + " which would have set the locSeed to " + locSeed);
                    }
                    final int startX = (int)(x * 1280 - 128);
                    final int startY = (int)(z * 1280 - 128);
                    final int endX = (int)(z * 1280 - 128);
                    final int endY = (int)(z * 1280 + 1151);
                    nextPositonRange = new positonRange(startX, startY, endX, endY, System.currentTimeMillis());
                    ChatUtils.sendMsg(Formatting.AQUA, "[Randar] Located someone between " + startX + "," + startY + " and " + endX + "," + endY);
                    return;
                }
            }
            locSeed = (locSeed * 246154705703781L + 107048004364969L) & 281474976710655L;
        }
        if(verboseMode.get()) {
            ChatUtils.sendMsg(Formatting.AQUA, "[Randar] Failed to crack. This probably means that your world locSeed is incorrect, or there were no chunk loads recently.");
        }
    }
}
