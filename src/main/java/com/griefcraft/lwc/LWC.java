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

package com.griefcraft.lwc;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.cache.CacheKey;
import com.griefcraft.cache.ProtectionCache;
import com.griefcraft.integration.ICurrency;
import com.griefcraft.integration.IPermissions;
import com.griefcraft.integration.currency.NoCurrency;
import com.griefcraft.integration.currency.VaultCurrency;
import com.griefcraft.integration.permissions.SuperPermsPermissions;
import com.griefcraft.integration.permissions.VaultPermissions;
import com.griefcraft.migration.ConfigPost300;
import com.griefcraft.migration.MySQLPost200;
import com.griefcraft.model.LWCPlayer;
import com.griefcraft.model.Permission;
import com.griefcraft.model.Protection;
import com.griefcraft.model.Protection.Type;
import com.griefcraft.modules.admin.AdminCache;
import com.griefcraft.modules.admin.AdminCleanup;
import com.griefcraft.modules.admin.AdminClear;
import com.griefcraft.modules.admin.AdminDump;
import com.griefcraft.modules.admin.AdminExpire;
import com.griefcraft.modules.admin.AdminFind;
import com.griefcraft.modules.admin.AdminForceOwner;
import com.griefcraft.modules.admin.AdminLocale;
import com.griefcraft.modules.admin.AdminPurge;
import com.griefcraft.modules.admin.AdminPurgeBanned;
import com.griefcraft.modules.admin.AdminRebuild;
import com.griefcraft.modules.admin.AdminReload;
import com.griefcraft.modules.admin.AdminRemove;
import com.griefcraft.modules.admin.AdminReport;
import com.griefcraft.modules.admin.AdminTransfer;
import com.griefcraft.modules.admin.AdminVersion;
import com.griefcraft.modules.admin.AdminView;
import com.griefcraft.modules.admin.BaseAdminModule;
import com.griefcraft.modules.confirm.ConfirmModule;
import com.griefcraft.modules.create.CreateModule;
import com.griefcraft.modules.credits.CreditsModule;
import com.griefcraft.modules.debug.DebugModule;
import com.griefcraft.modules.destroy.DestroyModule;
import com.griefcraft.modules.doors.DoorsModule;
import com.griefcraft.modules.flag.BaseFlagModule;
import com.griefcraft.modules.free.FreeModule;
import com.griefcraft.modules.history.HistoryModule;
import com.griefcraft.modules.info.InfoModule;
import com.griefcraft.modules.limits.LimitsModule;
import com.griefcraft.modules.limits.LimitsV2;
import com.griefcraft.modules.modes.BaseModeModule;
import com.griefcraft.modules.modes.NoSpamModule;
import com.griefcraft.modules.modes.PersistModule;
import com.griefcraft.modules.modify.ModifyModule;
import com.griefcraft.modules.owners.OwnersModule;
import com.griefcraft.modules.pluginsupport.WorldGuard;
import com.griefcraft.modules.redstone.RedstoneModule;
import com.griefcraft.modules.setup.BaseSetupModule;
import com.griefcraft.modules.setup.DatabaseSetupModule;
import com.griefcraft.modules.setup.LimitsSetup;
import com.griefcraft.modules.unlock.UnlockModule;
import com.griefcraft.scripting.Module;
import com.griefcraft.scripting.ModuleLoader;
import com.griefcraft.scripting.event.LWCAccessEvent;
import com.griefcraft.scripting.event.LWCReloadEvent;
import com.griefcraft.scripting.event.LWCSendLocaleEvent;
import com.griefcraft.sql.Database;
import com.griefcraft.sql.PhysDB;
import com.griefcraft.util.BlockUtil;
import com.griefcraft.util.Colors;
import com.griefcraft.util.ProtectionFinder;
import com.griefcraft.util.Statistics;
import com.griefcraft.util.StringUtil;
import com.griefcraft.util.UUIDRegistry;
import com.griefcraft.util.config.Configuration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

public class LWC {

    /**
     * If LWC is currently enabled
     */
    public static boolean ENABLED = false;

    /**
     * The current instance of LWC
     */
    private static LWC instance;

    /**
     * Core LWC configuration
     */
    private Configuration configuration;

    /**
     * The module loader
     */
    private final ModuleLoader moduleLoader;

    /**
     * The protection cache
     */
    private final ProtectionCache protectionCache;

    /**
     * Physical database instance
     */
    private PhysDB physicalDatabase;

    /**
     * Plugin instance
     */
    private LWCPlugin plugin;

    /**
     * The permissions handler
     */
    private IPermissions permissions;

    /**
     * The currency handler
     */
    private ICurrency currency;

    private HashSet<EntityType> protectableEntites = new HashSet<>();

    private HashSet<Material> protectableBlocks = new HashSet<>();

    /**
     * Protection configuration cache
     */
    private final Map<String, String> protectionConfigurationCache = new HashMap<>();

    public LWC(LWCPlugin plugin) {
        this.plugin = plugin;
        LWC.instance = this;
        configuration = Configuration.load("core.yml");
        protectionCache = new ProtectionCache(this);
        moduleLoader = new ModuleLoader(this);
    }

    /**
     * Get the currently loaded LWC instance
     *
     * @return
     */
    public static LWC getInstance() {
        return instance;
    }

    /**
     * Get a string representation of a block material
     *
     * @param material
     * @return
     */
    public static String materialToString(Material material) {
        if (material != null) {
            String materialName = normalizeMaterialName(material);

            // attempt to match the locale
            String locale = LWC.getInstance().getPlugin().getMessageParser()
                    .parseMessage(materialName.toLowerCase());

            // if it starts with UNKNOWN_LOCALE, use the default material name
            if (locale == null) {
                locale = materialName;
            }

            return StringUtil.capitalizeFirstLetter(StringUtil.fastReplace(locale, '_', ' '));
        }

        return "";
    }

