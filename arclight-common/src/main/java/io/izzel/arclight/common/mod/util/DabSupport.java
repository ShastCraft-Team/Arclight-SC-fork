package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.conf.DabSpec;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.raid.Raider;
import org.spigotmc.SpigotWorldConfig;

/**
 * DAB - distance based AI throttling.
 * <p>
 * Мобы, удалённые от игроков, пересчитывают AI реже. Приоритет (интервал тика AI)
 * считается один раз за тик во время свипа по игрокам в
 * {@code ServerWorldMixin_ActivationRange}, а применяется в
 * {@code MobMixin_Optimization} (goal-селекторы) и {@code BrainMixin_DAB} (brain-мобы).
 * <p>
 * Ближние мобы всегда получают приоритет 1 (полная скорость), поэтому бой и фермы
 * в поле зрения игрока ведут себя как в ванили.
 */
public final class DabSupport {

    private DabSupport() {
    }

    public static final boolean ENABLED;
    public static final int MAX_TICK_FREQ;

    private static final double START_DIST_SQ;
    private static final int DIST_MOD;

    static {
        DabSpec spec = ArclightConfig.spec().getOptimization().getDab();
        ENABLED = spec.isEnabled();
        int startDistance = Math.max(1, spec.getStartDistance());
        START_DIST_SQ = (double) startDistance * startDistance;
        // сдвиг должен оставаться в разумных пределах, иначе приоритет вырождается
        DIST_MOD = Math.max(1, Math.min(31, spec.getActivationDistMod()));
        MAX_TICK_FREQ = Math.max(1, spec.getMaxTickFreq());
    }

    /**
     * Интервал тика AI для сущности на расстоянии {@code distSq} (квадрат) от игрока.
     * 1 = полная скорость.
     */
    public static int priorityFor(double distSq) {
        if (distSq <= START_DIST_SQ) {
            return 1;
        }
        long excess = (long) (distSq - START_DIST_SQ);
        long prio = (excess >> DIST_MOD) + 1L;
        if (prio > MAX_TICK_FREQ) {
            return MAX_TICK_FREQ;
        }
        return (int) prio;
    }

    /**
     * Радиус свипа - тот же, что использует Spigot ActivationRange, чтобы DAB работал
     * на той же окрестности игрока. Сущности за его пределами получают MAX_TICK_FREQ
     * автоматически (см. bridge$dabPriority).
     */
    public static int sweepRange(SpigotWorldConfig config) {
        if (config == null) {
            return 64;
        }
        int maxRange = Math.max(config.monsterActivationRange, config.animalActivationRange);
        maxRange = Math.max(maxRange, config.raiderActivationRange);
        maxRange = Math.max(maxRange, config.miscActivationRange);
        maxRange = Math.min((config.simulationDistance << 4) - 8, maxRange);
        return Math.max(maxRange, 16);
    }

    /**
     * Мобы, которые не должны троттлиться независимо от дистанции: в бою, верхом,
     * с наездником, на поводке, рейдеры во время рейда.
     */
    public static boolean shouldThrottle(Mob mob) {
        if (mob.getTarget() != null) {
            return false;
        }
        if (mob.isPassenger() || mob.isVehicle() || mob.isLeashed()) {
            return false;
        }
        if (mob instanceof Raider raider && raider.hasActiveRaid()) {
            return false;
        }
        return true;
    }
}
