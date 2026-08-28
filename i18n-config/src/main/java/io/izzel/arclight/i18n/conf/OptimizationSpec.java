package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Collections;
import java.util.Map;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class OptimizationSpec {

    @Setting("cache-plugin-class")
    private boolean cachePluginClass;

    @Setting("goal-selector-update-interval")
    private int goalSelectorInterval;

    @Setting("max-entities-per-chunk")
    private Map<String, Integer> maxEntitiesPerChunk;

    @Setting("chunk-load-entity-cull")
    private boolean chunkLoadEntityCull;

    @Setting("ping-cache-seconds")
    private int pingCacheSeconds;

    @Setting("tick-armor-stands")
    private boolean tickArmorStands = true;

    @Setting("async-scheduler-max-threads")
    private int asyncSchedulerMaxThreads = -1;

    @Setting("spawn-census-interval")
    private int spawnCensusInterval = 4;

    @Setting("cache-block-entity-valid")
    private boolean cacheBlockEntityValid;

    @Setting("lag-compensation")
    private LagCompensationSpec lagCompensation = new LagCompensationSpec();

    @Setting("dab")
    private DabSpec dab = new DabSpec();

    @Setting("disable-block-physics-event")
    private boolean disableBlockPhysicsEvent;

    @Setting("use-activation-and-tracking-range")
    private boolean useActivationAndTrackingRange;

    public boolean useActivationAndTrackingRange() {
        return useActivationAndTrackingRange;
    }

    public boolean isCachePluginClass() {
        return cachePluginClass;
    }

    public int getGoalSelectorInterval() {
        return goalSelectorInterval;
    }

    public boolean isDisableBlockPhysicsEvent() {
        return disableBlockPhysicsEvent;
    }

    public DabSpec getDab() {
        return dab == null ? new DabSpec() : dab;
    }

    public LagCompensationSpec getLagCompensation() {
        return lagCompensation == null ? new LagCompensationSpec() : lagCompensation;
    }

    public Map<String, Integer> getMaxEntitiesPerChunk() {
        return maxEntitiesPerChunk == null ? Collections.emptyMap() : maxEntitiesPerChunk;
    }

    public boolean isChunkLoadEntityCull() {
        return chunkLoadEntityCull;
    }

    public int getPingCacheSeconds() {
        return Math.max(0, pingCacheSeconds);
    }

    public boolean isTickArmorStands() {
        return tickArmorStands;
    }

    public int getAsyncSchedulerMaxThreads() {
        return asyncSchedulerMaxThreads;
    }

    public int getSpawnCensusInterval() {
        return Math.max(1, spawnCensusInterval);
    }

    public boolean isCacheBlockEntityValid() {
        return cacheBlockEntityValid;
    }
}
