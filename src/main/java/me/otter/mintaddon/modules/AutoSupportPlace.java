package me.otter.mintaddon.modules;

import me.otter.mintaddon.util.DamageUtils;
import com.google.common.collect.Streams;
import dev.boze.api.addon.module.ToggleableModule;
import dev.boze.api.client.ChatHelper;
import dev.boze.api.client.FriendManager;
import dev.boze.api.event.EventPlayerUpdate;
import dev.boze.api.event.EventWorldRender;

import dev.boze.api.interaction.Interaction;
import dev.boze.api.interaction.PlaceInteraction;
import dev.boze.api.interaction.Rotation;
import dev.boze.api.interaction.RotationHelper;
import dev.boze.api.render.DrawColor;
import dev.boze.api.setting.*;
import me.otter.mintaddon.util.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import static me.otter.mintaddon.MintAddon.mc;
import static me.otter.mintaddon.util.BlockUtils.isSolidBlock;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class
AutoSupportPlace extends ToggleableModule{


    List prioritymode = new ArrayList();
    List placemode = new ArrayList();

    final SettingMode mode = new SettingMode(this, "Priority", "Mode for choosing the best target.", prioritymode);
    final SettingMode placem = new SettingMode(this, "PlaceMode", "If you want to place only once or continuous.", placemode);

    final SettingSlider searchtargetrange = new SettingSlider(this, "SearchTargetRange", "In what range to search for target.", 8,1,15,1);
    final SettingSlider searchrange = new SettingSlider(this, "SearchAroundTargetRange", "In what range to search for placeposition around target.", 4,1,6,1);
    final SettingSlider mindamage = new SettingSlider(this, "MinDamage", "What minimum damage has to be met, so placeposition is considered as good.", 4,1,36,1);
    final SettingSlider placerange = new SettingSlider(this, "PlaceRange", "Maximum range for placing.", 4,1,10,1);
    final SettingSlider placedelay = new SettingSlider(this, "PlaceDelay", "Minimum delay between placing blocks for mode continuous (ticks).", 1,0,20,1);
    final SettingToggle searchdownwards = new SettingToggle(this, "SearchDownwards", "Look downwards for placepositions. Otherwise it only looks on target level.", true);
    final SettingToggle waitForNeed = new SettingToggle(this, "Wait for Need", "Wait, if a good Obsidian block is already available.", true);
    final SettingToggle rotate = new SettingToggle(this, "Rotate", "If you want to rotate for placing.", true);
    final SettingToggle swing = new SettingToggle(this, "Swing", "If you want to swing when playing.", true);
    final SettingToggle wait = new SettingToggle(this, "Wait for Boze", "Wait if Boze is currently interacting.",false);

    final SettingColor color = new SettingColor(this, "TargetColor", "Render color for target in mode continuous.", DrawColor.create(200,20,20,100));
    final SettingToggle debug = new SettingToggle(this, "Debug", "Notify in chat when trying to place at position.", true);
    //airplace
    //maxselfdamage

    PlayerEntity target;
    String targetname;
    BlockPos bestplacelocation = null;
    int delayCounter = 0;
    boolean firstBlockPlaced = false;
    int obbyslot = -1;

    Runnable callback = new Runnable() {
        @Override
        public void run() {

        }
    };

    private void togglemodule() {
        this.setState(false);
    }

    public AutoSupportPlace() {
        super("SupportPlacement", "Place supporting obby to place crystals on.");
        prioritymode.add("Distance");
        prioritymode.add("Health");
        placemode.add("Continuous");
        placemode.add("Once");
    }

    @Override
    public void onDisable() {
        firstBlockPlaced = false;
        targetname = null;
        delayCounter = 0;
        obbyslot = -1;
    }

    @EventHandler
    public void onUpdate(EventPlayerUpdate eventPlayerUpdate) {

        if (firstBlockPlaced && placem.getValue() == 0) {
            delayCounter++;
            if (delayCounter < placedelay.getValue()) {
                return;
            }
        }

        target = getBestTarget();

        if(getBestTarget() == null) {
            return;
        }

        targetname = target.getGameProfile().getName();
        obbyslot = -1;
        obbyslot = InventoryUtils.findItemInHotbar(Items.OBSIDIAN);

        if(getBestPlaceLocation() == null || obbyslot == -1) {
            return;
        }
        bestplacelocation = getBestPlaceLocation();

        if(bestplacelocation == null) {
            return;
        }

        if (!nogoodobbyaroundtarget() && waitForNeed.getValue()) {
            return;
        }

        if(wait.getValue() && eventPlayerUpdate.isBozeInteracting()) {
            return;
        }

        placebestoption(eventPlayerUpdate);

        if(placem.getValue() == 1) {
            togglemodule();
        }

        delayCounter = 0;
        firstBlockPlaced = true;
    }

    public void placebestoption(EventPlayerUpdate eventPlayerUpdate) {
        Interaction interaction;
        Rotation rotation = null;
        Direction placedirection = determinePlaceDirection(bestplacelocation);

        if(rotate.getValue()) {
            rotation = RotationHelper.calculateAngle(bestplacelocation.toCenterPos());
        }

        interaction = new PlaceInteraction(callback, rotation, bestplacelocation, placedirection, Hand.MAIN_HAND, swing.getValue(), obbyslot);
        eventPlayerUpdate.addInteraction(interaction);
        senddebug(bestplacelocation);
    }

    private void senddebug(BlockPos placelocation) {
        if(debug.getValue()) {
            ChatHelper.sendMsg(this.getName(), "["+ placelocation.getX() + ", " + placelocation.getY() + ", " + placelocation.getZ() + "] ");
        }
    }

    private PlayerEntity getBestTarget() {
        List<Entity> potentialtargetlist = null;
        potentialtargetlist = Playerlistsorted();
        if(potentialtargetlist == null) {
            return null;
        }
        if(potentialtargetlist.isEmpty()) {
            return null;
        }
        return (PlayerEntity) potentialtargetlist.get(0);
    }

    private BlockPos getBestPlaceLocation() {
        List<BlockPos> bestplacelocations = null;
        bestplacelocations = getPlacePositionsAroundTarget();
        if(bestplacelocations == null) {
            return null;
        }
        if(bestplacelocations.isEmpty()) {
            return null;
        }
        return bestplacelocations.get(0);
    }


    private boolean nogoodobbyaroundtarget() {

        if(target == null) {
            return false;
        }

        Stream entitiess = Streams.stream(mc.world.getEntities())
                .filter(entity -> target.distanceTo(entity) < searchrange.getValue());

        List<Entity> potentialcollidingentities = (List<Entity>) entitiess.collect(Collectors.toList());
        List<BlockPos> obbyblocksintargetrange = getBlockPositions(Blocks.OBSIDIAN);

        int nogoodobbycount = 0;
        for (int i = 0; i < obbyblocksintargetrange.size(); i++) {
            BlockPos pos = obbyblocksintargetrange.get(i);
            BlockState state = mc.world.getBlockState(pos.up());
            if(DamageUtils.getExplosionDamage(Vec3d.of(pos.up()), 6f, target) < mindamage.getValue()
                    || state.getBlock() != Blocks.AIR
                    || !isPositionSafeFromEntities(pos.up(), potentialcollidingentities)
                    || mc.player.squaredDistanceTo(Vec3d.of(pos)) > placerange.getValue()* placerange.getValue() ) {
                nogoodobbycount++;
            }
        }

        if(nogoodobbycount == obbyblocksintargetrange.size()) { // no position
            return true;
        }
        return false;
    }



    public List<BlockPos> getPlacePositionsAroundTarget() {
        List<BlockPos> placepositions = new ArrayList<>();

        if (target == null) {
            return null;
        }

        Stream entitiess = Streams.stream(mc.world.getEntities())
                .filter(entity -> target.distanceTo(entity) < searchrange.getValue());

        List<Entity> potentialcollidingentities = (List<Entity>) entitiess.collect(Collectors.toList());

        //get every air block around target within search range
        for (BlockPos block: getBlockPositions(Blocks.AIR)) {
            placepositions.add(block);
        }

        //see if obsidian is actually good
        Stream<BlockPos> positionstream = Streams.stream(placepositions)
                .filter(blockPos -> mc.world.getBlockState(blockPos.up()).getBlock() == Blocks.AIR) // see if air is over placepos
                .filter(blockPos -> DamageUtils.getExplosionDamage(Vec3d.of(blockPos.up()), 6f, target) >= mindamage.getValue()) //check crystal damage at pos
                .filter(blockPos -> mc.player.squaredDistanceTo(Vec3d.of(blockPos)) < placerange.getValue() * placerange.getValue()) // see if player can actually place at pos
                .filter(blockPos -> isPositionSafeFromEntities(blockPos.up(), potentialcollidingentities)) //check entity intersections
                .filter(this::canPlaceAt); // check if blockpos is next to solid block -> can place

        List<BlockPos> bestPlacePositionsSorted = positionstream
                .sorted((b1, b2) -> AutoSupportPlace.sortObbyDamage(b1, b2, target)) // sort crystal damage
                .collect(Collectors.toList());

        Collections.reverse(bestPlacePositionsSorted); // get high damage pos at start of list

        return bestPlacePositionsSorted;
    }


    public List<BlockPos> getBlockPositions(Block block) {
        List<BlockPos> blockpositions = new ArrayList<>();
        if (target == null) {
            return null;
        }

        int X_Pos = (int) target.getX();
        int Y_Pos = (int) target.getY();
        int Z_Pos = (int) target.getZ();

        double searchRangeValue = searchrange.getValue();
        int searchquared = (int) (searchRangeValue*searchRangeValue);

        int startX = (int) (X_Pos - searchRangeValue);
        int endX = (int) (X_Pos + searchRangeValue);
        int startY = 0;

        if(searchdownwards.getValue()) {
            startY = (int) (Y_Pos - searchRangeValue);
        } else {
            startY = Y_Pos-1;
        }


        int startZ = (int) (Z_Pos - searchRangeValue);
        int endZ = (int) (Z_Pos + searchRangeValue);


        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < Y_Pos; j++) { // dont search up -> no good positions anyways
                for (int k = startZ; k <= endZ; k++) {
                    BlockPos pos = new BlockPos(i,j,k);
                    BlockState state = mc.world.getBlockState(pos);
                    if(state.getBlock() == block && mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < searchquared) {
                        blockpositions.add(pos);
                    }
                }
            }
        }

        return blockpositions;
    }

    public List<Entity> Playerlistsorted() {

        Stream<Entity> entities = Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof PlayerEntity)
                .filter(entity -> !entity.equals(mc.player))
                .filter(entity -> !((PlayerEntity) entity).isDead())
                .filter(entity -> mc.player.distanceTo(entity) <= searchtargetrange.getValue())
                .filter(entity -> !FriendManager.isFriend(((PlayerEntity) entity).getGameProfile().getName()));

        List<Entity> sortedEntities = null;

        if (mode.getValue() == 0) { // Distance
            sortedEntities = entities.sorted(Comparator.comparing(entity -> mc.player.distanceTo(entity))).collect(Collectors.toList());

        } else if (mode.getValue() == 1) { // Health
            sortedEntities = entities.sorted(AutoSupportPlace::sortHealth).collect(Collectors.toList());

        }

        return sortedEntities;
    }


    //skidded
    private static int sortHealth(Entity e1, Entity e2) {
        boolean e1l = e1 instanceof LivingEntity;
        boolean e2l = e2 instanceof LivingEntity;

        if (!e1l && !e2l) return 0;
        else if (e1l && !e2l) return 1;
        else if (!e1l) return -1;

        return Float.compare(((LivingEntity) e1).getHealth(), ((LivingEntity) e2).getHealth());
    }

    private static int sortObbyDamage(BlockPos b1, BlockPos b2, Entity target) {
        return Float.compare(DamageUtils.getExplosionDamage(Vec3d.of(b1.up()), 6f, (LivingEntity) target), DamageUtils.getExplosionDamage(Vec3d.of(b2.up()), 6f, (LivingEntity) target));
    }

    private boolean isPositionSafeFromEntities(BlockPos pos, List<Entity> entities) {
        Box blockBox = new Box(pos);

        for (Entity entity : entities) {
            if (entity.getBoundingBox().intersects(blockBox)) {
                return false;
            }
        }

        return true;
    }

    private boolean canPlaceAt(BlockPos pos) {
        return isSolidBlock(pos.north()) || isSolidBlock(pos.south()) ||
                isSolidBlock(pos.east()) || isSolidBlock(pos.west()) ||
                isSolidBlock(pos.down());
    }

    private Direction determinePlaceDirection(BlockPos placeLocation) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = placeLocation.offset(direction);

            if (isSolidBlock(adjacentPos)) {
                return direction;
            }
        }
        return Direction.DOWN;
    }

    @EventHandler
    public void onWorldRender(EventWorldRender worldRender) {

        worldRender.drawer.startDrawing();

        if(target != null && placem.getValue() == 0) { // only render in continuous mode
            worldRender.drawer.box(target.getBoundingBox(), color.getValue());
        }

        worldRender.drawer.stopDrawing(worldRender.matrices);
    }
}
