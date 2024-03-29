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

package com.griefcraft.modules.doors;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCProtectionInteractEvent;
import com.griefcraft.util.config.Configuration;
import com.griefcraft.util.matchers.DoorMatcher;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;

public class DoorsModule extends JavaModule {

    /**
     * The amount of server ticks there usually are per second
     */
    private final static int TICKS_PER_SECOND = 20;

    /**
     * The course of action for opening doors
     */
    private enum Action {

        /**
         * The door should automatically open and then close
         */
        OPEN_AND_CLOSE,

        /**
         * The doors should just be opened, not closed after a set amount of time
         */
        TOGGLE

    }

    /**
     * The configuration file
     */
    private final Configuration configuration = Configuration.load("doors.yml");

    /**
     * The LWC object, set by load ()
     */
    private LWC lwc;

    /**
     * The current action to use, default to toggling the door open and closed
     */
    private Action action = Action.TOGGLE;

    private HashSet<UUID> hasInteractedThisTick;

    @Override
    public void load(LWC lwc) {
        this.lwc = lwc;
        this.hasInteractedThisTick = new HashSet<>();
        lwc.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(lwc.getPlugin(), new Runnable() {

            @Override
            public void run() {
                if (!hasInteractedThisTick.isEmpty()) {
                    hasInteractedThisTick.clear();
                }
            }
        }, 1, 1);
        loadAction();
    }

    @Override
    public void onProtectionInteract(LWCProtectionInteractEvent event) {
        if (event.getResult() == Result.CANCEL || !isEnabled() || event.getEvent().getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || event.getPlayer().isSneaking()) {
            return;
        }

        // The more important check
        if (!event.canAccess()) {
            return;
        }

        Protection protection = event.getProtection();
        Block block = event.getEvent().getClickedBlock(); // The block they actually clicked :)
        Player player = event.getPlayer();

        if (block instanceof EntityBlock) {
            return;
        }

        // Check if the block is even something that should be opened
        if (!isValid(block.getType())) {
            return;
        }

        // The BOTTOM half of the other side of the double door
        Block doubleDoorBlock = null;

        // special handling for doors
        if (DoorMatcher.PROTECTABLES_DOORS.contains(block.getType())) {
            // Are we looking at the top half?
            // If we are, we need to get the bottom half instead
            if (((Bisected) block.getBlockData()).getHalf() == Half.TOP) {
                // Inspect the bottom half instead, fool!
                block = block.getRelative(BlockFace.DOWN);
                if (!isValid(block.getType())) {
                    return;
                }
            }

            // Should we look for double doors?
            boolean doubleDoors = usingDoubleDoors();

            // Only waste CPU if we need the double door block
            if (doubleDoors) {
                doubleDoorBlock = getDoubleDoor(block);

                if (doubleDoorBlock != null) {
                    Protection other = lwc.findProtection(doubleDoorBlock.getLocation());
                    if (!lwc.canAccessProtection(player, other)) {
                        doubleDoorBlock = null; // don't open the other door :-)
                    }
                }
            }
        }

        if (!hasInteractedThisTick.add(event.getPlayer().getUniqueId())) {
            event.setResult(CANCEL);
            event.getEvent().setCancelled(true);
            return;
        }

        // toggle the other side of the door open
        boolean opensWhenClicked = (DoorMatcher.WOODEN_DOORS.contains(block.getType()) || DoorMatcher.FENCE_GATES.contains(block.getType()) || DoorMatcher.TRAPDOORS.contains(block.getType()));
        changeDoorStates(true, (opensWhenClicked ? null : block) /* opens when clicked */, doubleDoorBlock);
        if (!opensWhenClicked && !event.getPlayer().isSneaking()) {
            event.getEvent().setCancelled(true); // cancel to avoid things like block placing
        }

        if (action == Action.OPEN_AND_CLOSE || protection.hasFlag(Flag.Type.AUTOCLOSE)) {
            // Abuse the fact that we still use final variables inside the task
            // The double door block object is initially only assigned if we need
            // it, so we just create a second variable ^^
            final Block finalBlock = block;
            final Block finalDoubleDoorBlock = doubleDoorBlock;

            // Calculate the wait time
            // This is basically Interval * TICKS_PER_SECOND
            int wait = getAutoCloseInterval() * TICKS_PER_SECOND;

            // Create the task
            // If we are set to close the door after a set period, let's create a sync task for it
            lwc.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(lwc.getPlugin(), new Runnable() {
                @Override
                public void run() {

                    // Essentially all we need to do is reset the door states
                    // But DO NOT open the door if it's closed !
                    changeDoorStates(false, finalBlock, finalDoubleDoorBlock);

                }
            }, wait);
        }

    }

