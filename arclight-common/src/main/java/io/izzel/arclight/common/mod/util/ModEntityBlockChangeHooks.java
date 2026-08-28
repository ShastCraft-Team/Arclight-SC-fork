package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.i18n.ArclightConfig;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Защита регионов от машин модов (Create и т.п.).
 * <p>
 * Когда блок меняется во время тика МОДОВОЙ сущности (контрапшены с бурами,
 * харвестерами, укладчиками), стреляет отменяемый {@link EntityChangeBlockEvent} -
 * его уже обрабатывают плагины регионов (WorldGuard). Ядро не ссылается на классы
 * Create или WorldGuard.
 * <p>
 * Машины собирают лут через {@code Block.getDrops} ДО вызова setBlock, поэтому
 * отменять только setBlock мало: блок остаётся, а дроп уже выдан (дюп). Гейт
 * {@link #allowDrops} закрывает сбор лута тем же событием; решение кешируется
 * на тик по позиции, чтобы событие не стреляло дважды за одно ломание.
 * <p>
 * Не покрывает стационарные машины (чистые block entity - у них нет entity-контекста)
 * и fake-игроков (деплойеры) - те уже проходят через player-события Break/Place.
 */
public final class ModEntityBlockChangeHooks {

    private ModEntityBlockChangeHooks() {
    }

    private static final boolean ENABLED =
        ArclightConfig.spec().getCompat().isModEntityBlockChangeEvents();

    private static final Map<EntityType<?>, Boolean> MODDED = new ConcurrentHashMap<>();

    /** Guard от рекурсии: слушатель события сам может менять блоки. Только main-thread. */
    private static boolean firing;

    /** Кеш решений текущего тика (main-thread): pos -> разрешено. */
    private static final Long2BooleanOpenHashMap DECISIONS = new Long2BooleanOpenHashMap();
    private static long decisionsTick = Long.MIN_VALUE;
    private static Level decisionsLevel;

    /**
     * true = изменение разрешено. Вызывается из setBlock-хука LevelMixin, когда
     * существующий capture (ваниль) не сработал.
     */
    public static boolean allow(Level level, Object craftWorld, boolean populating, BlockPos pos, BlockState newState) {
        if (!ENABLED || firing || craftWorld == null || populating || level.isClientSide()) {
            return true;
        }
        Entity ticking = eligibleTicking(level);
        if (ticking == null) {
            return true;
        }
        return decide(level, ticking, pos, newState);
    }

    /**
     * Гейт сбора лута ({@code Block.getDrops} / {@code popExperience}): true = можно.
     * false - лут подавить (машина ломает блок в запрещённом месте).
     */
    public static boolean allowDrops(Level level, BlockPos pos) {
        if (!ENABLED || firing || level.isClientSide()) {
            return true;
        }
        Entity ticking = eligibleTicking(level);
        if (ticking == null) {
            return true;
        }
        return decide(level, ticking, pos, Blocks.AIR.defaultBlockState());
    }

    private static Entity eligibleTicking(Level level) {
        if (EntityChangeBlockEvent.getHandlerList().getRegisteredListeners().length == 0) {
            return null;
        }
        Entity ticking = ArclightCaptures.getTickingEntity();
        if (ticking == null || ticking.level() != level) {
            return null;
        }
        if (ticking instanceof ServerPlayer) {
            // fake-игроки (деплойеры) уже покрыты BlockBreak/BlockPlace-событиями
            return null;
        }
        if (!MODDED.computeIfAbsent(ticking.getType(),
            t -> !EntityType.getKey(t).getNamespace().equals("minecraft"))) {
            return null;
        }
        return ticking;
    }

    private static boolean decide(Level level, Entity ticking, BlockPos pos, BlockState newState) {
        long time = level.getGameTime();
        if (time != decisionsTick || level != decisionsLevel) {
            DECISIONS.clear();
            decisionsTick = time;
            decisionsLevel = level;
        }
        long key = pos.asLong();
        if (DECISIONS.containsKey(key)) {
            return DECISIONS.get(key);
        }
        firing = true;
        boolean allowed;
        try {
            allowed = CraftEventFactory.callEntityChangeBlockEvent(ticking, pos, newState);
        } finally {
            firing = false;
        }
        DECISIONS.put(key, allowed);
        return allowed;
    }
}
