package org.cataclysm.global.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.cataclysm.Cataclysm;
import org.cataclysm.api.boss.CataclysmBoss;
import org.cataclysm.api.data.PersistentData;
import org.cataclysm.api.item.ItemCatalogue;
import org.cataclysm.api.mob.CataclysmMob;
import org.cataclysm.api.structure.CataclysmStructure;
import org.cataclysm.api.structure.data.StructureLoader;
import org.cataclysm.api.structure.raid.RaidStructure;
import org.cataclysm.game.block.arcane.table.ArcaneTableMob;
import org.cataclysm.game.events.limited.CataclysmEvents;
import org.cataclysm.game.effect.ImmunityEffect;
import org.cataclysm.game.effect.MortemEffect;
import org.cataclysm.game.effect.PaleCorrosionEffect;
import org.cataclysm.game.events.raids.bosses.Bosses;
import org.cataclysm.game.events.raids.structures.RaidStructures;
import org.cataclysm.game.items.CataclysmItems;
import org.cataclysm.game.mob.custom.block.CalamityVault;
import org.cataclysm.game.player.CataclysmPlayer;
import org.cataclysm.game.player.mechanics.upgrade.Upgrades;
import org.cataclysm.game.player.tag.role.RoleManager;
import org.cataclysm.game.player.tag.role.RoleType;
import org.cataclysm.game.player.tag.team.TeamManager;
import org.cataclysm.game.player.tag.team.Teams;
import org.cataclysm.game.events.raids.bosses.twisted_warden.TwistedWarden;
import org.cataclysm.game.world.Dimensions;
import org.cataclysm.game.world.dungeons.*;
import org.cataclysm.game.world.ragnarok.Ragnarok;
import org.cataclysm.game.world.ragnarok.RagnarokData;
import org.cataclysm.game.world.ragnarok.RagnarokManager;
import org.cataclysm.global.utils.chat.ChatMessenger;
import org.cataclysm.server.tablist.CataclysmTablist;

import java.time.Duration;
import java.util.UUID;

@CommandAlias("staff")
@CommandPermission("admin.perms")
public class StaffCommand extends BaseCommand {

