package joshie.vbc.config;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;


public class VillagerGlobalTickCache {
    private static volatile Long2ObjectMap<float[]>[] partCaches = new Long2ObjectMap[0];
    public static final Long2ByteMap nodeTypeCache = new Long2ByteOpenHashMap(4096);
    
    private static volatile long currentTick = -1;
    
    public static int malusHits = 0;
    public static int malusMisses = 0;
    public static int nodeTypeHits = 0;
    public static int nodeTypeMisses = 0;
    

    public static float[] getPenaltyParts(BlockPos pos, int professionIndex, java.util.function.Supplier<float[]> computer) {
        long posKey = pos.asLong();
        
        ensureTickValid();
        
        Long2ObjectMap<float[]> cache = cache(professionIndex);
        
        float[] parts = cache.get(posKey);
        if (parts != null) {
            malusHits++;
            return parts;
        }
        
        parts = computer.get();
        cache.put(posKey, parts);
        malusMisses++;
        
        return parts;
    }

    private static Long2ObjectMap<float[]> cache(int professionIndex) {
        int slot = professionIndex + 1;
        Long2ObjectMap<float[]>[] caches = partCaches;
        if (slot < caches.length) {
            return caches[slot];
        }
        synchronized (VillagerGlobalTickCache.class) {
            caches = partCaches;
            if (slot < caches.length) {
                return caches[slot];
            }
            Long2ObjectMap<float[]>[] grown = new Long2ObjectMap[slot + 1];
            System.arraycopy(caches, 0, grown, 0, caches.length);
            for (int i = caches.length; i < grown.length; i++) {
                grown[i] = new Long2ObjectOpenHashMap<>(256);
            }
            partCaches = grown;
            return grown[slot];
        }
    }

    public static PathType getNodeType(int x, int y, int z, java.util.function.Supplier<PathType> computer) {
        long posKey = BlockPos.asLong(x, y, z);
        
        ensureTickValid();
        
        if (nodeTypeCache.containsKey(posKey)) {
            nodeTypeHits++;
            byte encoded = nodeTypeCache.get(posKey);
            return PathType.values()[encoded];
        }
        
        PathType result = computer.get();
        if (result != null) {
            nodeTypeCache.put(posKey, (byte) result.ordinal());
            nodeTypeMisses++;
        }
        
        return result;
    }
    

    public static void ensureTickValid() {
        long tick = getCurrentTick();
        if (currentTick != tick) {
            reset();
            currentTick = tick;
        }
    }
    

    public static void reset() {
        for (Long2ObjectMap<float[]> cache : partCaches) {
            cache.clear();
        }
        nodeTypeCache.clear();
        
        malusHits = 0;
        malusMisses = 0;
        nodeTypeHits = 0;
        nodeTypeMisses = 0;
    }
    

    private static long getCurrentTick() {
        return System.currentTimeMillis() / 50;
    }

    public static boolean hasNodeTypeCached(long posKey) {
        ensureTickValid();
        return nodeTypeCache.containsKey(posKey);
    }

    public static PathType getCachedNodeType(long posKey) {
        if (nodeTypeCache.containsKey(posKey)) {
            nodeTypeHits++;
            byte encoded = nodeTypeCache.get(posKey);
            return PathType.values()[encoded];
        }
        return null;
    }

    public static void cacheNodeType(long posKey, PathType pathType) {
        if (pathType != null) {
            ensureTickValid();
            nodeTypeCache.put(posKey, (byte) pathType.ordinal());
            nodeTypeMisses++;
        }
    }

    public static CacheStats getStats() {
        int malusSize = 0;
        for (Long2ObjectMap<float[]> cache : partCaches) {
            malusSize += cache.size();
        }
        return new CacheStats(
            malusSize, nodeTypeCache.size(),
            malusHits, malusMisses, nodeTypeHits, nodeTypeMisses,
            currentTick
        );
    }
    
    public static class CacheStats {
        public final int malusCacheSize;
        public final int nodeTypeCacheSize;
        public final int malusHits;
        public final int malusMisses;
        public final int nodeTypeHits;
        public final int nodeTypeMisses;
        public final long currentTick;
        
        CacheStats(int malusCacheSize, int nodeTypeCacheSize, 
                  int malusHits, int malusMisses, int nodeTypeHits, int nodeTypeMisses, 
                  long currentTick) {
            this.malusCacheSize = malusCacheSize;
            this.nodeTypeCacheSize = nodeTypeCacheSize;
            this.malusHits = malusHits;
            this.malusMisses = malusMisses;
            this.nodeTypeHits = nodeTypeHits;
            this.nodeTypeMisses = nodeTypeMisses;
            this.currentTick = currentTick;
        }
        
        public double getMalusHitRate() {
            int total = malusHits + malusMisses;
            return total > 0 ? (malusHits * 100.0 / total) : 0.0;
        }
        
        public double getNodeTypeHitRate() {
            int total = nodeTypeHits + nodeTypeMisses;
            return total > 0 ? (nodeTypeHits * 100.0 / total) : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "GlobalTickCache[malus=%d(%.1f%%), nodeType=%d(%.1f%%), tick=%d]",
                malusCacheSize, getMalusHitRate(), 
                nodeTypeCacheSize, getNodeTypeHitRate(), 
                currentTick
            );
        }
    }
} 