package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityInteractEvent;

public class BlockBreakEventDispatcher {

    // Деплойеры и схематикannon Create действуют от имени Forge FakePlayer.
    // Ключ compatibility.fake-player-block-events позволяет не слать по ним
    // события Bukkit, если плагины на них реагируют неадекватно.
    private static final boolean FIRE_FAKE_PLAYER_EVENTS =
        io.izzel.arclight.i18n.ArclightConfig.spec().getCompat().isFakePlayerBlockEvents();

    @SubscribeEvent(receiveCanceled = true)
    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!FIRE_FAKE_PLAYER_EVENTS && event.getPlayer() instanceof net.minecraftforge.common.util.FakePlayer) {
            return;
        }
        if (DistValidate.isValid(event.getLevel())) {
            CraftBlock craftBlock = CraftBlock.at(event.getLevel(), event.getPos());
            BlockBreakEvent breakEvent = new BlockBreakEvent(craftBlock, ((ServerPlayerEntityBridge) event.getPlayer()).bridge$getBukkitEntity());
            ArclightCaptures.captureBlockBreakPlayer(breakEvent);
            breakEvent.setCancelled(event.isCanceled());
            breakEvent.setExpToDrop(event.getExpToDrop());
            Bukkit.getPluginManager().callEvent(breakEvent);
            event.setCanceled(breakEvent.isCancelled());
            event.setExpToDrop(breakEvent.getExpToDrop());
        }
    }

    @SubscribeEvent
    public void onFarmlandBreak(BlockEvent.FarmlandTrampleEvent event) {
        if (!DistValidate.isValid(event.getLevel())) return;
        Entity entity = event.getEntity();
        Cancellable cancellable;
        if (entity instanceof Player) {
            cancellable = CraftEventFactory.callPlayerInteractEvent((Player) entity, Action.PHYSICAL, event.getPos(), null, null, null);
        } else {
            cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(event.getLevel(), event.getPos()));
            Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
        }

        if (cancellable.isCancelled()) {
            event.setCanceled(true);
            return;
        }

        if (!CraftEventFactory.callEntityChangeBlockEvent(entity, event.getPos(), Blocks.DIRT.defaultBlockState())) {
            event.setCanceled(true);
        }
    }
}
