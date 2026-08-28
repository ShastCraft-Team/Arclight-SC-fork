package io.izzel.arclight.common.mixin.optimization.general.realtime;

import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Кулдаун атаки по реальному времени.
 * <p>
 * Клиент рисует шкалу удара в реальном времени, а сервер считает её в тиках. Когда тик
 * длится 85 мс вместо 50, игрок видит полностью заряженный удар, бьёт - и получает
 * ослабленный урон, потому что сервер насчитал только 60% готовности. Здесь счётчик
 * растёт по настенным часам ({@code ArclightConstants.currentTick} = мс/50), и клиент
 * с сервером снова сходятся.
 * <p>
 * Поле {@code attackStrengthTicker} объявлено в {@link LivingEntity}, а инкремент стоит в
 * {@code Player.tick()}, поэтому значение читается и пишется через аксессор - {@code @Shadow}
 * в миксине на {@code Player} унаследованное поле не находит.
 * <p>
 * Продолжение семейства {@code realtime}-миксинов ядра (ломание блоков, дроп предметов).
 */
@Mixin(Player.class)
public class PlayerMixin_Realtime {

    private int arclight$lastAttackTick = ArclightConstants.currentTick - 1;

    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/player/Player;attackStrengthTicker:I"))
    private void arclight$attackCooldownWallTime(Player player, int value) {
        var accessor = (AttackTickerAccessor) player;
        var spec = ArclightConfig.spec().getOptimization().getLagCompensation();
        if (!spec.isBlockBreaking()) {
            accessor.arclight$setAttackStrengthTicker(value);
            return;
        }
        int elapsed = ArclightConstants.currentTick - this.arclight$lastAttackTick;
        if (elapsed < 1) {
            elapsed = 1;
        } else if (elapsed > spec.getMaxCatchupTicks()) {
            // после длинного фриза не выдаём игроку пачку бесплатных зарядов
            elapsed = spec.getMaxCatchupTicks();
        }
        accessor.arclight$setAttackStrengthTicker(accessor.arclight$getAttackStrengthTicker() + elapsed);
        this.arclight$lastAttackTick = ArclightConstants.currentTick;
    }

    @Mixin(LivingEntity.class)
    public interface AttackTickerAccessor {

        @Accessor("attackStrengthTicker")
        int arclight$getAttackStrengthTicker();

        @Accessor("attackStrengthTicker")
        void arclight$setAttackStrengthTicker(int value);
    }
}
