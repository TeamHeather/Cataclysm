package org.cataclysm.game.events.raids.structures;

import lombok.Getter;
import org.cataclysm.api.structure.raid.RaidStructure;
import org.cataclysm.game.events.raids.structures.mother.Mother;

@Getter
public enum RaidStructures {
    TWISTED_NEST(new TwistedNest()),
    HYDRAS_DUNGEON(new HydrasDungeon()),
    PALE_PALACE(new PalePalace()),
    MOTHER(new Mother()),

    ;

    private final RaidStructure structure;

    RaidStructures(RaidStructure raidStructure) {
        this.structure = raidStructure;
    }
}
