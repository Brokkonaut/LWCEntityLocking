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

package com.griefcraft.cache;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class ProtectionCache {

    /**
     * The LWC instance this set belongs to
     */
    private final LWC lwc;

    /**
     * All protections by location
     */
    private final Map<CacheKey, Protection> byLocation;

    /**
     * All protections by id
     */
    private final Map<Integer, Protection> byId;
    
    private final LRUCache<CacheKey, Object> knownNullByLocation;

    /**
     * The method counter
     */
    private final MethodCounter counter = new MethodCounter();

    /**
     * Used for byKnownNulls
     */
    private final static Object FAKE_VALUE = new Object();

    public ProtectionCache(LWC lwc) {
        this.lwc = lwc;

        this.byLocation = new HashMap<>();
        this.byId = new HashMap<>();
        this.knownNullByLocation = new LRUCache<>(500000);
    }

    /**
     * Get the method counter for this class
     *
     * @return
     */
    public MethodCounter getMethodCounter() {
        return counter;
    }

    /**
     * Clears the entire protection cache
     */
    public void clear() {
        byLocation.clear();
        byId.clear();
    }

    /**
     * Gets the amount of protections that are loaded
     *
     * @return
     */
    public int size() {
        return byId.size();
    }

    /**
     * Cache a protection
     *
     * @param protection
     */
    public void addProtection(Protection protection) {
        if (protection == null) {
            return;
        }

        counter.increment("addProtection");

        knownNullByLocation.remove(protection.getCacheKey());
        byLocation.put(protection.getCacheKey(), protection);
        byId.put(protection.getId(), protection);
        protection.clearAdditionalProtectedBlocks();
    }

    /**
     * Remove the protection from the cache
     *
     * @param protection
     */
    public void removeProtection(Protection protection) {
        counter.increment("removeProtection");

        byId.remove(protection.getId());
        byLocation.remove(protection.getCacheKey());
        if (protection.getAdditionalProtectedBlocks() != null) {
            for (CacheKey other : protection.getAdditionalProtectedBlocks()) {
                byLocation.remove(other);
                protection.clearAdditionalProtectedBlocks();
            }
        }
    }

    /**
     * Remove the given cache key from any caches
     *
     * @param cacheKey
     */
    public void remove(CacheKey cacheKey) {
        Protection removed = byLocation.remove(cacheKey);
        if(removed != null) {
            byId.remove(removed.getId());
        }
        knownNullByLocation.remove(cacheKey);
    }

    /**
     * Make a cache key known as null in the cache
     *
     * @param cacheKey
     */
    public void addKnownNull(CacheKey cacheKey) {
        counter.increment("addKnownNull");
        knownNullByLocation.put(cacheKey, FAKE_VALUE);
    }

    /**
     * Check if a cache key is known to not exist in the database
     *
     * @param cacheKey
     * @return
     */
    public boolean isKnownNull(CacheKey cacheKey) {
        counter.increment("isKnownNull");
        return knownNullByLocation.containsKey(cacheKey);
    }

    /**
     * Get a protection in the cache via its cache key
     *
     * @param cacheKey
     * @return
     */
    public Protection getProtection(CacheKey cacheKey) {
        counter.increment("getProtection");
        return byLocation.get(cacheKey);
    }

    /**
     * Get a protection in the cache located on the given block
     *
     * @param block
     * @return
     */
    public Protection getProtection(Block block) {
        return getProtection(cacheKey(block));
    }

    /**
     * Get a protection in the cache via its id
     *
     * @param id
     * @return
     */
    public Protection getProtectionById(int id) {
        counter.increment("getProtectionById");
        return byId.get(id);
    }

    /**
     * Gets the cache key for the given location
     *
     * @param location
     * @return
     */
    public static CacheKey cacheKey(Location location) {
        return cacheKey(location.getWorld().getName(), location.getBlockX(),
                location.getBlockY(), location.getBlockZ());
    }

    /**
     * Generate a cache key using the given data
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static CacheKey cacheKey(String world, int x, int y, int z) {
        return new CacheKey(world, x, y, z);
    }

    public static CacheKey cacheKey(Block block) {
        return new CacheKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public LWC getLwc() {
        return lwc;
    }

}
