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

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;
import com.griefcraft.scripting.event.LWCProtectionRegistrationPostEvent;
import com.griefcraft.util.Colors;
import java.util.Iterator;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class LWCEntityListener implements Listener {

    /**
     * The plugin instance
     */
    private LWCPlugin plugin;

    private UUID placedArmorStandOrSpawnEggPlayer;

    public LWCEntityListener(LWCPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                onTick();
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    protected void onTick() {
        placedArmorStandOrSpawnEggPlayer = null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityPlace(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getEntity();

        entityCreatedByPlayer(entity, player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getEntity();

        entityCreatedByPlayer(entity, player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.useItemInHand() == Result.DENY) {
            return;
        }
        ItemStack inHand = e.getItem();
        if (inHand != null && (inHand.getType() == Material.ARMOR_STAND || inHand.getType().name().endsWith("_SPAWN_EGG"))) {
            placedArmorStandOrSpawnEggPlayer = e.getPlayer().getUniqueId();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        ItemStack inHand = e.getHand() == EquipmentSlot.OFF_HAND ? e.getPlayer().getInventory().getItemInOffHand() : e.getPlayer().getInventory().getItemInMainHand();
        if (inHand != null && (inHand.getType() == Material.ARMOR_STAND || inHand.getType().name().endsWith("_SPAWN_EGG"))) {
            placedArmorStandOrSpawnEggPlayer = e.getPlayer().getUniqueId();
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (placedArmorStandOrSpawnEggPlayer != null) {
            Player player = plugin.getServer().getPlayer(placedArmorStandOrSpawnEggPlayer);
            Entity entity = e.getEntity();
            placedArmorStandOrSpawnEggPlayer = null;
            if (player != null && !e.isCancelled() && (e.getEntityType() == EntityType.ARMOR_STAND || e.getSpawnReason() == SpawnReason.SPAWNER_EGG)) {
                if (player.getWorld().equals(entity.getWorld()) && player.getLocation().distanceSquared(entity.getLocation()) <= 25) {
                    entityCreatedByPlayer(entity, player);
                }
            }
        }
    }

    private void entityCreatedByPlayer(Entity entity, Player player) {
        if (!LWC.ENABLED || player == null) {
            return;
        }
        LWC lwc = plugin.getLWC();
        if (!lwc.isProtectable(entity)) {
            return;
        }

        String autoRegisterType = lwc.getAutoRegisterType(entity);

        // is it auto protectable?
        if (!autoRegisterType.equalsIgnoreCase("private") && !autoRegisterType.equalsIgnoreCase("public")) {
            return;
        }

        if (!lwc.hasPermission(player, "lwc.create." + autoRegisterType, "lwc.create", "lwc.protect")) {
            return;
        }

        // Parse the type
        Protection.Type type;

        try {
            type = Protection.Type.valueOf(autoRegisterType.toUpperCase());
        } catch (IllegalArgumentException e) {
            // No auto protect type found
            return;
        }

        // Is it okay?
        if (type == null) {
            player.sendMessage(Colors.Red + "LWC_INVALID_CONFIG_autoRegister");
            return;
        }

        if (plugin.getLWC().findProtection(entity) != null) {
            return;
        }

        Protection protection = null;
        try {
            Block entityBlock = EntityBlock.getEntityBlock(entity);

            LWCProtectionRegisterEvent evt = new LWCProtectionRegisterEvent(player, entityBlock);
            lwc.getModuleLoader().dispatchEvent(evt);

            // something cancelled registration
            if (evt.isCancelled()) {
                return;
            }

            // All good!
            protection = lwc.getPhysicalDatabase().registerEntityProtection(entity, type, entity.getWorld().getName(), player.getUniqueId().toString(), "", entityBlock.getX(), entityBlock.getY(), entityBlock.getZ());

            if (!Boolean.parseBoolean(lwc.resolveProtectionConfiguration(entityBlock, "quiet"))) {
                lwc.sendLocale(player, "protection.onplace.create.finalize", "type", lwc.getPlugin().getMessageParser().parseMessage(autoRegisterType.toLowerCase()), "block", LWC.materialToString(entityBlock));
            }

            if (protection != null) {
                lwc.getModuleLoader().dispatchEvent(new LWCProtectionRegistrationPostEvent(protection));
            }
        } catch (Exception e) {
            lwc.logAndPrintInternalException(player, "ENTITY_CREATE", e, protection);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void entityInteract(EntityInteractEvent event) {
        Block block = event.getBlock();

        Protection protection = plugin.getLWC().findProtection(block.getLocation());

        if (protection != null) {
            boolean allowEntityInteract = Boolean.parseBoolean(plugin.getLWC().resolveProtectionConfiguration(block, "allowEntityInteract"));

            if (!allowEntityInteract) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void entityBreakDoor(EntityBreakDoorEvent event) {
        Block block = event.getBlock();

        // See if there is a protection there
        Protection protection = plugin.getLWC().findProtection(block.getLocation());

        if (protection != null) {
            // protections.allowEntityBreakDoor
            boolean allowEntityBreakDoor = Boolean.parseBoolean(plugin.getLWC().resolveProtectionConfiguration(block, "allowEntityBreakDoor"));

            if (!allowEntityBreakDoor) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!LWC.ENABLED) {
            return;
        }
        if (event.getBlock().getType() == event.getBlockData().getMaterial()) {
            return; // allowed to change the block if it does not destroy it
        }
        LWC lwc = plugin.getLWC();
        if ((lwc.findProtection(event.getBlock()) != null)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        plugin.getLWC().updateLoadedLegacyProtection(event.getEntity());
    }
}
