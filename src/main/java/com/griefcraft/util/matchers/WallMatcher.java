/*
 * Copyright 2011 Tyler Blair. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package com.griefcraft.util.matchers;

import com.griefcraft.util.ProtectionFinder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.type.Switch;

/**
 * Matches wall entities
 * TODO fix buttons and levers
 */
public class WallMatcher implements ProtectionFinder.Matcher {

    /**
     * Blocks that can be attached to the wall and be protected.
     * This assumes that the block is DESTROYED if the wall they are attached to is broken.
     */
    public static final Set<Material> PROTECTABLES_WALL = new HashSet<>(List.of(Material.ACACIA_WALL_SIGN, Material.BIRCH_WALL_SIGN,
            Material.DARK_OAK_WALL_SIGN, Material.SPRUCE_WALL_SIGN, Material.JUNGLE_WALL_SIGN, Material.OAK_WALL_SIGN, //
            Material.WHITE_WALL_BANNER, Material.ORANGE_WALL_BANNER, Material.MAGENTA_WALL_BANNER, Material.LIGHT_BLUE_WALL_BANNER, //
            Material.YELLOW_WALL_BANNER, Material.LIME_WALL_BANNER, Material.PINK_WALL_BANNER, Material.GRAY_WALL_BANNER, //
            Material.LIGHT_GRAY_WALL_BANNER, Material.CYAN_WALL_BANNER, Material.PURPLE_WALL_BANNER, Material.BLUE_WALL_BANNER, //
            Material.BROWN_WALL_BANNER, Material.GREEN_WALL_BANNER, Material.RED_WALL_BANNER, Material.BLACK_WALL_BANNER, //
            Material.STONE_BUTTON, Material.LEVER, //
            Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.DARK_OAK_BUTTON,
            Material.ACACIA_BUTTON, Material.WARPED_BUTTON, Material.CRIMSON_BUTTON, Material.MANGROVE_BUTTON, 
            Material.WARPED_WALL_SIGN, Material.CRIMSON_WALL_SIGN, Material.MANGROVE_WALL_SIGN, Material.BAMBOO_BUTTON, Material.BAMBOO_WALL_SIGN));

    /**
     * Possible faces around the base block that protections could be at
     */
    public static final BlockFace[] POSSIBLE_FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

    @Override
    public boolean matches(ProtectionFinder finder) {
        // The block we are working on
        Block block = finder.getBaseBlock().getBlock();

        // Match wall signs to the wall it's attached to
        for (BlockFace blockFace : POSSIBLE_FACES) {
            Block face; // the relative block

            if ((face = block.getRelative(blockFace)) != null) {
                // Try and match it
                Block matched = tryMatchBlock(face, blockFace);

                // We found something ..! Try and load the protection
                if (matched != null) {
                    finder.addBlock(matched);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Try and match a wall block
     *
     * @param block
     * @param matchingFace
     * @return
     */
    private Block tryMatchBlock(Block block, BlockFace matchingFace) {
        // Blocks such as wall signs
        if (PROTECTABLES_WALL.contains(block.getType())) {
            BlockData blockData = block.getBlockData();
            if (!(blockData instanceof Directional)) {
                return null;
            }
            BlockFace existingFace = ((Directional) blockData).getFacing();
            if (blockData instanceof Switch) {
                Switch switcher = (Switch) blockData;
                if (switcher.getAttachedFace() != AttachedFace.WALL) {
                    existingFace = switcher.getAttachedFace() == AttachedFace.FLOOR ? BlockFace.UP : BlockFace.DOWN;
                }
            }
            if (existingFace == matchingFace) {
                return block;
            }
        }
        return null;
    }

}
