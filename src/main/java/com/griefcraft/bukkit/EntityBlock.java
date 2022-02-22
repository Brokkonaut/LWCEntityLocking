package com.griefcraft.bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class EntityBlock implements Block {
    public static final int ENTITY_BLOCK_ID = 5000;
    public static final int POSITION_OFFSET = 50000;
    private Entity entity;
    public String version;

    public EntityBlock(Entity entity) {
        this.entity = entity;
    }

    @Override
    public int getX() {
        return POSITION_OFFSET + this.entity.getUniqueId().hashCode();
    }

    @Override
    public int getY() {
        return POSITION_OFFSET + this.entity.getUniqueId().hashCode();
    }

    @Override
    public int getZ() {
        return POSITION_OFFSET + this.entity.getUniqueId().hashCode();
    }

    @Override
    public World getWorld() {
        return this.entity.getWorld();
    }

    public Entity getEntity() {
        return this.entity;
    }

    public static Block getEntityBlock(Entity entity) {
        return new EntityBlock(entity);
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return null;
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return false;
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
    }

    @Override
    public byte getData() {
        return 0;
    }

    @Override
    public Block getRelative(int modX, int modY, int modZ) {
        return null;
    }

    @Override
    public Block getRelative(BlockFace face) {
        return null;
    }

    @Override
    public Block getRelative(BlockFace face, int distance) {
        return null;
    }

    @Override
    public Material getType() {
        return Material.AIR;
    }

    @Override
    public byte getLightLevel() {
        return 0;
    }

    @Override
    public byte getLightFromSky() {
        return 0;
    }

    @Override
    public byte getLightFromBlocks() {
        return 0;
    }

    @Override
    public Location getLocation() {
        return entity.getLocation();
    }

    @Override
    public Location getLocation(Location loc) {
        return entity.getLocation(loc);
    }

    @Override
    public Chunk getChunk() {
        return getLocation().getChunk();
    }

    @Override
    public void setType(Material type) {
    }

    @Override
    public void setType(Material type, boolean applyPhysics) {
    }

    @Override
    public BlockData getBlockData() {
        return null;
    }

    @Override
    public void setBlockData(BlockData data) {
    }

    @Override
    public void setBlockData(BlockData data, boolean applyPhysics) {
    }

    @Override
    public BlockFace getFace(Block block) {
        return null;
    }

    @Override
    public BlockState getState() {
        return null;
    }

    public BlockState getState(boolean create) {
        return null;
    }

    @Override
    public Biome getBiome() {
        return null;
    }

    @Override
    public void setBiome(Biome bio) {

    }

    @Override
    public boolean isBlockPowered() {
        return false;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    @Override
    public boolean isBlockFacePowered(BlockFace face) {
        return false;
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return false;
    }

    @Override
    public int getBlockPower(BlockFace face) {
        return 0;
    }

    @Override
    public int getBlockPower() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isLiquid() {
        return false;
    }

    @Override
    public double getTemperature() {
        return 0;
    }

    @Override
    public double getHumidity() {
        return 0;
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    @Override
    public boolean breakNaturally() {
        return false;
    }

    @Override
    public boolean breakNaturally(ItemStack tool) {
        return false;
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool) {
        return Collections.emptyList();
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    @Override
    public RayTraceResult rayTrace(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode) {
        return null;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool, Entity entity) {
        return Collections.emptyList();
    }
}
