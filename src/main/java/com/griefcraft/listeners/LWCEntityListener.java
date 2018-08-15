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

import java.util.Iterator;
import java.util.UUID;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;
import com.griefcraft.scripting.event.LWCProtectionRegistrationPostEvent;
import com.griefcraft.util.Colors;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LWCEntityListener implements Listener {

	/**
	 * The plugin instance
	 */
	private LWCPlugin plugin;
	
	private UUID placedArmorStandPlayer;

	public LWCEntityListener(LWCPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onHangingPlace(HangingPlaceEvent event) {
		Player player = event.getPlayer();
		Entity block = event.getEntity();
		
		entityCreatedByPlayer(block, player);
	}

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent e) {
        ItemStack inHand = e.getItem();
        if (inHand != null && inHand.getType() == Material.ARMOR_STAND) {
            placedArmorStandPlayer = e.getPlayer().getUniqueId();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (placedArmorStandPlayer != null) {
            Player player = plugin.getServer().getPlayer(placedArmorStandPlayer);
            Entity block = e.getEntity();
            placedArmorStandPlayer = null;
            if (player != null && block.getType() == EntityType.ARMOR_STAND) {
                if (player.getWorld().equals(block.getWorld()) && player.getLocation().distanceSquared(block.getLocation()) <= 25) {
                    entityCreatedByPlayer(block, player);
                }
            }
        }
    }

    private void entityCreatedByPlayer(Entity entity, Player player) {
        if (!LWC.ENABLED) {
            return;
        }
        LWC lwc = plugin.getLWC();
        if (!lwc.isProtectable(entity.getType())) {
            return;
        }

        String autoRegisterType = lwc.resolveProtectionConfiguration(
                entity.getType(), "autoRegister");

        // is it auto protectable?
        if (!autoRegisterType.equalsIgnoreCase("private")
                && !autoRegisterType.equalsIgnoreCase("public")) {
            return;
        }

        if (!lwc.hasPermission(player, "lwc.create." + autoRegisterType,
                "lwc.create", "lwc.protect")) {
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

        try {
            Block entityBlock = EntityBlock.getEntityBlock(entity);
                    
            LWCProtectionRegisterEvent evt = new LWCProtectionRegisterEvent(
                    player, entityBlock);
            lwc.getModuleLoader().dispatchEvent(evt);

            // something cancelled registration
            if (evt.isCancelled()) {
                return;
            }

            // All good!
            Protection protection = lwc.getPhysicalDatabase()
                    .registerEntityProtection(entity, type, entity.getWorld().getName(),
                            player.getUniqueId().toString(), "", entityBlock.getX(), entityBlock.getY(), entityBlock.getZ());

            if (!Boolean.parseBoolean(lwc.resolveProtectionConfiguration(
                    entityBlock, "quiet"))) {
                lwc.sendLocale(player, "protection.onplace.create.finalize",
                        "type", lwc.getPlugin().getMessageParser()
                                .parseMessage(autoRegisterType.toLowerCase()),
                        "block", LWC.materialToString(entityBlock));
            }

            if (protection != null) {
                lwc.getModuleLoader().dispatchEvent(
                        new LWCProtectionRegistrationPostEvent(protection));
            }
        } catch (Exception e) {
            lwc.sendLocale(player, "protection.internalerror", "id",
                    "PLAYER_INTERACT");
            e.printStackTrace();
        }
    }

	@EventHandler
	public void entityInteract(EntityInteractEvent event) {
		Block block = event.getBlock();

		Protection protection = plugin.getLWC().findProtection(
				block.getLocation());

		if (protection != null) {
			boolean allowEntityInteract = Boolean.parseBoolean(plugin.getLWC()
					.resolveProtectionConfiguration(block,
							"allowEntityInteract"));

			if (!allowEntityInteract) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void entityBreakDoor(EntityBreakDoorEvent event) {
		Block block = event.getBlock();

		// See if there is a protection there
		Protection protection = plugin.getLWC().findProtection(
				block.getLocation());

		if (protection != null) {
			// protections.allowEntityBreakDoor
			boolean allowEntityBreakDoor = Boolean.parseBoolean(plugin.getLWC()
					.resolveProtectionConfiguration(block,
							"allowEntityBreakDoor"));

			if (!allowEntityBreakDoor) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
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
				boolean ignoreExplosions = Boolean.parseBoolean(lwc
						.resolveProtectionConfiguration(protection.getBlock(),
								"ignoreExplosions"));

				if (!(ignoreExplosions || protection
						.hasFlag(Flag.Type.ALLOWEXPLOSIONS))) {
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
        LWC lwc = plugin.getLWC();
        if ((lwc.findProtection(event.getBlock()) != null)) {
            event.setCancelled(true);
            return;
        }
    }
}
