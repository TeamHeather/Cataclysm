package org.cataclysm.api.boss;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.cataclysm.Cataclysm;
import org.cataclysm.api.boss.ability.Ability;
import org.cataclysm.api.boss.ability.AbilityBooster;
import org.cataclysm.api.boss.events.BossCastAbilityEvent;
import org.cataclysm.api.boss.events.BossChannelAbilityEvent;
import org.cataclysm.api.boss.events.BossFightEndEvent;
import org.cataclysm.api.item.ItemBuilder;
import org.cataclysm.api.listener.registrable.Registrable;
import org.cataclysm.game.events.raids.bosses.pale_king.PaleKing;
import org.cataclysm.global.utils.chat.ChatMessenger;
import org.cataclysm.global.utils.text.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

@Registrable
public class BossListener implements Listener {

    @EventHandler
    public void onBossFightEnd(BossFightEndEvent event) {
        CataclysmBoss boss = event.getBoss();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 0.9F);
            player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1.50F);
            player.showTitle(Title.title(
                    MiniMessage.miniMessage().deserialize("<#a18d60>¡Incursión Finalizada!"),
                    MiniMessage.miniMessage().deserialize("<#b0a897>" + boss.getName() + " derrotado"),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(4), Duration.ofSeconds(2))
            ));
            boss.getBossBar().removeViewer(player);
            boss.getHealthBar().removeViewer(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        var bossFight = Cataclysm.getBoss();
        var damager = event.getDamager();

        if (bossFight == null || !bossFight.controller.equals(damager)) return;

        if (damager instanceof AreaEffectCloud || (bossFight instanceof PaleKing paleKing && paleKing.phase.elapsing)) {
            event.setCancelled(true);
            return;
        }

        if (damager instanceof Player player) {
            var itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.MACE) event.setDamage(0);
        }
    }

    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var bossFight = Cataclysm.getBoss();
        if (bossFight == null || !bossFight.controller.equals(player)) return;

        var cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.CUSTOM) event.setDamage(0);

        player.setFireTicks(0);

        List<EntityDamageEvent.DamageCause> immunities = List.of(
                EntityDamageEvent.DamageCause.FALL,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
                EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
                EntityDamageEvent.DamageCause.LIGHTNING
        );

        if (immunities.contains(cause)) {
            event.setCancelled(true);
            return;
        }

        final var damage = (int) event.getDamage();
        bossFight.health -= damage;
        bossFight.updateBar();

        event.setDamage(0);

        //if (bossFight.health <= 0) bossFight.stopFight();
    }

    @EventHandler
    private void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var bossFight = Cataclysm.getBoss();
        if (bossFight == null || !bossFight.controller.equals(player)) return;

        event.setFoodLevel(20);
        event.setCancelled(true);
    }

    @EventHandler
    private void onBossCastAbility(BossCastAbilityEvent event) {
        var ability = event.getAbility();
        var boss = event.getBoss();

        if (ability.isBoosted()) {
            Bukkit.getScheduler().runTaskLater(Cataclysm.getInstance(), () -> {
                ability.setBoosted(false);
                boss.setBoosted(false);
            }, 20);
        }
    }

    @EventHandler
    private void onBossChannelAbility(BossChannelAbilityEvent event) {
        var boss = event.getBoss();
        if (!boss.getAbilityVisibility()) return;

        var ability = event.getAbility().clone();

        if (!ability.isBroadcast()) return;

        var glitchedCondition = (boss.isBoosted() && !(ability instanceof AbilityBooster));

        var notification = getTextColor(ability);
        if (glitchedCondition) {
            notification = TextUtils.buildGlitchedNotification(notification);
            ability.setChannelTime(ability.getChannelTime() / 2);
        } else if (boss instanceof PaleKing paleKing && paleKing.phase.getCurrent() >= 1) {
            ability.setChannelTime(ability.getChannelTime() / 2);
        }

        var playersInArena = boss.arena.getPlayersInArena();
        for (var player : playersInArena) {
            ChatMessenger.sendMessage(player, notification);
            player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 1.0F, 0.85F);
            player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 1.0F, 1.25F);
            if (glitchedCondition) player.playSound(player, Sound.ENTITY_ZOMBIE_HORSE_DEATH, 1.0F, 0.75F);
        }
    }

    private static @NotNull String getTextColor(Ability ability) {
        var textColor = ChatMessenger.getTextColor();
        return "<#CFCFCF>☠ " + textColor + "El jefe usará la habilidad " + ability.getDisplay() + textColor + ".";
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        var action = event.getAction();
        if (action.isLeftClick()) return;

        var itemStack = event.getItem();
        if (itemStack == null) return;

        var builder = new ItemBuilder(itemStack);

        var id = builder.getID();
        if (id == null) return;

        var player = event.getPlayer();
        var isController = CataclysmBoss.isController(player);
        if (!isController || player.hasCooldown(itemStack)) return;

        var bossFight = Cataclysm.getBoss();
        if (bossFight == null) return;

        var abilities = bossFight.abilityManager.getAbilities();
        abilities.forEach(ability -> {
            var triggerID = new ItemBuilder(ability.getTrigger()).getID();
            if (triggerID == null || !triggerID.equals(id)) return;
            bossFight.castAbility(ability);
            event.setCancelled(true);
        });
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        var bossFight = Cataclysm.getBoss();

        if (bossFight != null) {
            bossFight.bossBar.addViewer(player);
            bossFight.healthBar.addViewer(player);
        }

        var controlling = CataclysmBoss.isController(player);
        if (controlling) {
            if (bossFight != null) bossFight.setController(player);
            else CataclysmBoss.setControllerData(player, false);
        }
    }

    @EventHandler
    private void onEntityTargetBoss(EntityTargetEvent event) {
        var bossFight = Cataclysm.getBoss();
        if (bossFight == null) return;

        if (event.getTarget() != null && event.getTarget() == bossFight.controller) event.setCancelled(true);
    }
}
