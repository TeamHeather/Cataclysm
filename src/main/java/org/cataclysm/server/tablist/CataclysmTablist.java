package org.cataclysm.server.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.cataclysm.Cataclysm;
import org.cataclysm.game.player.CataclysmPlayer;
import org.cataclysm.game.player.tag.role.RoleManager;
import org.cataclysm.game.player.tag.team.TeamManager;
import org.cataclysm.global.utils.chat.ChatMessenger;
import org.cataclysm.global.utils.text.font.TinyCaps;
import org.jetbrains.annotations.NotNull;

public class CataclysmTablist {
    public static void update(Player player) {
        var header = "\n" + ChatMessenger.getCataclysmColor() + " Tʜᴇ Cᴀᴛᴀᴄʟʏꜱᴍ SMP \n" + ChatMessenger.getTextColor() + getSubtitle(Cataclysm.getDay()) + " \n";
        player.sendPlayerListHeader(MiniMessage.miniMessage().deserialize(ChatMessenger.getCataclysmColor() + header));
        player.sendPlayerListFooter(MiniMessage.miniMessage().deserialize("\n " + ChatMessenger.getTextColor() + "Día: <#CCCCCC>" + Cataclysm.getDay() + ChatMessenger.getTextColor() + "/35\n"));
    }

    private static @NotNull String getSubtitle(int day) {
        var week = (day / 7) + 1;
        var text = "";
        switch (week) {
            case 1 -> text = "primera semana";
            case 2 -> text = "segunda semana";
            case 3 -> text = "tercera semana";
            case 4 -> text = "cuarta semana";
            case 5 -> text = "quinta semana";
        }
        return TinyCaps.tinyCaps(text);
    }

    public static void organizePlayer(Player player) {
        var data = CataclysmPlayer.getCataclysmPlayer(player).getData();
        var role = new RoleManager(data).getRole();

        if (role == null) return;

        TablistUtils.setScoreboardTeam(player, role.ordinal() + "-" + role.name());
        updatePlayerName(player);
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

}
