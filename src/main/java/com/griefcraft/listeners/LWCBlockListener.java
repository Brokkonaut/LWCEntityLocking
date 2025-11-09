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

package com.griefcraft.listeners;

import com.griefcraft.cache.CacheKey;
import com.griefcraft.cache.ProtectionCache;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.event.LWCProtectionDestroyEvent;
import com.griefcraft.scripting.event.LWCRedstoneEvent;
import com.griefcraft.util.BlockUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class LWCBlockListener implements Listener {

    /**
     * The plugin instance
     */
    private LWCPlugin plugin;

    /**
     * A set of blacklisted blocks
     */
    private final HashSet<Material> blacklistedBlocks = new HashSet<>();

    public LWCBlockListener(LWCPlugin plugin) {
        this.plugin = plugin;
        loadAndProcessConfig();
    }

    public static final BlockFace[] POSSIBLE_FACES = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Block block = event.getBlock();

        if (block == null) {
            return;
        }

        Protection protection = lwc.findProtection(block.getLocation());

        if (protection == null) {
            return;
        }

        LWCRedstoneEvent evt = new LWCRedstoneEvent(event, protection);
        lwc.getModuleLoader().dispatchEvent(evt);

        if (evt.isCancelled()) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = LWC.getInstance();
        // the blocks that were changed / replaced
        List<BlockState> blocks = event.getBlocks();

        for (BlockState newblock : blocks) {
            Block block = newblock.getBlock();
            if (!lwc.isProtectable(block)) {
                continue;
            }

            Protection protection = lwc.findProtection(block);
            if (protection != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block == null) {
            return;
        }

        Protection protection = lwc.findProtection(block.getLocation());

        if (protection == null) {
            return;
        }

        boolean canAccess = lwc.canAccessProtection(player, protection);

        if (!canAccess) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        Block block = event.getBlock();

        boolean ignoreBlockDestruction = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "ignoreBlockDestruction"));

        if (ignoreBlockDestruction) {
            return;
        }

        ProtectionCache cache = lwc.getProtectionCache();
        CacheKey cacheKey = ProtectionCache.cacheKey(block.getLocation());

        // In the event they place a block, remove any known nulls there
        if (cache.isKnownNull(cacheKey)) {
            cache.remove(cacheKey);
        }

        Protection protection = lwc.findProtection(block.getLocation());

        if (protection == null) {
            return;
        }

        boolean canAccess = lwc.canAccessProtection(player, protection);
        boolean canAdmin = lwc.canAdminProtection(player, protection);

        // when destroying a chest, it's possible they are also destroying a
        // double chest
        // in the event they're trying to destroy a double chest, we should just
        // move
        // the protection to the chest that is not destroyed, if it is not that
        // one already.
        if (lwc.canDestoryProtection(player, protection)) {
            Block doubleChest = BlockUtil.findAdjacentDoubleChest(block);

            if (doubleChest != null) {
                // if they destroyed the protected block we want to move it aye?
                protection.setBlockMaterial(doubleChest.getType());
                protection.setX(doubleChest.getX());
                protection.setY(doubleChest.getY());
                protection.setZ(doubleChest.getZ());
                protection.saveNow();

                // Repair the cache
                protection.radiusRemoveCache();

                if (protection.getProtectionFinder() != null) {
                    protection.getProtectionFinder().removeBlock(block.getState());
                }

                lwc.getProtectionCache().addProtection(protection);

                return;
            }
        }

        try {
            LWCProtectionDestroyEvent evt = new LWCProtectionDestroyEvent(player, protection, LWCProtectionDestroyEvent.Method.BLOCK_DESTRUCTION, canAccess, canAdmin);
            lwc.getModuleLoader().dispatchEvent(evt);

            if (evt.isCancelled() || !canAccess) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            event.setCancelled(true);
            lwc.logAndPrintInternalException(player, "BLOCK_BREAK", e, protection);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        LWC lwc = plugin.getLWC();

        for (BlockState state : event.getReplacedBlockStates()) {
            Protection protection = lwc.findProtection(state);

            if (protection != null && !lwc.canAccessProtection(event.getPlayer(), protection)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (!LWC.ENABLED) {
            return;
        }
        LWC lwc = this.plugin.getLWC();

        for (Block moved : event.getBlocks()) {
            if ((lwc.findProtection(moved) != null)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (!LWC.ENABLED) {
            return;
        }
        LWC lwc = plugin.getLWC();
        for (Block moved : event.getBlocks()) {
            if ((lwc.findProtection(moved) != null)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = LWC.getInstance();
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            Protection protection = plugin.getLWC().findProtection(block);

            if (protection != null) {
                boolean ignoreExplosions = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(protection.getBlock(), "ignoreExplosions"));

                if (!(ignoreExplosions || protection.hasFlag(Flag.Type.ALLOWEXPLOSIONS))) {
                    it.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Block block = event.getToBlock();
        Protection protection = plugin.getLWC().findProtection(block);

        if (protection != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Block block = event.getBlock();
        Protection protection = plugin.getLWC().findProtection(block);

        if (protection != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Block block = event.getBlock();
        Protection protection = plugin.getLWC().findProtection(block);

        if (protection != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Block block = event.getBlock();
        Protection protection = plugin.getLWC().findProtection(block);

        if (protection != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location location = block.getLocation();

        ProtectionCache cache = lwc.getProtectionCache();
        CacheKey cacheKey = ProtectionCache.cacheKey(location);

        if (lwc.isProtectable(event.getBlockReplacedState())) {
            Protection existingProtection = lwc.findProtection(block);

            // special case: placing a book in a lectern is also a "block place", this is checked in the PlayerInteract event handler
            if (existingProtection != null && block.getType() == Material.LECTERN && event.getBlockReplacedState().getType() == Material.LECTERN) {
                return;
            }

            if (!lwc.canAdminProtection(player, existingProtection)) {
                // they can't access the protection ..
                event.setCancelled(true);
                return;
            }
        } else {
            // remove stale protection
            Protection existingProtection = lwc.getPhysicalDatabase().loadProtection(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
            if (existingProtection != null) {
                existingProtection.remove();
            }
        }

        // In the event they place a block, remove any known nulls there
        if (cache.isKnownNull(cacheKey)) {
            cache.remove(cacheKey);
        }

        // check if the block is blacklisted
        boolean blockIsBlacklisted = blacklistedBlocks.contains(block.getType());

        if (blockIsBlacklisted) {
            // it's blacklisted, check for a protected chest
            for (Protection protection : lwc.findAdjacentProtectionsOnAllSides(block)) {
                if (protection != null) {
                    if (!lwc.canAccessProtection(player, protection) || ((protection.getType() == Protection.Type.DONATION || protection.getType() == Protection.Type.SHOWCASE) && !lwc.canAdminProtection(player, protection))) {
                        // they can't access the protection ..
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Used for auto registering placed protections
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlaceMonitor(BlockPlaceEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        lwc.tryProtectPlacedBlockForPlayer(player, block);
    }


    /**
     * Load and process the configuration
     */
    public void loadAndProcessConfig() {
        List<String> ids = LWC.getInstance().getConfiguration().getStringList("optional.blacklistedBlocks", new ArrayList<String>());

        for (String sId : ids) {
            Material mat = Material.matchMaterial(sId);
            if (mat == null) {
                plugin.getLogger().log(Level.SEVERE, "Invalid block name in optional.blacklistedBlocks: " + sId);
            } else {
                blacklistedBlocks.add(mat);
            }
        }
    }
}
