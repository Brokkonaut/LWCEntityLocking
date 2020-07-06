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

import com.google.common.base.Objects;
import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.BlockMap;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.LWCPlayer;
import com.griefcraft.model.Protection;
import com.griefcraft.model.Protection.Type;
import com.griefcraft.scripting.Module;
import com.griefcraft.scripting.event.LWCBlockInteractEvent;
import com.griefcraft.scripting.event.LWCDropItemEvent;
import com.griefcraft.scripting.event.LWCProtectionInteractEvent;
import com.griefcraft.util.UUIDRegistry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Hopper;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;

public class LWCPlayerListener implements Listener {

    /**
     * The plugin instance
     */
    private LWCPlugin plugin;

    public LWCPlayerListener(LWCPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                onTick();
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    protected void onTick() {
        lastHopper = null;
        lastEntityInteract = null;
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent e) {
        Entity entity = e.getEntity();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity)) {
            return;
        }
        Protection protection = lwc.findProtection(entity);
        if (protection == null) {
            return;
        }
        if (e.getCause() != RemoveCause.ENTITY || !(e.getRemover() instanceof Player)) {
            e.setCancelled(true); // never allow non-players to damage protected entities
            return;
        }
        Player p = (Player) e.getRemover();
        // owner permission is required for removing hangings
        if (!lwc.canDestoryProtection(p, protection)) {
            e.setCancelled(true);
            return;
        }
        protection.remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent e) {
        Entity entity = e.getEntity();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity)) {
            return;
        }
        Protection protection = lwc.findProtection(entity);
        if (protection == null) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMinecartBreak(VehicleDestroyEvent e) {
        Entity entity = e.getVehicle();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity)) {
            return;
        }
        Protection protection = lwc.findProtection(entity);
        if (protection == null) {
            return;
        }
        if (!(e.getAttacker() instanceof Player)) {
            e.setCancelled(true); // never allow non-players to damage protected entities
            return;
        }
        Player p = (Player) e.getAttacker();
        // owner permission is required for minecarts
        if (!lwc.canDestoryProtection(p, protection)) {
            e.setCancelled(true);
            return;
        }
        protection.remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event instanceof HangingBreakByEntityEvent) {
            return;
        }
        Entity entity = event.getEntity();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity)) {
            return;
        }
        Protection protection = lwc.findProtection(entity);
        if (protection != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProtectedEntityDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        LWC lwc = LWC.getInstance();
        if (entity instanceof Player || e instanceof EntityDamageByEntityEvent || !lwc.isProtectable(entity)) {
            return;
        }
        Protection protection = lwc.findProtection(entity);
        if (protection != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProtectedEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        LWC lwc = LWC.getInstance();
        if (entity instanceof Player || !lwc.isProtectable(entity)) {
            return;
        }
        Player p = (e.getDamager() instanceof Player) ? (Player) e.getDamager() : null;
        if (p != null && onPlayerEntityInteract(p, entity, e.isCancelled(), true)) {
            e.setCancelled(true);
            return;
        }
        Protection protection = lwc.findProtection(entity);
        if (protection == null) {
            return;
        }
        if (e.getCause() == DamageCause.BLOCK_EXPLOSION || e.getCause() == DamageCause.ENTITY_EXPLOSION || p == null) {
            e.setCancelled(true); // never allow explosions or non-players to damage protected entities
            return;
        }
        // owner permission is required for damaging entities
        if (!lwc.canDestoryProtection(p, protection)) {
            if (e.getCause() == DamageCause.ENTITY_ATTACK && entity instanceof ItemFrame) {
                ItemStack item = ((ItemFrame) entity).getItem();
                if (item != null && item.getType() != Material.AIR) {
                    return; // special case for ItemFrames: they call a damageevent when the contained item is removed.
                    // this is allowed for protection members.
                }
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity)) {
            return;
        }

        Protection protection = lwc.findProtection(entity);
        if (protection != null) {
            protection.remove();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        Entity entity = e.getRightClicked();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity)) {
            return;
        }

        Player p = e.getPlayer();
        if (onPlayerEntityInteract(p, entity, e.isCancelled(), true)) {
            e.setCancelled(true);
        }
        Protection protection = lwc.findProtection(entity);
        if (protection == null) {
            return;
        }
        if (!e.isCancelled() && !lwc.canAccessProtectionContents(p, protection)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        LWC lwc = LWC.getInstance();
        if (entity instanceof Player || !lwc.isProtectable(entity)) {
            return;
        }
        Player p = e.getPlayer();
        if (onPlayerEntityInteract(p, entity, e.isCancelled(), e.getHand() == EquipmentSlot.HAND)) {
            e.setCancelled(true);
            return;
        }
        Protection protection = lwc.findProtection(entity);
        if (protection == null) {
            return;
        }
        if (!lwc.canAccessProtectionContents(p, protection)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAtEntityInteract(PlayerInteractAtEntityEvent e) {
        Entity entity = e.getRightClicked();
        LWC lwc = LWC.getInstance();
        if (entity instanceof Player || !lwc.isProtectable(entity)) {
            return;
        }
        Player p = e.getPlayer();
        if (onPlayerEntityInteract(p, entity, e.isCancelled(), e.getHand() == EquipmentSlot.HAND)) {
            e.setCancelled(true);
        }
        Protection protection = lwc.findProtection(entity);
        if (protection == null) {
            return;
        }
        if (!lwc.canAccessProtectionContents(p, protection)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void storageMinecraftInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if ((!(holder instanceof StorageMinecart)) && (!(holder instanceof HopperMinecart))) {
            return;
        }
        Entity entity = (Entity) holder;
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity)) {
            return;
        }
        if (onPlayerEntityInteract((Player) event.getPlayer(), entity, event.isCancelled(), true)) {
            event.setCancelled(true);
        }
    }

    private UUID lastEntityInteract;
    private boolean lastEntityInteractResult;

    private boolean onPlayerEntityInteract(Player player, Entity entity, boolean cancelled, boolean showMessage) {
        if (entity.getUniqueId().equals(lastEntityInteract)) {
            return lastEntityInteractResult;
        }
        // attempt to load the protection for this cart
        LWC lwc = LWC.getInstance();
        Protection protection = lwc.findProtection(entity);
        LWCPlayer lwcPlayer = lwc.wrapPlayer(player);

        try {
            Set<String> actions = lwcPlayer.getActionNames();
            Module.Result result;
            boolean canAccess = lwc.canAccessProtectionContents(player, protection);

            // Calculate if the player has a pending action (i.e any action
            // besides 'interacted')
            int actionCount = actions.size();
            boolean hasInteracted = actions.contains("interacted");
            boolean hasPendingAction = (hasInteracted && actionCount > 1) || (!hasInteracted && actionCount > 0);

            // If the event was cancelled and they have an action, warn them
            if (cancelled) {
                // only send it if a non-"interacted" action is set which is
                // always set on the player
                if (hasPendingAction) {
                    lwc.sendLocale(player, "lwc.pendingaction");
                }

                // it's cancelled, do not continue !
                return false;
            }

            // register in an action what protection they interacted with (if
            // applicable.)
            if (protection != null) {
                com.griefcraft.model.Action action = new com.griefcraft.model.Action();
                action.setName("interacted");
                action.setPlayer(lwcPlayer);
                action.setProtection(protection);

                lwcPlayer.addAction(action);
            }

            // events are only used when they already have an action pending
            boolean canAdmin = lwc.canAdminProtection(player, protection);
            Block fakeBlock = EntityBlock.getEntityBlock(entity);
            PlayerInteractEvent fakeEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, fakeBlock, null);

            if (protection != null) {
                LWCProtectionInteractEvent evt = new LWCProtectionInteractEvent(fakeEvent, protection, actions, canAccess, canAdmin);
                lwc.getModuleLoader().dispatchEvent(evt);

                result = evt.getResult();
            } else {
                LWCBlockInteractEvent evt = new LWCBlockInteractEvent(fakeEvent, fakeBlock, actions);
                lwc.getModuleLoader().dispatchEvent(evt);

                result = evt.getResult();
            }

            if (result == Module.Result.ALLOW) {
                return false;
            }

            // optional.onlyProtectIfOwnerIsOnline
            if (protection != null && !canAccess && lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOnline", false)) {
                Player owner = protection.getBukkitOwner();

                // If they aren't online, allow them in :P
                if (owner == null || !owner.isOnline()) {
                    return false;
                }
            }

            // optional.onlyProtectIfOwnerIsOffline
            if (protection != null && !canAccess && lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOffline", false)) {
                Player owner = protection.getBukkitOwner();

                // If they aren't online, allow them in :P
                if (owner != null && owner.isOnline()) {
                    return false;
                }
            }

            if (result == Module.Result.DEFAULT) {
                canAccess = lwc.enforceAccess(player, protection, fakeBlock, canAccess, showMessage);
            }

            if (!canAccess || result == Module.Result.CANCEL) {
                lastEntityInteract = entity.getUniqueId();
                lastEntityInteractResult = true;
                return true;
            }
        } catch (Exception e) {
            lwc.sendLocale(player, "protection.internalerror", "id", "PLAYER_INTERACT");
            e.printStackTrace();
            return true;
        }
        lastEntityInteract = entity.getUniqueId();
        lastEntityInteractResult = false;
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUIDRegistry.updateCache(player.getUniqueId(), player.getName());
    }

    private Inventory lastHopper;
    private boolean lastHopperWasSource;
    private boolean lastHopperResult;

    @EventHandler(ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        boolean result;

        // if the initiator is the same as the source it is a dropper i.e.
        // depositing items
        if (event.getInitiator() == event.getSource()) {
            if (Objects.equal(lastHopper, event.getInitiator()) && lastHopperWasSource == true) {
                result = lastHopperResult;
                // plugin.getLogger().info("Hopper == lasthopper");
            } else {
                result = handleMoveItemEvent(event.getInitiator(), event.getDestination());

                lastHopper = event.getInitiator();
                lastHopperWasSource = true;
                lastHopperResult = result;
                // plugin.getLogger().info("Hopper != lasthopper");
            }
        } else {
            if (Objects.equal(lastHopper, event.getInitiator()) && lastHopperWasSource == false) {
                result = lastHopperResult;
                // plugin.getLogger().info("Hopper == lasthopper");
            } else {
                result = handleMoveItemEvent(event.getInitiator(), event.getSource());

                lastHopper = event.getInitiator();
                lastHopperWasSource = false;
                lastHopperResult = result;
                // plugin.getLogger().info("Hopper != lasthopper");
            }
        }

        if (result) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle the item move event
     *
     * @param inventory
     * @return true if the move should be cancelled
     */
    private boolean handleMoveItemEvent(Inventory initiator, Inventory inventory) {
        LWC lwc = LWC.getInstance();

        if (inventory == null) {
            return false;
        }

        Location location = null;
        Entity entityHolder = null;
        InventoryHolder holder;
        Location initiatorLocation = null;
        Entity initiatorEntity = null;
        InventoryHolder initiatorHolder;

        try {
            holder = inventory.getHolder();
            initiatorHolder = initiator.getHolder();
        } catch (AbstractMethodError e) {
            return false;
        }

        try {
            if (holder instanceof BlockState) {
                location = ((BlockState) holder).getLocation();
            } else if (holder instanceof DoubleChest) {
                location = ((DoubleChest) holder).getLocation();
            } else if (holder instanceof Entity && lwc.isProtectable((Entity) holder)) {
                entityHolder = (Entity) holder;
            } else {
                return false;
            }

            if (initiatorHolder instanceof Hopper) {
                initiatorLocation = ((Hopper) initiatorHolder).getLocation();
            } else if (initiatorHolder instanceof Entity && lwc.isProtectable((Entity) initiatorHolder)) {
                initiatorEntity = (Entity) initiatorHolder;
            }
        } catch (Exception e) {
            return false;
        }

        // High-intensity zone: increase protection cache if it's full,
        // otherwise
        // the database will be getting rammed
        lwc.getProtectionCache().increaseIfNecessary();

        // Attempt to load the protection at that location/entity
        Protection protection = entityHolder == null ? lwc.findProtection(location) : lwc.findProtection(entityHolder);

        // If no protection was found we can safely ignore it
        if (protection == null) {
            return false;
        }

        if ((initiatorLocation != null || initiatorEntity != null) && Boolean.parseBoolean(lwc.resolveProtectionConfiguration(Material.HOPPER, "enabled"))) {
            Protection initiatorProtection = initiatorEntity == null ? lwc.findProtection(initiatorLocation) : lwc.findProtection(initiatorEntity);

            if (initiatorProtection != null) {
                // if they're owned by the same person then we can allow the
                // move
                if (protection.getOwner().equals(initiatorProtection.getOwner())) {
                    return false;
                }
            }
        }
        String denyHoppersString;
        if (entityHolder == null) {
            denyHoppersString = lwc.resolveProtectionConfiguration(BlockMap.instance().getMaterial(protection.getBlockId()), "denyHoppers");
        } else {
            denyHoppersString = lwc.resolveProtectionConfiguration(entityHolder.getType(), "denyHoppers");
        }
        boolean denyHoppers = Boolean.parseBoolean(denyHoppersString);

        // xor = (a && !b) || (!a && b)
        if (denyHoppers ^ protection.hasFlag(Flag.Type.HOPPER)) {
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled() || !LWC.ENABLED) {
            return;
        }

        Player player = event.getPlayer();

        LWCDropItemEvent evt = new LWCDropItemEvent(player, event);
        plugin.getLWC().getModuleLoader().dispatchEvent(evt);

        if (evt.isCancelled()) {
            event.setCancelled(true);
        }
    }

    /*
     * @EventHandler public void onPlayerChat(PlayerChatEvent event) { if
     * (event.isCancelled() || !LWC.ENABLED) { return; }
     * 
     * LWC lwc = plugin.getLWC(); if
     * (!lwc.getConfiguration().getBoolean("core.filterunlock", true)) { return;
     * }
     * 
     * // We want to block messages starting with cunlock incase someone screws
     * up /cunlock password. String message = event.getMessage();
     * 
     * if (message.startsWith("cunlock") || message.startsWith("lcunlock") ||
     * message.startsWith(".cunlock")) { event.setCancelled(true);
     * lwc.sendLocale(event.getPlayer(), "lwc.blockedmessage"); } }
     */

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        LWCPlayer lwcPlayer = lwc.wrapPlayer(player);

        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        BlockState state;

        try {
            state = block.getState();
        } catch (NullPointerException e) {
            //
            lwc.log("Invalid Tile Entity detected at " + block.getLocation());
            lwc.log("This is either an issue with your world or a bug in Bukkit");
            return;
        }

        // Prevent players with lwc.deny from interacting with blocks that have
        // an inventory
        if (state instanceof InventoryHolder && lwc.isProtectable(block)) {
            if (!lwc.hasPermission(player, "lwc.protect") && lwc.hasPermission(player, "lwc.deny") && !lwc.isAdmin(player) && !lwc.isMod(player)) {
                lwc.sendLocale(player, "protection.interact.error.blocked");
                event.setCancelled(true);
                return;
            }
        }

        try {
            Set<String> actions = lwcPlayer.getActionNames();
            Protection protection = lwc.findProtection(block.getLocation());
            Module.Result result;
            boolean canAccess = protection == null || lwc.canAccessProtection(player, protection);

            // special case for lecterns: interact might add a book, so we have to check canAccessProtectionContents
            if (protection != null && block.getType() == Material.LECTERN && state instanceof Lectern && event.getItem() != null) {
                if (event.getItem().getType() == Material.WRITTEN_BOOK || event.getItem().getType() == Material.WRITABLE_BOOK) {
                    Lectern lectern = (Lectern) state;
                    ItemStack book = lectern.getInventory().getItem(0);
                    if (book == null || book.getType() == Material.AIR) {
                        canAccess = lwc.canAccessProtectionContents(player, protection) || protection.getType() == Type.DONATION;
                    }
                }
            }

            // Calculate if the player has a pending action (i.e any action
            // besides 'interacted')
            int actionCount = actions.size();
            boolean hasInteracted = actions.contains("interacted");
            boolean hasPendingAction = (hasInteracted && actionCount > 1) || (!hasInteracted && actionCount > 0);

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                boolean ignoreLeftClick = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "ignoreLeftClick"));

                if (ignoreLeftClick) {
                    return;
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                boolean ignoreRightClick = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "ignoreRightClick"));

                if (ignoreRightClick) {
                    return;
                }
            }

            // If the event was cancelled and they have an action, warn them
            if (event.useInteractedBlock() == Result.DENY) {
                // only send it if a non-"interacted" action is set which is
                // always set on the player
                if (hasPendingAction) {
                    lwc.sendLocale(player, "lwc.pendingaction");
                }

                // it's cancelled, do not continue !
                return;
            }

            // register in an action what protection they interacted with (if
            // applicable.)
            if (protection != null) {
                com.griefcraft.model.Action action = new com.griefcraft.model.Action();
                action.setName("interacted");
                action.setPlayer(lwcPlayer);
                action.setProtection(protection);

                lwcPlayer.addAction(action);
            }

            // events are only used when they already have an action pending
            boolean canAdmin = lwc.canAdminProtection(player, protection);

            if (protection != null) {
                LWCProtectionInteractEvent evt = new LWCProtectionInteractEvent(event, protection, actions, canAccess, canAdmin);
                lwc.getModuleLoader().dispatchEvent(evt);

                result = evt.getResult();
            } else {
                LWCBlockInteractEvent evt = new LWCBlockInteractEvent(event, block, actions);
                lwc.getModuleLoader().dispatchEvent(evt);

                result = evt.getResult();
            }

            if (result == Module.Result.ALLOW) {
                return;
            }

            // optional.onlyProtectIfOwnerIsOnline
            if (protection != null && !canAccess && lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOnline", false)) {
                Player owner = protection.getBukkitOwner();

                // If they aren't online, allow them in :P
                if (owner == null || !owner.isOnline()) {
                    return;
                }
            }

            // optional.onlyProtectIfOwnerIsOffline
            if (protection != null && !canAccess && lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOffline", false)) {
                Player owner = protection.getBukkitOwner();

                // If they aren't online, allow them in :P
                if (owner != null && owner.isOnline()) {
                    return;
                }
            }

            if (result == Module.Result.DEFAULT) {
                canAccess = lwc.enforceAccess(player, protection, block, canAccess, event.getHand() == EquipmentSlot.HAND);
            }

            if (!canAccess || result == Module.Result.CANCEL) {
                event.setCancelled(true);
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            }
        } catch (Exception e) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            lwc.sendLocale(player, "protection.internalerror", "id", "PLAYER_INTERACT");
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        // remove the place from the player cache and reset anything they can
        // access
        LWCPlayer.removePlayer(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTakeLecternBook(PlayerTakeLecternBookEvent event) {
        LWC lwc = LWC.getInstance();

        Protection protection = lwc.findProtection(event.getLectern());
        if (protection == null) {
            return;
        }

        boolean canAccess = lwc.canAccessProtectionContents(event.getPlayer(), protection);
        canAccess = lwc.enforceAccess(event.getPlayer(), protection, event.getLectern().getBlock(), canAccess, true);
        if (!canAccess) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        LWC lwc = LWC.getInstance();

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Player interacting with the inventory
        Player player = (Player) event.getWhoClicked();

        // The inventory they are using
        Inventory inventory = event.getInventory();

        if (inventory == null || event.getSlot() < 0) {
            return;
        }

        // Location of the container
        Location location;
        InventoryHolder holder = null;

        try {
            holder = event.getInventory().getHolder();
        } catch (AbstractMethodError e) {
            lwc.log("Caught issue with Bukkit's Inventory.getHolder() method! This is occuring NEAR the player: " + player.getName());
            lwc.log("This player is located at: " + player.getLocation().toString());
            lwc.log("This should be reported to the Bukkit developers.");
            e.printStackTrace();
            return;
        }

        try {
            if (holder instanceof BlockState) {
                location = ((BlockState) holder).getLocation();
            } else if (holder instanceof DoubleChest) {
                location = ((DoubleChest) holder).getLocation();
            } else {
                return;
            }
        } catch (Exception e) {
            Location ploc = player.getLocation();
            String holderName = holder != null ? holder.getClass().getSimpleName() : "Unknown Block";
            lwc.log("Exception with getting the location of a " + holderName + " has occurred NEAR the player: " + player.getName() + " [" + ploc.getBlockX() + " " + ploc.getBlockY() + " " + ploc.getBlockZ() + "]");
            lwc.log("The exact location of the block is not possible to obtain. This is caused by a Minecraft or Bukkit exception normally.");
            e.printStackTrace();
            return;
        }

        // Attempt to load the protection at that location
        Protection protection = lwc.findProtection(location);

        // If no protection was found we can safely ignore it
        if (protection == null) {
            return;
        }

        // If it's not a donation or showcase chest, ignore if
        if (protection.getType() != Protection.Type.DONATION && protection.getType() != Protection.Type.SHOWCASE) {
            return;
        }

        if (protection.getType() != Protection.Type.SHOWCASE && event.getAction() != InventoryAction.COLLECT_TO_CURSOR) {
            // If it's not a container, we don't want it
            if (event.getSlotType() != InventoryType.SlotType.CONTAINER) {
                return;
            }

            // Nifty trick: these will different IFF they are interacting with
            // the player's inventory or hotbar instead of the block's inventory
            if (event.getSlot() != event.getRawSlot()) {
                return;
            }

            // The item they are taking/swapping with
            ItemStack item;

            try {
                item = event.getCurrentItem();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }

            // Item their cursor has
            ItemStack cursor = event.getCursor();

            if (item == null || item.getType() == null || item.getType() == Material.AIR) {
                return;
            }

            // if it's not a right click or a shift click it should be a left
            // click (no shift)
            // this is for when players are INSERTing items (i.e. item in hand
            // and left clicking)
            if (player.getInventory().getItemInMainHand() == null && (!event.isRightClick() && !event.isShiftClick())) {
                return;
            }

            // Are they inserting a stack?
            if (cursor != null && item.isSimilar(cursor)) {
                // If they are clicking an item of the stack type, they are
                // inserting it into the inventory, not switching it
                return;
            }
        }

        // Can they use it? (remove items/etc)
        boolean canAccessContents = lwc.canAccessProtectionContents(player, protection);

        // nope.avi
        if (!canAccessContents) {
            event.setCancelled(true);
        }
    }
}