    /**
     * Normalize a name to a more readable & usable form.
     * <p/>
     * E.g sign/wall_sign = Sign
     *
     * @param material
     * @return
     */
    public static String normalizeMaterialName(Material material) {
        String name = material.toString().toLowerCase();

        // some name normalizations
        if (name.contains("sign")) {
            name = "sign";
        }

        return name.toLowerCase();
    }

    /**
     * Check if a player has the ability to access a protection
     *
     * @param player
     * @param block
     * @return
     */
    public boolean canAccessProtection(Player player, Block block) {
        Protection protection = findProtection(block.getLocation());

        return protection != null && canAccessProtection(player, protection);
    }

    /**
     * Check if a player has the ability to access a protection
     *
     * @param player
     * @param x
     * @param y
     * @param z
     * @return
     */
    public boolean canAccessProtection(Player player, int x, int y, int z) {
        return canAccessProtection(player, physicalDatabase.loadProtection(
                player.getWorld().getName(), x, y, z));
    }

    /**
     * Check if a player has the ability to administrate a protection
     *
     * @param player
     * @param block
     * @return
     */
    public boolean canAdminProtection(Player player, Block block) {
        Protection protection = findProtection(block.getLocation());

        return protection != null && canAdminProtection(player, protection);
    }

    /**
     * Check if a player has the ability to administrate a protection
     *
     * @param player
     * @param protection
     * @return
     */
    public boolean canAdminProtection(Player player, Protection protection) {
        if (protection == null || player == null) {
            return true;
        }

        if (isAdmin(player)) {
            return true;
        }

        // Their access level
        Permission.Access access = Permission.Access.NONE;

        switch (protection.getType()) {
            case PUBLIC:
            case PASSWORD:
            case PRIVATE:
            case DONATION:
            case SHOWCASE:
                if (protection.isOwner(player)) {
                    return true;
                }

                if (protection.getAccess(player.getUniqueId().toString(),
                        Permission.Type.PLAYER) == Permission.Access.ADMIN) {
                    return true;
                }

                if (protection.getAccess(player.getName(), Permission.Type.PLAYER) == Permission.Access.ADMIN) {
                    return true;
                }

                for (String groupName : permissions.getGroups(player)) {
                    if (protection.getAccess(groupName, Permission.Type.GROUP) == Permission.Access.ADMIN) {
                        return true;
                    }
                }

                break;
            default:
                break;
        }

        // call the canAccessProtection hook
        LWCAccessEvent event = new LWCAccessEvent(player, protection, access);
        moduleLoader.dispatchEvent(event);

        return event.getAccess() == Permission.Access.ADMIN;
    }

    /**
     * Check if a player has the ability to destroy a protection
     *
     * @param player
     * @param protection
     * @return
     */
    public boolean canDestoryProtection(Player player, Protection protection) {
        if (protection.isOwner(player)) {
            return true;
        }
        if (canAdminProtection(player, protection) && getConfiguration().getBoolean("optional.protectionAdminCanDestroyProtections", false)) {
            return true;
        }
        return false;
    }

    /**
     * Find a block that is adjacent to another block given a Material
     *
     * @param block
     * @param material
     * @param ignore
     * @return
     */
    public Block findAdjacentBlock(Block block, Material material,
            Block... ignore) {
        BlockFace[] faces = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST };
        List<Block> ignoreList = Arrays.asList(ignore);

        for (BlockFace face : faces) {
            Block adjacentBlock = block.getRelative(face);

            if (adjacentBlock.getType() == material
                    && !ignoreList.contains(adjacentBlock)) {
                return adjacentBlock;
            }
        }

