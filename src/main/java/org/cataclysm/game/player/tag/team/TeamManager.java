package org.cataclysm.game.player.tag.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.cataclysm.game.player.data.PlayerData;
import org.jetbrains.annotations.NotNull;

public record TeamManager(PlayerData data) {

    public @NotNull Component build() {
        var badge = this.getTeam().getBadge();
        if (badge == null) return Component.text("");
        return MiniMessage.miniMessage().deserialize("<#ffffff>" + this.getTeam().getBadge());
    }

    public void setTeam(@NotNull Teams team) {
        this.data.setTeam(team.name());
    }

    public Teams getTeam() {
        if (this.data.getTeam() == null) return Teams.NONE;
        return Teams.valueOf(this.data.getTeam());
    }

}