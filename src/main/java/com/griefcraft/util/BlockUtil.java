package com.griefcraft.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.util.Vector;

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

    public static Block findAdjacentBedPart(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Bed)) {
            return null;
        }

        Bed bedData = (Bed) blockData;
        BlockFace bedFace = bedData.getFacing();
        if (bedData.getPart() == Bed.Part.HEAD) {
            bedFace = bedFace.getOppositeFace();
        }

        Block relativeBlock = block.getRelative(bedFace);
        if (relativeBlock.getType() == block.getType()) {
            return relativeBlock;
        }
        return null;
    }

    public static float getRelativeHitCoordinatesForBlockFace(Vector relativeHitPosition, BlockFace direction) {
        return switch (direction) {
            case NORTH -> (float) (1.0D - relativeHitPosition.getX());
            case SOUTH -> (float) (relativeHitPosition.getX());
            case WEST -> (float) (relativeHitPosition.getZ());
            case EAST -> (float) (1.0D - relativeHitPosition.getZ());
            default -> throw new IllegalArgumentException("direction must be cardinal");
        };
    }
    
    public static BlockFace rotateClockwise(BlockFace direction) {
        return switch (direction) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            case EAST -> BlockFace.SOUTH;
            default -> throw new IllegalArgumentException("direction must be cardinal");
        };
    }
}