    @Subcommand("boss start")
    @CommandCompletion(" true|false")
    private void start(CommandSender commandSender, Bosses boss, boolean pasteArena) {
        if (!(commandSender instanceof Player player)) return;

        if (pasteArena) {
            RaidStructures structure = switch (boss) {
                case TWISTED_WARDEN -> RaidStructures.TWISTED_NEST;
                case CALAMITY_HYDRA -> RaidStructures.HYDRAS_DUNGEON;
                case PALE_KING -> RaidStructures.PALE_PALACE;
            };
            RaidStructure instance = structure.getStructure();
            instance.pasteStructure(false);
            ChatMessenger.sendStaffMessage(player, "Se ha pegado la arena " + instance.getName().toUpperCase() + ": ");
            player.sendMessage(instance.getArea().toString());
        }

        for (Player players : Bukkit.getOnlinePlayers()) {
            players.teleport(boss.getInstance().getArena().center());
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 100, 0));
        }

        ChatMessenger.sendStaffMessage(player, "El jefe iniciará en 5 segundos");
        Bukkit.getScheduler().runTaskLater(Cataclysm.getInstance(), () -> {
            CataclysmBoss manager = boss.getInstance();
            manager.setController(player);
            manager.startFight();
        }, 100);
    }

    @Subcommand("boss stop")
    private void stop(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;

        CataclysmBoss manager = Cataclysm.getBoss();
        if (manager == null) {
            ChatMessenger.sendStaffMessage(player, "No hay un jefe activo.");
            return;
        }

        manager.stopFight();
        ChatMessenger.sendStaffMessage(player, "Jefe detenido.");
    }

    @Subcommand("event")
    @CommandCompletion(" start|stop")
    private void event(CataclysmEvents event, String action) {
        var eventManager = event.getEvent();
        switch (action) {
            case "start" -> eventManager.start();
            case "stop" -> eventManager.stop();
        }
    }

    @Subcommand("player role set")
    @CommandCompletion(" @players")
    private void roleSet(CommandSender commandSender, RoleType role, String username) {
        var player = Bukkit.getPlayer(username);

        if (player == null) return;

        var cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(player);
        new RoleManager(cataclysmPlayer.getData()).setRole(role);
        CataclysmTablist.organizePlayer(player);
    }

    @Subcommand("player team join")
    @CommandCompletion(" @players")
    private void teamJoin(CommandSender commandSender, Teams teams, String username) {
        var player = Bukkit.getPlayer(username);

        if (player == null) return;

        var cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(player);
        new TeamManager(cataclysmPlayer.getData()).setTeam(teams);
        CataclysmTablist.organizePlayer(player);
    }

    @Subcommand("teleport")
    private void teleport(CommandSender commandSender, Dimensions dimensions) {
        if (!(commandSender instanceof Player player)) return;
        player.teleport(dimensions.createWorld().getSpawnLocation());
    }

    @Subcommand("structure")
    @Description("Staff command to manage Cataclysm structures.")
    private void structure(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;
        ChatMessenger.sendStaffMessage(player, "Estructuras disponibles: " + CataclysmStructure.getStructures().size());
    }

    @Subcommand("structure loader load")
    private void structureLoaderLoad(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;
        ChatMessenger.sendStaffMessage(player, "Cargando estructuras...");
        StructureLoader.loadAll();
        ChatMessenger.sendStaffMessage(player, "Todas las estructuras han sido cargadas correcctamente.");
    }

    @Subcommand("structure list")
    @CommandCompletion("monolith|paragon_temple|calamity_chamber|mirage_citadel|pale_temple")
    private void structureRestore(CommandSender commandSender, String structureName) {
        if (!(commandSender instanceof Player player)) return;

        var structureHashMap = CataclysmStructure.getStructures();

        var amount = 0;
        for (var set : structureHashMap.entrySet()) {
            var structure = set.getValue();
            var config = structure.getConfig();

            if (config.getId().equalsIgnoreCase(structureName)) {
                amount++;
                var location = structure.getLevel().getLocation();
                String teleportMessage = "<click:run_command:'/tp " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "'>" +
                        "<hover:show_text:'<gray>Click to teleport to structure'>" +
                        amount + "-. <#7A7A7A>" + structure.getUuid() + "</hover></click>";

                player.sendMessage(MiniMessage.miniMessage().deserialize(teleportMessage));
            }
        }
    }

    @Subcommand("structure restore")
    @CommandCompletion("monolith|paragon_temple")
    private void structureRestore(CommandSender commandSender, String structureName, String uuid) {
        if (!(commandSender instanceof Player player)) return;

        player.sendMessage(MiniMessage.miniMessage().deserialize("Iniciando restauración de estructura..."));

        var structure = CataclysmStructure.getStructures().get(UUID.fromString(uuid));
        var config = structure.getConfig();

        if (config.getId().equalsIgnoreCase(structureName) && !structure.isLooted()) {
            var location = structure.getLevel().getLocation();

            player.sendMessage(MiniMessage.miniMessage().deserialize("<#ABABAB>1-. " + structure.getUuid()));

            String coordsClickable = "<click:run_command:'/tp " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "'>" +
                    "<hover:show_text:'<gray>Click to teleport to structure'>" +
                    "<#7A7A7A>" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " (" + config.getId() + ")</hover></click>";

            player.sendMessage(MiniMessage.miniMessage().deserialize(coordsClickable));

            structure.delete();
            structure.duplicate();

            player.sendMessage(MiniMessage.miniMessage().deserialize("✔ Restauración completada."));
        }
    }

    @Subcommand("structure generate PALE_TEMPLE")
    private void structureGeneratePaleTemple(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;

        for (var staff : Bukkit.getOnlinePlayers()) {
            if (!staff.isOp()) continue;
            ChatMessenger.sendStaffMessage(staff, player.getName() + " esta generando un Pale Temple");
        }

        var structure = new PaleTemple();
        structure.generate(player.getLocation(), "default");
    }

    @Subcommand("structure generate PARAGON_TEMPLE")
    @CommandCompletion("stripes|eye|dark")
    private void structureGenerateTemple(CommandSender commandSender, String variant) {
        if (!(commandSender instanceof Player player)) return;

        for (var staff : Bukkit.getOnlinePlayers()) {
            if (!staff.isOp()) continue;
            ChatMessenger.sendStaffMessage(staff, player.getName() + " esta generando un Paragon Temple (" + variant.toUpperCase() + ")");
        }

        var structure = new ParagonTemple();
        structure.generate(player.getLocation(), variant);
    }

    @Subcommand("structure generate MONOLITH")
    @CommandCompletion("stone|deepslate")
    private void structureGenerateMonolith(CommandSender commandSender, String variant) {
        if (!(commandSender instanceof Player player)) return;

        for (var staff : Bukkit.getOnlinePlayers()) {
            if (!staff.isOp()) continue;
            ChatMessenger.sendStaffMessage(staff, player.getName() + " esta generando un Monolito (" + variant.toUpperCase() + ")");
        }

        var structure = new Monolith();
        structure.generate(player.getLocation(), variant);
    }

    @Subcommand("structure generate CALAMITY_CHAMBERS")
    private void structureGenerateChamber(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;

        for (var staff : Bukkit.getOnlinePlayers()) {
            if (!staff.isOp()) continue;
            ChatMessenger.sendStaffMessage(staff, player.getName() + " esta generando una Calamity Chamber");
        }

        var structure = new CalamityChamber();
        structure.generate(player.getLocation(), "default");
    }

    @Subcommand("structure generate MIRAGE_CITADEL")
    private void structureGenerateMirageCitadel(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;

        for (var staff : Bukkit.getOnlinePlayers()) {
            if (!staff.isOp()) continue;
            ChatMessenger.sendStaffMessage(staff, player.getName() + " esta generando una Mirage Citadel");
        }

        var structure = new MirageCitadel();
        structure.generate(player.getLocation(), "default");
    }

    @Subcommand("structure generate GARDEN")
    @CommandCompletion("default")
    private void structureGenerateGarden(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;

        for (var staff : Bukkit.getOnlinePlayers()) {
            if (!staff.isOp()) continue;
            ChatMessenger.sendStaffMessage(staff, player.getName() + " esta generando un Pale Garden");
        }

        var structure = new PaleGarden();
        structure.generate(player.getLocation(), "default");
    }

    @Subcommand("give")
    @Description("Staff command to give a specified Cataclysm Item to a player.")
    private void give(CommandSender commandSender, @Optional CataclysmItems item, @Optional Integer amount) {
        if (!(commandSender instanceof Player player)) return;

        if (item == null) {
            new ItemCatalogue(player).open();
            return;
        }

        if (amount == null) amount = 1;

        var itemStack = item.build().clone();
        itemStack.setAmount(amount);

        player.getInventory().addItem(itemStack);
        ChatMessenger.sendStaffMessage(player, "Has recibido " + amount + "x " + item.name() + ".");
    }

    @Subcommand("summon")
    @CommandCompletion("@CataclysmMobs")
    private void summon(CommandSender sender, String mobName, @Optional Integer amount) {
        if (sender instanceof Player commandSender) {
            Location location = commandSender.getLocation();
            if (amount == null) amount = 1;
            for (int i = 0; i < amount; i++) {
                CataclysmMob mob = CataclysmMob.instantiateMob(mobName, ((CraftWorld) location.getWorld()).getHandle());
                if (mob != null) mob.addFreshEntity(location, CreatureSpawnEvent.SpawnReason.COMMAND);
            }
            Bukkit.getConsoleSender().sendMessage(commandSender.getName() + " summoned " + mobName + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")");
        }
    }

    @Subcommand("setBlock")
    @CommandCompletion("ArcaneTable|CalamityVault|VoidEssence")
    private void summonArcaneTable(CommandSender sender, String blockName) {
        if (!(sender instanceof Player player)) return;
        var level = ((CraftWorld) player.getWorld()).getHandle();

        switch (blockName) {
            case "ArcaneTable" -> new ArcaneTableMob(level).addFreshEntity(player.getLocation(), CreatureSpawnEvent.SpawnReason.COMMAND);
            case "CalamityVault" -> new CalamityVault(level).addFreshEntity(player.getLocation(), CreatureSpawnEvent.SpawnReason.COMMAND);
            default -> throw new IllegalArgumentException("Unknown block name: " + blockName);
        }
    }

    //TODO IMPROVE THIS
    @Subcommand("player cooldown")
    @CommandCompletion("@players")
    private void playerCooldown(String nickname) {
        Bukkit.getConsoleSender().sendMessage(nickname);
        CataclysmPlayer cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(nickname);
        if (cataclysmPlayer == null) throw new NullPointerException("Cataclysm Player is NULL.");
        cataclysmPlayer.getCooldownManager().getCooldowns().forEach(playerCooldown -> playerCooldown.stop(cataclysmPlayer.getData()));
    }

    @Subcommand("player mortality set")
    @CommandCompletion("@players <float>")
    private void playerMortality(CommandSender commandSender, String nickname, float decimal) {
        if (!(commandSender instanceof Player sender)) return;

        CataclysmPlayer cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(nickname);
        cataclysmPlayer.getMortalityManager().setValue(decimal);
        ChatMessenger.sendStaffMessage(sender, nickname + "'s mortality: " + cataclysmPlayer.getMortalityManager().getPercentage());
    }

    @Subcommand("player mortality get")
    @CommandCompletion("@players")
    private void playerMortalityGet(CommandSender commandSender, String nickname) {
        if (!(commandSender instanceof Player sender)) return;

        CataclysmPlayer cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(nickname);
        ChatMessenger.sendStaffMessage(sender, nickname + "'s mortality: " + cataclysmPlayer.getMortalityManager().getPercentage());
    }

    @Subcommand("player totems set")
    @CommandCompletion("@players <value>")
    private void playerTotemsSet(CommandSender commandSender,  String nickname, int value) {
        if (!(commandSender instanceof Player sender)) return;
        CataclysmPlayer cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(nickname);
        cataclysmPlayer.getTotemManager().setPoppedTotems(value);
        ChatMessenger.sendStaffMessage(sender, nickname + "'s popped totems: " + cataclysmPlayer.getTotemManager().getPoppedTotems());
    }

    @Subcommand("player totems get")
    @CommandCompletion("@players")
    private void playerTotemsGet(CommandSender commandSender, String nickname) {
        if (!(commandSender instanceof Player sender)) return;

        CataclysmPlayer cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(nickname);
        ChatMessenger.sendStaffMessage(sender, nickname + "'s popped totems: " + cataclysmPlayer.getTotemManager().getPoppedTotems());
    }

    @Subcommand("player deathmessage set")
    @CommandCompletion("@players <message>")
    private void playerDeathMessageSet(String nickname, String deathMessage) {
        CataclysmPlayer cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(nickname);
        cataclysmPlayer.getDeathMessageManager().setDeathMessage(deathMessage);
    }

    @Subcommand("player deathmessage get")
    @CommandCompletion("@players")
    private void playerDeathMessageGet(CommandSender commandSender, String nickname) {
        if (!(commandSender instanceof Player sender)) return;

        CataclysmPlayer cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(nickname);
        ChatMessenger.sendStaffMessage(sender, cataclysmPlayer.getDeathMessageManager().getFormattedChatMessage());
    }

    @Subcommand("player upgrades clear")
    @CommandCompletion("@players")
    private void upgradesClear(CommandSender commandSender, String nickname) {
        if (!(commandSender instanceof Player sender)) return;
        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        var um = CataclysmPlayer.getCataclysmPlayer(player).getUpgradeManager();
        for (var upgrade : Upgrades.values()) {
            um.setUpgradeLevel(upgrade, 0);
        }

        ChatMessenger.sendStaffMessage(sender, "Cleared " + nickname + "'s upgrades");
    }

    @Subcommand("player upgrades set")
    @CommandCompletion("@players <value>")
    private void upgradesSet(CommandSender commandSender, String nickname, Upgrades upgrade, int level) {
        if (!(commandSender instanceof Player sender)) return;
        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        var cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(player);
        cataclysmPlayer.getUpgradeManager().setUpgradeLevel(upgrade, level);

        ChatMessenger.sendStaffMessage(sender, "Set upgrade " + upgrade.name() + "for player " + nickname);
    }

    @Subcommand("player incursion reward set")
    @CommandCompletion("@players <value>")
    private void playerIncursionRewardSet(String nickname, int value) {
        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        PersistentData.set(player, "COMPLETED_INCURSIONS", PersistentDataType.INTEGER, value);
    }

    @Subcommand("player incursion reward get")
    @CommandCompletion("@players")
    private void playerIncursionRewardGet(CommandSender commandSender, String nickname) {
        if (!(commandSender instanceof Player sender)) return;

        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        var completedIncursions = PersistentData.get(player, "COMPLETED_INCURSIONS", PersistentDataType.INTEGER);
        ChatMessenger.sendStaffMessage(sender, "Completed incursions: " + completedIncursions);
    }

    @Subcommand("player incursion health set")
    @CommandCompletion("@players <incursionsPassed>")
    private void playerIncursionHealthSet(String nickname, int completedIncursions) {
        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        PersistentData.set(player, "INCURSION_EXTRA_HEALTH", PersistentDataType.INTEGER, completedIncursions);
    }

    @Subcommand("player incursion health get")
    @CommandCompletion("@players <incursionsCompleted>")
    private void playerIncursionHealthGet(String nickname) {
        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        var completedIncursions = PersistentData.get(player, "INCURSION_EXTRA_HEALTH", PersistentDataType.INTEGER);
        ChatMessenger.sendStaffMessage(player, "Incursions with extra health reward completed: " + completedIncursions);
    }

    @Subcommand("player noincursion health set")
    @CommandCompletion("@players <uncompletedIncursions>")
    private void playerNoIncursionHealthSet(String nickname, int notCompletedIncursions) {
        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        PersistentData.set(player, "NO_INCURSION_EXTRA_HEALTH", PersistentDataType.INTEGER, notCompletedIncursions);
    }

    @Subcommand("player noincursion health get")
    @CommandCompletion("@players <notCompletedIncursions>")
    private void playerNoIncursionHealthGet(String nickname) {
        var player = Bukkit.getPlayer(nickname);
        if (player == null) return;
        var notCompletedIncursions = PersistentData.get(player, "NO_INCURSION_EXTRA_HEALTH", PersistentDataType.INTEGER);
        ChatMessenger.sendStaffMessage(player, "Incursions with extra health reward not completed: " + notCompletedIncursions);
    }

    @Subcommand("broadcast")
    private void broadcast(String[] message) {
        if (message.length == 0) return;
        StringBuilder builder = new StringBuilder();
        for (String part : message) {
            builder.append(part).append(" ");
        }
        String finalMessage = builder.toString();
        ChatMessenger.broadcastMessage(ChatMessenger.getTextColor() + finalMessage);
        for (Player player : Bukkit.getOnlinePlayers()) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
    }

    @Subcommand("ragnarok start")
    @CommandCompletion("<value>")
    private void ragnarokStart(double value) {
        int hours = (int) (value * 3600);
        var ragnarok = Cataclysm.getRagnarok();
        int deathCount = Cataclysm.getGameManager().data().getDeathCount();
        int level = ((int) Math.floor(((double) deathCount / 10))) + 1;

        if (ragnarok != null) {
            var data = ragnarok.getData();
            data.setDuration(hours);
            data.setTimeLeft(hours);
            data.setLevel(level);
            ragnarok.setData(ragnarok.getData().append(data));

        } else {
            var data = new RagnarokData(hours, level);
            new RagnarokManager(new Ragnarok(data)).start();
        }
    }

    @Subcommand("ragnarok stop")
    private void ragnarokStop(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) return;
        Ragnarok ragnarok = Cataclysm.getRagnarok();

        if (ragnarok == null) {
            ChatMessenger.sendStaffMessage(player, "No hay ragnarok activa");
            return;
        }

        new RagnarokManager(ragnarok).stop();
    }

    @Subcommand("deathcount")
    private void deathCount(CommandSender commandSender, @Optional Integer deathCount) {
        if (!(commandSender instanceof Player player)) return;

        var data = Cataclysm.getGameManager().data();
        if (deathCount != null) data.setDeathCount(deathCount);
        ChatMessenger.sendStaffMessage(player, "Deathcount: " + data.getDeathCount());
    }

    @Subcommand("day set")
    private void setDay(CommandSender commandSender, int day) {
        if (!(commandSender instanceof Player)) return;
        Cataclysm.getTimeManager().setDay(day);
    }
}