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

import com.griefcraft.util.BlockUtil;
import com.griefcraft.util.ProtectionFinder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * Matches beds
 */
public class BedMatcher implements ProtectionFinder.Matcher {

    public static final Set<Material> BEDS = Collections.unmodifiableSet(new HashSet<>(List.of(Material.BLACK_BED, Material.BLUE_BED,
            Material.BROWN_BED, Material.CYAN_BED, Material.GRAY_BED, Material.GREEN_BED, Material.LIGHT_BLUE_BED,
            Material.LIGHT_GRAY_BED, Material.LIME_BED, Material.MAGENTA_BED, Material.ORANGE_BED, Material.PINK_BED,
            Material.PURPLE_BED, Material.RED_BED, Material.WHITE_BED, Material.YELLOW_BED)));

    @Override
    public boolean matches(ProtectionFinder finder) {
        BlockState baseBlockState = finder.getBaseBlock();
        Block otherHalf = BlockUtil.findAdjacentBedPart(baseBlockState.getBlock());
        if (otherHalf != null) {
            finder.addBlock(otherHalf);
            return true;
        }
        return false;
    }

}
