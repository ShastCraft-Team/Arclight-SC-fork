package io.izzel.arclight.common.mixin.optimization.general.realtime;

import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerGameMode.class)
public class PlayerInteractionManagerMixin_Realtime {

    @Shadow private int gameTicks;

    private int lastTick = ArclightConstants.currentTick - 1;

    // Прогресс ломания блока считается как destroySpeed * (gameTicks - startTick), а
    // ArclightConstants.currentTick идёт по настенным часам (мс/50). Поэтому приращение
    // по currentTick делает ломание независимым от того, укладывается ли сервер в 50 мс:
    // при MSPT 85 блок ломается за то же реальное время, что и при 20 TPS.
    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/level/ServerPlayerGameMode;gameTicks:I"))
    private void arclight$useWallTime(ServerPlayerGameMode playerInteractionManager, int value) {
        var spec = ArclightConfig.spec().getOptimization().getLagCompensation();
        if (!spec.isBlockBreaking()) {
            this.gameTicks = value;
            return;
        }
        int elapsedTicks = ArclightConstants.currentTick - this.lastTick;
        if (elapsedTicks < 1) {
            elapsedTicks = 1;
        } else if (elapsedTicks > spec.getMaxCatchupTicks()) {
            // после длинного фриза не досчитываем всё пропущенное время разом
            elapsedTicks = spec.getMaxCatchupTicks();
        }
        this.gameTicks += elapsedTicks;
        this.lastTick = ArclightConstants.currentTick;
    }
}
