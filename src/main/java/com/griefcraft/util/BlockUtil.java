package com.griefcraft.util;

import com.griefcraft.util.matchers.BedMatcher;
import com.griefcraft.util.matchers.DoorMatcher;
import com.griefcraft.util.matchers.GravityMatcher;
import com.griefcraft.util.matchers.WallMatcher;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;

public class BlockUtil {

    /**
     * Look for a double chest adjacent to a chest
     *
     * @param block
     * @return
     */
    public static Block findAdjacentDoubleChest(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Chest chestData)) {
            return null;
        }

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
        if (!(blockData instanceof Bed bedData)) {
            return null;
        }

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

    public static Block findAdjacentDoorHalf(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Door doorData)) {
            return null;
        }

        BlockFace otherHalfFace;
        Half half = doorData.getHalf();
        otherHalfFace = half == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP;

        Block relativeBlock = block.getRelative(otherHalfFace);
        if (relativeBlock.getType() == block.getType()) {
            return relativeBlock;
        }
        return null;
    }

    /**
     * Checks if the block in direction neighbour is also protected if the block original (having originalData) is also protected.
     * 
     * @param original
     *            the block we asume to be protected (this does not have to be true)
     * @param originalData
     *            the data of this block
     * @param the
     *            BlockFace directing from original to neighbour
     * @param neighbour
     *            the neighbours block, for that we have to check if it is protected
     * @return
     */
    public boolean isAdditionalProtectedBlock(Block original, BlockFace neighbourDirection, Block neighbour) {
        Material originalType = original.getType();
        if (originalType == Material.CHEST || originalType == Material.TRAPPED_CHEST) {
            // double chests protect the other half
            if (neighbourDirection.getModY() != 0) {
                return false;
            }

            BlockData blockData = original.getBlockData();
            if (!(blockData instanceof Chest chestData)) {
                return false; // should not be possible
            }

            if (chestData.getType() == Chest.Type.SINGLE) {
                return false;
            }

            BlockFace originalChestFace = chestData.getFacing();
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

            if (chestFace != neighbourDirection) {
                return false;
            }

            Chest.Type expectedNeighbourPart = chestData.getType() == Chest.Type.RIGHT ? Chest.Type.LEFT : Chest.Type.RIGHT;

            BlockData neighbourData = neighbour.getBlockData();
            return (neighbourData.getMaterial() == originalType) && neighbourData instanceof Chest neightbourChestData && neightbourChestData.getType() == expectedNeighbourPart && neightbourChestData.getFacing() == originalChestFace;
        } else if (DoorMatcher.PROTECTABLES_DOORS.contains(originalType)) {
            // doors protect the other half
            if (neighbourDirection.getModY() == 0) {
                return false;
            }

            BlockData blockData = original.getBlockData();
            if (!(blockData instanceof Door doorData)) {
                return false; // should not be possible
            }

            BlockFace otherFace = doorData.getHalf() == Half.BOTTOM ? BlockFace.UP : BlockFace.DOWN;

            if (otherFace != neighbourDirection) {
                return false;
            }

            BlockData neighbourData = neighbour.getBlockData();
            return (neighbourData.getMaterial() == originalType) && neighbourData instanceof Door neightbourDoorData && neightbourDoorData.getHalf() != doorData.getHalf();
        } else if (BedMatcher.BEDS.contains(originalType)) {
            // beds protect the other half
            if (neighbourDirection.getModY() != 0) {
                return false;
            }

            BlockData blockData = original.getBlockData();
            if (!(blockData instanceof Bed bedData)) {
                return false; // should not be possible
            }

            BlockFace originalBedFace = bedData.getFacing();
            BlockFace bedFace = originalBedFace;
            if (bedData.getPart() == Bed.Part.HEAD) {
                bedFace = bedFace.getOppositeFace();
            }

            if (bedFace != neighbourDirection) {
                return false; // the bed does not face where we are looking at
            }

            BlockData neighbourData = neighbour.getBlockData();
            return (neighbourData.getMaterial() == originalType) && neighbourData instanceof Bed neightbourBedData && neightbourBedData.getPart() != bedData.getPart() && neightbourBedData.getFacing() == originalBedFace;
        } else if (WallMatcher.PROTECTABLES_WALL.contains(originalType)) {
            BlockData blockData = original.getBlockData();

            if (blockData instanceof FaceAttachable faceAttachableData) { // lever, button
                if (faceAttachableData.getAttachedFace() == AttachedFace.WALL) {
                    if (blockData instanceof Directional directionalData) {
                        return directionalData.getFacing() == neighbourDirection.getOppositeFace();
                    }
                } else if (faceAttachableData.getAttachedFace() == AttachedFace.FLOOR) {
                    return neighbourDirection == BlockFace.DOWN; // special case, this better belongs to PROTECTABLES_POSTS
                }
            } else if (blockData instanceof Directional directionalData) { // wall_sign, wall_banner
                return directionalData.getFacing() == neighbourDirection.getOppositeFace();
            } else { // ?
                return false; // ??
            }
        } else if (GravityMatcher.PROTECTABLES_POSTS.contains(originalType)) {
            if (neighbourDirection == BlockFace.DOWN) {
                return (neighbour.getBlockData().isFaceSturdy(BlockFace.UP, BlockSupport.CENTER));
            }
        }
        return false;
    }
}
