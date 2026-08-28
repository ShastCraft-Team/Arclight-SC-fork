package io.izzel.arclight.common.bridge.core.world.level.entity;

import net.minecraft.world.entity.EntityType;

public interface PersistentEntitySectionManagerBridge {

    /**
     * Считает сущности заданного типа в чанке без аллокаций.
     * Используется лимитом {@code optimization.max-entities-per-chunk}.
     */
    int bridge$countEntitiesOfType(long chunkPos, EntityType<?> type);
}
