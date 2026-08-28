package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.mod.util.TickClock;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

/**
 * Не выкидывать игроков за то, что тормозил сам сервер.
 * <p>
 * Ваниль в {@code tick()} сравнивает {@code now - keepAliveTime >= 15000} и, если ответ
 * клиента ещё не пришёл, рвёт соединение с {@code disconnect.timeout}. Но ответ обрабатывается
 * в главном потоке: когда тот встаёт на 10-15 секунд, пакет лежит в очереди непрочитанным, и
 * на первом же тике после стойки игрока выбрасывает — хотя с его связью всё в порядке.
 * <p>
 * Здесь порог сдвигается ровно на то время, которое сервер простоял внутри текущего окна
 * keep-alive. Мёртвые клиенты по-прежнему отваливаются, как только сервер приходит в себя.
 * Значение {@code keepAliveTime} не трогаем, чтобы не врать о пинге игрока.
 * <p>
 * Только {@code @ModifyConstant}: метод {@code tick()} патчат сторонние моды
 * (ModernFix, Emotecraft, Lootr, Balanced Flight), перезапись сломала бы их.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin_KeepAlive {

    @Shadow private long keepAliveTime;

    /** Предохранитель: сколько бы сервер ни стоял, соединение-зомби не живёт вечно. */
    private static final long MAX_GRACE_MILLIS = 60_000L;

    @ModifyConstant(method = "tick", constant = @Constant(longValue = 15000L))
    private long arclight$keepAliveLagGrace(long vanilla) {
        var compat = ArclightConfig.spec().getCompat();
        long timeout = compat.getKeepAliveTimeoutMillis();
        if (!compat.isKeepAliveLagGrace()) {
            return timeout;
        }
        long grace = Math.min(TickClock.stallSince(this.keepAliveTime), MAX_GRACE_MILLIS);
        return timeout + grace;
    }
}
