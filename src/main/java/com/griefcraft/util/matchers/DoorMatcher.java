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

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;

/**
 * Matches doors (both Iron & Wooden)
 */
public class DoorMatcher {
    public static final Set<Material> PROTECTABLES_DOORS = EnumSet.of(Material.OAK_DOOR, Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR, Material.JUNGLE_DOOR, Material.SPRUCE_DOOR, Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.MANGROVE_DOOR, Material.IRON_DOOR);
    public static final Set<Material> WOODEN_DOORS = EnumSet.of(Material.OAK_DOOR, Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR, Material.JUNGLE_DOOR, Material.SPRUCE_DOOR, Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.MANGROVE_DOOR); // doors that open when clicked
    public static final Set<Material> PRESSURE_PLATES = EnumSet.of(Material.OAK_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE, Material.DARK_OAK_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE, Material.STONE_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE, Material.WARPED_PRESSURE_PLATE, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, Material.MANGROVE_PRESSURE_PLATE);
    public static final Set<Material> FENCE_GATES = EnumSet.of(Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.JUNGLE_FENCE_GATE, Material.DARK_OAK_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.WARPED_FENCE_GATE, Material.CRIMSON_FENCE_GATE, Material.MANGROVE_FENCE_GATE);
    public static final Set<Material> TRAPDOORS = EnumSet.of(Material.OAK_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.MANGROVE_TRAPDOOR); // , Material.IRON_TRAPDOOR
    public static final Set<Material> REDSTONE_OPENABLE = EnumSet.noneOf(Material.class);
    static {
        REDSTONE_OPENABLE.addAll(PROTECTABLES_DOORS);
        REDSTONE_OPENABLE.addAll(FENCE_GATES);
        REDSTONE_OPENABLE.addAll(TRAPDOORS);
        REDSTONE_OPENABLE.add(Material.IRON_TRAPDOOR);
    }
}
