package joshie.vbc.config;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;


public class VillagerGlobalTickCache {
    private static final int NODE_TYPE_SLOTS = 4;
    private static final PathType[] PATH_TYPES = PathType.values();
    private static final long[] nodeTypeSlotSignatures = new long[NODE_TYPE_SLOTS];
    private static final boolean[] nodeTypeSlotUsed = new boolean[NODE_TYPE_SLOTS];
    private static final Long2ByteMap[] nodeTypeSlotCaches = new Long2ByteMap[NODE_TYPE_SLOTS];
    private static int nodeTypeSlotCursor;

    static {
        for (int i = 0; i < NODE_TYPE_SLOTS; i++) {
            nodeTypeSlotCaches[i] = new Long2ByteOpenHashMap(1024);
        }
    }

    private static volatile Long2ObjectMap<float[]>[] partCaches = new Long2ObjectMap[0];
    
    private static volatile long currentTick = -1;
    
    public static int malusHits = 0;
    public static int malusMisses = 0;
    public static int nodeTypeHits = 0;
    public static int nodeTypeMisses = 0;
    /** Never reset by the 50 ms window, so tools can measure a rate across windows. */
    public static long malusCallsTotal = 0L;
    

    public static float[] getPenaltyParts(BlockPos pos, int professionIndex, java.util.function.Supplier<float[]> computer) {
        long posKey = pos.asLong();
        
        ensureTickValid();
        malusCallsTotal++;
        
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

    public static long nodeTypeSignature(Mob mob, boolean canOpenDoors, boolean canPassDoors) {
        int width = Mth.floor(mob.getBbWidth() + 1.0F);
        int height = Mth.floor(mob.getBbHeight() + 1.0F);
        long shape = ((long) (width & 7) << 9)
                | ((long) (height & 7) << 6)
                | ((long) (width & 7) << 3)
                | (canOpenDoors ? 2L : 0L)
                | (canPassDoors ? 1L : 0L);
        return ((long) mob.getType().hashCode() << 16) | shape;
    }

    private static int slotFor(long signature) {
        for (int i = 0; i < NODE_TYPE_SLOTS; i++) {
            if (nodeTypeSlotUsed[i] && nodeTypeSlotSignatures[i] == signature) {
                return i;
            }
        }
        return -1;
    }

    private static int claimSlot(long signature) {
        for (int i = 0; i < NODE_TYPE_SLOTS; i++) {
            if (!nodeTypeSlotUsed[i]) {
                nodeTypeSlotUsed[i] = true;
                nodeTypeSlotSignatures[i] = signature;
                return i;
            }
        }
        int slot = nodeTypeSlotCursor++ & (NODE_TYPE_SLOTS - 1);
        nodeTypeSlotCaches[slot].clear();
        nodeTypeSlotSignatures[slot] = signature;
        return slot;
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
        for (int i = 0; i < NODE_TYPE_SLOTS; i++) {
            nodeTypeSlotCaches[i].clear();
            nodeTypeSlotUsed[i] = false;
            nodeTypeSlotSignatures[i] = 0L;
        }
        nodeTypeSlotCursor = 0;
        
        malusHits = 0;
        malusMisses = 0;
        nodeTypeHits = 0;
        nodeTypeMisses = 0;
    }
    

    private static long getCurrentTick() {
        return System.currentTimeMillis() / 50;
    }

    public static PathType getCachedNodeType(long signature, long posKey) {
        ensureTickValid();
        int slot = slotFor(signature);
        if (slot < 0) {
            nodeTypeMisses++;
            return null;
        }
        Long2ByteMap cache = nodeTypeSlotCaches[slot];
        if (cache.containsKey(posKey)) {
            nodeTypeHits++;
            return PATH_TYPES[cache.get(posKey)];
        }
        nodeTypeMisses++;
        return null;
    }

    public static void cacheNodeType(long signature, long posKey, PathType pathType) {
        if (pathType == null) {
            return;
        }
        ensureTickValid();
        int slot = slotFor(signature);
        if (slot < 0) {
            slot = claimSlot(signature);
        }
        nodeTypeSlotCaches[slot].put(posKey, (byte) pathType.ordinal());
    }

    public static CacheStats getStats() {
        int malusSize = 0;
        for (Long2ObjectMap<float[]> cache : partCaches) {
            malusSize += cache.size();
        }
        int nodeTypeSize = 0;
        for (Long2ByteMap cache : nodeTypeSlotCaches) {
            nodeTypeSize += cache.size();
        }
        return new CacheStats(
            malusSize, nodeTypeSize,
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