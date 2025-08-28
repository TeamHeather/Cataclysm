package org.cataclysm.server.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.cataclysm.Cataclysm;
import org.cataclysm.game.player.CataclysmPlayer;
import org.cataclysm.game.player.tag.role.RoleManager;
import org.cataclysm.game.player.tag.team.TeamManager;
import org.cataclysm.global.utils.chat.ChatMessenger;
import org.cataclysm.global.utils.text.font.TinyCaps;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CataclysmTablist {
    //CACHE YOUR FUCKING DATA BRO!!
    private static final Map<Player, String> playerTeamCache = new HashMap<>();

    protected static void updateWeek(Player player) {
        var header = "\n" + ChatMessenger.getCataclysmColor() + " Tʜᴇ Cᴀᴛᴀᴄʟʏꜱᴍ SMP \n" + ChatMessenger.getTextColor() + getWeek() + " \n";
        player.sendPlayerListHeader(MiniMessage.miniMessage().deserialize(ChatMessenger.getCataclysmColor() + header));
        player.sendPlayerListFooter(MiniMessage.miniMessage().deserialize("\n " + ChatMessenger.getTextColor() + "Día: <#CCCCCC>" + Cataclysm.getDay() + ChatMessenger.getTextColor() + "/35 \n"));
    }

    private static @NotNull String getWeek() {
        var week = (Cataclysm.getDayManager().getDay() / 7) + 1;
        var text = "";
        switch (week) {
            case 1 -> text = "primera";
            case 2 -> text = "segunda";
            case 3 -> text = "tercera";
            case 4 -> text = "cuarta";
            case 5 -> text = "quinta";
            case 6 -> {
                return TinyCaps.tinyCaps("día final");
            }
        }
        return TinyCaps.tinyCaps(text + " semana");
    }

    public static void organizePlayer(Player player) {
        Bukkit.getScheduler().runTaskLater(Cataclysm.getInstance(), () -> {
            var data = CataclysmPlayer.getCataclysmPlayer(player).getData();
            var role = new RoleManager(data).getRole();

            if (role == null) {
                removePlayerFromTeam(player);
                return;
            }

            String teamName = role.ordinal() + "-" + role.name();
            String currentTeam = playerTeamCache.get(player);

            if (!teamName.equals(currentTeam)) {
                TablistUtils.setScoreboardTeam(player, teamName);
                playerTeamCache.put(player, teamName);
            }

            updatePlayerName(player);
        }, 1L);
    }

    public static void updatePlayerName(Player player) {
        var data = CataclysmPlayer.getCataclysmPlayer(player).getData();
        var role = new RoleManager(data);
        var team = new TeamManager(data);
        player.playerListName(role.build()
                .append(MiniMessage.miniMessage().deserialize(role.getRole().getHex() + " " + player.getName()))
                .append(Component.text(" "))
                .append(team.build())
                .append(Component.text(" ")));
    }


    public static void removePlayerFromTeam(Player player) {
        String currentTeam = playerTeamCache.remove(player);
        if (currentTeam != null) {
            Bukkit.getConsoleSender().sendMessage("REMOVED PLAYER FROM SCOREBOARD TEAM");
            TablistUtils.removePlayerFromScoreboardTeam(player);
        }
    }
}