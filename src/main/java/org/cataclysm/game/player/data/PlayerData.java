package org.cataclysm.game.player.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.cataclysm.game.player.mechanics.upgrade.Upgrades;
import org.cataclysm.game.player.tag.role.RoleType;
import org.cataclysm.game.player.tag.team.Teams;
import org.cataclysm.server.chat.ChatMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

@Getter @Setter
public class PlayerData {
    private final String nickname;
    private final HashMap<String, Integer> upgrades;
    private final HashMap<String, Integer> cooldowns;

    private int poppedTotems;
    private float mortalityPercentage;
    private @Nullable String deathMessage;

    private String team;
    private String roleType;

    private ChatMode chatMode;

    public PlayerData(String nickname) {
        this.nickname = nickname;
        this.mortalityPercentage = 1.0F;
        this.upgrades = this.getDefaultMap();
        this.cooldowns = new HashMap<>();
        this.chatMode = ChatMode.GLOBAL;
        this.team = Teams.NONE.name();
        this.roleType = RoleType.MEMBER.name();
    }

    public PlayerData(@NotNull PlayerData data) {
        this.nickname = data.getNickname();
        this.poppedTotems = data.getPoppedTotems();
        this.mortalityPercentage = data.getMortalityPercentage();
        this.deathMessage = data.getDeathMessage();
        this.upgrades = data.getUpgrades();
        this.team = data.getTeam();
        this.roleType = data.getRoleType();
        this.chatMode = data.getChatMode();

        if (chatMode == null) this.chatMode = ChatMode.GLOBAL;
        if (data.getCooldowns() == null) this.cooldowns = new HashMap<>();
        else this.cooldowns = data.getCooldowns();
    }

    private @NotNull HashMap<String, Integer> getDefaultMap() {
        HashMap<String, Integer> map = new HashMap<>();
        for (var upgrade : Upgrades.values()) map.put(upgrade.name(), 0);
        return map;
    }

    @Nullable
    public Player getHolder() {
        var player = Bukkit.getPlayer(this.nickname);
        if (player == null || !player.isOnline()) return null;
        return player;
    }
}
