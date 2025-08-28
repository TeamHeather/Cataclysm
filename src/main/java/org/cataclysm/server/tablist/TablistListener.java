package org.cataclysm.server.tablist;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cataclysm.api.listener.registrable.Registrable;
import org.cataclysm.game.world.day.events.ChangeDayEvent;

@Registrable
public class TablistListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerJoin(PlayerJoinEvent event) {
        CataclysmTablist.organizePlayer(event.getPlayer());
        CataclysmTablist.updateWeek(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerQuit(PlayerQuitEvent event) {
        CataclysmTablist.organizePlayer(event.getPlayer());
        CataclysmTablist.updateWeek(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onChangeDay(ChangeDayEvent event) {
        for (var player : Bukkit.getOnlinePlayers()) CataclysmTablist.updateWeek(player);
    }

}
