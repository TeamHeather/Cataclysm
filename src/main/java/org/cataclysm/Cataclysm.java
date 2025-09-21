package org.cataclysm;

import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import me.libraryaddict.disguise.DisguiseConfig;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.cataclysm.api.boss.CataclysmBoss;
import org.cataclysm.game.events.limited.EventManager;
import org.cataclysm.game.events.limited.data.EventLoader;
import org.cataclysm.api.item.crafting.CataclysmRecipes;
import org.cataclysm.api.listener.registrable.RegistrableUtils;
import org.cataclysm.api.mob.CataclysmMob;
import org.cataclysm.api.mob.MobUtils;
import org.cataclysm.api.mob.store.MobStore;
import org.cataclysm.api.structure.StructureManager;
import org.cataclysm.api.structure.data.StructureLoader;
import org.cataclysm.game.GameManager;
import org.cataclysm.game.data.GameDataManager;
import org.cataclysm.game.mob.task.MobTask;
import org.cataclysm.game.player.CataclysmPlayer;
import org.cataclysm.game.player.PlayerTask;
import org.cataclysm.game.player.data.PlayerLoader;
import org.cataclysm.game.player.survival.death.DeathSequence;
import org.cataclysm.game.events.raids.structures.RaidStructures;
import org.cataclysm.game.world.generator.CataclysmGenerator;
import org.cataclysm.game.world.ragnarok.Ragnarok;
import org.cataclysm.game.world.ragnarok.RagnarokLoader;
import org.cataclysm.game.world.time.TimeManager;
import org.cataclysm.game.world.time.data.TimeLoader;
import org.cataclysm.global.commands.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class Cataclysm extends JavaPlugin {
    private static final @Getter ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private static final @Getter HashMap<UUID, Integer> tasks = new HashMap<>();

    private static final @Getter Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final @Getter HashMap<String, CataclysmPlayer> cataclysmPlayers = new HashMap<>();

    private static @Getter @Setter TimeManager timeManager;
    private static @Getter @Setter int day;

    private static @Getter Cataclysm instance;
    private static @Getter MobStore store;

    private static @Getter @Setter Ragnarok ragnarok;
    private static @Getter @Setter @Nullable CataclysmBoss boss;

    private static @Getter @Setter DeathSequence deathSequence;
    private static @Getter @Setter EventManager eventManager;
    private static @Getter @Setter GameManager gameManager;

    @Override
    public void onEnable() {
        CataclysmMob.initializeMobConstructors();
        instance = this;
        store = new MobStore();
        try {
            StructureLoader.loadAll();
            PlayerLoader.loadAll();
            new TimeLoader().restore();
            new GameDataManager().restore();
            new RagnarokLoader().restore();
            new EventLoader().restore();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (var player : Bukkit.getOnlinePlayers()) {
            if (ragnarok != null) ragnarok.getBossBar().addViewer(player);
            if (eventManager != null) eventManager.barManager.bossBar.addViewer(player);
        }

        PaperCommandManager paperCommandManager = new PaperCommandManager(this);
        paperCommandManager.getCommandCompletions().registerAsyncCompletion("@CataclysmMobs", c -> MobUtils.getMobNames());
        paperCommandManager.registerCommand(new CataclysmCommand());
        paperCommandManager.registerCommand(new StaffCommand());
        paperCommandManager.registerCommand(new PodiumCommand());
        paperCommandManager.registerCommand(new RaidCommand());

        RegistrableUtils.registerListeners();
        CataclysmRecipes.updateRecipes();

        new PlayerTask().startTickTask(20);
        new MobTask().startTickTask(40);

        for (RaidStructures raidStructures : RaidStructures.values()) {
            Listener listener = raidStructures.getStructure().getListener();
            if (listener != null)
                Bukkit.getPluginManager().registerEvents(listener, Cataclysm.getInstance());
        }

        CataclysmGenerator.setUp();
        DisguiseConfig.setPlayerNameType(DisguiseConfig.PlayerNameType.VANILLA);

        Bukkit.getConsoleSender().sendMessage("   ___   _ _____ _   ___ _ __   _____ __  __ ");
        Bukkit.getConsoleSender().sendMessage("  / __| /_\\_   _/_\\ / __| |\\ \\ / / __|  \\/  |");
        Bukkit.getConsoleSender().sendMessage(" | (__ / _ \\| |/ _ \\ (__| |_\\ V /\\__ \\ |\\/| |");
        Bukkit.getConsoleSender().sendMessage("  \\___/_/ \\_\\_/_/ \\_\\___|____|_| |___/_|  |_|");
        Bukkit.getConsoleSender().sendMessage("");

        Bukkit.getConsoleSender().sendMessage("Cataclysm has been succesfully enabled.");
    }

    @Override
    public void onDisable() {
        if (ragnarok != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ragnarok.getBossBar().removeViewer(player);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (ragnarok != null) ragnarok.getBossBar().removeViewer(player);
            if (eventManager != null) eventManager.barManager.bossBar.removeViewer(player);
        }

        try {
            StructureManager.getInstance().shutdown();
            StructureLoader.saveAll();
            PlayerLoader.saveAll();

            new TimeLoader().save();
            new GameDataManager().save();
            new RagnarokLoader().save();
            new EventLoader().save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tasks.forEach((uuid, task) -> Bukkit.getScheduler().cancelTask(task));
        tasks.clear();
        Bukkit.getConsoleSender().sendMessage("Cataclysm has been succesfully disabled.");
    }
}