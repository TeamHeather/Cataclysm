package org.cataclysm.game.player;

import io.papermc.paper.event.entity.ElderGuardianAppearanceEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.util.Vector;
import org.cataclysm.Cataclysm;
import org.cataclysm.api.CataclysmColor;
import org.cataclysm.api.data.PersistentData;
import org.cataclysm.api.item.ItemBuilder;
import org.cataclysm.api.item.ItemRestorer;
import org.cataclysm.api.listener.registrable.Registrable;
import org.cataclysm.game.effect.ImmunityEffect;
import org.cataclysm.game.effect.MortemEffect;
import org.cataclysm.game.effect.PaleCorrosionEffect;
import org.cataclysm.game.items.CataclysmItems;
import org.cataclysm.game.items.ItemFamily;
import org.cataclysm.game.player.data.PlayerData;
import org.cataclysm.game.player.data.PlayerLoader;
import org.cataclysm.game.player.tag.role.RoleManager;
import org.cataclysm.game.player.tag.role.RoleType;
import org.cataclysm.game.player.tag.team.TeamManager;
import org.cataclysm.game.player.tag.team.Teams;
import org.cataclysm.game.world.Dimensions;
import org.cataclysm.global.utils.chat.ChatMessenger;
import org.cataclysm.global.utils.text.font.TinyCaps;

import java.time.Duration;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;

@Slf4j
@Registrable
public class PlayerListener implements Listener {

    @EventHandler
    private void onPlayerJoinWeek5(PlayerJoinEvent event) {
        if (Cataclysm.getDay() < 28) return;

        Player player = event.getPlayer();

        Boolean paleVoid = PersistentData.get(player, "PANTHEON-JOIN", PersistentDataType.BOOLEAN);
        if (paleVoid != null && paleVoid) return;

        Location location = Dimensions.PALE_VOID.createWorld().getSpawnLocation().clone();
        player.teleport(location.add(0, 25, 0));
        player.clearActivePotionEffects();
        player.addPotionEffect(new PotionEffect(ImmunityEffect.EFFECT_TYPE, 200, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, false));
        player.setGameMode(GameMode.SURVIVAL);

        player.showTitle(Title.title(
                MiniMessage.miniMessage().deserialize("<#E0E0E0>Mortuus es!"),
                MiniMessage.miniMessage().deserialize("<#A4A4A4>Salvete in " + TinyCaps.tinyCaps("Pale Void")),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(4), Duration.ofSeconds(1))
        ));

        player.playSound(player, org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1F, 0.45F);
        Bukkit.getScheduler().runTaskLater(Cataclysm.getInstance(), () -> player.playSound(player, org.bukkit.Sound.MUSIC_DISC_STAL, 1F, 0.85F), 40L);

        PersistentData.set(player, "PANTHEON-JOIN", PersistentDataType.BOOLEAN, true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current != null && current.getType() == Material.ELYTRA) {
            if (player.hasCooldown(Material.ELYTRA)) {
                if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.getSlot() == 38) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (event.getBlock().getType() != Material.HEAVY_CORE) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        var player = event.getPlayer();
        var scale = player.getAttribute(Attribute.SCALE);
        var ragnarok = Cataclysm.getRagnarok();
        var day = Cataclysm.getDay();

        if (day >= 7 || ragnarok != null) {
            if (scale != null) {
                PersistentData.set(player, "gnomification", PersistentDataType.INTEGER, 0);
                scale.setBaseValue(1);
            }
            PlayerUtils.cancelSleep(event);
        }
        else if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) ChatMessenger.broadcastMessage(player.getName() + " está durmiendo...");
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();

