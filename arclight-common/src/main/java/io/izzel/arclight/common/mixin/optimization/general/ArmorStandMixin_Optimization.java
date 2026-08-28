package io.izzel.arclight.common.mixin.optimization.general;

import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Опция tick-armor-stands=false отключает тик броневых стоек целиком.
 * Побочные эффекты: стойки не падают (гравитация) и не горят; экипировка,
 * взаимодействие и урон работают - они событийные, а не тиковые.
 */
@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin_Optimization {

    @Unique
    private static final boolean arclight$tickArmorStands =
        ArclightConfig.spec().getOptimization().isTickArmorStands();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void arclight$noTick(CallbackInfo ci) {
        if (!arclight$tickArmorStands) {
            ci.cancel();
        }
    }
}
