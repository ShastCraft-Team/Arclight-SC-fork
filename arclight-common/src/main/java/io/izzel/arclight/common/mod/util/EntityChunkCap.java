package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Лимит числа сущностей одного типа в чанке (анти-ферма).
 * <p>
 * Конфиг: {@code optimization.max-entities-per-chunk} - карта {@code "modid:entity" -> int},
 * ключ {@code default} задаёт лимит для всех прочих типов, {@code -1} = без лимита.
 * <p>
 * Разрешение id в {@link EntityType} ленивое: на момент статической инициализации
 * реестры Forge ещё не готовы.
 */
public final class EntityChunkCap {

    private EntityChunkCap() {
    }

    private static final Logger LOGGER = LogManager.getLogger("Aurelith");
    private static final String DEFAULT_KEY = "default";

    /**
     * true, если в конфиге есть хотя бы один неотрицательный лимит.
     * Считается по сырой карте, без обращения к реестрам, поэтому безопасно в static-init.
     */
    public static final boolean ENABLED;

    private static final Map<String, Integer> RAW;
    private static final int DEFAULT_CAP;

    private static volatile Map<EntityType<?>, Integer> resolved;

    static {
        RAW = ArclightConfig.spec().getOptimization().getMaxEntitiesPerChunk();
        int def = -1;
        boolean any = false;
        for (Map.Entry<String, Integer> entry : RAW.entrySet()) {
            Integer value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (DEFAULT_KEY.equals(entry.getKey())) {
                def = value;
            }
            if (value >= 0) {
                any = true;
            }
        }
        DEFAULT_CAP = def;
        ENABLED = any;
    }

    /**
     * Лимит для типа: явное значение из конфига, иначе {@code default}. -1 = без лимита.
     */
    public static int capFor(EntityType<?> type) {
        Map<EntityType<?>, Integer> map = resolved;
        if (map == null) {
            map = resolve();
        }
        Integer cap = map.get(type);
        return cap == null ? DEFAULT_CAP : cap;
    }

    private static synchronized Map<EntityType<?>, Integer> resolve() {
        if (resolved != null) {
            return resolved;
        }
        Map<EntityType<?>, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : RAW.entrySet()) {
            String key = entry.getKey();
            if (DEFAULT_KEY.equals(key) || entry.getValue() == null) {
                continue;
            }
            ResourceLocation location = ResourceLocation.tryParse(key);
            EntityType<?> type = location == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(location);
            if (type == null) {
                LOGGER.warn("Unknown entity id in optimization.max-entities-per-chunk: {}", key);
                continue;
            }
            map.put(type, entry.getValue());
        }
        resolved = map;
        return map;
    }
}