        var cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(player);
        try {
            new PlayerLoader(cataclysmPlayer.getData().getNickname()).save(cataclysmPlayer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        event.quitMessage(
                MiniMessage.miniMessage().deserialize("<#A4A4A4>[<#B4B4B4>-<#A4A4A4>]")
                        .append(MiniMessage.miniMessage().deserialize(" <#C3C3C3>" + player.getName() + " <#C3C3C3>se ha desconectado"))
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        try {
            new PlayerLoader(player.getName()).restore();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var cataclysmPlayer = CataclysmPlayer.getCataclysmPlayer(player);
        cataclysmPlayer.getTotemManager().updateStatistic();
        cataclysmPlayer.getCooldownManager().restore();

        PlayerData data = cataclysmPlayer.getData();
        if (data.getTeam() == null || data.getTeam().isEmpty()) new TeamManager(data).setTeam(Teams.NONE);
        if (data.getRoleType() == null || data.getRoleType().isEmpty()) new RoleManager(data).setRole(RoleType.MEMBER);

        event.joinMessage(
                MiniMessage.miniMessage().deserialize("<#6EEC6C>[<#7FEE7D>+<#6EEC6C>]")
                        .append(MiniMessage.miniMessage().deserialize(" <#9DE79B>" + player.getName() + " <#9DE79B>se ha conectado"))
        );
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        var player = event.getPlayer();
        var action = event.getAction();
        var inventory = player.getInventory();

        switch (action) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.END_PORTAL_FRAME) {
                    if (!player.isOp()) event.setCancelled(true);
                }

                var mainHand = inventory.getItemInMainHand();
                var handMeta = mainHand.getItemMeta();
                if (handMeta == null) return;
                if (!handMeta.isUnbreakable()) return;
                if (player.hasCooldown(mainHand.getType())) return;

                switch (mainHand.getType()) {
                    case NETHERITE_PICKAXE -> {
                        if (!player.isSneaking()) return;
                        if (mainHand.containsEnchantment(Enchantment.FORTUNE)) {
                            var itemBuilder = new ItemBuilder(mainHand).removeEnchant(Enchantment.FORTUNE).addEnchant(Enchantment.SILK_TOUCH, 1).setCustomModelData("twisted_pickaxe_silk_touch");
                            inventory.setItemInMainHand(itemBuilder.build());
                            player.playSound(Sound.sound(Key.key("item.mace.smash_ground_heavy"), Sound.Source.BLOCK, 1.0F, 0.57F));
                            player.setCooldown(mainHand.getType(), 20);
                            return;
                        }

                        if (mainHand.containsEnchantment(Enchantment.SILK_TOUCH)) {
                            var itemBuilder = new ItemBuilder(mainHand).removeEnchant(Enchantment.SILK_TOUCH).addEnchant(Enchantment.FORTUNE, 5).setCustomModelData("twisted_pickaxe_fortune");
                            inventory.setItemInMainHand(itemBuilder.build());
                            player.playSound(Sound.sound(Key.key("item.mace.smash_ground_heavy"), Sound.Source.BLOCK, 1.0F, 0.57F));
                            player.setCooldown(mainHand.getType(), 20);
                        }
                    }

                    case MACE -> {
                        ItemRestorer restorer = new ItemRestorer(player.getInventory());
                        ItemBuilder itemBuilder = ItemBuilder.stackToBuilder(mainHand.clone());
                        ItemStack stack = restorer.restore(CataclysmItems.MIRAGE_MACE.getBuilder().getID(), CataclysmItems.MIRAGE_MACE.build(), itemBuilder.build()).clone();

                        if (itemBuilder.getID().equals("mirage_mace_normal")) {
                            player.playSound(Sound.sound(Key.key("item.mace.smash_air"), Sound.Source.MASTER, 1.0F, 0.77F));
                            player.playSound(Sound.sound(Key.key("block.amethyst_block.break"), Sound.Source.MASTER, 1.0F, 0.77F));

                            var charge = player.getWorld().spawn(player.getEyeLocation(), WindCharge.class);
                            charge.setShooter(player);

                            var direction = player.getLocation().getDirection();
                            charge.setVelocity(direction.multiply(2));
                            player.setCooldown(mainHand.getType(), 15);

                            if (!player.isSneaking()) return;

                            ItemBuilder builder = new ItemBuilder(stack);
                            inventory.setItemInMainHand(builder.setID("mirage_mace_hook").build());
                            player.playSound(player, org.bukkit.Sound.BLOCK_END_PORTAL_FRAME_FILL, 1F, 0.57F);
                            player.playSound(player, org.bukkit.Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 0.6F, 0.57F);
                            player.setCooldown(mainHand.getType(), 5);
                            return;
                        }

                        if (itemBuilder.getID().equalsIgnoreCase("mirage_mace_hook")) {
                            if (player.isSneaking()) {
                                ItemBuilder builder = new ItemBuilder(stack);
                                inventory.setItemInMainHand(builder.setID("mirage_mace_normal").build());
                                player.playSound(player, org.bukkit.Sound.BLOCK_END_PORTAL_FRAME_FILL, 1F, 1.57F);
                                player.playSound(player, org.bukkit.Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 0.6F, 0.57F);
                                player.setCooldown(mainHand.getType(), 5);
                                return;
                            }

                            Vector vector = player.getLocation().getDirection();
                            Vector hookVector = new Vector(vector.getX() * 2.15, vector.getY() * 2.44, vector.getZ() * 2.15);
                            player.setVelocity(hookVector);
                            player.setCooldown(mainHand.getType(), 80);
                            player.playSound(player, org.bukkit.Sound.ENTITY_FISHING_BOBBER_THROW, 1.5F, 1.17F);
                            player.playSound(player, org.bukkit.Sound.ITEM_TRIDENT_RIPTIDE_1, 1.5F, 1.17F);

                            World world = player.getWorld();

                            UUID uuid = player.getUniqueId();

                            if (!Cataclysm.getTasks().containsKey(uuid)) {
                                final long startTime = System.currentTimeMillis();
                                final long timeoutDuration = 2500;

                                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Cataclysm.getInstance(), () -> {
                                    Location location = player.getLocation();
                                    world.spawnParticle(Particle.END_ROD, location.add(0.3, 0, 0.3), 2, 0, 0, 0, 0, null, true);

                                    long currentTime = System.currentTimeMillis();

                                    if (!player.isOnline()
                                            || location.clone().add(0, -.05, 0).getBlock().isSolid()
                                            || (currentTime - startTime) >= timeoutDuration) {
                                        Bukkit.getScheduler().cancelTask(Cataclysm.getTasks().get(uuid));
                                        Cataclysm.getTasks().remove(uuid);
                                    }
                                }, 2, 1);

                                Cataclysm.getTasks().putIfAbsent(uuid, task);
                            }

                            world.playSound(player, org.bukkit.Sound.ITEM_MACE_SMASH_AIR, 0.75F, 1.17F);
                            world.spawnParticle(Particle.EXPLOSION, player.getLocation(), 3);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        var player = event.getPlayer();

        if (Cataclysm.getDay() < 21) return;
        boolean bucketInHand = player.getInventory().getItemInMainHand().getType() == Material.BUCKET || player.getInventory().getItemInOffHand().getType() == Material.BUCKET;
        if (event.getRightClicked().getType() == EntityType.COW && bucketInHand) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var cause = event.getCause();
        int day = Cataclysm.getDay();
        var scale = player.getAttribute(Attribute.SCALE);
        double extraDamage = 0.0;
        double damageMultiplier = 1.0;

        if (day >= 7) {

            if (scale != null) {
                double difference = 1 - scale.getValue();
                damageMultiplier += difference;
            }

            if (cause.name().contains("FIRE") || cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.POISON) {
                if (!PlayerUtils.hasArmor(ItemFamily.CALAMITY_ARMOR, player) && !PlayerUtils.hasArmor(ItemFamily.PALE_ARMOR, player)) damageMultiplier += 4;
            }
        }

        if (day >= 21) {
            if (cause == EntityDamageEvent.DamageCause.FREEZE || cause == EntityDamageEvent.DamageCause.STARVATION) {
                if (!PlayerUtils.hasMirageHelmet(player)) extraDamage += 999;
            }

            if (cause == EntityDamageEvent.DamageCause.DROWNING) {
                if (!PlayerUtils.hasMirageHelmet(player)) damageMultiplier += 2;
            }
        }

        if (day >= 28) {
            if (cause == EntityDamageEvent.DamageCause.VOID) event.setDamage(999);

            if (player.getWorld().equals(Dimensions.PALE_VOID.createWorld())) {
                var distance = player.getLocation().distance(Dimensions.PALE_VOID.createWorld().getSpawnLocation());

                if (distance <= 150) return;

                PlayerUtils.breakElytras(player, 0);
            }
        }

        if (day >= 35) {
            if (cause == EntityDamageEvent.DamageCause.FALL) damageMultiplier *= 4;
        }

        event.setDamage(Math.min((event.getDamage() + extraDamage) * damageMultiplier, 10_000));

        if (cause.equals(EntityDamageEvent.DamageCause.POISON)) {
            if (event.getFinalDamage() >= player.getHealth()) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        Player player = null;
        if (event.getDamager() instanceof Player) player = (Player) event.getDamager();
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player) player = (Player) projectile.getShooter();

        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;

        var mainHand = player.getInventory().getItemInMainHand();
        var handMeta = mainHand.getItemMeta();
        if (handMeta == null) return;
        if (!handMeta.isUnbreakable()) return;
        if (!(mainHand.getType().equals(Material.NETHERITE_SWORD) || mainHand.getType().equals(Material.BOW))) return;

        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 6, 2));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 6, 2));

        ItemBuilder itemBuilder = ItemBuilder.stackToBuilder(mainHand.clone());

        if (itemBuilder.getID().contains("pale")) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 4));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
