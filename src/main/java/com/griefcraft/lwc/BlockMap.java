package com.griefcraft.lwc;

import com.google.common.base.Preconditions;
import com.griefcraft.bukkit.EntityBlock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import org.bukkit.Material;

public class BlockMap {
    private static BlockMap instance = new BlockMap();

    private HashMap<Material, Integer> blockToIdMap = new HashMap<>();

    private Material[] idToBlockMap = new Material[64];

    public static BlockMap instance() {
        return instance;
    }

    private BlockMap() {

    }

    public void init() {
        blockToIdMap.clear();
        idToBlockMap = new Material[64];
        HashMap<Integer, String> existingMappings = LWC.getInstance().getPhysicalDatabase().loadBlockMappings();
        HashMap<String, Integer> inverseMappings = new HashMap<>();
        for (Entry<Integer, String> e : existingMappings.entrySet()) {
            inverseMappings.put(e.getValue(), e.getKey());
        }
        for (Entry<Integer, String> e : existingMappings.entrySet()) {
            int id = e.getKey();
            String name = e.getValue();
            Material mat = Material.getMaterial(name);
            if (mat == null) {
                // try to convert to 1.13 materials
                mat = Material.getMaterial(name, true);
                if (name.equals("BURNING_FURNACE")) {
                    mat = Material.FURNACE;
                } else if (name.equals("STANDING_BANNER")) {
                    mat = Material.AIR;
                }
                if (mat == null || mat == Material.AIR) {
                    // -1 = unknown, will be updated on next protection access
                    if (mat == null) {
                        LWC.getInstance().getPlugin().getLogger().severe("Invalid block mapping: " + name);
                    } else {
                        LWC.getInstance().getPlugin().getLogger().info("Updating block mapping from " + name + " to UNKNOWN");
                    }
                    LWC.getInstance().getPhysicalDatabase().mergeBlockMapping(id, -1);
                    inverseMappings.remove(name);
                    continue;
                } else {
                    LWC.getInstance().getPlugin().getLogger().info("Updating block mapping from " + name + " to " + mat.name());
                    Integer mergeId = inverseMappings.get(mat.name());
                    if (mergeId != null) {
                        // we have a material collision (several materials to one) -> merge them
                        LWC.getInstance().getPlugin().getLogger().info("Merging block mapping with " + mergeId);
                        LWC.getInstance().getPhysicalDatabase().mergeBlockMapping(id, mergeId.intValue());
                        inverseMappings.remove(name);
                        continue;
                    } else {
                        // material was renamed
                        LWC.getInstance().getPhysicalDatabase().updateBlockMappingName(id, mat.name());
                        inverseMappings.remove(name);
                        inverseMappings.put(mat.name(), id);
                    }
                }
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
        Preconditions.checkNotNull(mat, "mat may not be null");
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
