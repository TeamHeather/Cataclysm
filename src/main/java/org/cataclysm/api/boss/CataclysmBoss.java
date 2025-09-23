package org.cataclysm.api.boss;

import lombok.Getter;
import lombok.Setter;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.DisguiseConfig;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.cataclysm.Cataclysm;
import org.cataclysm.api.boss.ability.Ability;
import org.cataclysm.api.boss.ability.AbilityBooster;
import org.cataclysm.api.boss.ability.AbilityManager;
import org.cataclysm.api.boss.events.BossCastAbilityEvent;
import org.cataclysm.api.boss.events.BossChannelAbilityEvent;
import org.cataclysm.api.boss.events.BossFightEndEvent;
import org.cataclysm.api.boss.events.BossFightStartEvent;
import org.cataclysm.api.data.PersistentData;
import org.cataclysm.api.Soundtrack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public abstract class CataclysmBoss implements Cloneable {
    protected final @Getter BossThread thread = new BossThread(this);
    protected final @Getter AbilityManager abilityManager = new AbilityManager();
    protected final @Getter Soundtrack soundtrack = new Soundtrack();

    protected @Getter BossBar bossBar;
    protected @Getter BossBar healthBar;

    protected @Getter Player controller;
    protected @Getter CataclysmArea arena;

    protected @Getter @Setter String name;
    public final @Getter double maxHealth;
    public @Getter @Setter double health;

    protected @Nullable Listener listener;

    public CataclysmBoss(String name, int health) {
        this.name = name;
        this.health = health;
        this.maxHealth = health;
        this.bossBar = this.buildBossBar();
        this.healthBar = BossBar.bossBar(this.getHealthBarName(), 1.0F, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        this.registerSoundtrack();
        this.registerAbilities();
    }

    public abstract void onStart();

    public abstract void onStop();

    public abstract void registerSoundtrack();

    public abstract void registerAbilities();

    public abstract void tick();

    public abstract BossBar buildBossBar();

    public void startFight() {
        if (this.listener != null) Bukkit.getPluginManager().registerEvents(this.listener, Cataclysm.getInstance());
        this.thread.startTickTask();

        setUpBossBar(true);
        setUpController(true);

        new BossFightStartEvent(this).callEvent();
        onStart();

        Cataclysm.setBoss(this);
    }

    public void stopFight() {
        this.thread.service.shutdownNow();
        this.soundtrack.stopAll();

        setUpBossBar(false);
        setUpController(false);

        new BossFightEndEvent(this).callEvent();
        onStop();

        Cataclysm.setBoss(null);
    }

    public void castAbility(@NotNull Ability ability) {
        if (this.controller.hasCooldown(ability.getTrigger())) return;
        this.controller.setCooldown(ability.getTrigger(), (int) (ability.getCooldown() * 20));

        ability.setBoosted(this.isBoosted());

        new BossChannelAbilityEvent(ability, this.getFighters(), this).callEvent();
        ability.channel();

        double channelTime = ability.getChannelTime();

        TimeUnit timeUnit;
        if (channelTime % 1 == 0) timeUnit = TimeUnit.SECONDS;
        else {
            timeUnit = TimeUnit.MILLISECONDS;
            channelTime *= 1000;
        }

        this.thread.service.schedule(() -> Bukkit.getScheduler().runTask(Cataclysm.getInstance(), () -> {
            new BossCastAbilityEvent(ability, this.getFighters(), this).callEvent();
            ability.cast();
        }), (int) channelTime, timeUnit);
    }

    public void setController(@NotNull Player controller) {
        this.controller = controller;
        setUpInventory();
        setControllerData(controller, true);
    }

    public void setUpBossBar(boolean bossBar) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (bossBar) {
                this.bossBar.addViewer(player);
                this.healthBar.addViewer(player);
            } else {
                this.bossBar.removeViewer(player);
                this.healthBar.removeViewer(player);
            }
        }
    }

    public void setUpController(boolean fighting) {
        if (this.controller == null) return;

        double value;
        if (fighting) {
            this.controller.setGameMode(GameMode.ADVENTURE);
            this.controller.teleport(this.arena.center());
            this.setAbilityVisibility(true);
            this.setUpInventory();
            value = 1.2;
        } else {
            this.controller.setGameMode(GameMode.SPECTATOR);
            this.controller.getInventory().clear();
            value = 1;
        }

        var stepHeight = this.controller.getAttribute(Attribute.STEP_HEIGHT);
        if (stepHeight != null) stepHeight.setBaseValue(value);

        setControllerData(this.controller, fighting);
    }

    private void setUpInventory() {
        if (this.controller == null) return;

        this.controller.getInventory().clear();

        for (var ability : this.abilityManager.getAbilities()) {
            this.controller.getInventory().addItem(ability.getTrigger());
        }
    }

    public Collection<LivingEntity> getNearbyLivingEntities(@NotNull Location location, double radius) {
        var livingEntities = location.getNearbyLivingEntities(radius, radius, radius);
        livingEntities.removeIf(livingEntity -> this.controller.equals(livingEntity));
        return livingEntities;
    }

    public Collection<Player> getNearbyFighters(@NotNull Location location, double radius) {
        var fighters = location.getNearbyPlayers(radius, radius, radius);
        fighters.removeIf(fighter -> fighter.getGameMode() == GameMode.SPECTATOR || this.controller.equals(fighter));
        return fighters;
    }

    public Collection<Player> getFighters() {
        var arena = this.arena;
        return this.getNearbyFighters(arena.center(), arena.radius());
    }

    public void setBoosted(boolean boosted) {
        PersistentData.set(this.controller, "BOOSTED", PersistentDataType.BOOLEAN, boosted);
    }

    public boolean isBoosted() {
        return Boolean.TRUE.equals(PersistentData.get(this.controller, "BOOSTED", PersistentDataType.BOOLEAN));
    }

    public void setAbilityVisibility(boolean show) {
        PersistentData.set(this.controller, "SAM", PersistentDataType.BOOLEAN, show);
    }

    public boolean getAbilityVisibility() {
        return Boolean.TRUE.equals(PersistentData.get(this.controller, "SAM", PersistentDataType.BOOLEAN));
    }

    public void resetAbilityCooldown() {
        this.controller.resetCooldown();
        this.abilityManager.getAbilities().forEach(ability -> {
            if (ability instanceof AbilityBooster) return;
            this.controller.setCooldown(ability.getTrigger(), 0);
        });
    }

    public void updateBar() {
        float progress = (float) ((float) this.health/this.maxHealth);
        this.bossBar.progress(progress);
        this.healthBar.name(this.getHealthBarName());
    }

    public static void setControllerData(@NotNull Player player, boolean controlling) {
        PersistentData.set(player, "CONTROLLER", PersistentDataType.BOOLEAN, controlling);
    }

    public static boolean isController(@NotNull Player player) {
        return Boolean.TRUE.equals(PersistentData.get(player, "CONTROLLER", PersistentDataType.BOOLEAN));
    }

    public void resetAttribute(Attribute attribute) {
        AttributeInstance instance = this.controller.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(instance.getDefaultValue());
    }

    public void setAttribute(Attribute attribute, double baseValue) {
        AttributeInstance instance = this.controller.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(baseValue);
    }

    public double getAttribute(Attribute attribute) {
        AttributeInstance instance = this.controller.getAttribute(attribute);
        if (instance != null) return instance.getValue();
        return 0.0;
    }

    private Component getHealthBarName() {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return MiniMessage.miniMessage().deserialize("« " + formatter.format(this.health) + " »");
    }

    public void removeModel() {
        if (DisguiseAPI.getDisguise(controller) != null) {
            DisguiseAPI.getDisguise(controller).removeDisguise();
        }
    }

    public void damage(LivingEntity livingEntity, double amount) {
        if (livingEntity.equals(getController())) return;
        livingEntity.damage(amount);
        if (livingEntity.getNoDamageTicks() == 0)
            livingEntity.setNoDamageTicks(25);
    }

    public void updateModel(EntityType type, String name) {
        if (DisguiseAPI.getDisguise(controller) != null) DisguiseAPI.getDisguise(controller).removeDisguise();
        DisguiseConfig.setPlayerNameType(DisguiseConfig.PlayerNameType.VANILLA);

        Entity entity = controller.getWorld().spawnEntity(controller.getLocation(), type, CreatureSpawnEvent.SpawnReason.CUSTOM);
        entity.customName(MiniMessage.miniMessage().deserialize(name));
        entity.setCustomNameVisible(false);

        DisguiseAPI.disguiseToAll(controller, DisguiseAPI.constructDisguise(entity));
        DisguiseAPI.setActionBarShown(controller, false);
        DisguiseAPI.getDisguise(controller).getInternals().setSelfDisguiseTallScaleMax(.25);

        entity.remove();
    }

    @Override
    public CataclysmBoss clone() {
        try {
            return (CataclysmBoss) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}