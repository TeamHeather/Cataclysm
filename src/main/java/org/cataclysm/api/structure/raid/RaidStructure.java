package org.cataclysm.api.structure.raid;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Listener;
import org.cataclysm.api.boss.CataclysmArea;
import org.cataclysm.api.structure.schematic.SchematicLoader;

@Getter
public abstract class RaidStructure {
    private final String name;
    private @Setter Listener listener;

    public RaidStructure(String name) {
        this.name = name;
    }

    public void pasteStructure(boolean ignoreAir) {
        SchematicLoader loader = new SchematicLoader("schematics/" + this.name + ".schem");
        loader.pasteSchematic(getArea().center().clone().add(0, -1, 0), ignoreAir);
    }

    public abstract CataclysmArea getArea();
}