    /**
     * Change all of the given door states to be inverse; that is, if a door is open, it will be closed afterwards.
     * If the door is closed, it will become open.
     * <p/>
     * Note that the blocks given must be the bottom block of the door.
     *
     * @param allowDoorToOpen
     *            If FALSE, and the door is currently CLOSED, it will NOT be opened!
     * @param doors
     *            Blocks given must be the bottom block of the door
     */
    private void changeDoorStates(boolean allowDoorToOpen, Block... doors) {
        for (Block door : doors) {
            if (door == null || !(door.getBlockData() instanceof Openable)) {
                continue;
            }

            Openable doorBlock = (Openable) door.getBlockData();

            boolean wasClosed = !doorBlock.isOpen();
            // If we aren't allowing the door to open, check if it's already closed
            if (!allowDoorToOpen && wasClosed) {
                // The door is already closed and we don't want to open it
                // the bit 0x4 is set when the door is open
                continue;
            }

            // Now xor both data values with 0x4, the flag that states if the door is open
            doorBlock.setOpen(wasClosed);
            door.setBlockData(doorBlock);

            // Play the door open/close sound
            // door.getWorld().playEffect(door.getLocation(), Effect.DOOR_TOGGLE, 0);
            Sound s = null;
            Material type = door.getType();
            if (DoorMatcher.WOODEN_DOORS.contains(type)) {
                s = wasClosed ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE;
            } else if (DoorMatcher.PROTECTABLES_DOORS.contains(type)) {
                s = wasClosed ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
            } else if (DoorMatcher.FENCE_GATES.contains(type)) {
                s = wasClosed ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
            } else if (type == Material.IRON_TRAPDOOR) {
                s = wasClosed ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
            } else if (DoorMatcher.TRAPDOORS.contains(type)) {
                s = wasClosed ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
            }
            if (s != null) {
                door.getWorld().playSound(door.getLocation(), s, SoundCategory.BLOCKS, 1, 1);
            }

            // Open the upper half of the door
            if (DoorMatcher.PROTECTABLES_DOORS.contains(type)) {
                // Get the top half of the door
                Block topHalf = door.getRelative(BlockFace.UP);

                // Only change the block above it if it is something we can open or close
                if (type == topHalf.getType()) {
                    Openable topHalfData = (Openable) topHalf.getBlockData();
                    topHalfData.setOpen(doorBlock.isOpen());
                    topHalf.setBlockData(topHalfData);
                }
            }
        }
    }

    /**
     * Get the double door for the given block
     *
     * @param block
     * @return
     */
    private Block getDoubleDoor(Block block) {
        if (!isValid(block.getType())) {
            return null;
        }

        Block found;

        for (Material material : DoorMatcher.PROTECTABLES_DOORS) {
            if ((found = lwc.findAdjacentBlock(block, material)) != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Check if automatic door opening is enabled
     *
     * @return
     */
    public boolean isEnabled() {
        return configuration.getBoolean("doors.enabled", true);
    }

    /**
     * Check if the material is auto openable/closable
     *
     * @param material
     * @return
     */
    private boolean isValid(Material material) {
        if (DoorMatcher.PROTECTABLES_DOORS.contains(material)) {
            return true;
        }

        else if (DoorMatcher.FENCE_GATES.contains(material)) {
            return true;
        }

        if (DoorMatcher.TRAPDOORS.contains(material) || material == Material.IRON_TRAPDOOR) {
            return true;
        }

        return false;
    }

    /**
     * Get the amount of seconds after opening a door it should be closed
     *
     * @return
     */
    private int getAutoCloseInterval() {
        return configuration.getInt("doors.interval", 3);
    }

    /**
     * Get if we are allowing double doors to be used
     *
     * @return
     */
    private boolean usingDoubleDoors() {
        return configuration.getBoolean("doors.doubleDoors", true);
    }

    /**
     * Load the action from the configuration
     */
    private void loadAction() {
        String strAction = configuration.getString("doors.action");

        if (strAction.equalsIgnoreCase("openAndClose")) {
            this.action = Action.OPEN_AND_CLOSE;
        } else {
            this.action = Action.TOGGLE;
        }
    }

}
