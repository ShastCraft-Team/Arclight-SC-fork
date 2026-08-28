package io.izzel.arclight.common.mixin.optimization.general.dab;

import io.izzel.arclight.common.bridge.optimization.EntityBridge_ActivationRange;
import io.izzel.arclight.common.mod.util.DabSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DAB для brain-мобов (жители, пиглины, варден, аксолотли и т.д.).
 * <p>
 * Они ходят мимо goal-селекторов, поэтому {@link io.izzel.arclight.common.mixin.optimization.general.MobMixin_Optimization}
 * их не покрывает. Именно этот миксин даёт основной выигрыш на серверах с деревнями
 * и железофермами.
 */
@Mixin(Brain.class)
public class BrainMixin_DAB<E extends LivingEntity> {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void arclight$dabThrottle(ServerLevel level, E entity, CallbackInfo ci) {
        if (!DabSupport.ENABLED) {
            return;
        }
        if (entity instanceof Mob mob) {
            int prio = ((EntityBridge_ActivationRange) mob).bridge$dabPriority();
            if (prio > 1 && mob.tickCount % prio != 0 && DabSupport.shouldThrottle(mob)) {
                ci.cancel();
            }
        }
    }
}
