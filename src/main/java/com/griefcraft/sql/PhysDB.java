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

package com.griefcraft.sql;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.cache.CacheKey;
import com.griefcraft.cache.LRUCache;
import com.griefcraft.cache.ProtectionCache;
import com.griefcraft.lwc.BlockMap;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Flag;
import com.griefcraft.model.History;
import com.griefcraft.model.Permission;
import com.griefcraft.model.Protection;
import com.griefcraft.modules.limits.LimitsModule;
import com.griefcraft.scripting.Module;
import com.griefcraft.util.Colors;
import com.griefcraft.util.Statistics;
import com.griefcraft.util.UUIDRegistry;
import com.griefcraft.util.config.Configuration;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PhysDB extends Database {

    /**
     * The JSON Parser object
     */
    private final JSONParser jsonParser = new JSONParser();

    /**
     * The database version
     */
    private int databaseVersion = 0;
    private int entityLockingDatabaseVersion = 0;

    /**
     * The number of protections that should exist
     */
    private int protectionCount = 0;

    public PhysDB() {
        super();
    }

    public PhysDB(Type currentType) {
        super(currentType);
    }

    /**
     * Decrement the known protection counter
     */
    public void decrementProtectionCount() {
        protectionCount--;
    }

    /**
     * Check if the protection cache has all of the known protections cached
     *
     * @return
     */
    public boolean hasAllProtectionsCached() {
        ProtectionCache cache = LWC.getInstance().getProtectionCache();

        return cache.size() >= protectionCount;
    }

    /**
     * Fetch an object from the sql database
     *
     * @param sql
     * @param column
     * @return
     */
    private Object fetch(String sql, String column, Object... toBind) throws SQLException {
        PreparedStatement statement = prepare(sql);

        int index = 1;
        for (Object bind : toBind) {
            statement.setObject(index, bind);
            index++;
        }

        ResultSet set = statement.executeQuery();

        Object object = null;
        if (set.next()) {
            object = set.getObject(column);
        }
        set.close();
        return object;
    }

    /**
     * Get the total amount of protections
     *
     * @return the number of protections
     */
    public int getProtectionCount() {
        return runAndThrowModuleExceptionIfFailing(() -> {
            return Integer.decode(fetch("SELECT COUNT(*) AS count FROM " + prefix + "protections", "count").toString());
        });
    }

    /**
     * Get the amount of protections for the given protection type
     *
     * @param type
     * @return the number of protected chests
     */
    public int getProtectionCount(Protection.Type type) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            return Integer.decode(fetch("SELECT COUNT(*) AS count FROM " + prefix + "protections WHERE type = " + type.ordinal(), "count").toString());
        });
    }

    /**
     * @return the number of history items stored
     */
    public int getHistoryCount() {
        return runAndThrowModuleExceptionIfFailing(() -> {
            return Integer.decode(fetch("SELECT COUNT(*) AS count FROM " + prefix + "history", "count").toString());
        });
    }

    /**
     * Get the amount of protections a player has
     *
     * @param player
     * @return the amount of protections they have
     */
    public int getProtectionCount(String player) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            int count = 0;

            PreparedStatement statement = prepare("SELECT COUNT(*) as count FROM " + prefix + "protections WHERE owner = ?");
            UUID uuid = UUIDRegistry.getUUID(player);
            statement.setString(1, uuid != null ? uuid.toString() : player);

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                count = set.getInt("count");
            }

            set.close();

            return count;
        });
    }

    /**
     * Get the amount of protections a player has
     *
     * @param player
     * @return the amount of protections they have
     */
    public int getHistoryCount(String player) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            int count = 0;

            PreparedStatement statement = prepare("SELECT COUNT(*) AS count FROM " + prefix + "history WHERE LOWER(player) = LOWER(?)");
            UUID uuid = UUIDRegistry.getUUID(player);
            statement.setString(1, uuid != null ? uuid.toString() : player);
            ResultSet set = statement.executeQuery();

            if (set.next()) {
                count = set.getInt("count");
            }

            set.close();

            return count;
        });
    }

    /**
     * Get the amount of chests a player has of a specific block id
     *
     * @param player
     * @return the amount of protections they have of blockId
     */
    public int getProtectionCount(String player, Material block) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            int count = 0;

            PreparedStatement statement = prepare("SELECT COUNT(*) AS count FROM " + prefix + "protections WHERE owner = ? AND blockId = ?");
            UUID uuid = UUIDRegistry.getUUID(player);
            statement.setString(1, uuid != null ? uuid.toString() : player);
            statement.setInt(2, BlockMap.instance().getId(block));

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                count = set.getInt("count");
            }

            set.close();

            return count;
        });
    }

    /**
     * Load the database and do any updating required or create the tables
     */
    @Override
    public void load() {
        if (loaded) {
            return;
        }

        databaseVersion = 0;
        loadDatabaseVersion();
        entityLockingDatabaseVersion = 0;
        loadEntityLockingDatabaseVersion();

        if (entityLockingDatabaseVersion < 1) {
            /**
             * Updates that alter or rename a table go here
             */
            doUpdate301();
            doUpdate302();
            doUpdate330();
            doUpdate400_4();
            doUpdate400_5();
            doUpdate400_6();
            doUpdate5_0_12();
            boolean resetDatabaseVersion = doUpdateModernLWC();

            Column column;

            Table protections = new Table(this, "protections");
            {
                column = new Column("id");
                column.setType("INTEGER");
                column.setPrimary(true);
                protections.add(column);

                column = new Column("owner");
                column.setType("VARCHAR(36)");
                protections.add(column);

                column = new Column("type");
                column.setType("INTEGER");
                protections.add(column);

                column = new Column("x");
                column.setType("INTEGER");
                protections.add(column);

                column = new Column("y");
                column.setType("INTEGER");
                protections.add(column);

                column = new Column("z");
                column.setType("INTEGER");
                protections.add(column);

                column = new Column("entityid");
                column.setType("VARCHAR(36)");
                protections.add(column);

                column = new Column("data");
                column.setType("TEXT");
                protections.add(column);

                column = new Column("blockId");
                column.setType("INTEGER");
                protections.add(column);

                column = new Column("world");
                column.setType("VARCHAR(50)");
                protections.add(column);

                column = new Column("password");
                column.setType("VARCHAR(255)");
                protections.add(column);

                column = new Column("date");
                column.setType("VARCHAR(50)");
                protections.add(column);

                column = new Column("last_accessed");
                column.setType("INTEGER");
                protections.add(column);
            }

            Table history = new Table(this, "history");
            {
                column = new Column("id");
                column.setType("INTEGER");
                column.setPrimary(true);
                history.add(column);

                column = new Column("protectionId");
                column.setType("INTEGER");
                history.add(column);

                column = new Column("player");
                column.setType("VARCHAR(36)");
                history.add(column);

                column = new Column("x");
                column.setType("INTEGER");
                history.add(column);

                column = new Column("y");
                column.setType("INTEGER");
                history.add(column);

                column = new Column("z");
                column.setType("INTEGER");
                history.add(column);

                column = new Column("type");
                column.setType("INTEGER");
                history.add(column);

                column = new Column("status");
                column.setType("INTEGER");
                history.add(column);

                column = new Column("metadata");
                column.setType("VARCHAR(255)");
                history.add(column);

                column = new Column("timestamp");
                column.setType("long");
                history.add(column);
            }

            Table internal = new Table(this, "internal");
            {
                column = new Column("name");
                column.setType("VARCHAR(40)");
                column.setPrimary(true);
                column.setAutoIncrement(false);
                internal.add(column);

                column = new Column("value");
                column.setType("VARCHAR(40)");
                internal.add(column);
            }

            Table blockMappings = new Table(this, "blocks");
            {
                column = new Column("id");
                column.setType("INTEGER");
                column.setPrimary(true);
                column.setAutoIncrement(false);
                blockMappings.add(column);

                column = new Column("name");
                column.setType("VARCHAR(40)");
                blockMappings.add(column);
            }

            runAndThrowModuleExceptionIfFailing(() -> {
                protections.execute();
                history.execute();
                internal.execute();
                blockMappings.execute();
            });

            // Load the database version
            databaseVersion = 0;
            entityLockingDatabaseVersion = 0;
            if (!resetDatabaseVersion) {
                loadDatabaseVersion();
                loadEntityLockingDatabaseVersion();
                if (databaseVersion < 0) {
                    databaseVersion = 0;
                }
                if (entityLockingDatabaseVersion < 0) {
                    entityLockingDatabaseVersion = 0;
                }
            }
        }

        // perform database upgrades
        performDatabaseUpdates();

        // get the amount of protections
        protectionCount = getProtectionCount();

        loaded = true;
    }

    /**
     * Perform any database updates
     */
    public void performDatabaseUpdates() {
        LWC lwc = LWC.getInstance();

        if (entityLockingDatabaseVersion == 0) {
            databaseVersion = 0; // reset to check all updates
        }

        // Indexes
        if (databaseVersion == 0) {
            // Drop old, old indexes
            dropIndex("protections", "in1");
            dropIndex("protections", "in6");
            dropIndex("protections", "in7");
            dropIndex("history", "in8");
            dropIndex("history", "in9");
            dropIndex("protections", "in10");
            dropIndex("history", "in12");
            dropIndex("history", "in13");
            dropIndex("history", "in14");

            // Create our updated (good) indexes
            doUpdatesDatabaseVersion7(); // do this here to avoid regenerating the index
            createIndex("protections", "protections_main", "x, y, z, world");
            createIndex("protections", "protections_entity", "entityid");
            createIndex("protections", "protections_utility", "owner");
            createIndex("history", "history_main", "protectionId");
            createIndex("history", "history_utility", "player");
            createIndex("history", "history_utility2", "x, y, z");

            // increment the database version
            incrementDatabaseVersion();
        }

        if (databaseVersion == 1) {
            createIndex("internal", "internal_main", "name");
            incrementDatabaseVersion();
        }

        if (databaseVersion == 2) {
            doUpdate400_2();
            incrementDatabaseVersion();
        }

        if (databaseVersion == 3) {
            createIndex("protections", "protections_type", "type");
            incrementDatabaseVersion();
        }

        if (databaseVersion == 4) {
            // List<String> blacklistedBlocks = lwc.getConfiguration().getStringList("optional.blacklistedBlocks", new ArrayList<String>());
            //
            // if (!blacklistedBlocks.contains(Material.HOPPER.name())) {
            // blacklistedBlocks.add(Material.HOPPER.name());
            // lwc.getConfiguration().setProperty("optional.blacklistedBlocks", blacklistedBlocks);
            // lwc.getConfiguration().save();
            // Configuration.reload();
            //
            // lwc.log("Added Hoppers to Blacklisted Blocks in core.yml (optional.blacklistedBlocks)");
            // lwc.log("This means that Hoppers CANNOT be placed around protections a player does not have access to");
            // lwc.log("If you DO NOT want this feature, simply remove " + Material.HOPPER.name() + " (Hoppers) from blacklistedBlocks :-)");
            // }

            incrementDatabaseVersion();
        }

        if (databaseVersion == 5) {
            boolean foundTrappedChest = false;

            for (String key : lwc.getConfiguration().getNode("protections.blocks").getKeys(null)) {
                if (key.equalsIgnoreCase("trapped_chest")) {
                    foundTrappedChest = true;
                    break;
                }
            }

            if (!foundTrappedChest) {
                lwc.getConfiguration().setProperty("protections.blocks.trapped_chest.enabled", true);
                lwc.getConfiguration().setProperty("protections.blocks.trapped_chest.autoRegister", "private");
                lwc.getConfiguration().save();
                Configuration.reload();

                lwc.log("Added Trapped Chests to core.yml as default protectable (ENABLED & AUTO REGISTERED)");
                lwc.log("Trapped chests are nearly the same as reg chests but can light up! They can also be double chests.");
                lwc.log("If you DO NOT want this as protected, simply remove it from core.yml! (search/look for trapped_chests under protections -> blocks");
            }

            incrementDatabaseVersion();
        }

        if (databaseVersion == 6) {
            doUpdatesDatabaseVersion7(); // do this here to avoid regenerating the index
            createIndex("protections", "protections_main", "x, y, z, world");
            createIndex("protections", "protections_utility", "owner");
            createIndex("history", "history_utility", "player");

            incrementDatabaseVersion();
        }

        if (databaseVersion == 7) {
            doUpdatesDatabaseVersion7();

            incrementDatabaseVersion();
        }
        
        if (databaseVersion == 8) {
            doUpdatesDatabaseVersion8();

            incrementDatabaseVersion(); 
        }

        if (entityLockingDatabaseVersion == 0) {
            incrementEntityLockingDatabaseVersion();
        }
    }

    /**
     * Increment the database version
     */
    public void incrementDatabaseVersion() {
        setDatabaseVersion(++databaseVersion);
    }

    /**
     * Increment the database version
     */
    public void incrementEntityLockingDatabaseVersion() {
        setEntityLockingDatabaseVersion(++entityLockingDatabaseVersion);
    }

    /**
     * Set the database version and sync it to the database
     *
     * @param databaseVersion
     */
    public void setDatabaseVersion(int databaseVersion) {
        // set it locally
        this.databaseVersion = databaseVersion;

        // ship it to the database
        runAndLogException(() -> {
            PreparedStatement statement = prepare("UPDATE " + prefix + "internal SET value = ? WHERE name = ?");
            statement.setInt(1, databaseVersion);
            statement.setString(2, "version");

            // ok
            statement.executeUpdate();
        });
    }

    /**
     * Set the database version and sync it to the database
     *
     * @param databaseVersion
     */
    public void setEntityLockingDatabaseVersion(int databaseVersion) {
        // set it locally
        this.entityLockingDatabaseVersion = databaseVersion;

        // ship it to the database
        runAndLogException(() -> {
            PreparedStatement statement = prepare("UPDATE " + prefix + "internal SET value = ? WHERE name = ?");
            statement.setInt(1, entityLockingDatabaseVersion);
            statement.setString(2, "entityversion");

            // ok
            statement.executeUpdate();
        });
    }

    /**
     * Get a value in the internal table
     *
     * @param key
     * @return the value found, otherwise NULL if none exists
     */
    public String getInternal(String key) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            String value = null;
            PreparedStatement statement = prepare("SELECT value FROM " + prefix + "internal WHERE name = ?");
            statement.setString(1, key);

            ResultSet set = statement.executeQuery();
            if (set.next()) {
                value = set.getString("value");
            }
            set.close();
            return value;
        });
    }

    /**
     * Set a value in the internal table
     *
     * @param key
     * @param value
     */
    public void setInternal(String key, String value) {
        runAndThrowModuleExceptionIfFailing(() -> {
            try {
                PreparedStatement statement = prepare("INSERT INTO " + prefix + "internal (name, value) VALUES (?, ?)");
                statement.setString(1, key);
                statement.setString(2, value);

                statement.executeUpdate();
            } catch (SQLException e) {
                // Already exists
                PreparedStatement statement = prepare("UPDATE " + prefix + "internal SET value = ? WHERE name = ?");
                statement.setString(1, value);
                statement.setString(2, key);

                statement.executeUpdate();
            }
        });
    }

    private boolean hasInternalTable() throws SQLException {
        PreparedStatement statement = null;
        if (getType() == Type.SQLite) {
            statement = prepare("SELECT name FROM sqlite_master WHERE name = ?");
        } else if (getType() == Type.MySQL) {
            statement = prepare("SHOW TABLES LIKE ?");
        } else {
            return false;
        }
        statement.setString(1, prefix + "internal");
        ResultSet set = statement.executeQuery();
        boolean hasTable = set.next();
        set.close();
        return hasTable;
    }

    /**
     * Load the database internal version
     */
    public void loadDatabaseVersion() {
        databaseVersion = runAndLogException(() -> {
            int databaseVersion = -1;

            if (!hasInternalTable()) {
                return -1;
            }

            PreparedStatement statement = prepare("SELECT value FROM " + prefix + "internal WHERE name = ?");
            statement.setString(1, "version");

            // Execute it
            ResultSet set = statement.executeQuery();

            // load the version
            if (set.next()) {
                databaseVersion = Integer.parseInt(set.getString("value"));
            } else {
                // Doesn't exist, create it
                statement = prepare("INSERT INTO " + prefix + "internal (name, value) VALUES(?, ?)");
                statement.setString(1, "version");
                statement.setInt(2, databaseVersion);

                // ok
                statement.executeUpdate();
            }

            // close everything
            set.close();

            return databaseVersion;
        });
    }

    /**
     * Load the database internal version
     */
    public void loadEntityLockingDatabaseVersion() {
        entityLockingDatabaseVersion = runAndLogException(() -> {
            int entityLockingDatabaseVersion = -1;

            if (!hasInternalTable()) {
                return -1;
            }

            PreparedStatement statement = prepare("SELECT value FROM " + prefix + "internal WHERE name = ?");
            statement.setString(1, "entityversion");

            // Execute it
            ResultSet set = statement.executeQuery();

            // load the version
            if (set.next()) {
                entityLockingDatabaseVersion = Integer.parseInt(set.getString("value"));
            } else {
                // Doesn't exist, create it
                statement = prepare("INSERT INTO " + prefix + "internal (name, value) VALUES(?, ?)");
                statement.setString(1, "entityversion");
                statement.setInt(2, entityLockingDatabaseVersion);

                // ok
                statement.executeUpdate();
            }

            // close everything
            set.close();

            return entityLockingDatabaseVersion;
        });
    }

    /**
     * Load a protection with the given id
     *
     * @param id
     * @return the Chest object
     */
    public Protection loadProtection(int id) {
        // the protection cache
        ProtectionCache cache = LWC.getInstance().getProtectionCache();

        // check if the protection is already cached
        Protection cached = cache.getProtectionById(id);
        if (cached != null) {
            return cached;
        }

        Protection protection = runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE id = ?");
            statement.setInt(1, id);

            return resolveProtection(statement);
        });

        if (protection != null) {
            cache.addProtection(protection);
        }
        return protection;
    }

    /**
     * Load protections using a specific type
     *
     * @param type
     * @return the Protection object
     */
    public List<Protection> loadProtectionsUsingType(Protection.Type type) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE type = ?");
            statement.setInt(1, type.ordinal());

            return resolveProtections(statement);
        });
    }

    /**
     * Resolve one protection from a ResultSet. The ResultSet is not closed.
     *
     * @param set
     * @return
     */
    private Protection resolveProtection(ResultSet set) throws SQLException {
        Protection protection = new Protection();

        int protectionId = set.getInt("id");
        int x = set.getInt("x");
        int y = set.getInt("y");
        int z = set.getInt("z");
        String entityidString = set.getString("entityid");
        UUID entityid = entityidString == null ? null : UUID.fromString(entityidString);
        int blockId = set.getInt("blockId");
        int type = set.getInt("type");
        String world = set.getString("world");
        String owner = set.getString("owner");
        String password = set.getString("password");
        String date = set.getString("date");
        long lastAccessed = set.getLong("last_accessed");

        protection.setId(protectionId);
        protection.setX(x);
        protection.setY(y);
        protection.setZ(z);
        if (blockId == EntityBlock.ENTITY_BLOCK_ID) {
            protection.setIsEntity(true);
        } else {
            protection.setBlockMaterial(BlockMap.instance().getMaterial(blockId));
        }
        protection.setEntityId(entityid);
        protection.setType(Protection.Type.values()[type]);
        protection.setWorld(world);
        protection.setOwner(owner);
        protection.setPassword(password);
        protection.setCreation(date);
        protection.setLastAccessed(lastAccessed);

        // check for oh so beautiful data!
        String data = set.getString("data");

        if (data == null || data.isBlank()) {
            return protection;
        }

        // rev up them JSON parsers!
        Object object = null;

        try {
            object = jsonParser.parse(data);
        } catch (ParseException e) {
            return protection;
        }

        if (!(object instanceof JSONObject root)) {
            return protection;
        }

        // Attempt to parse rights
        if (root.get("rights") instanceof JSONArray array) {
            for (Object node : array) {
                // we only want to use the maps
                if (!(node instanceof JSONObject map)) {
                    continue;
                }

                // decode the map
                Permission permission = Permission.decodeJSON(map);

                // bingo!
                if (permission != null) {
                    protection.addPermission(permission);
                }
            }
        }

        // Attempt to parse flags
        if (root.get("flags") instanceof JSONArray array) {
            for (Object node : array) {
                if (!(node instanceof JSONObject map)) {
                    continue;
                }

                Flag flag = Flag.decodeJSON(map);

                if (flag != null) {
                    protection.addFlag(flag);
                }
            }
        }

        return protection;
    }

    /**
     * Resolve every protection from a result set
     *
     * @param set
     * @return
     */
    private List<Protection> resolveProtections(ResultSet set) throws SQLException {
        List<Protection> protections = new ArrayList<>();
        while (set.next()) {
            protections.add(resolveProtection(set));
        }
        return protections;
    }

    /**
     * Resolve a list of protections from a statement
     *
     * @param statement
     * @return
     */
    private List<Protection> resolveProtections(PreparedStatement statement) throws SQLException {
        return resolveProtections(statement.executeQuery());
    }

    /**
     * Resolve the first protection from a statement
     *
     * @param statement
     * @return
     */
    private Protection resolveProtection(PreparedStatement statement) throws SQLException {
        List<Protection> protections = resolveProtections(statement);
        return protections.size() == 0 ? null : protections.get(0);
    }

    /**
     * Fill the protection cache as much as possible with protections Caches the
     * most recent protections
     */
    public void precache() {
        LWC lwc = LWC.getInstance();
        ProtectionCache cache = lwc.getProtectionCache();

        // clear the cache incase we're working on a dirty cache
        cache.clear();

        int precacheSize = lwc.getConfiguration().getInt("core.precache", -1);

        if (precacheSize == -1) {
            precacheSize = lwc.getConfiguration().getInt("core.cacheSize", 10000);
        }
        int finalPrecacheSize = precacheSize;

        List<Protection> protections = runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections ORDER BY id DESC LIMIT ?");
            statement.setInt(1, finalPrecacheSize);
            statement.setFetchSize(10);

            // scrape the protections from the result set now
            return resolveProtections(statement);
        });

        // throw all of the protections in
        for (Protection protection : protections) {
            cache.addProtection(protection);
        }
    }

    /**
     * Load a protection at the given coordinates
     *
     * @param x
     * @param y
     * @param z
     * @return the Protection object
     */
    public Protection loadProtection(String worldName, int x, int y, int z) {
        return loadProtection(worldName, x, y, z, false);
    }

    /**
     * Load a protection at the given coordinates
     *
     * @param x
     * @param y
     * @param z
     * @param ignoreProtectionCount
     * @return the Protection object
     */
    private Protection loadProtection(String worldName, int x, int y, int z, boolean ignoreProtectionCount) {
        // the unique key to use in the cache
        CacheKey cacheKey = ProtectionCache.cacheKey(worldName, x, y, z);

        // the protection cache
        ProtectionCache cache = LWC.getInstance().getProtectionCache();

        // check if the protection is already cached
        Protection cached = cache.getProtection(cacheKey);
        if (cached != null) {
            // System.out.println("loadProtection() => CACHE HIT");
            if (x == y && x == z) {
                Statistics.addEntityCacheHit();
            } else {
                Statistics.addBlockCacheHit();
            }
            return cached;
        }
        if (cache.isDirectKnownNull(cacheKey) || cache.isKnownNull(cacheKey)) {
            if (x == y && x == z) {
                Statistics.addEntityCacheHitNull();
            } else {
                Statistics.addBlockCacheHitNull();
            }
            return null;
        }

        // Is it possible that there are protections in the cache?
        if (!ignoreProtectionCount && hasAllProtectionsCached()) {
            // System.out.println("loadProtection() => HAS_ALL_PROTECTIONS_CACHED");
            return null; // nothing was in the cache, nothing assumed to be in
                         // the database
        }
        // System.out.println("loadProtection() => QUERYING");

        Protection protection = runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE x = ? AND y = ? AND z = ? AND world = ? AND entityid IS NULL");
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);
            statement.setString(4, worldName);

            return resolveProtection(statement);
        });

        if (protection != null) {
            // cache the protection
            cache.addProtection(protection);
            if (x == y && x == z) {
                Statistics.addEntityCacheMiss();
            } else {
                Statistics.addBlockCacheMiss();
            }
        } else {
            if (x == y && x == z) {
                Statistics.addEntityCacheMissNull();
            } else {
                Statistics.addBlockCacheMissNull();
            }
            cache.addDirectKnownNull(cacheKey);
        }

        return protection;
    }

    /**
     * Load a protection for an entity at the given coordinates
     *
     * @param entity
     * @return the Protection object
     */
    public Protection loadProtection(Entity entity, boolean ignoreProtectionCount) {
        return loadProtection(entity.getUniqueId(), ignoreProtectionCount);
    }

    public Protection loadProtection(UUID entityId, boolean ignoreProtectionCount) {
        // Is it possible that there are protections in the cache?
        if (!ignoreProtectionCount && hasAllProtectionsCached()) {
            // System.out.println("loadProtection() => HAS_ALL_PROTECTIONS_CACHED");
            return null; // nothing was in the cache, nothing assumed to be in
                         // the database
        }
        ProtectionCache cache = LWC.getInstance().getProtectionCache();
        if (cache.isKnownNull(entityId)) {
            return null;
        }
        // System.out.println("loadProtection() => QUERYING");

        Protection protection = runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE entityid = ?");
            statement.setString(1, entityId.toString());

            return resolveProtection(statement);
        });

        if (protection != null) {
            // cache the protection
            cache.addProtection(protection);
            Statistics.addEntityCacheMiss();
        } else {
            cache.addKnownNull(entityId);
            Statistics.addEntityCacheMissNull();
        }

        return protection;
    }

    /**
     * Load all protections (use sparingly !!)
     *
     * @return
     */
    public List<Protection> loadProtections() {
        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections");

            return resolveProtections(statement);
        });
    }

    /**
     * Load all protections ordered by the chunk they are in (use sparingly !!)
     *
     * @return
     */
    public List<Protection> loadProtectionsOrderedByChunk() {
        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed, x>>4 AS xshift4, z>>4 AS zshift4 FROM " + prefix + "protections ORDER BY xshift4, zshift4");

            return resolveProtections(statement);
        });
    }

    /**
     * Remove all protections for a given player
     *
     * @param player
     * @return the amount of protections removed
     */
    public int removeProtectionsByPlayer(String player) {
        int removed = 0;

        for (Protection protection : loadProtectionsByPlayer(player)) {
            protection.remove();
            removed++;
        }

        return removed;
    }

    /**
     * Load all protections in the coordinate ranges
     *
     * @param world
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @param z1
     * @param z2
     * @return list of Protection objects found
     */
    public List<Protection> loadProtections(String world, int x1, int x2, int y1, int y2, int z1, int z2) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE world = ? AND x >= ? AND x <= ? AND y >= ? AND y <= ? AND z >= ? AND z <= ? AND entityid IS NULL");

            statement.setString(1, world);
            statement.setInt(2, x1);
            statement.setInt(3, x2);
            statement.setInt(4, y1);
            statement.setInt(5, y2);
            statement.setInt(6, z1);
            statement.setInt(7, z2);

            return resolveProtections(statement);
        });
    }

    /**
     * Load protections by a player if he is the owner of the protection or has expicit permission to it.
     * This requires a full table scan so can be slow.
     *
     * @param player
     * @return
     */
    public List<Protection> loadProtectionsByPlayerAlsoIfNotOwner(String player) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE owner = ? OR data LIKE ?");
            UUID uuid = UUIDRegistry.getUUID(player);
            String playerString = uuid != null ? uuid.toString() : player;
            statement.setString(1, playerString);
            statement.setString(2, "%\"" + playerString + "\"%");

            return resolveProtections(statement);
        });
    }

    /**
     * Load protections by a player
     *
     * @param player
     * @return
     */
    public List<Protection> loadProtectionsByPlayer(String player) {
        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE owner = ?");
            UUID uuid = UUIDRegistry.getUUID(player);
            statement.setString(1, uuid != null ? uuid.toString() : player);

            return resolveProtections(statement);
        });
    }

    /**
     * Load protections by a player
     *
     * @param player
     * @param start
     * @param count
     * @return
     */
    public List<Protection> loadProtectionsByPlayer(String player, int start, int count) {
        UUID uuid = UUIDRegistry.getUUID(player);
        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE owner = ? ORDER BY id DESC limit ?,?");
            statement.setString(1, uuid != null ? uuid.toString() : player);
            statement.setInt(2, start);
            statement.setInt(3, count);

            return resolveProtections(statement);
        });
    }

    /**
     * Register a protection
     *
     * @param block
     * @param type
     * @param world
     * @param player
     * @param data
     * @param x
     * @param y
     * @param z
     * @return
     */
    public Protection registerEntityProtection(Entity entity, Protection.Type type, String world, String player, String data, int x, int y, int z) {
        return registerProtection(EntityBlock.ENTITY_BLOCK_ID, type, world, player, data, x, y, z, entity.getUniqueId());
    }

    public Protection registerProtection(Material block, Protection.Type type, String world, String player, String data, int x, int y, int z) {
        int blockId = BlockMap.instance().registerOrGetId(block);
        return registerProtection(blockId, type, world, player, data, x, y, z, null);
    }

    private Protection registerProtection(int blockId, Protection.Type type, String world, String player, String data, int x, int y, int z, UUID entityId) {
        ProtectionCache cache = LWC.getInstance().getProtectionCache();

        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("INSERT INTO " + prefix + "protections (blockId, type, world, owner, password, x, y, z, entityid, date, last_accessed) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            statement.setInt(1, blockId);
            statement.setInt(2, type.ordinal());
            statement.setString(3, world);
            statement.setString(4, player);
            statement.setString(5, data);
            statement.setInt(6, x);
            statement.setInt(7, y);
            statement.setInt(8, z);
            statement.setString(9, entityId != null ? entityId.toString() : null);
            statement.setString(10, new Timestamp(new Date().getTime()).toString());
            statement.setLong(11, System.currentTimeMillis() / 1000L);

            statement.executeUpdate();
        });

        // We need to create the initial transaction for this protection
        // this transaction is viewable and modifiable during
        // POST_REGISTRATION
        if (entityId == null) {
            cache.remove(ProtectionCache.cacheKey(world, x, y, z));
        }

        Protection protection = entityId != null ? loadProtection(entityId, true) : loadProtection(world, x, y, z, true);
        protection.removeCache();

        // if history logging is enabled, create it
        if (LWC.getInstance().isHistoryEnabled()) {
            History transaction = protection.createHistoryObject();

            transaction.setPlayer(player);
            transaction.setType(History.Type.TRANSACTION);
            transaction.setStatus(History.Status.ACTIVE);

            // store the player that created the protection
            transaction.addMetaData("creator=" + player);

            // now sync the history object to the database
            transaction.saveNow();
        }

        // Cache it
        cache.addProtection(protection);
        protectionCount++;

        // return the newly created protection
        return protection;
    }

    /**
     * Sync a History object to the database or save a newly created one
     *
     * @param history
     */
    public void saveHistory(History history) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement;

            if (history.doesExist()) {
                statement = prepare("UPDATE " + prefix + "history SET protectionId = ?, player = ?, x = ?, y = ?, z = ?, type = ?, status = ?, metadata = ?, timestamp = ? WHERE id = ?");
            } else {
                statement = prepare("INSERT INTO " + prefix + "history (protectionId, player, x, y, z, type, status, metadata, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", true);
                history.setTimestamp(System.currentTimeMillis() / 1000L);
            }

            statement.setInt(1, history.getProtectionId());
            statement.setString(2, history.getPlayer());
            statement.setInt(3, history.getX());
            statement.setInt(4, history.getY());
            statement.setInt(5, history.getZ());
            statement.setInt(6, history.getType().ordinal());
            statement.setInt(7, history.getStatus().ordinal());
            statement.setString(8, history.getSafeMetaData());
            statement.setLong(9, history.getTimestamp());

            if (history.doesExist()) {
                statement.setInt(10, history.getId());
            }

            int affectedRows = statement.executeUpdate();

            // set the history id if inserting
            if (!history.doesExist()) {
                if (affectedRows > 0) {
                    ResultSet generatedKeys = statement.getGeneratedKeys();

                    // get the key inserted
                    if (generatedKeys.next()) {
                        history.setId(generatedKeys.getInt(1));
                    }

                    generatedKeys.close();
                }
            }
        });
    }

    /**
     * Invalid all history objects for a player
     *
     * @param player
     */
    public void invalidateHistory(String player) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("UPDATE " + prefix + "history SET status = ? WHERE player = ?");
            statement.setInt(1, History.Status.INACTIVE.ordinal());
            statement.setString(2, player);

            statement.executeUpdate();
        });
    }

    /**
     * Resolve 1 history object from the result set but do not close it
     *
     * @return
     */
    private History resolveHistory(History history, ResultSet set) throws SQLException {
        if (history == null) {
            return null;
        }

        int historyId = set.getInt("id");
        int protectionId = set.getInt("protectionId");
        int x = set.getInt("x");
        int y = set.getInt("y");
        int z = set.getInt("z");
        String player = set.getString("player");
        int type_ord = set.getInt("type");
        int status_ord = set.getInt("status");
        String[] metadata = set.getString("metadata").split(",");
        long timestamp = set.getLong("timestamp");

        History.Type type = History.Type.values()[type_ord];
        History.Status status = History.Status.values()[status_ord];

        history.setId(historyId);
        history.setProtectionId(protectionId);
        history.setType(type);
        history.setPlayer(player);
        history.setX(x);
        history.setY(y);
        history.setZ(z);
        history.setStatus(status);
        history.setMetaData(metadata);
        history.setTimestamp(timestamp);

        return history;
    }

    /**
     * Load all of the History objects for a given protection
     *
     * @param protection
     * @return
     */
    public List<History> loadHistory(Protection protection) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE protectionId = ? ORDER BY id DESC");
            statement.setInt(1, protection.getId());

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(protection.createHistoryObject(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Load all protection history that the given player created
     *
     * @param player
     * @return
     */
    public List<History> loadHistory(Player player) {
        return loadHistory(player.getName());
    }

    /**
     * Load all protection history that the given player created
     *
     * @param player
     * @return
     */
    public List<History> loadHistory(String player) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE LOWER(player) = LOWER(?) ORDER BY id DESC");
            statement.setString(1, player);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Load all protection history that has the given history id
     *
     * @param historyId
     * @return
     */
    public History loadHistory(int historyId) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return null;
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE id = ?");
            statement.setInt(1, historyId);

            ResultSet set = statement.executeQuery();
            History history = null;
            if (set.next()) {
                history = resolveHistory(new History(), set);
            }
            set.close();
            return history;
        });
    }

    /**
     * Load all protection history that the given player created for a given
     * page, getting count history items.
     *
     * @param player
     * @param start
     * @param count
     * @return
     */
    public List<History> loadHistory(Player player, int start, int count) {
        return loadHistory(player.getName(), start, count);
    }

    /**
     * Load all protection history that the given player created for a given
     * page, getting count history items.
     *
     * @param player
     * @param start
     * @param count
     * @return
     */
    public List<History> loadHistory(String player, int start, int count) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE LOWER(player) = LOWER(?) ORDER BY id DESC LIMIT ?,?");
            statement.setString(1, player);
            statement.setInt(2, start);
            statement.setInt(3, count);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Load all protection history
     *
     * @return
     */
    public List<History> loadHistory() {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history ORDER BY id DESC");
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Load all protection history for the given status
     *
     * @return
     */
    public List<History> loadHistory(History.Status status) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE status = ? ORDER BY id DESC");
            statement.setInt(1, status.ordinal());
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Load all of the history at the given location
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    public List<History> loadHistory(int x, int y, int z) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE x = ? AND y = ? AND z = ?");
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Load all of the history at the given location
     *
     * @param player
     * @param x
     * @param y
     * @param z
     * @return
     */
    public List<History> loadHistory(String player, int x, int y, int z) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE LOWER(player) = LOWER(?) AND x = ? AND y = ? AND z = ?");
            statement.setString(1, player);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Load all protection history
     *
     * @return
     */
    public List<History> loadHistory(int start, int count) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return Collections.emptyList();
        }

        return runAndThrowModuleExceptionIfFailing(() -> {
            List<History> temp = new ArrayList<>();
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history ORDER BY id DESC LIMIT ?,?");
            statement.setInt(1, start);
            statement.setInt(2, count);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
            return temp;
        });
    }

    /**
     * Save a protection to the database
     *
     * @param protection
     */
    public void saveProtection(Protection protection) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("REPLACE INTO " + prefix + "protections (id, type, blockId, world, data, owner, password, x, y, z, entityid, date, last_accessed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            statement.setInt(1, protection.getId());
            statement.setInt(2, protection.getType().ordinal());
            statement.setInt(3, protection.getBlockId());
            statement.setString(4, protection.getWorld());
            JSONObject data = protection.getData();
            statement.setString(5, data == null ? null : data.toJSONString());
            statement.setString(6, protection.getOwner());
            statement.setString(7, protection.getPassword());
            statement.setInt(8, protection.getX());
            statement.setInt(9, protection.getY());
            statement.setInt(10, protection.getZ());
            statement.setString(11, protection.getEntityId() != null ? protection.getEntityId().toString() : null);
            statement.setString(12, protection.getCreation());
            statement.setLong(13, protection.getLastAccessed());

            statement.executeUpdate();
        });
    }

    public void saveProtectionLastAccessed(Protection protection) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("UPDATE " + prefix + "protections SET last_accessed = ? WHERE id = ?");

            statement.setLong(1, protection.getLastAccessed());
            statement.setInt(2, protection.getId());

            statement.executeUpdate();
        });
    }

    /**
     * Free a chest from protection
     *
     * @param protectionId
     *            the protection Id
     */
    public void removeProtection(int protectionId) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("DELETE FROM " + prefix + "protections WHERE id = ?");
            statement.setInt(1, protectionId);

            int affected = statement.executeUpdate();

            if (affected >= 1) {
                protectionCount -= affected;
            }
        });

        // removeProtectionHistory(protectionId);
    }

    public void removeProtectionHistory(int protectionId) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("DELETE FROM " + prefix + "history WHERE protectionId = ?");
            statement.setInt(1, protectionId);

            statement.executeUpdate();
        });
    }

    public void removeHistory(int historyId) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement statement = prepare("DELETE FROM " + prefix + "history WHERE id = ?");
            statement.setInt(1, historyId);

            statement.executeUpdate();
        });
    }

    /**
     * Remove **<b>ALL</b>** all of the protections registered by LWC
     */
    public void removeAllProtections() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM " + prefix + "protections");
            protectionCount = 0;
            statement.close();
        });
    }

    /**
     * Attempt to create an index on the table
     *
     * @param table
     * @param indexName
     * @param columns
     */
    private void createIndex(String table, String indexName, String columns) {
        runAndIgnoreException(() -> {
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE INDEX" + (currentType == Type.SQLite ? " IF NOT EXISTS" : "") + " " + indexName + " ON " + prefix + table + " (" + columns + ")");
            statement.close();
        });
    }

    /**
     * Attempt to create an index on the table
     *
     * @param indexName
     */
    private void dropIndex(String table, String indexName) {
        runAndIgnoreException(() -> {
            Statement statement = connection.createStatement();
            if (currentType == Type.SQLite) {
                statement.executeUpdate("DROP INDEX IF EXISTS " + indexName);
            } else {
                statement.executeUpdate("DROP INDEX " + indexName + " ON " + prefix + table);
            }
            statement.close();
        });
    }

    /**
     * 3.01
     */
    private void doUpdate301() {
        runAndThrowModuleExceptionIfFailing(() -> {
            // check limits table
            try {
                Statement statement = connection.createStatement();
                statement.executeQuery("SELECT * FROM limits LIMIT 1");
                statement.close();
            } catch (SQLException e) {
                return;
            }

            // Convert limits
            LWC lwc = LWC.getInstance();
            Module rawModule = lwc.getModuleLoader().getModule(LimitsModule.class);

            if (rawModule == null) {
                log("Failed to load the Limits module. Something is wrong!");
                return;
            }

            LimitsModule limits = (LimitsModule) rawModule;

            // start going through the database
            PreparedStatement statement = prepare("SELECT * FROM limits");

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                int type = result.getInt("type");
                int amount = result.getInt("amount");
                String entity = result.getString("entity");

                switch (type) {
                    // Global
                    case 2:
                        limits.set("master.type", "default");
                        limits.set("master.limit", amount);
                        break;

                    // Group
                    case 0:
                        limits.set("groups." + entity + ".type", "default");
                        limits.set("groups." + entity + ".limit", amount);
                        break;

                    // Player
                    case 1:
                        limits.set("players." + entity + ".type", "default");
                        limits.set("players." + entity + ".limit", amount);
                        break;
                }
            }
            
            limits.save();
            dropTable("limits");
        });
    }

    /**
     * 3.02
     */
    private void doUpdate302() {
        if (prefix == null || prefix.length() == 0) {
            return;
        }

        runAndThrowModuleExceptionIfFailing(() -> {
            // check for the table
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute("SELECT id FROM " + prefix + "protections limit 1");
            } catch (SQLException e) {
                // The table does not exist, let's go ahead and rename all of the
                // tables
                renameTable("protections", prefix + "protections");
                renameTable("rights", prefix + "rights");
                renameTable("menu_styles", prefix + "menu_styles");
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }

    /**
     * 3.30
     */
    private void doUpdate330() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute("SELECT last_accessed FROM " + prefix + "protections LIMIT 1");
            } catch (SQLException e) {
                addColumn(prefix + "protections", "last_accessed", "INTEGER");
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }

    /**
     * 4.0.0, update 2
     */
    private void doUpdate400_2() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute("SELECT id FROM " + prefix + "rights LIMIT 1");

                log("Migrating LWC3 rights to LWC4 format");

                // it exists ..!
                Statement stmt = connection.createStatement();
                ResultSet set = stmt.executeQuery("SELECT * FROM " + prefix + "rights");

                // keep a mini-cache of protections, max size of 100k should be OK!
                LRUCache<Integer, Protection> cache = new LRUCache<>(1000 * 100);

                while (set.next()) {
                    // load the data we will be using
                    int protectionId = set.getInt("chest");
                    String entity = set.getString("entity");
                    int access = set.getInt("rights");
                    int type = set.getInt("type");

                    // begin loading the protection
                    Protection protection = null;

                    // check cache
                    if (cache.containsKey(protectionId)) {
                        protection = cache.get(protectionId);
                    } else {
                        // else, load it...
                        protection = loadProtection(protectionId);

                        if (protection == null) {
                            continue;
                        }

                        cache.put(protectionId, protection);
                    }

                    if (protection == null) {
                        continue;
                    }

                    // create the permission
                    Permission permission = new Permission(entity, Permission.Type.values()[type], Permission.Access.values()[access]);

                    // add it to the protection and queue it for saving!
                    protection.addPermission(permission);
                }

                // Save all of the protections
                for (Protection protection : cache.values()) {
                    protection.saveNow();
                }

                // Good!
                set.close();
                stmt.close();

                // drop the rights table
                dropTable(prefix + "rights");
                precache();
            } catch (SQLException e) {
                // no need to convert!
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }

    /**
     * 4.0.0, update 4
     */
    private void doUpdate400_4() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute("SELECT data FROM " + prefix + "protections LIMIT 1");
            } catch (SQLException e) {
                dropColumn(prefix + "protections", "rights");
                addColumn(prefix + "protections", "data", "TEXT");
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }

    /**
     * 4.0.0, update 5
     */
    private void doUpdate400_5() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.executeQuery("SELECT flags FROM " + prefix + "protections LIMIT 1");

                // The flags column is still there ..!
                // instead of looping through every protection, let's do this a
                // better way
                PreparedStatement pStatement = prepare("SELECT * FROM " + prefix + "protections WHERE flags = 8"); // exempt

                for (Protection protection : resolveProtections(pStatement)) {
                    Flag flag = new Flag(Flag.Type.EXEMPTION);
                    protection.addFlag(flag);
                    protection.save();
                }

                pStatement = prepare("SELECT * FROM " + prefix + "protections WHERE flags = 3"); // redstone

                for (Protection protection : resolveProtections(pStatement)) {
                    Flag flag = new Flag(Flag.Type.MAGNET);
                    protection.addFlag(flag);
                    protection.save();
                }

                pStatement = prepare("SELECT * FROM " + prefix + "protections WHERE flags = 2"); // redstone

                for (Protection protection : resolveProtections(pStatement)) {
                    Flag flag = new Flag(Flag.Type.REDSTONE);
                    protection.addFlag(flag);
                    protection.save();
                }

                dropColumn(prefix + "protections", "flags");
            } catch (SQLException e) {

            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }

    /**
     * 4.0.0, update 6 (alpha7)
     */
    private void doUpdate400_6() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT x FROM " + prefix + "history LIMIT 1");
                rs.close();
            } catch (SQLException e) {
                // add x, y, z
                addColumn(prefix + "history", "x", "INTEGER");
                addColumn(prefix + "history", "y", "INTEGER");
                addColumn(prefix + "history", "z", "INTEGER");
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }

    /**
     * 5.0.12
     */
    private void doUpdate5_0_12() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                try {
                    ResultSet rs = statement.executeQuery("SELECT blockId FROM " + prefix + "protections LIMIT 1");
                    rs.close();
                } catch (SQLException e) {
                    return; // no protectiosn table, no update
                }
                ResultSet rs = statement.executeQuery("SELECT id FROM " + prefix + "blocks LIMIT 1");
                rs.close();
            } catch (SQLException e) {
                // create and initialize table
                LWC.getInstance().getPlugin().getLogger().info("Creating block mappings table");
                Table blockMappings = new Table(this, "blocks");
                {
                    Column column = new Column("id");
                    column.setType("INTEGER");
                    column.setPrimary(true);
                    column.setAutoIncrement(false);
                    blockMappings.add(column);

                    column = new Column("name");
                    column.setType("VARCHAR(40)");
                    blockMappings.add(column);
                }
                blockMappings.execute();

                statement.executeUpdate("UPDATE " + prefix + "protections SET blockId = -1 WHERE blockId IS NULL");
                ResultSet rs = statement.executeQuery("SELECT DISTINCT blockId FROM " + prefix + "protections");
                PreparedStatement insertSmt = prepare("INSERT INTO " + prefix + "blocks (`id`,`name`) VALUES (?, ?)");
                while (rs.next()) {
                    int id = rs.getInt("blockId");
                    if (id >= 0 && id != EntityBlock.ENTITY_BLOCK_ID) {
                        Material mat = Material.matchMaterial(Integer.toString(id));
                        if (mat != null) {
                            insertSmt.setInt(1, id);
                            insertSmt.setString(2, mat.name());
                            insertSmt.executeUpdate();
                        } else {
                            mergeBlockMapping(id, -1);
                        }
                    }
                }
                rs.close();
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }

    /**
     * Update from ModernLWC
     */
    private boolean doUpdateModernLWC() {
        return runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT DISTINCT blockName FROM " + prefix + "protections");
                LWC.getInstance().getPlugin().getLogger().info("Upgrading from ModernLWC");
                try {
                    PreparedStatement updateSmt = prepare("UPDATE " + prefix + "protections SET blockId = ? WHERE blockName = ?");
                    HashSet<String> typeMap = new HashSet<>();
                    typeMap.add("Entity");
                    for (EntityType e : EntityType.values()) {
                        typeMap.add(e.name());
                    }
                    while (rs.next()) {
                        String blockName = rs.getString(1);
                        if (typeMap.contains(blockName)) {
                            updateSmt.setInt(1, EntityBlock.ENTITY_BLOCK_ID);
                            updateSmt.setString(2, blockName);
                            updateSmt.executeUpdate();
                        }
                    }
                    rs.close();
                    statement.executeUpdate("ALTER TABLE " + prefix + "protections DROP COLUMN blockName");
                    statement.executeUpdate("UPDATE " + prefix + "protections SET blockId = -1 WHERE blockId IS NULL");
                } catch (SQLException e) {
                    printException(e);
                }
            } catch (SQLException e) {
                return false; // column does not exist, ignore
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
            return true;
        });
    }

    /**
     * Optimize some columns
     */
    private void doUpdatesDatabaseVersion7() {
        runAndIgnoreException(() -> {
            Statement statement = connection.createStatement();
            statement.executeUpdate("ALTER TABLE `" + prefix + "protections` CHANGE `owner` `owner` VARCHAR(36)");
            statement.executeUpdate("ALTER TABLE `" + prefix + "protections` CHANGE `world` `world` VARCHAR(50)");
            statement.executeUpdate("ALTER TABLE `" + prefix + "protections` CHANGE `date` `date` VARCHAR(50)");
            statement.executeUpdate("ALTER TABLE `" + prefix + "history` CHANGE `owner` `owner` VARCHAR(36)");
            statement.close();
        });
        runAndIgnoreException(() -> {
            dropColumn(prefix + "protections", "rights");
        });
    }
    
    /**
     * Add entityid column
     */
    private void doUpdatesDatabaseVersion8() {
        runAndThrowModuleExceptionIfFailing(() -> {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute("SELECT entityid FROM " + prefix + "protections LIMIT 1");
            } catch (SQLException e) {
                addColumn(prefix + "protections", "entityid", "VARCHAR(36) AFTER `z`");
                createIndex("protections", "protections_entity", "entityid");
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        });
    }
    

    public HashMap<Integer, String> loadBlockMappings() {
        return runAndThrowModuleExceptionIfFailing(() -> {
            HashMap<Integer, String> rv = new HashMap<>();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT `id`,`name` FROM " + prefix + "blocks");
            while (rs.next()) {
                rv.put(rs.getInt(1), rs.getString(2));
            }
            statement.close();
            return rv;
        });
    }

    public void addBlockMapping(int id, String name) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement insertSmt = prepare("INSERT INTO " + prefix + "blocks (`id`,`name`) VALUES (?, ?)");
            insertSmt.setInt(1, id);
            insertSmt.setString(2, name);
            insertSmt.executeUpdate();
        });
    }

    public void updateBlockMappingName(int id, String name) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement insertSmt = prepare("UPDATE " + prefix + "blocks SET name = ? WHERE id = ?");
            insertSmt.setString(1, name);
            insertSmt.setInt(2, id);
            insertSmt.executeUpdate();
        });
    }

    public void mergeBlockMapping(int oldid, int newid) {
        runAndThrowModuleExceptionIfFailing(() -> {
            PreparedStatement updateSmt = prepare("UPDATE " + prefix + "protections SET blockId = ? WHERE blockId = ?");
            updateSmt.setInt(1, newid);
            updateSmt.setInt(2, oldid);
            int updated = updateSmt.executeUpdate();
            LWC.getInstance().getPlugin().getLogger().info("Updated " + updated + " protections!");

            PreparedStatement insertSmt = prepare("DELETE FROM " + prefix + "blocks WHERE id = ?");
            insertSmt.setInt(1, oldid);
            insertSmt.executeUpdate();
        });
    }

    public List<Protection> streamDeleteProtections(String where, CommandSender sender) {
        int totalProtections = getProtectionCount();
        ArrayList<Protection> resultList = runAndThrowModuleExceptionIfFailing(() -> {
            Statement resultStatement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            if (getType() == Database.Type.MySQL) {
                resultStatement.setFetchSize(Integer.MIN_VALUE);
            }

            ResultSet result = resultStatement.executeQuery("SELECT id, owner, type, x, y, z, entityid, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections" + where);

            List<Integer> exemptedBlocks = LWC.getInstance().getConfiguration().getIntList("optional.exemptBlocks", new ArrayList<Integer>());
            // int completed = 0;
            int count = 0;
            ArrayList<Protection> toRemove = new ArrayList<>();
            List<Integer> toRemoveIds = new ArrayList<>();
            while (result.next()) {
                Protection protection = resolveProtection(result);
                World world = protection.getBukkitWorld();

                // check if the protection is exempt from being removed
                if (protection.hasFlag(Flag.Type.EXEMPTION)
                        || exemptedBlocks.contains(protection.getBlockId())) {
                    continue;
                }

                count++;

                if (count % 100000 == 0 || count == totalProtections
                        || count == 1) {
                    sender.sendMessage(Colors.Red + count + " / " + totalProtections);
                }

                if (world == null) {
                    continue;
                }

                // remove the protection
                toRemove.add(protection);
                toRemoveIds.add(protection.getId());

                // completed++;
            }

            // Close the streaming statement
            result.close();
            resultStatement.close();
            
            // delete protections
            
            StringBuilder deleteProtectionsQuery = new StringBuilder();
            StringBuilder deleteHistoryQuery = new StringBuilder();
            int total = toRemove.size();
            count = 0;

            // iterate over the items to remove
            Iterator<Integer> iter = toRemoveIds.iterator();

            // create the statement to use
            Statement statement = connection.createStatement();

            while (iter.hasNext()) {
                int protectionId = iter.next();

                if (count % 10000 == 0) {
                    deleteProtectionsQuery.append("DELETE FROM ").append(prefix)
                            .append("protections WHERE id IN (")
                            .append(protectionId);
                    deleteHistoryQuery
                            .append("UPDATE ")
                            .append(prefix)
                            .append("history SET status = "
                                    + History.Status.INACTIVE.ordinal()
                                    + " WHERE protectionId IN(")
                            .append(protectionId);
                } else {
                    deleteProtectionsQuery.append(",").append(protectionId);
                    deleteHistoryQuery.append(",").append(protectionId);
                }

                if (count % 10000 == 9999 || count == (total - 1)) {
                    deleteProtectionsQuery.append(")");
                    deleteHistoryQuery.append(")");
                    statement.executeUpdate(deleteProtectionsQuery.toString());
                    statement.executeUpdate(deleteHistoryQuery.toString());
                    deleteProtectionsQuery.setLength(0);
                    deleteHistoryQuery.setLength(0);

                    sender.sendMessage(Colors.Green + "REMOVED " + (count + 1)
                            + " / " + total);
                }

                count++;
            }

            statement.close();
            
            return toRemove;
        });
        protectionCount -= resultList.size();
        return resultList;
    }

    public void batchDeleteProtections(ArrayDeque<Integer> protectionsToRemove) {
        if (protectionsToRemove.isEmpty()) {
            return;
        }
        runAndThrowModuleExceptionIfFailing(() -> {
            // create the statement to use
            Statement statement = connection.createStatement();
            final StringBuilder builder = new StringBuilder();

            int count = 0;
            while (!protectionsToRemove.isEmpty()) {
                int protectionId = protectionsToRemove.removeFirst();
                if (count == 0) {
                    builder.append("DELETE FROM ").append(prefix).append("protections WHERE id IN (").append(protectionId);
                } else {
                    builder.append(",").append(protectionId);
                }
                count++;
            }
            builder.append(")");
            statement.executeUpdate(builder.toString());
            statement.close();
        });
    }
}
