package io.izzel.arclight.common.mixin.optimization.general.activationrange;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.optimization.EntityBridge_ActivationRange;
import io.izzel.arclight.common.mod.util.DabSupport;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerWorldMixin_ActivationRange {

    @Unique
    private static final boolean arclight$applyInactive = ArclightConfig.spec().getOptimization().useActivationAndTrackingRange();

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;entityTickList:Lnet/minecraft/world/level/entity/EntityTickList;"))
    private void activationRange$activateEntity(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        ActivationRange.activateEntities(level);
        arclight$dabSweep(level);
    }

    /**
     * Свип DAB: раз за тик проставляет каждой сущности рядом с игроками интервал тика AI
     * по дистанции до ближайшего игрока. Сущности, не попавшие в свип, считаются дальними
     * и троттлятся по максимуму (см. EntityMixin_ActivationRange#bridge$dabPriority).
     */
    @Unique
    private void arclight$dabSweep(ServerLevel level) {
        if (!DabSupport.ENABLED) {
            return;
        }
        int range = DabSupport.sweepRange(((WorldBridge) level).bridge$spigotConfig());
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            AABB box = player.getBoundingBox().inflate(range, 256.0D, range);
            level.getEntities().get(box, entity ->
                ((EntityBridge_ActivationRange) entity).bridge$dabAccumulate(player.distanceToSqr(entity)));
        }
    }

    @Inject(method = "tickNonPassenger", cancellable = true, at = @At(value = "HEAD"))
    private void activationRange$inactiveTick(Entity entityIn, CallbackInfo ci) {
        if (arclight$applyInactive && !ActivationRange.checkIfActive(entityIn)) {
            ++entityIn.tickCount;
            if (entityIn.canUpdate()) {
                ((EntityBridge_ActivationRange) entityIn).bridge$inactiveTick();
            }
            ci.cancel();
        }
    }
}
