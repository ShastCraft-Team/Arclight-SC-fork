package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.izzel.arclight.i18n.ArclightConfig;
import org.bukkit.craftbukkit.v.scheduler.CraftScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Mixin(value = CraftScheduler.class, remap = false)
public class CraftSchedulerMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/util/concurrent/ThreadFactoryBuilder;build()Ljava/util/concurrent/ThreadFactory;"))
    private ThreadFactory arclight$setDaemon(ThreadFactoryBuilder instance) {
        return instance.setDaemon(true).build();
    }

    /**
     * Vanilla CraftBukkit uses Executors.newCachedThreadPool which spawns a new thread for
     * every concurrent async task. Plugins that schedule one task per chunk load (e.g.
     * ChunkSpawnerLimiter during pregeneration) explode this to 100k+ threads and crash the
     * server. Cap the pool and queue the excess instead; threads still time out when idle.
     */
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/Executors;newCachedThreadPool(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService arclight$boundedPool(ThreadFactory factory) {
        int configured = ArclightConfig.spec().getOptimization().getAsyncSchedulerMaxThreads();
        int max = configured > 0 ? configured
            : Math.min(16, Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
        ThreadPoolExecutor executor = new ThreadPoolExecutor(max, max, 30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), factory);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
