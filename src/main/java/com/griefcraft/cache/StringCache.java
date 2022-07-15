package com.griefcraft.cache;

import java.util.concurrent.ConcurrentHashMap;

public class StringCache {
    private static final ConcurrentHashMap<String, String> VALUES = new ConcurrentHashMap<>();

    public static String intern(String s) {
        return s == null ? null : VALUES.computeIfAbsent(s, (str) -> str);
    }
}
