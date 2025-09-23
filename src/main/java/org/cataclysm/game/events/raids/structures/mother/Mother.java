package org.cataclysm.game.events.raids.structures.mother;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minecraft.world.level.Level;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.cataclysm.Cataclysm;
import org.cataclysm.api.boss.CataclysmArea;
import org.cataclysm.api.mob.CataclysmMob;
import org.cataclysm.api.structure.raid.RaidStructure;
import org.cataclysm.game.mob.custom.cataclysm.mirage.*;
import org.cataclysm.game.world.Dimensions;

import java.util.List;

public class Mother extends RaidStructure {
    public Mother() {
        super("MOTHER");
        super.setListener(new MotherListener(this));
    }

    @Override
    public CataclysmArea getArea() {
        Location location = new Location(Dimensions.THE_END.createWorld(), 0, 53, 0);
        return new CataclysmArea(location, 500);
    }

    static class MotherListener implements Listener {
        private final Mother mother;

        public MotherListener(Mother mother) {
            this.mother = mother;
        }

        @EventHandler
        public void onEntityExplode(EntityExplodeEvent event) {
            var location = event.getLocation();
            if (!this.mother.getArea().isLocationInArea(location)) return;

            Entity entity = event.getEntity();
            World world = entity.getWorld();
            if (entity instanceof WindCharge || entity instanceof BreezeWindCharge) return;

            List<EntityType> types = List.of(
                    EntityType.CREEPER,
                    EntityType.ARMADILLO,
                    EntityType.PUFFERFISH,
                    EntityType.SHULKER,
                    EntityType.SHULKER_BULLET
            );

            if (entity instanceof Explosive || types.contains(entity.getType())) {
                world.playSound(location, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1);
                world.createExplosion(entity, event.getYield(), false, false);
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onEntitySpawner(SpawnerSpawnEvent event) {
            var location = event.getLocation();
            if (!this.mother.getArea().isLocationInArea(location)) return;

            Entity entity = event.getEntity();
            World world = entity.getWorld();
            Level level = ((CraftWorld) world).getHandle();
            if (entity instanceof WindCharge || entity instanceof BreezeWindCharge) return;
            CataclysmMob mobToSpawn = null;

            switch (entity.getType()) {
                case GHAST -> mobToSpawn = new MirageWhale(level);
                case CREEPER -> mobToSpawn = new MirageCreeper(level);
                case PHANTOM -> mobToSpawn = new MirageEye(level);
                case ZOMBIE -> mobToSpawn = new MirageBeast(level);
                case ENDERMAN -> mobToSpawn = new MirageEnderman(level);
                case ENDERMITE -> mobToSpawn = new MirageEndermite(level);
                case SKELETON -> mobToSpawn = new MirageSkeleton(level);
            }

            if (mobToSpawn != null) {
                entity.remove();
                mobToSpawn.addFreshEntity(entity.getLocation());
            }

        }

        @EventHandler
        public void onEntityDeath(EntityDeathEvent event) {
            var location = event.getEntity().getLocation();
            if (!this.mother.getArea().isLocationInArea(location)) return;
            event.getDrops().clear();
        }

        @EventHandler
        public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
            Player player = event.getPlayer();
            if (!this.mother.getArea().isLivingEntityInArea(player)) return;

            Material bucket = event.getBucket();
            if (bucket == Material.LAVA_BUCKET || bucket == Material.WATER_BUCKET) {
                event.setCancelled(true);
                event.getPlayer().playSound(Sound.sound(Key.key("block.netherite_block.break"), Sound.Source.BLOCK, 1.0F, 0.65F));
            }
        }

        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event) {
//            Player player = event.getPlayer();
//            if (!this.mother.getArea().isLivingEntityInArea(player)) return;
//
//            if (player.getGameMode() == GameMode.CREATIVE) return;
//
//            event.setCancelled(true);
//            player.playSound(Sound.sound(Key.key("block.netherite_block.break"), Sound.Source.BLOCK, 1.0F, 0.65F));
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
//            Player player = event.getPlayer();
//            if (!this.mother.getArea().isLivingEntityInArea(player)) return;
//
//            if (player.getGameMode() == GameMode.CREATIVE) return;
//
//            Block block = event.getBlock();
//            List<Material> allowedBlocks = List.of(
//                    Material.FIRE,
//                    Material.SOUL_FIRE,
//                    Material.NETHER_WART,
//                    Material.SPAWNER
//            );
//
//            if (allowedBlocks.contains(block.getType())) return;
//
//            event.setCancelled(true);
//            player.playSound(Sound.sound(Key.key("block.netherite_block.break"), Sound.Source.BLOCK, 1.0F, 0.65F));
        }
    }
}
