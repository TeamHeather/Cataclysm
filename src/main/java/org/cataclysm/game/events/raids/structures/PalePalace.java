package org.cataclysm.game.events.raids.structures;

import org.bukkit.Location;
import org.cataclysm.api.boss.CataclysmArea;
import org.cataclysm.api.structure.raid.RaidStructure;
import org.cataclysm.game.world.Dimensions;

public class PalePalace extends RaidStructure {
    public PalePalace() {
        super("PALE_PALACE");
    }

    @Override
    public CataclysmArea getArea() {
        return new CataclysmArea(
                new Location(
                        Dimensions.THE_END.createWorld(),
                        -145, 30, -648), 65);
    }
}