        return null;
    }

    /**
     * Find a block that is adjacent to another block on any of the block's 6
     * sides given a Material
     *
     * @param block
     * @param material
     * @param ignore
     * @return
     */
    public Block findAdjacentBlockOnAllSides(Block block, Material material,
            Block... ignore) {
        BlockFace[] faces = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };
        List<Block> ignoreList = Arrays.asList(ignore);

        for (BlockFace face : faces) {
            Block adjacentBlock = block.getRelative(face);

            if (adjacentBlock.getType() == material
                    && !ignoreList.contains(adjacentBlock)) {
                return adjacentBlock;
            }
        }

        return null;
    }

    /**
     * Find a protection that is adjacent to another block on any of the block's
     * 6 sides
     *
     * @param block
     * @param ignore
     * @return
     */
    public List<Protection> findAdjacentProtectionsOnAllSides(Block block,
            Block... ignore) {
        BlockFace[] faces = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };
        List<Block> ignoreList = Arrays.asList(ignore);
        List<Protection> found = new ArrayList<>();

        for (BlockFace face : faces) {
            Protection protection;
            Block adjacentBlock = block.getRelative(face);

            if (!ignoreList.contains(adjacentBlock)
                    && (protection = findProtection(adjacentBlock.getLocation())) != null) {
                found.add(protection);
            }
        }

        return found;
    }

    /**
     * Free some memory (LWC was disabled)
     */
    public void destruct() {
        // destroy the modules
        moduleLoader.shutdown();

        if (physicalDatabase != null) {
            physicalDatabase.dispose();
        }

        physicalDatabase = null;
    }

    /**
     * Log a string
     *
     * @param str
     */
    public void log(String str) {
        plugin.getLogger().info(str);
    }

    /**
     * Encrypt a string using SHA1
     *
     * @param text
     * @return
     */
    public String encrypt(String text) {
        return StringUtil.encrypt(text);
    }

    /**
     * Enforce access to a protected block
     *
     * @param player
     * @param protection
     * @param block
     * @return true if the player was granted access
     */
    public boolean enforceAccess(Player player, Protection protection,
            Block block, boolean hasAccess, boolean showMessage) {
        MessageParser parser = plugin.getMessageParser();

        if (block == null || protection == null) {
            return true;
        }

        // support for old protection dbs that do not contain the block id
        if (!protection.isEntity() && (protection.getBlockId() <= 0 || block.getType() != protection.getBlockMaterial())) {
            protection.setBlockMaterial(block.getType());
            protection.save();
        }

        // multi-world, update old protections
        if ((protection.getWorld() == null || !block.getWorld()
                .getName().equals(protection.getWorld()))) {
            protection.setWorld(block.getWorld().getName());
            protection.save();
        }
        boolean messageInActionBar = protection.getType() == Type.PUBLIC;
        // update timestamp
        if (hasAccess) {
            long timestamp = System.currentTimeMillis() / 1000L;

            // check that they aren't an admin and if they are, they need to be
            // the owner of the protection or have access through /cmodify
            if (protection.isRealOwner(player)
                    || protection.getAccess(player.getUniqueId().toString(),
                            Permission.Type.PLAYER) != Permission.Access.NONE) {
                if (Math.abs(protection.getLastAccessed() - timestamp) > 5) {
                    protection.setLastAccessed(timestamp);
                    protection.saveLastAccessed();
                }
                messageInActionBar = true;
            }
        }

        boolean permShowNotices = hasPermission(player, "lwc.shownotices");
        boolean messageSent = false;
        if ((permShowNotices && configuration.getBoolean("core.showNotices",
                true))
                && !Boolean.parseBoolean(resolveProtectionConfiguration(block,
                        "quiet"))) {
            boolean isOwner = protection.isOwner(player);
            boolean showMyNotices = configuration.getBoolean(
                    "core.showMyNotices", true);

            if (!isOwner || (isOwner && (showMyNotices || permShowNotices))) {
                String owner;

                // replace your username with "you" if you own the protection
                if (protection.isRealOwner(player)) {
                    owner = parser.parseMessage("you");
                } else {
                    owner = protection.getOwnerName();
                }

                String blockName = materialToString(block);
                String protectionTypeToString = parser.parseMessage(protection
                        .typeToString().toLowerCase());

                if (protectionTypeToString == null) {
                    protectionTypeToString = "Unknown";
                }

                if (showMessage) {
                    if (parser.parseMessage("protection." + blockName.toLowerCase() + ".notice.protected") != null) {
                        if (messageInActionBar) {
                            sendLocaleToActionBar(player, "protection." + blockName.toLowerCase() + ".notice.protected", "type", protectionTypeToString, "block", blockName, "owner", owner);
                        } else {
                            sendLocale(player, "protection." + blockName.toLowerCase() + ".notice.protected", "type", protectionTypeToString, "block", blockName, "owner", owner);
                        }
                    } else {
                        if (messageInActionBar) {
                            sendLocaleToActionBar(player, "protection.general.notice.protected", "type", protectionTypeToString, "block", blockName, "owner", owner);
                        } else {
                            sendLocale(player, "protection.general.notice.protected", "type", protectionTypeToString, "block", blockName, "owner", owner);
                        }
                    }
                    messageSent = true;
                }
            }
        }

        if (!hasAccess && showMessage) {
            Protection.Type type = protection.getType();

            if (type == Protection.Type.PASSWORD) {
                sendLocale(player, "protection.general.locked.password",
                        "block", materialToString(block), "owner",
                        protection.getOwnerName());
            } else if ((type == Protection.Type.PRIVATE
                    || type == Protection.Type.DONATION
                    || type == Protection.Type.SHOWCASE) && !messageSent) {
                sendLocale(player, "protection.general.locked.private",
                        "block", materialToString(block), "owner",
                        protection.getOwnerName());
            }
        }

        return hasAccess;
    }

    /**
     * Check if a player has the ability to access a protection
     *
     * @param player
     * @param protection
     * @return
     */
    public boolean canAccessProtection(Player player, Protection protection) {
        if (protection == null || player == null) {
            return true;
        }

        if (isAdmin(player)) {
            return true;
        }

        if (isMod(player)) {
            return true;
        }

        // Their access level
        Permission.Access access = Permission.Access.NONE;

        switch (protection.getType()) {
            case PUBLIC:
            case DONATION:
            case SHOWCASE:
                return true;

            case PASSWORD:
                if (wrapPlayer(player).isProtectionAccessible(protection)) {
                    return true;
                }
                // fallthrough intended!
            case PRIVATE:
                if (protection.isOwner(player)) {
                    return true;
                }

                if (protection.getAccess(player.getUniqueId().toString(),
                        Permission.Type.PLAYER).ordinal() >= Permission.Access.PLAYER
                                .ordinal()) {
                    return true;
                }

                if (protection.getAccess(player.getName(), Permission.Type.PLAYER)
                        .ordinal() >= Permission.Access.PLAYER.ordinal()) {
                    return true;
                }

                // Check for item keys
                for (Permission permission : protection.getPermissions()) {
                    if (permission.getType() != Permission.Type.ITEM) {
                        continue;
                    }

                    // Get the item they need to have

                    // Are they wielding it?
                    if (player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInMainHand().getType().name().equals(permission.getName())) {
                        return true;
                    }
                }

                for (String groupName : permissions.getGroups(player)) {
                    if (protection.getAccess(groupName, Permission.Type.GROUP)
                            .ordinal() >= Permission.Access.PLAYER.ordinal()) {
                        return true;
                    }
                }

                break;
            default:
                break;
        }

        // call the canAccessProtection hook
        LWCAccessEvent event = new LWCAccessEvent(player, protection, access);
        moduleLoader.dispatchEvent(event);

        return event.getAccess() == Permission.Access.PLAYER
                || event.getAccess() == Permission.Access.ADMIN;
    }

    /**
     * Check if a player has the ability to access a protections contents (take items)
     *
     * @param player
     * @param protection
     * @return
     */
    public boolean canAccessProtectionContents(Player player, Protection protection) {
        if (protection == null || player == null) {
            return true;
        }

        if (isAdmin(player)) {
            return true;
        }

        if (isMod(player)) {
            return true;
        }

        // Their access level
        Permission.Access access = Permission.Access.NONE;

        switch (protection.getType()) {
            case PUBLIC:
                return true;

            case PASSWORD:
                if (wrapPlayer(player).isProtectionAccessible(protection)) {
                    return true;
                }
                // fallthrough intended!
            case PRIVATE:
            case DONATION:
            case SHOWCASE:
                if (protection.isOwner(player)) {
                    return true;
                }

                if (protection.getAccess(player.getUniqueId().toString(),
                        Permission.Type.PLAYER).ordinal() >= Permission.Access.PLAYER
                                .ordinal()) {
                    return true;
                }

                if (protection.getAccess(player.getName(), Permission.Type.PLAYER)
                        .ordinal() >= Permission.Access.PLAYER.ordinal()) {
                    return true;
                }

                // Check for item keys
                for (Permission permission : protection.getPermissions()) {
                    if (permission.getType() != Permission.Type.ITEM) {
                        continue;
                    }

                    // Get the item they need to have

                    // Are they wielding it?
                    if (player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInMainHand().getType().name().equals(permission.getName())) {
                        return true;
                    }
                }

                for (String groupName : permissions.getGroups(player)) {
                    if (protection.getAccess(groupName, Permission.Type.GROUP)
                            .ordinal() >= Permission.Access.PLAYER.ordinal()) {
                        return true;
                    }
                }

                break;
            default:
                break;
        }

        // call the canAccessProtection hook
        LWCAccessEvent event = new LWCAccessEvent(player, protection, access);
        moduleLoader.dispatchEvent(event);

        return event.getAccess() == Permission.Access.PLAYER
                || event.getAccess() == Permission.Access.ADMIN;
    }

    /**
     * Check if a player can do mod functions on LWC
     *
     * @param player
     *            the player to check
     * @return true if the player is an LWC mod
     */
    public boolean isMod(Player player) {
        return hasPermission(player, "lwc.mod");
    }

    /**
     * Check if a player can do admin functions on LWC
     *
     * @param player
     *            the player to check
     * @return true if the player is an LWC admin
     */
    public boolean isAdmin(Player player) {
        if (player.isOp()) {
            if (configuration.getBoolean("core.opIsLWCAdmin", true)) {
                return true;
            }
        }

        return hasPermission(player, "lwc.admin");
    }

    /**
     * Check if a player has a permissions node
     *
     * @param player
     * @param node
     * @return
     */
    public boolean hasPermission(Player player, String node) {
        try {
            return player.hasPermission(node);
        } catch (NoSuchMethodError e) {
            // their server does not support Superperms..
            return !node.contains("admin") && !node.contains("mod");
        }
    }

    /**
     * Create an LWCPlayer object for a player
     *
     * @param sender
     * @return
     */
    public LWCPlayer wrapPlayer(CommandSender sender) {
        if (sender instanceof LWCPlayer) {
            return (LWCPlayer) sender;
        }

        if (!(sender instanceof Player)) {
            return null;
        }

        return LWCPlayer.getPlayer((Player) sender);
    }

    /**
     * Find a player in the given ranges
     *
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     * @param minZ
     * @param maxZ
     * @return
     */
    public Player findPlayer(World world, int minX, int maxX, int minY, int maxY, int minZ,
            int maxZ) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location location = player.getLocation();
            int plrX = location.getBlockX();
            int plrY = location.getBlockY();
            int plrZ = location.getBlockZ();

            // simple check of the ranges
            if (location.getWorld() == world && plrX >= minX && plrX <= maxX && plrY >= plrY && plrY <= maxY
                    && plrZ >= minZ && plrZ <= maxZ) {
                return player;
            }
        }

        return null;
    }

    /**
     * Send a locale to a player or console
     *
     * @param sender
     * @param key
     * @param args
     */
    public void sendLocaleToActionBar(CommandSender sender, String key, Object... args) {
        String[] message; // The message to send to the player
        MessageParser parser = plugin.getMessageParser();
        String parsed = parser.parseMessage(key, args);

        if (parsed == null) {
            return; // Nothing to send
        }

        // message = parsed.split("\\n");
        message = StringUtils.split(parsed, '\n');

        // broadcast an event if they are a player
        if (sender instanceof Player) {
            LWCSendLocaleEvent evt = new LWCSendLocaleEvent((Player) sender,
                    key);
            moduleLoader.dispatchEvent(evt);

            // did they cancel it?
            if (evt.isCancelled()) {
                return;
            }
        }

        if (message == null) {
            sender.sendMessage(Colors.Red + "LWC: " + Colors.White
                    + "Undefined locale: \"" + Colors.Gray + key + Colors.White
                    + "\"");
            return;
        }

        if (message.length > 0 && message[0].equalsIgnoreCase("null")) {
            return;
        }

        // Send the message!
        // sender.sendMessage(message);
        boolean firstLine = true;
        for (String line : message) {
            if (firstLine && (sender instanceof Player)) {
                ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(line));
            } else {
                sender.sendMessage(line);
            }
            firstLine = false;
        }
    }

    /**
     * Send a locale to a player or console
     *
     * @param sender
     * @param key
     * @param args
     */
    public void sendLocale(CommandSender sender, String key, Object... args) {
        String[] message; // The message to send to the player
        MessageParser parser = plugin.getMessageParser();
        String parsed = parser.parseMessage(key, args);

        if (parsed == null) {
            return; // Nothing to send
        }

        // message = parsed.split("\\n");
        message = StringUtils.split(parsed, '\n');

        // broadcast an event if they are a player
        if (sender instanceof Player) {
            LWCSendLocaleEvent evt = new LWCSendLocaleEvent((Player) sender,
                    key);
            moduleLoader.dispatchEvent(evt);

            // did they cancel it?
            if (evt.isCancelled()) {
                return;
            }
        }

        if (message == null) {
            sender.sendMessage(Colors.Red + "LWC: " + Colors.White
                    + "Undefined locale: \"" + Colors.Gray + key + Colors.White
                    + "\"");
            return;
        }

        if (message.length > 0 && message[0].equalsIgnoreCase("null")) {
            return;
        }

        // Send the message!
        // sender.sendMessage(message);
        for (String line : message) {
            sender.sendMessage(line);
        }
    }

    /**
     * Get a string representation of a block's material
     *
     * @param block
     * @return
     */
    public static String materialToString(Block block) {
        if (block instanceof EntityBlock) {
            String name = ((EntityBlock) block).getEntity().getType().name();
            return StringUtil.capitalizeFirstLetter(StringUtil.fastReplace(name, '_', ' '));
        }
        return materialToString(block.getType());
    }

    /**
     * Fast remove all protections for a player. ~100k protections / second.
     *
     * @param sender
     * @param player
     * @param shouldRemoveBlocks
     * @return
     */
    public int fastRemoveProtectionsByPlayer(CommandSender sender,
            String player, boolean shouldRemoveBlocks) {
        UUID uuid = UUIDRegistry.getUUID(player);
        int ret = fastRemoveProtections(sender, "Lower(owner) = Lower('"
                + (uuid != null ? uuid.toString() : player) + "')",
                shouldRemoveBlocks);

        // invalid any history objects associated with the player
        physicalDatabase.invalidateHistory(uuid != null ? uuid.toString() : player);

        return ret;
    }

    public int transferProtectionsOfPlayer(CommandSender sender, String oldplayer, UUID newplayerid) {
        String newplayeridstring = newplayerid.toString();
        UUID uuid = UUIDRegistry.getUUID(oldplayer);
        String oldPlayerString = uuid != null ? uuid.toString() : oldplayer;

        List<Protection> protections = physicalDatabase.loadProtectionsByPlayerAlsoIfNotOwner(oldplayer);
        for (Protection p : protections) {
            if (p.getOwner() != null && p.getOwner().equalsIgnoreCase(oldPlayerString)) {
                p.setOwner(newplayerid);
            }
            for (Permission perm : p.getPermissions()) {
                if (perm.getType() == Permission.Type.PLAYER && perm.getName() != null && perm.getName().equalsIgnoreCase(oldPlayerString)) {
                    perm.setName(newplayeridstring);
                    p.setModified();
                }
            }
            p.save();
            protectionCache.addProtection(p);
        }
        return protections.size();
    }

    /**
     * Remove protections very quickly with raw SQL calls
     *
     * @param sender
     * @param where
     * @param shouldRemoveBlocks
     * @return
     */
    public int fastRemoveProtections(CommandSender sender, String where,
            boolean shouldRemoveBlocks) {
        List<Block> removeBlocks = null;

        if (shouldRemoveBlocks) {
            removeBlocks = new ArrayList<>();
        }

        if (where != null && !where.trim().isEmpty()) {
            where = " WHERE " + where.trim();
        }

        sender.sendMessage("Loading protections via STREAM mode");
        List<Protection> protections = physicalDatabase.streamDeleteProtections(where, sender);
        for(Protection protection : protections) {
            // remove the block ?
            if (shouldRemoveBlocks) {
                removeBlocks.add(protection.getBlock());
            }

            // Remove it from the cache if it's in there
            Protection cached = protectionCache.getProtection(protection.getCacheKey());
            if (cached != null) {
                cached.removeCache();
            }
        }

        if (shouldRemoveBlocks) {
            removeBlocks(sender, removeBlocks);
        }

        return protections.size();
    }

    /**
     * Remove a list of blocks from the world
     *
     * @param sender
     * @param blocks
     */
    private void removeBlocks(CommandSender sender, List<Block> blocks) {
        int count = 0;

        for (Block block : blocks) {
            if (block == null || !isProtectable(block)) {
                continue;
            }

            // possibility of a double chest
            Block doubleChest = BlockUtil.findAdjacentDoubleChest(block);
            if (doubleChest != null) {
                removeInventory(doubleChest);
                doubleChest.setType(Material.AIR);
            }

            // remove the inventory from the block if it has one
            removeInventory(block);

            // and now remove the block
            block.setType(Material.AIR);

            count++;
        }

        sender.sendMessage("Removed " + count + " blocks from the world");
    }

    /**
     * Remove the inventory from a block
     *
     * @param block
     */
    private void removeInventory(Block block) {
        if (block == null) {
            return;
        }

        if (!(block.getState() instanceof InventoryHolder)) {
            return;
        }

        InventoryHolder holder = (InventoryHolder) block.getState();
        holder.getInventory().clear();
    }

    /**
     * Compares two blocks if they are equal
     *
     * @param block
     * @param block2
     * @return
     */
    public boolean blockEquals(Block block, Block block2) {
        return block.getType() == block2.getType()
                && block.getX() == block2.getX()
                && block.getY() == block2.getY()
                && block.getZ() == block2.getZ();
    }

    /**
     * Find a protection for an entity
     *
     * @param entity
     *            the maybe protected entity
     * @return
     */
    public Protection findProtection(Entity entity) {
        int A = EntityBlock.POSITION_OFFSET + entity.getUniqueId().hashCode();
        CacheKey cacheKey = ProtectionCache.cacheKey(entity.getWorld().getName(), A, A, A);
        if (protectionCache.isKnownNull(cacheKey)) {
            return null;
        }
        Protection protection = physicalDatabase.loadProtection(entity.getWorld().getName(), A, A, A);
        if (protection == null) {
            protectionCache.addKnownNull(cacheKey);
        }
        return (protection != null && protection.isEntity()) ? protection : null;
    }

    /**
     * Find a protection linked to the location
     *
     * @param location
     * @return
     */
    public Protection findProtection(Location location) {
        CacheKey cacheKey = ProtectionCache.cacheKey(location);

        if (protectionCache.isKnownNull(cacheKey)) {
            return null;
        }

        Protection protection = protectionCache.getProtection(cacheKey);

        return protection != null ? protection : findProtection(location.getBlock().getState());
    }

    /**
     * Find a protection linked to the block
     *
     * @param block
     * @return
     */
    public Protection findProtection(Block block) {
        return findProtection(block.getState());
    }

    public Protection findProtection(BlockState block) {
        if (block instanceof EntityBlock) {
            return findProtection(((EntityBlock) block).getEntity());
        }
        // If the block type is AIR, then we have a problem .. but attempt to
        // load a protection anyway
        // Note: this call stems from a very old bug in Bukkit that likely does
        // not exist anymore at all
        // but is kept just incase. At one point getBlock() in Bukkit would
        // sometimes say a block
        // is an eir block even though the client and server sees it differently
        // (ie a chest).
        // This was of course very problematic!
        if (block.getType() == Material.AIR) {
            // We won't be able to match any other blocks anyway, so the least
            // we can do is attempt to load a protection
            return physicalDatabase.loadProtection(block.getWorld().getName(),
                    block.getX(), block.getY(), block.getZ());
        }

        // Create a protection finder
        ProtectionFinder finder = new ProtectionFinder(this);

        // Search for a protection
        boolean result = finder.matchBlocks(block);

        Protection found = null;

        // We're done, load the possibly loaded protection
        if (result) {
            found = finder.loadProtection();
        }

        if (found == null) {
            protectionCache.addKnownNull(ProtectionCache.cacheKey(block
                    .getLocation()));
        }

        return found;
    }

    /**
     * Find a protection linked to the block at [x, y, z]
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     */

    public boolean blockEquals(BlockState block, BlockState block2) {
        return block.getType() == block2.getType()
                && block.getX() == block2.getX()
                && block.getY() == block2.getY()
                && block.getZ() == block2.getZ();
    }

    public Protection findProtection(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }

        return findProtection(new Location(world, x, y, z));
    }

    /**
     * Matches all possible blocks that can be considered a 'protection' e.g
     * clicking a chest will match double chests, clicking a door or block below
     * a door matches the whole door
     *
     * @param world
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate
     * @return the List of possible blocks
     */
    public boolean isProtectable(BlockState state) {
        Material material = state.getType();

        if (material == null) {
            return false;
        }

        return protectableBlocks.contains(state.getType());
    }

    public boolean isProtectable(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (protectableEntites.contains(entity.getType())) {
            return true;
        }
        if ((entity instanceof LivingEntity) && !(entity instanceof Player) && !(entity instanceof ArmorStand) && !((LivingEntity) entity).hasAI()) {
            if (Boolean.parseBoolean(resolveSpecialProtectionConfiguration("noaimob", "enabled"))) {
                return true;
            }
        }
        return false;
    }

    public String getAutoRegisterType(Entity entity) {
        if ((entity instanceof LivingEntity) && !(entity instanceof Player) && !(entity instanceof ArmorStand) && !((LivingEntity) entity).hasAI()) {
            String protection = resolveSpecialProtectionConfiguration("noaimob", "autoRegister");
            if (protection != null) {
                return protection;
            }
        }
        return resolveProtectionConfiguration(entity.getType(), "autoRegister");
    }

    public String resolveProtectionConfiguration(EntityType state, String node) {
        if (state == null) {
            return configuration.getString("protections." + node);
        }
        String cacheKey = "e-" + state.name() + "-" + node;
        if (protectionConfigurationCache.containsKey(cacheKey)) {
            return protectionConfigurationCache.get(cacheKey);
        }

        String value = configuration.getString("protections." + node);

        String temp = configuration.getString("protections.entities." + state.name().toLowerCase()
                + "." + node);

        if (temp != null && !temp.isEmpty()) {
            value = temp;
        }

        protectionConfigurationCache.put(cacheKey, value);
        return value;
    }

    public String resolveSpecialProtectionConfiguration(String special, String node) {
        String cacheKey = "s-" + special + "-" + node;
        if (protectionConfigurationCache.containsKey(cacheKey)) {
            return protectionConfigurationCache.get(cacheKey);
        }

        String value = configuration.getString("protections.special." + special + "." + node);

        protectionConfigurationCache.put(cacheKey, value);
        return value;
    }

    /**
     * Check if a player has either access to lwc.admin or the specified node
     *
     * @param sender
     * @param node
     * @return
     */
    public boolean hasAdminPermission(CommandSender sender, String node) {
        return isAdmin(sender) || hasPermission(sender, node, "lwc.admin");
    }

    /**
     * Check if a player is an LWC admin -- Console defaults to *YES*
     *
     * @param sender
     * @return
     */
    public boolean isAdmin(CommandSender sender) {
        return !(sender instanceof Player) || isAdmin((Player) sender);
    }

    /**
     * Check a player for a node, using a fallback as a default (e.g
     * lwc.protect)
     *
     * @param sender
     * @param node
     * @param fallback
     * @return
     */
    public boolean hasPermission(CommandSender sender, String node,
            String... fallback) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        boolean hasNode = hasPermission(player, node);

        if (!hasNode) {
            for (String temp : fallback) {
                if (hasPermission(player, temp)) {
                    return true;
                }
            }
        }

        return hasNode;
    }

    /**
     * Check if a player has either access to lwc.protect or the specified node
     *
     * @param sender
     * @param node
     * @return
     */
    public boolean hasPlayerPermission(CommandSender sender, String node) {
        return hasPermission(sender, node, "lwc.protect");
    }

    /**
     * Check if a mode is enabled
     *
     * @param mode
     * @return
     */
    public boolean isModeEnabled(String mode) {
        return configuration.getBoolean("modes." + mode + ".enabled", true);
    }

    /**
     * Check if a mode is whitelisted for a player
     *
     * @param mode
     * @return
     */
    public boolean isModeWhitelisted(Player player, String mode) {
        return hasPermission(player, "lwc.mode." + mode, "lwc.allmodes");
    }

    /**
     * Check a block to see if it is protectable
     *
     * @param block
     * @return
     */
    public boolean isProtectable(Block block) {
        Material material = block.getType();
        if (block instanceof EntityBlock) {
            return isProtectable(((EntityBlock) block).getEntity());
        }

        if (material == null) {
            return false;
        }

        return protectableBlocks.contains(block.getType());
    }

    /**
     * Get the appropriate config value for the block (protections.block.node)
     *
     * @param block
     * @param node
     * @return
     */
    public String resolveProtectionConfiguration(Block block, String node) {
        if (block == null) {
            return null;
        }
        if (block instanceof EntityBlock entityBlock) {
            return resolveProtectionConfiguration(entityBlock.getEntity().getType(), node);
        }
        return resolveProtectionConfiguration(block.getType(), node);
    }

    /**
     * Get the appropriate config value for the block (protections.block.node)
     *
     * @param material
     * @param node
     * @return
     */
    public String resolveProtectionConfiguration(Material material, String node) {
        if (material == null) {
            return configuration.getString("protections." + node);
        }
        String cacheKey = "b-" + material.name() + "-" + node;
        if (protectionConfigurationCache.containsKey(cacheKey)) {
            return protectionConfigurationCache.get(cacheKey);
        }

        List<String> names = new ArrayList<>();

        String materialName = normalizeMaterialName(material);

        // add the name & the block id
        names.add(materialName);

        if (!materialName.equals(material.toString().toLowerCase())) {
            names.add(material.toString().toLowerCase());
        }

        // Add the wildcards last so it can be overriden
        names.add("*");

        String value = configuration.getString("protections." + node);

        for (String name : names) {
            String temp = configuration.getString("protections.blocks." + name
                    + "." + node);

            if (temp != null && !temp.isEmpty()) {
                value = temp;
            }
        }

        protectionConfigurationCache.put(cacheKey, value);
        return value;
    }

    /**
     * Load sqlite (done only when LWC is loaded so memory isn't used
     * unnecessarily)
     */
    public void load() {
        configuration = Configuration.load("core.yml");
        registerCoreModules();

        // check for upgrade before everything else
        new ConfigPost300().run();
        plugin.loadDatabase();

        Statistics.init();

        physicalDatabase = new PhysDB();

        // Permissions init
        permissions = new SuperPermsPermissions();

        if (resolvePlugin("Vault") != null) {
            permissions = new VaultPermissions();
        }

        // Currency init
        currency = new NoCurrency();

        if (resolvePlugin("Vault") != null) {
            currency = new VaultCurrency();
        }

        log("Connecting to " + Database.DefaultType);
        try {
            if (!physicalDatabase.connect()) {
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }
            physicalDatabase.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // check any major conversions
        new MySQLPost200().run();

        preloadProtectables();
        BlockMap.instance().init();

        // precache protections
        physicalDatabase.precache();

        // We are now done loading!
        moduleLoader.loadAll();
    }

    /**
     * Register the core modules for LWC
     */
    private void registerCoreModules() {
        // core
        registerModule(new LimitsV2());
        registerModule(new LimitsModule());
        registerModule(new CreateModule());
        registerModule(new ModifyModule());
        registerModule(new DestroyModule());
        registerModule(new FreeModule());
        registerModule(new InfoModule());
        registerModule(new UnlockModule());
        registerModule(new OwnersModule());
        registerModule(new DoorsModule());
        registerModule(new DebugModule());
        registerModule(new CreditsModule());
        registerModule(new HistoryModule());
        registerModule(new ConfirmModule());

        // admin commands
        registerModule(new BaseAdminModule());
        registerModule(new AdminCache());
        registerModule(new AdminTransfer());
        registerModule(new AdminCleanup());
        registerModule(new AdminClear());
        registerModule(new AdminFind());
        registerModule(new AdminForceOwner());
        registerModule(new AdminLocale());
        registerModule(new AdminPurge());
        registerModule(new AdminReload());
        registerModule(new AdminRemove());
        registerModule(new AdminReport());
        registerModule(new AdminVersion());
        registerModule(new AdminPurgeBanned());
        registerModule(new AdminExpire());
        registerModule(new AdminDump());
        registerModule(new AdminRebuild());
        registerModule(new AdminView());

        // /lwc setup
        registerModule(new BaseSetupModule());
        registerModule(new DatabaseSetupModule());
        registerModule(new LimitsSetup());

        // flags
        registerModule(new BaseFlagModule());
        registerModule(new RedstoneModule());

        // modes
        registerModule(new BaseModeModule());
        registerModule(new PersistModule());
        registerModule(new NoSpamModule());

        // non-core modules but are included with LWC anyway
        if (resolvePlugin("WorldGuard") != null) {
            registerModule(new WorldGuard());
        }
    }

    /**
     * Register a module
     *
     * @param module
     */
    private void registerModule(Module module) {
        moduleLoader.registerModule(plugin, module);
    }

    /**
     * Get a plugin by the name. Does not have to be enabled, and will remain
     * disabled if it is disabled.
     *
     * @param name
     * @return
     */
    private Plugin resolvePlugin(String name) {
        Plugin temp = plugin.getServer().getPluginManager().getPlugin(name);

        if (temp == null) {
            return null;
        }

        return temp;
    }

    /**
     * Process rights inputted for a protection and add or remove them to the
     * given protection
     *
     * @param sender
     * @param protection
     * @param arguments
     */
    public void processRightsModifications(CommandSender sender,
            Protection protection, String... arguments) {
        // Does it match a protection type?
        try {
            Protection.Type protectionType = Protection.Type
                    .matchType(arguments[0]);

            if (protectionType != null) {
                protection.setType(protectionType);

                // If it's being passworded, we need to set the password
                if (protectionType == Protection.Type.PASSWORD) {
                    String password = StringUtil.join(arguments, 1);
                    protection.setPassword(encrypt(password));
                }
                protection.save();

                sendLocale(
                        sender,
                        "protection.typechanged",
                        "type",
                        plugin.getMessageParser().parseMessage(
                                protectionType.toString().toLowerCase()));
                return;
            }
        } catch (IllegalArgumentException e) {
            // It's normal for this to be thrown if nothing was matched
        }

        for (String value : arguments) {
            boolean remove = false;
            boolean isAdmin = false;
            Permission.Type type = Permission.Type.PLAYER;

            // Gracefully ignore id
            if (value.startsWith("id:")) {
                continue;
            }

            if (value.startsWith("-")) {
                remove = true;
                value = value.substring(1);
            }

            if (value.startsWith("@")) {
                isAdmin = true;
                value = value.substring(1);
            }

            if (value.toLowerCase().startsWith("p:")) {
                type = Permission.Type.PLAYER;
                value = value.substring(2);
            }

            if (value.toLowerCase().startsWith("g:")) {
                type = Permission.Type.GROUP;
                value = value.substring(2);
            }

            if (value.toLowerCase().startsWith("t:")) {
                type = Permission.Type.TOWN;
                value = value.substring(2);
            }

            if (value.toLowerCase().startsWith("town:")) {
                type = Permission.Type.TOWN;
                value = value.substring(5);
            }

            if (value.toLowerCase().startsWith("item:")) {
                type = Permission.Type.ITEM;
                value = value.substring(5);
            }

            if (value.toLowerCase().startsWith("r:")) {
                type = Permission.Type.REGION;
                value = value.substring(2);
            }

            if (value.toLowerCase().startsWith("region:")) {
                type = Permission.Type.REGION;
                value = value.substring(7);
            }

            if (value.trim().isEmpty()) {
                continue;
            }

            String localeChild = type.toString().toLowerCase();

            // If it's a player, convert it to UUID
            if (type == Permission.Type.PLAYER) {
                UUID uuid = UUIDRegistry.getUUID(value);

                if (uuid != null) {
                    value = uuid.toString();
                }
            }

            if (!remove) {
                Permission permission = new Permission(value, type);
                permission.setAccess(isAdmin ? Permission.Access.ADMIN
                        : Permission.Access.PLAYER);

                // add it to the protection and queue it to be saved
                protection.addPermission(permission);
                protection.save();

                if (type == Permission.Type.PLAYER) {
                    sendLocale(sender, "protection.interact.rights.register."
                            + localeChild, "name",
                            UUIDRegistry.getNameOrUUID(value), "isadmin",
                            isAdmin ? " [" + Colors.Red + "ADMIN" + Colors.Gold
                                    + "]" : "");
                } else {
                    sendLocale(sender, "protection.interact.rights.register."
                            + localeChild, "name", value, "isadmin",
                            isAdmin ? " [" + Colors.Red + "ADMIN" + Colors.Gold
                                    + "]" : "");
                }
            } else {
                protection.removePermissions(value, type);
                protection.save();

                if (type == Permission.Type.PLAYER) {
                    sendLocale(sender, "protection.interact.rights.remove."
                            + localeChild, "name",
                            UUIDRegistry.getNameOrUUID(value), "isadmin",
                            isAdmin ? " [" + Colors.Red + "ADMIN" + Colors.Gold
                                    + "]" : "");
                } else {
                    sendLocale(sender, "protection.interact.rights.remove."
                            + localeChild, "name", value, "isadmin",
                            isAdmin ? " [" + Colors.Red + "ADMIN" + Colors.Gold
                                    + "]" : "");
                }
            }
        }
    }

    private void preloadProtectables() {
        protectableBlocks.clear();
        protectableEntites.clear();
        for (Material t : Material.values()) {
            if (Boolean.parseBoolean(resolveProtectionConfiguration(t, "enabled"))) {
                protectableBlocks.add(t);
            }
        }
        for (EntityType t : EntityType.values()) {
            if (Boolean.parseBoolean(resolveProtectionConfiguration(t, "enabled"))) {
                protectableEntites.add(t);
            }
        }
    }

    /**
     * Reload internal data structures
     */
    public void reload() {
        plugin.loadLocales();
        protectionConfigurationCache.clear();
        Configuration.reload();
        preloadProtectables();
        moduleLoader.dispatchEvent(new LWCReloadEvent());
    }

    /**
     * Reload the database
     */
    public void reloadDatabase() {
        try {
            physicalDatabase = new PhysDB();
            physicalDatabase.connect();
            physicalDatabase.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove all modes if the player is not in persistent mode
     *
     * @param sender
     */
    public void removeModes(CommandSender sender) {
        if (sender instanceof Player) {
            Player bPlayer = (Player) sender;

            if (notInPersistentMode(bPlayer.getName())) {
                wrapPlayer(bPlayer).removeAllActions();
            }
        } else if (sender instanceof LWCPlayer) {
            removeModes(((LWCPlayer) sender).getBukkitPlayer());
        }
    }

    /**
     * Return if the player is in persistent mode
     *
     * @param player
     *            the player to check
     * @return true if the player is NOT in persistent mode
     */
    public boolean notInPersistentMode(String player) {
        return !wrapPlayer(Bukkit.getServer().getPlayer(player)).hasMode(
                "persist");
    }

    /**
     * Send the full help to a player
     *
     * @param sender
     *            the player to send to
     */
    public void sendFullHelp(CommandSender sender) {
        sendLocale(sender, "help.basic");

        if (isAdmin(sender)) {
            sender.sendMessage("");
            sender.sendMessage(Colors.Red + "/lwc admin - Administration");
        }
    }

    /**
     * Send the simple usage of a command
     *
     * @param player
     * @param command
     */
    public void sendSimpleUsage(CommandSender player, String command) {
        sendLocale(player, "help.simpleusage", "command", command);
    }

    /**
     * @return the configuration object
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @return the Currency handler
     */
    public ICurrency getCurrency() {
        return currency;
    }

    /**
     * @return the module loader
     */
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * @return the Permissions handler
     */
    public IPermissions getPermissions() {
        return permissions;
    }

    /**
     * @return physical database object
     */
    public PhysDB getPhysicalDatabase() {
        return physicalDatabase;
    }

    /**
     * @return the plugin class
     */
    public LWCPlugin getPlugin() {
        return plugin;
    }

    /**
     * @return the protection cache
     */
    public ProtectionCache getProtectionCache() {
        return protectionCache;
    }

    /**
     * @return the plugin version
     */
    public double getVersion() {
        return Double.parseDouble(plugin.getDescription().getVersion());
    }

    /**
     * @return true if history logging is enabled
     */
    public boolean isHistoryEnabled() {
        return !configuration.getBoolean("core.disableHistory", false);
    }
}
