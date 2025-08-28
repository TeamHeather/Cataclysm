package org.cataclysm.game.player.tag.role;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.cataclysm.game.player.data.PlayerData;
import org.cataclysm.global.utils.chat.ChatMessenger;
import org.jetbrains.annotations.NotNull;

public record RoleManager(PlayerData data) {

    public @NotNull Component build() {
        if (this.getRole() == null) this.setRole(RoleType.MEMBER);
        return MiniMessage.miniMessage().deserialize(ChatMessenger.getTextColor() + "[<#ffffff>" + this.getRole().getBadge() + ChatMessenger.getTextColor() + "]");
    }

    public void setRole(@NotNull RoleType type) {
        this.data.setRoleType(type.name());
    }

    public RoleType getRole() {
        if (this.data.getRoleType() == null) return RoleType.MEMBER;
        else return RoleType.valueOf(this.data.getRoleType());
    }

}
