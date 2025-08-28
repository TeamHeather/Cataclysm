package org.cataclysm.server.tablist;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TablistUtils {

    public static void setScoreboardTeam(@NotNull Player player, String teamName) {
        removePlayerFromCurrentTeam(player);
        var scoreboard = player.getScoreboard();
        var minecraftTeam = scoreboard.getTeam(teamName);

        if (minecraftTeam == null) minecraftTeam = scoreboard.registerNewTeam(teamName);
        minecraftTeam.addPlayer(player);
    }

    public static void removePlayerFromScoreboardTeam(@NotNull Player player) {
        removePlayerFromCurrentTeam(player);
    }

    private static void removePlayerFromCurrentTeam(@NotNull Player player) {
        var scoreboard = player.getScoreboard();
        var currentTeam = scoreboard.getPlayerTeam(player);

        if (currentTeam != null) {
            currentTeam.removePlayer(player);

            // Clean up empty teams to prevent huge packets
            if (currentTeam.getSize() == 0) {
                currentTeam.unregister();
            }
        }
    }
}