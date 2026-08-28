package io.izzel.arclight.common.mixin.optimization.general;

import io.izzel.arclight.common.bridge.optimization.EntityBridge_ActivationRange;
import io.izzel.arclight.common.mod.util.DabSupport;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Mob.class)
public class MobMixin_Optimization {

    @Unique
    private static final int arclight$flatInterval =
        Math.max(1, ArclightConfig.spec().getOptimization().getGoalSelectorInterval());

    /**
     * Ванильный {@code serverAiStep} гейтит пересчёт целей через {@code (tickCount + id) % 2}.
     * Подменяем константу: базово - плоский goal-selector-update-interval, а при включённом
     * DAB для дальних мобов интервал растёт с дистанцией (плоское значение остаётся полом).
     */
    @ModifyConstant(method = "serverAiStep", constant = @Constant(intValue = 2))
    private int arclight$goalUpdateInterval(int orig) {
        if (DabSupport.ENABLED) {
            int dab = ((EntityBridge_ActivationRange) this).bridge$dabPriority();
            if (dab > arclight$flatInterval && DabSupport.shouldThrottle((Mob) (Object) this)) {
                return dab;
            }
        }
        return arclight$flatInterval;
    }
}
