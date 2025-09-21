package org.cataclysm.api.mob.store;

import lombok.Getter;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.cataclysm.api.mob.CataclysmMob;
import org.cataclysm.api.mob.MobLoader;
import org.cataclysm.global.utils.security.CataclysmToken;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MobStore {
    protected final List<MobBox> store;
    private final Storer storer;

    public MobStore() {
        this.store = new ArrayList<>();
        this.storer = new Storer(this.store);
    }

    public record Storer(List<MobBox> store) {
        public void store(CataclysmMob cataclysmMob) {
            store.add(new MobBox(cataclysmMob.getMobToken(), cataclysmMob));
        }

        public void respawn(LivingEntity livingEntity) {
            var token = CataclysmMob.getToken(livingEntity);
            respawn(getStoredBoxWithToken(token), livingEntity);
        }

        private void respawn(MobBox box, LivingEntity livingEntity) {
            if (box == null) {
                livingEntity.remove();
                return;
            }

            this.store.remove(box);
            CataclysmMob mob = box.cataclysmMob;

            mob.restore(((CraftLivingEntity) livingEntity).getHandle());
            mob.cloneMob(livingEntity.getLocation());

            var structure = CataclysmMob.getStructure(livingEntity);
            if (structure != null) mob.setStructure(structure);

            var file = MobLoader.getMobFile(structure, livingEntity);
            if (file == null) return;
            if (mob.getListener() != null) HandlerList.unregisterAll(mob.getListener());

            livingEntity.remove();
        }

        public void removeFromStore(CataclysmToken token) {
            MobBox box = getStoredBoxWithToken(token);
            if (box != null) {
                store.remove(box);
            }
        }

        public @Nullable CataclysmMob getMobWithToken(CataclysmToken cataclysmToken) {
            MobBox box = this.getStoredBoxWithToken(cataclysmToken);
            if (box != null) return box.cataclysmMob;
            else return  null;
        }

        public @Nullable MobBox getStoredBoxWithToken(CataclysmToken cataclysmToken) {
            for (MobBox box : new ArrayList<>(store)) {
                if (box.cataclysmToken().key().equals(cataclysmToken.key())) return box;
            }
            return null;
        }
    }

    public record MobBox(CataclysmToken cataclysmToken, CataclysmMob cataclysmMob) {
    }
}