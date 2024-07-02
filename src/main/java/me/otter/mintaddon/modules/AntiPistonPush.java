package me.otter.mintaddon.modules;

import dev.boze.api.addon.module.ToggleableModule;
import dev.boze.api.event.EventPlayerUpdate;
import dev.boze.api.exception.ModuleNotFoundException;
import dev.boze.api.interaction.Interaction;
import dev.boze.api.interaction.PlaceInteraction;
import dev.boze.api.interaction.Rotation;
import dev.boze.api.interaction.RotationHelper;
import dev.boze.api.module.ModuleHelper;
import dev.boze.api.setting.SettingMode;
import dev.boze.api.setting.SettingToggle;
import me.otter.mintaddon.util.HoleUtils;
import me.otter.mintaddon.util.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import static me.otter.mintaddon.MintAddon.mc;
import net.minecraft.entity.EntityPose;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

import static me.otter.mintaddon.util.BlockUtils.*;

public class AntiPistonPush extends ToggleableModule {

    List modes = new ArrayList();
    private boolean setpacketfly = false;

    final SettingMode mode = new SettingMode(this, "Mode", "In what way to prevent getting pushed.", modes);
    final SettingToggle rotate = new SettingToggle(this, "Rotate", "If you want to rotate for placing.", true);
    final SettingToggle wait = new SettingToggle(this, "Wait for Boze", "Wait if Boze is currently interacting.", false);
    final SettingToggle swing = new SettingToggle(this, "Swing", "If you want to swing when playing.", true);
    final SettingToggle waitpower = new SettingToggle(this, "WaitPistonActivation", "Wait for the piston to recieve restone power.", true);
    final SettingToggle onlyinhole = new SettingToggle(this, "OnlyInHole", "Only prevent pushes, when in 1x1 hole.", true);

    BlockPos placelocation = null;

    public AntiPistonPush() {
        super("AntiPistonPush", "Prevent getting pushed by pistons.");
        modes.add("Block"); //prevent push by placing block behind you
        modes.add("Packetfly"); //prevent push by toggling packetfly -> lazy approach
    }

    Runnable mycallback = new Runnable() {
        @Override
        public void run() {

        }
    };

    @Override
    public void onDisable() {
        if(mode.getValue() == 1 && setpacketfly) {
            try {
                ModuleHelper.setState("PacketFly", false);
            } catch (ModuleNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onUpdate(EventPlayerUpdate update) {

        if(wait.getValue() && update.isBozeInteracting()) {
            return;
        }

        if(!HoleUtils.isinhole(mc.player) && onlyinhole.getValue()) {
            return;
        }

        if(mode.getValue() == 0) { //block
            int bestslot = -1;
            bestslot = InventoryUtils.findItemInHotbar(Items.OBSIDIAN);
            placelocation = null;
            placelocation = getplacelocation();

            if(placelocation == null || bestslot == -1 || !(mc.player.getPose() == EntityPose.STANDING)) {
                return;
            }

            Rotation rotation = null;
            if(rotate.getValue()) {
                rotation = RotationHelper.calculateAngle(placelocation.toCenterPos());
            }

            if(canPlaceAt(placelocation)) {
                Interaction interaction = new PlaceInteraction(mycallback, rotation, placelocation, determinePlaceDirection(placelocation), Hand.MAIN_HAND, swing.getValue(), bestslot);
                update.addInteraction(interaction);
            }
        } else if(mode.getValue() == 1) { //packetfly
            try {
                if(detectpiston() && !(mc.player.getPose() == EntityPose.STANDING)) {
                    ModuleHelper.setState("PacketFly", true);
                    setpacketfly = true;
                } else if(setpacketfly){
                    ModuleHelper.setState("PacketFly", false);
                }
            } catch (ModuleNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public BlockPos getplacelocation() {
        BlockPos playerheadpos = mc.player.getBlockPos().up();

        if(isdangerouspiston(playerheadpos.north(), Direction.SOUTH) && !isSolidBlock(playerheadpos.south())) {
            return playerheadpos.south();
        } else if(isdangerouspiston(playerheadpos.east(), Direction.WEST) && !isSolidBlock(playerheadpos.west())) {
            return playerheadpos.west();
        } else if(isdangerouspiston(playerheadpos.south(), Direction.NORTH) && !isSolidBlock(playerheadpos.north())) {
            return playerheadpos.north();
        } else if(isdangerouspiston(playerheadpos.west(), Direction.EAST) && !isSolidBlock(playerheadpos.east())) {
            return playerheadpos.east();
        }
        return null;
    }

    public boolean detectpiston() {

        if(!(mc.player.getPose() == EntityPose.STANDING)) {
            return false;
        }

        BlockPos playerheadpos = mc.player.getBlockPos().up();
        if(isdangerouspiston(playerheadpos.north(), Direction.SOUTH)
                || isdangerouspiston(playerheadpos.east(), Direction.WEST)
                || isdangerouspiston(playerheadpos.south(), Direction.NORTH)
                || isdangerouspiston(playerheadpos.west(), Direction.EAST)) {
            return true;
        }
        return false;
    }

    public boolean isdangerouspiston(BlockPos pos, Direction direction) {
        BlockState state = mc.world.getBlockState(pos);

        if(state.getBlock() == Blocks.PISTON || state.getBlock() == Blocks.STICKY_PISTON || state.getBlock() == Blocks.MOVING_PISTON) {
            if(state.get(Properties.FACING) == direction) {
                if(waitpower.getValue() && !pistongettingpower(pos)) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPowerSource(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.emitsRedstonePower();
    }

    private boolean pistongettingpower(BlockPos pos) {
        return isPowerSource(pos.north()) || isPowerSource(pos.south()) ||
                isPowerSource(pos.east()) || isPowerSource(pos.west()) ||
                isPowerSource(pos.down());
    }
}