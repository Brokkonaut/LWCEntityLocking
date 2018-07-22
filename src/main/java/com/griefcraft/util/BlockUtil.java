package com.griefcraft.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;

public class BlockUtil {

    /**
     * Look for a double chest adjacent to a chest
     *
     * @param block
     * @return
     */
    public static Block findAdjacentDoubleChest(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Chest)) {
            return null;
        }
        
        Chest chestData = (Chest) blockData;
        if (chestData.getType() != Chest.Type.SINGLE) {
            BlockFace chestFace = chestData.getFacing();
            // we have to rotate is to get the adjacent chest
            // west, right -> south
            // west, left -> north
            if (chestFace == BlockFace.WEST) {
                chestFace = BlockFace.NORTH;
            } else if (chestFace == BlockFace.NORTH) {
                chestFace = BlockFace.EAST;
            } else if (chestFace == BlockFace.EAST) {
                chestFace = BlockFace.SOUTH;
            } else if (chestFace == BlockFace.SOUTH) {
                chestFace = BlockFace.WEST;
            }
            if (chestData.getType() == Chest.Type.RIGHT) {
                chestFace = chestFace.getOppositeFace();
            }

            Block face = block.getRelative(chestFace);

            // They're placing it beside a chest, check if it's already
            // protected
            if (face.getType() == block.getType()) {
                return face;
            }
        }

        return null;
    }
}
