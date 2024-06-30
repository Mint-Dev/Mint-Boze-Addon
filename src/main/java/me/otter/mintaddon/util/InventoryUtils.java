package me.otter.mintaddon.util;

import net.minecraft.item.Item;

import static me.otter.mintaddon.MintAddon.mc;

public class InventoryUtils {

    public static int getItemfromHotbar(Item item) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if(mc.player.getInventory().getStack(i).getItem() == item) {
                slot = i;
            }
        }
        return slot;
    }

}
