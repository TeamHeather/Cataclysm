package org.cataclysm.game.events.raids.bosses.pale_king.abilities;

import org.bukkit.Material;
import org.cataclysm.api.boss.ability.Ability;
import org.cataclysm.game.events.raids.bosses.BossAbility;
import org.cataclysm.game.events.raids.bosses.pale_king.PaleKing;

public abstract class PaleAbility extends Ability {
    protected final PaleKing king;

    public PaleAbility(PaleKing king, Material triggerMaterial, String name, int channelTime, double cooldown) {
        this(king, triggerMaterial, name, channelTime, cooldown, false);
    }

    public PaleAbility(PaleKing king, Material triggerMaterial, String name, int channelTime, double cooldown, boolean ultimate) {
        this(king, triggerMaterial, name, channelTime, cooldown, getColors(ultimate));
    }

    public PaleAbility(PaleKing king, Material triggerMaterial, String name, int channelTime, double cooldown, String... colors) {
        super(triggerMaterial, name, channelTime, cooldown, true, colors);
        this.king = king;
    }

    private static String[] getColors(boolean ultimate) {
        String[] colors;
        if (ultimate) colors = new String[]{"#666666", "#1c1c1c"};
        else colors = new String[]{"#bdbebf", "#ffffff"};
        return colors;
    }

    @Override
    public void channel() {}

    @Override
    public void cast() {}
}
