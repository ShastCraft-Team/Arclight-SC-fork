package io.izzel.arclight.common.mixin.core.world.level.entity;

import io.izzel.arclight.common.bridge.core.world.level.entity.PersistentEntitySectionManagerBridge;
import io.izzel.arclight.common.mod.util.EntityChunkCap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import org.apache.logging.log4j.LogManager;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> implements PersistentEntitySectionManagerBridge {

    // @formatter:off
    @Shadow public abstract void close() throws IOException;
    @Shadow @Final private EntityPersistentStorage<T> permanentStorage;
    @Shadow @Final EntitySectionStorage<T> sectionStorage;
    @Shadow @Final private Long2ObjectMap<PersistentEntitySectionManager.ChunkLoadStatus> chunkLoadStatuses;
    // @formatter:on

    public void close(boolean save) throws IOException {
        if (save) {
            this.close();
        } else {
            this.permanentStorage.close();
        }
    }

    public List<Entity> getEntities(ChunkPos chunkCoordIntPair) {
        return sectionStorage.getExistingSectionsInChunk(chunkCoordIntPair.toLong())
            .flatMap(EntitySection::getEntities).map(o -> (Entity) o).collect(Collectors.toList());
    }

    public boolean isPending(long cord) {
        return this.chunkLoadStatuses.get(cord) == PersistentEntitySectionManager.ChunkLoadStatus.PENDING;
    }

    @Override
    public int bridge$countEntitiesOfType(long chunkPos, EntityType<?> type) {
        int[] count = new int[1];
        this.sectionStorage.getExistingSectionsInChunk(chunkPos).forEach(section ->
            section.getEntities().forEach(entity -> {
                if (((Entity) entity).getType() == type) {
                    count[0]++;
                }
            }));
        return count[0];
    }

    @Unique private boolean arclight$fireEvent = false;

    @Inject(method = "storeChunkSections", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityPersistentStorage;storeEntities(Lnet/minecraft/world/level/entity/ChunkEntities;)V"))
    private void arclight$fireUnload(long pos, Consumer<T> consumer, CallbackInfoReturnable<Boolean> cir, @Coerce Object status, List<T> list) {
        if (arclight$fireEvent) {
            CraftEventFactory.callEntitiesUnloadEvent(((EntityStorage) permanentStorage).level, new ChunkPos(pos),
                list.stream().map(entity -> (Entity) entity).collect(Collectors.toList()));
        }
    }

    @Inject(method = "storeChunkSections", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityPersistentStorage;storeEntities(Lnet/minecraft/world/level/entity/ChunkEntities;)V"))
    private void arclight$resetFlag(long pos, Consumer<T> consumer, CallbackInfoReturnable<Boolean> cir) {
        arclight$fireEvent = false;
    }

    @Inject(method = "processChunkUnload", at = @At("HEAD"))
    private void arclight$fireEvent(long pChunkPosValue, CallbackInfoReturnable<Boolean> cir) {
        arclight$fireEvent = true;
    }

    @Inject(method = "processPendingLoads", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false, target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private void arclight$fireLoad(CallbackInfo ci, ChunkEntities<T> chunkEntities) {
        List<Entity> entities = getEntities(chunkEntities.getPos());
        CraftEventFactory.callEntitiesLoadEvent(((EntityStorage) permanentStorage).level, chunkEntities.getPos(), entities);
        if (arclight$cullOnLoad && EntityChunkCap.ENABLED) {
            arclight$cullOverCap(chunkEntities.getPos(), entities);
        }
    }

    @Unique
    private static final boolean arclight$cullOnLoad =
        io.izzel.arclight.i18n.ArclightConfig.spec().getOptimization().isChunkLoadEntityCull();

    /**
     * Зачистка накопленных сверх лимита сущностей при загрузке чанка: типы с лимитом
     * max-entities-per-chunk >= 0 обрезаются до 2x лимита. Именные, прирученные и
     * persistence-сущности не удаляются никогда. Удаление - с конца списка (свежие первыми).
     */
    @Unique
    private void arclight$cullOverCap(ChunkPos pos, List<Entity> entities) {
        Map<EntityType<?>, List<Entity>> byType = null;
        for (Entity entity : entities) {
            if (EntityChunkCap.capFor(entity.getType()) >= 0) {
                if (byType == null) {
                    byType = new HashMap<>();
                }
                byType.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
            }
        }
        if (byType == null) {
            return;
        }
        for (Map.Entry<EntityType<?>, List<Entity>> entry : byType.entrySet()) {
            List<Entity> list = entry.getValue();
            int limit = EntityChunkCap.capFor(entry.getKey()) * 2;
            if (list.size() <= limit) {
                continue;
            }
            int alive = list.size();
            int removed = 0;
            for (int i = list.size() - 1; i >= 0 && alive > limit; i--) {
                Entity entity = list.get(i);
                if (entity.hasCustomName()) {
                    continue;
                }
                if (entity instanceof Mob mob && mob.isPersistenceRequired()) {
                    continue;
                }
                if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
                    continue;
                }
                entity.discard();
                alive--;
                removed++;
            }
            if (removed > 0) {
                LogManager.getLogger("Aurelith").debug("chunk-load-entity-cull: removed {} of {} at {}",
                    removed, EntityType.getKey(entry.getKey()), pos);
            }
        }
    }
}
