package io.izzel.arclight.common.mixin.optimization.general.activationrange;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.optimization.EntityBridge_ActivationRange;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.util.DabSupport;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin_ActivationRange implements EntityBridge_ActivationRange {

    // @formatter:off
    @Shadow public abstract void refreshDimensions();
    @Shadow public int tickCount;
    @Shadow public abstract Level level();
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract void discard();
    // @formatter:on

    public ActivationRange.ActivationType activationType;
    public boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;

    public int arclight$dabPriority = 1;
    public long arclight$dabLastSweep = Integer.MIN_VALUE;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<?> entityTypeIn, Level worldIn, CallbackInfo ci) {
        activationType = ActivationRange.initializeEntityActivationType((Entity) (Object) this);
        if (DistValidate.isValid(worldIn)) {
            var config = ((WorldBridge) worldIn).bridge$spigotConfig();
            if (config != null) {
                this.defaultActivationState = ActivationRange.initializeEntityActivationState((Entity) (Object) this, config);
            } else {
                this.defaultActivationState = false;
            }
        } else {
            this.defaultActivationState = false;
        }
    }

    public void inactiveTick() {
    }

    @Override
    public void bridge$inactiveTick() {
        this.inactiveTick();
    }

    @Override
    public void bridge$dabAccumulate(double distSq) {
        int prio = DabSupport.priorityFor(distSq);
        long tick = ArclightConstants.currentTick;
        if (this.arclight$dabLastSweep != tick) {
            // первый игрок, увидевший сущность в этом тике - сбрасываем накопленное
            this.arclight$dabLastSweep = tick;
            this.arclight$dabPriority = prio;
        } else if (prio < this.arclight$dabPriority) {
            this.arclight$dabPriority = prio;
        }
    }

    @Override
    public int bridge$dabPriority() {
        if (!DabSupport.ENABLED || this.defaultActivationState) {
            return 1;
        }
        if (this.arclight$dabLastSweep < ArclightConstants.currentTick) {
            // сущность не попала в свип ни одного игрока - она заведомо далеко
            // (сюда же попадают мобы с иммунитетом активации, например стоящие в воде)
            return DabSupport.MAX_TICK_FREQ;
        }
        return this.arclight$dabPriority;
    }

    @Override
    public void bridge$updateActivation() {
        if (ArclightConstants.currentTick > this.activatedTick) {
            if (this.defaultActivationState) {
                this.activatedTick = ArclightConstants.currentTick;
            } else if (this.activationType.boundingBox.intersects(this.getBoundingBox())) {
                this.activatedTick = ArclightConstants.currentTick;
            }
        }
    }
}