//            entity.setMaximumNoDamageTicks(0);
//            entity.setNoDamageTicks(0);
        }
    }

    @EventHandler
    public void elderGuardianCurse(ElderGuardianAppearanceEvent event) {
        Player player = event.getAffectedPlayer();
        int day = Cataclysm.getDay();

        if (day >= 7) {
            PotionEffectType[] effects = {PotionEffectType.BLINDNESS, PotionEffectType.WEAKNESS, PotionEffectType.UNLUCK};
            for (PotionEffectType effect : effects) player.addPotionEffect(new PotionEffect(effect, 4800, effect == PotionEffectType.WEAKNESS ? 1 : 0));
        }
    }

    @EventHandler
    public void playerTrade(PlayerTradeEvent event) {
        SplittableRandom random = new SplittableRandom();
        int day = Cataclysm.getDay();

        if (day >= 14) {
            if (random.nextInt(100) < (day >= 21 ? 100 : 10)) {
                event.setCancelled(true);
                BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

                for (BlockFace face : faces) {
                    Location spawnLocation = event.getVillager().getLocation().getBlock().getRelative(face).getLocation();
                    spawnLocation.add(0, 1, 0).getWorld().spawnEntity(spawnLocation, EntityType.VEX);
                }
            }
        }
    }

    @EventHandler
    public void playerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        int day = Cataclysm.getDay();
        var id = new ItemBuilder(item).getID();

        if (id != null) {
            switch (id) {
                case "calamity_carrot" -> {
                    ((CraftPlayer) player).getHandle().getFoodData().eat(0, 0.4F);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 3));
                    return;
                }

                case "calamity_apple" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 3));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 150, 1));
                    return;
                }

                case "enchanted_calamity_apple" -> {

                    for (var effect : player.getActivePotionEffects()) {
                        if (!effect.getType().getCategory().equals(PotionEffectTypeCategory.HARMFUL)) continue;
                        if (effect.getType().equals(PaleCorrosionEffect.EFFECT_TYPE)) continue;
                        if (effect.getType().equals(MortemEffect.EFFECT_TYPE)) continue;
                        player.removePotionEffect(effect.getType());
                    }

                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 2));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 4));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 300, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 15, day < 28 ? 2 : 0));
                    return;
                }

            }
        }

        //? Vanilla Items
        if (day >= 14) {
            switch (item.getType()) {
                case BEEF, CHICKEN, PORKCHOP, MUTTON, SALMON, COD, TROPICAL_FISH, RABBIT, ROTTEN_FLESH, POTATO -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
                    return;
                }
            }

        }

        if (day >= 21) {
            if (!item.getType().name().contains("GOLDEN")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 400, 2));
            }
        }
    }


    @EventHandler
    public void playerPotionEffect(EntityPotionEffectEvent event) {
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return;
        if (!(event.getEntity() instanceof Player player)) return;
        int day = Cataclysm.getDay();
        if (day < 14) return;
        EntityPotionEffectEvent.Cause cause = event.getCause();
        List<EntityPotionEffectEvent.Cause> ignoredCauses = List.of(
                EntityPotionEffectEvent.Cause.PLUGIN,
                EntityPotionEffectEvent.Cause.COMMAND,
                EntityPotionEffectEvent.Cause.FOOD,
                EntityPotionEffectEvent.Cause.BEACON,
                EntityPotionEffectEvent.Cause.TOTEM);

        if (ignoredCauses.contains(cause)) return;
        PotionEffectType type = newEffect.getType();

        if (type.equals(PotionEffectType.FIRE_RESISTANCE)) {
            Bukkit.getScheduler().runTaskLater(Cataclysm.getInstance(), () -> player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE), 10L);
        }

        if (day >= 21) {
            PotionEffectType[] reducedEffects = new PotionEffectType[]{
                    PotionEffectType.REGENERATION,
                    PotionEffectType.ABSORPTION,
                    PotionEffectType.RESISTANCE,
                    PotionEffectType.SPEED,
                    PotionEffectType.HASTE,
                    PotionEffectType.STRENGTH,
                    PotionEffectType.INSTANT_HEALTH,
                    PotionEffectType.JUMP_BOOST,
                    PotionEffectType.INVISIBILITY,
                    PotionEffectType.NIGHT_VISION,
                    PotionEffectType.HEALTH_BOOST,
                    PotionEffectType.SATURATION,
                    PotionEffectType.SLOW_FALLING,
                    PotionEffectType.DOLPHINS_GRACE,
                    PotionEffectType.CONDUIT_POWER,
                    PotionEffectType.HERO_OF_THE_VILLAGE
            };

            if (ArrayUtils.contains(reducedEffects, type)) {
                Bukkit.getScheduler().runTaskLater(Cataclysm.getInstance(), () -> {
                    player.removePotionEffect(type);
                    player.addPotionEffect(new PotionEffect(type, newEffect.getDuration() / 2, newEffect.getAmplifier()));

                    var ragnarok = Cataclysm.getRagnarok();
                    if (ragnarok != null) {
                        var level = ragnarok.getData().getLevel();
                        if (level >= 9) {
                            player.removePotionEffect(type);
                        }
                    }

                }, 5L);
            }
        }
    }

    @EventHandler
    @SuppressWarnings("all")
    public void airChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isUnderWater()) return;

        if (PlayerUtils.hasMirageHelmet(player)) {
            event.setAmount(player.getMaximumAir());
            return;
        }

        int day = Cataclysm.getDay();
        if (day < 14) return;

        int amount = event.getAmount();
        int newAmount = amount - (150 / 20);
        if (newAmount < 0) newAmount = 0;

        if (player.getRemainingAir() == 0) {
            player.damage(2, DamageSource.builder(DamageType.DROWN).build());
            return;
        }

        event.setAmount(newAmount);
    }

    @EventHandler
    public void onSmithItem(PrepareSmithingEvent event) {
        var item = event.getResult();
        if (item == null) return;
        var itemMeta = item.getItemMeta();
        if (itemMeta == null) return;
        if (itemMeta instanceof ArmorMeta meta && meta.isUnbreakable()) event.setResult(null);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        var player = event.getPlayer();
        var cause = event.getCause();
        var day = Cataclysm.getDay();
        var locationFrom = event.getFrom();
        var locationTo = event.getTo();

        if (day < 21) return;

        if (day >= 35 && cause.equals(PlayerTeleportEvent.TeleportCause.END_PORTAL)) {
            var inventory = player.getInventory();
            boolean flag = inventory.contains(CataclysmItems.OVERWORLDS_CHALICE.build())
                    && inventory.contains(CataclysmItems.NETHERS_CHALICE.build())
                    && inventory.contains(CataclysmItems.ENDS_CHALICE.build());
            if (flag) {
                event.setCancelled(true);
                player.setGameMode(GameMode.SPECTATOR);
                //TODO Add title and music
            }

            return;
        }

        switch (cause) {
            case NETHER_PORTAL, END_PORTAL, END_GATEWAY -> player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 5, 0));
        }

        if (locationFrom.getWorld().equals(Dimensions.OVERWORLD.createWorld())
                && locationTo.getWorld().getName().equalsIgnoreCase("world_the_end")) {

            event.setCancelled(true);

            // Teleport diferido para evitar recursión infinita
            Bukkit.getScheduler().runTask(Cataclysm.getInstance(), () -> player.teleport(new Location(Dimensions.THE_END.createWorld(), 0, -56, -290)));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (event.getClickedBlock() == null) return;

        var block = event.getClickedBlock();
        if (block.getType() != Material.LECTERN) return;

        var lectern = (Lectern) block.getState();
        var book = lectern.getInventory().getItem(0);

        if (book == null || book.getType() != Material.WRITTEN_BOOK) return;

        var meta = (BookMeta) book.getItemMeta();

        var title = meta.getTitle();
        var author = meta.getAuthor();

        if (title == null || author == null || !title.equalsIgnoreCase("mirage_key") || !author.equalsIgnoreCase("wsasu")) return;

        player.playSound(Sound.sound(Key.key("entity.guardian.death"), Sound.Source.BLOCK, 1.0F, 0.5F));
        player.playSound(Sound.sound(Key.key("entity.elder_guardian.ambient"), Sound.Source.BLOCK, 1.0F, 0.5F));

        block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 0.75F, 0.8F);
        block.setType(Material.AIR);

        ChatMessenger.broadcastMessage(player.getName() + " ha encontrado una <" + CataclysmColor.MIRAGE.getColor() + ">Mirage Key" + ChatMessenger.getTextColor() + ".");
    }


}