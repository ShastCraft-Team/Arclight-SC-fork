package io.izzel.arclight.common.mixin.optimization.general;

import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Кеш результата {@code BlockEntityType.isValid(BlockState)} на пути тика block entity.
 * <p>
 * Ваниль дёргает {@code isValid} для КАЖДОГО тикающего block entity КАЖДЫЙ тик, чтобы
 * убедиться, что BE всё ещё соответствует блоку на своей позиции. Сам по себе вызов
 * дешёвый, но моды инжектят в него свои проверки, и тогда за них платит каждый BE в мире.
 * <p>
 * На профиле ShastCraft (38 игроков, 38 Create-аддонов) инжект Jaden's Nether Expansion
 * в {@code isValid} стоил ~2% тика плюс ~0.5% на аллокацию {@code CallbackInfoReturnable} -
 * при том что нужен он только для табличек этого мода.
 * <p>
 * {@code isValid} - чистая функция от пары (тип BE, блок-стейт), а тип BE за время жизни
 * обёртки не меняется. {@code BlockState} - иммутабельный синглтон из палитры, поэтому
 * сравниваем по ссылке. Пока блок на позиции не менялся, оригинал (и чужие инжекты
 * вместе с ним) не вызывается вообще.
 */
@Mixin(targets = "net/minecraft/world/level/chunk/LevelChunk$BoundTickingBlockEntity")
public class BoundTickingBlockEntityMixin_Optimization {

    @Unique
    private static final boolean arclight$enabled =
        ArclightConfig.spec().getOptimization().isCacheBlockEntityValid();

    @Unique
    private BlockState arclight$cachedState;

    @Unique
    private boolean arclight$cachedValid;

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntityType;isValid(Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean arclight$cachedIsValid(BlockEntityType<?> type, BlockState state) {
        if (!arclight$enabled) {
            return type.isValid(state);
        }
        if (state == this.arclight$cachedState) {
            return this.arclight$cachedValid;
        }
        boolean valid = type.isValid(state);
        this.arclight$cachedState = state;
        this.arclight$cachedValid = valid;
        return valid;
    }
}
