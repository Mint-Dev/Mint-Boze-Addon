package me.otter.mintaddon.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import static me.otter.mintaddon.MintAddon.mc;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class HoleUtils {

    public static boolean isinhole(PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        int check = 0;

        BlockPos[] positionsToCheck = {
                playerPos.north(),
                playerPos.south(),
                playerPos.east(),
                playerPos.west(),
        };

        for (BlockPos pos: positionsToCheck) {
            BlockState state = mc.world.getBlockState(pos);
            if(state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.OBSIDIAN) {
                check++;
            }
        }
        return check == 4;
    }
}
