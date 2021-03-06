package com.griefcraft.cache;

import java.util.Objects;

public final class CacheKey {
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final int hashCode;

    public CacheKey(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hashCode = ((Objects.hashCode(world) * 17 + x) * 23 + y) * 43 + z;
    }

    public final int hashCode() {
        return hashCode;
    }

    public final boolean equals(Object obj) {
        if (obj.getClass() != CacheKey.class) {
            return false;
        }
        CacheKey o = (CacheKey) obj;
        return Objects.equals(o.world, world) && o.x == x && o.y == y && o.z == z;
    }
}
