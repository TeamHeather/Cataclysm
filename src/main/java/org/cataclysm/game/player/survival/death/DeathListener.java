package org.cataclysm.game.player.survival.death;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.cataclysm.Cataclysm;
import org.cataclysm.api.data.PersistentData;
import org.cataclysm.api.listener.registrable.Registrable;
import org.cataclysm.game.events.raids.bosses.pale_king.PaleKing;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

@Registrable
public class DeathListener implements Listener {

    @EventHandler
    private void onDeath(@NotNull PlayerDeathEvent event) {
        var player = event.getPlayer();
        var loc = player.getLocation();
        var world = player.getWorld();
        if (world.getName().equals("world_beta")) return;

        if (Cataclysm.getBoss() != null && Cataclysm.getBoss().getController().equals(player)) {
            event.deathMessage(null);
            event.setCancelled(true);
            return;
        }

        player.setGameMode(GameMode.SPECTATOR);
        if (loc.getY() < -63) player.teleport(loc.set(loc.getX(), 64, loc.getZ()));
        var data = Cataclysm.getGameManager().data();
        data.setDeathCount(data.getDeathCount() + 1);

        var deathInChamber = PersistentData.get(player, "DEATH-IN-CHAMBER", PersistentDataType.BOOLEAN);
        var bossfight = Cataclysm.getBoss() != null;

        DeathAltar altar = new DeathAltar(player);
        if ((deathInChamber == null || !deathInChamber) && !bossfight) altar.placeComplex();
        else altar.placeSimple();

        Audience audience = Audience.audience(Bukkit.getOnlinePlayers());
        var sequence = new DeathSequence(event, audience);
        var currentSequence = Cataclysm.getDeathSequence();

        if (currentSequence != null) {
            if (currentSequence.canStop) {
                currentSequence.stop();
                Cataclysm.setDeathSequence(sequence);
            }
        }

        if (Cataclysm.getBoss() != null && Cataclysm.getBoss() instanceof PaleKing king && king.phase.getCurrent() > 1) {
            sequence.paleVoid();
            sequence.cast(DeathTitleType.SIMPLE);
            event.deathMessage(null);
            event.setCancelled(true);
            return;
        }

        String banMsg;
        var titleType = bossfight ? DeathTitleType.SIMPLE : DeathTitleType.ANIMATION;
        if (player.getUniqueId().equals(UUID.fromString("87bc8c76-68de-416b-834b-33296b1e8679")) ||
        player.getName().equals("LAWOFBALANCE")) {
            banMsg = "Me limpio el culo con lo sinco deo";
            titleType = DeathTitleType.FABO;
        } else {
            banMsg = "Has encontrado un destino terrible...";
        }
        sequence.cast(titleType);

        event.deathMessage(null);
        event.setCancelled(true);

        if (player.isOp()) return;

        Bukkit.getScheduler().runTaskLater(Cataclysm.getInstance(), () -> {
            player.ban(banMsg, (Date) null ,"", true);
            player.kick();
        }   , 145);
    }

}