package io.izzel.arclight.common.mod.util;

/**
 * Учёт «стоек» главного потока — времени, на которое тик вылез за отведённые 50 мс.
 * <p>
 * Когда сервер отстаёт, страдает не только TPS: клиент не получает ответа на keep-alive
 * и отваливается по таймауту, а накопленные пакеты движения выглядят для проверок как
 * читерский рывок и игрока откатывает назад. Обе проблемы лечатся, если знать, сколько
 * сервер реально простоял, — этим и занимается класс.
 * <p>
 * История хранится посекундными корзинами (кольцевой буфер), чтобы можно было спросить
 * «сколько сервер стоял начиная с момента X», а не только суммарно за сессию.
 * <p>
 * Обновляется исключительно из главного потока в начале тика; чтение потокобезопасно
 * в том смысле, что худший случай — увидеть значение на тик старее.
 */
public final class TickClock {

    private TickClock() {
    }

    private static final long NORMAL_TICK_NANOS = 50_000_000L;
    /** Глубина истории: 64 секунды с запасом перекрывают окно keep-alive (15 с). */
    private static final int BUCKETS = 64;
    private static final long BUCKET_MILLIS = 1000L;

    /** Начало корзины (wall-clock мс) и накопленная в ней стойка. */
    private static final long[] bucketStart = new long[BUCKETS];
    private static final long[] bucketStall = new long[BUCKETS];

    private static long lastTickNanos = System.nanoTime();
    private static volatile long lastStallMillis;
    private static volatile long totalStallMillis;

    /**
     * Вызывается в начале каждого серверного тика. Дельта между началами тиков при
     * нормальной работе равна 50 мс (цикл досыпает), поэтому всё сверх — это реальная
     * задержка, которую игроки почувствовали.
     */
    public static void onTickStart() {
        long now = System.nanoTime();
        long elapsed = now - lastTickNanos;
        lastTickNanos = now;

        long overNanos = elapsed - NORMAL_TICK_NANOS;
        if (overNanos <= 0L) {
            lastStallMillis = 0L;
            return;
        }
        long stall = overNanos / 1_000_000L;
        if (stall <= 0L) {
            lastStallMillis = 0L;
            return;
        }
        lastStallMillis = stall;
        totalStallMillis += stall;

        long nowMillis = System.currentTimeMillis();
        int idx = (int) ((nowMillis / BUCKET_MILLIS) % BUCKETS);
        if (bucketStart[idx] != nowMillis / BUCKET_MILLIS * BUCKET_MILLIS) {
            // корзина из прошлого оборота буфера - переиспользуем
            bucketStart[idx] = nowMillis / BUCKET_MILLIS * BUCKET_MILLIS;
            bucketStall[idx] = stall;
        } else {
            bucketStall[idx] += stall;
        }
    }

    /** Стойка последнего тика в миллисекундах; 0 — сервер укладывается в бюджет. */
    public static long lastStallMillis() {
        return lastStallMillis;
    }

    /** Сервер отстал прямо сейчас больше, чем на {@code thresholdMillis}. */
    public static boolean stalling(long thresholdMillis) {
        return lastStallMillis >= thresholdMillis;
    }

    /** Суммарное время стоек за сессию — для диагностики. */
    public static long totalStallMillis() {
        return totalStallMillis;
    }

    /**
     * Сколько сервер простоял начиная с момента {@code sinceWallMillis}
     * (значение {@link System#currentTimeMillis()}). Глубже {@link #BUCKETS} секунд
     * история не хранится, поэтому результат ограничен этим окном.
     */
    public static long stallSince(long sinceWallMillis) {
        long now = System.currentTimeMillis();
        long oldest = now - BUCKETS * BUCKET_MILLIS;
        long from = Math.max(sinceWallMillis, oldest);
        long sum = 0L;
        for (int i = 0; i < BUCKETS; i++) {
            long start = bucketStart[i];
            if (start >= from && start <= now) {
                sum += bucketStall[i];
            }
        }
        return sum;
    }
}
