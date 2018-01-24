package com.griefcraft.lwc;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map.Entry;

import org.bukkit.Material;

import com.google.common.base.Preconditions;
import com.griefcraft.bukkit.EntityBlock;

public class BlockMap {
    private static BlockMap instance = new BlockMap();

    private EnumMap<Material, Integer> blockToIdMap = new EnumMap<>(Material.class);

    private Material[] idToBlockMap = new Material[64];

    public static BlockMap instance() {
        return instance;
    }

    private BlockMap() {

    }

    public void init() {
        blockToIdMap.clear();
        idToBlockMap = new Material[64];
        for (Entry<Integer, String> e : LWC.getInstance().getPhysicalDatabase().loadBlockMappings().entrySet()) {
            int id = e.getKey();
            String name = e.getValue();
            Material mat = Material.matchMaterial(name);
            if (mat == null) {
                System.out.println("Invalid block mapping: " + name);
                continue;
            }
            internalAddMapping(id, mat);
        }
    }

    private void internalAddMapping(int id, Material mat) {
        blockToIdMap.put(mat, id);
        if (idToBlockMap.length <= id) {
            idToBlockMap = Arrays.copyOf(idToBlockMap, Math.max(idToBlockMap.length * 2, id + 1));
        }
        idToBlockMap[id] = mat;
    }

    public int getId(Material mat) {
        Integer val = blockToIdMap.get(mat);
        return val == null ? -1 : val;
    }

    public Material getMaterial(int id) {
        return id >= 0 && id < idToBlockMap.length ? idToBlockMap[id] : null;
    }

    public int registerOrGetId(Material mat) {
        Preconditions.checkNotNull(mat,"mat may not be null");
        Integer val = blockToIdMap.get(mat);
        if (val != null) {
            return val;
        }
        int id = 1;
        while (getMaterial(id) != null || id == EntityBlock.ENTITY_BLOCK_ID) {
            id++;
        }
        internalAddMapping(id, mat);
        LWC.getInstance().getPhysicalDatabase().addBlockMapping(id, mat.name());
        return id;
    }
}
