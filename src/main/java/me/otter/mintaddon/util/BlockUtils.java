package me.otter.mintaddon.util;

import net.minecraft.block.BlockState;
import static me.otter.mintaddon.MintAddon.mc;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockUtils {

    public static boolean canPlaceAt(BlockPos pos) {
        return isSolidBlock(pos.north()) || isSolidBlock(pos.south()) ||
                isSolidBlock(pos.east()) || isSolidBlock(pos.west()) ||
                isSolidBlock(pos.down());
    }

    //aids af but cba to fix rn
    public static boolean isSolidBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isSolidBlock(mc.world, pos);
    }

    public static Direction determinePlaceDirection(BlockPos placeLocation) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = placeLocation.offset(direction);

            if (isSolidBlock(adjacentPos)) {
                return direction;
            }
        }
        return Direction.DOWN;
    }

}
