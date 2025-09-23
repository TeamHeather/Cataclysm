package org.cataclysm.game.events.raids.bosses;

import lombok.Getter;
import org.cataclysm.api.boss.CataclysmBoss;
import org.cataclysm.game.events.raids.bosses.calamity_hydra.CalamityHydra;
import org.cataclysm.game.events.raids.bosses.pale_king.PaleKing;
import org.cataclysm.game.events.raids.bosses.twisted_warden.TwistedWarden;

@Getter
public enum Bosses {
    TWISTED_WARDEN(new TwistedWarden()),
    CALAMITY_HYDRA(new CalamityHydra()),
    PALE_KING(new PaleKing()),

    ;

    private final CataclysmBoss instance;

    Bosses(CataclysmBoss boss) {this.instance = boss;}
}
