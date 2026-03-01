package com.stormai.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FireMazePlugin extends JavaPlugin implements Listener, CommandExecutor {
    private Set<UUID> inHeatZone = new HashSet<>();
    private Map<UUID, BukkitTask> damageTasks = new HashMap<>();
    private Map<UUID, Long> lastDamageTime = new HashMap<>();
    private Set<Location> lavaTriggers = new HashSet<>();
    private Set<Location> fireMobSpawns = new HashSet<>();
    private Set<Location> flameWallSections = new HashSet<>();
    private FileConfiguration config;
    private boolean isHeatActive = false;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("firemaze").setExecutor(this);
        saveDefaultConfig();
        config = getConfig();
        loadConfig();
        getLogger().info("FireMaze Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FireMaze Plugin disabled!");
        for (BukkitTask task : damageTasks.values()) {
            task.cancel();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage("FireMaze Commands:");
            sender.sendMessage("/firemaze create <type> - Create maze elements (heatzone, lavatrigger, firemob, flamewall)");
            sender.sendMessage("/firemaze start - Start heat damage");
            sender.sendMessage("/firemaze stop - Stop heat damage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /firemaze create <type>");
                    return true;
                }
                createMazeElement(player, args[1].toLowerCase());
                break;
            case "start":
                startHeatDamage();
                break;
            case "stop":
                stopHeatDamage();
                break;
            default:
                sender.sendMessage("Unknown subcommand!");
        }
        return true;
    }

    private void createMazeElement(Player player, String type) {
        Location loc = player.getLocation().getBlock().getLocation();
        Block block = loc.getBlock();

        switch (type) {
            case "heatzone":
                inHeatZone.add(player.getUniqueId());
                player.sendMessage("Heat zone created at your location!");
                break;
            case "lavatrigger":
                lavaTriggers.add(loc);
                block.setType(Material.LAVA);
                player.sendMessage("Lava trigger created!");
                break;
            case "firemob":
                fireMobSpawns.add(loc);
                block.setType(Material.NETHERRACK);
                player.sendMessage("Fire mob spawn zone created!");
                break;
            case "flamewall":
                flameWallSections.add(loc);
                block.setType(Material.CAMPFIRE);
                player.sendMessage("Flame wall section created!");
                break;
            default:
                player.sendMessage("Unknown type: " + type);
        }
    }

    private void startHeatDamage() {
        isHeatActive = true;
        getServer().getOnlinePlayers().forEach(player -> {
            if (inHeatZone.contains(player.getUniqueId())) {
                startHeatDamageForPlayer(player);
            }
        });
        getLogger().info("Heat damage started!");
    }

    private void stopHeatDamage() {
        isHeatActive = false;
        damageTasks.values().forEach(BukkitTask::cancel);
        damageTasks.clear();
        getLogger().info("Heat damage stopped!");
    }

    private void startHeatDamageForPlayer(Player player) {
        BukkitTask task = new BukkitRunnable() {
            public void run() {
                if (!isHeatActive || !inHeatZone.contains(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                double damageRate = config.getDouble("damage-rate", 1.0);
                long interval = config.getLong("damage-interval", 2000);

                long now = System.currentTimeMillis();
                if (lastDamageTime.containsKey(player.getUniqueId()) && 
                    (now - lastDamageTime.get(player.getUniqueId())) < interval) {
                    return;
                }

                player.damage(damageRate);
                player.setFireTicks((int) interval);
                lastDamageTime.put(player.getUniqueId(), now);

                player.sendMessage("You're taking heat damage!");
            }
        }.runTaskTimer(this, 0, 20);
        damageTasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();

        if (lavaTriggers.contains(loc)) {
            triggerLavaTrap(player, loc);
        } else if (fireMobSpawns.contains(loc)) {
            spawnFireMobs(player, loc);
        } else if (flameWallSections.contains(loc)) {
            activateFlameWall(player, loc);
        }
    }

    private void triggerLavaTrap(Player player, Location loc) {
        player.sendMessage("Lava trap triggered!");
        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().playSound(loc, org.bukkit.Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
        player.damage(3.0);
        player.setFireTicks(100);
    }

    private void spawnFireMobs(Player player, Location loc) {
        int mobCount = config.getInt("fire-mob-count", 3);
        for (int i = 0; i < mobCount; i++) {
            LivingEntity mob = (LivingEntity) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.ZOMBIFIED_PIGLIN);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 999999, 1));
            mob.setFireTicks(999999);
        }
        player.sendMessage("Fire mobs spawned!");
    }

    private void activateFlameWall(Location loc) {
        loc.getWorld().createExplosion(loc, 2.0f, true, true);
        loc.getWorld().playSound(loc, org.bukkit.Sound.BLOCK_FIRE_AMBIENT, 2.0f, 1.0f);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!inHeatZone.contains(player.getUniqueId())) return;

        if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
            event.setDamage(event.getDamage() * config.getDouble("fire-damage-multiplier", 1.5));
        }
    }

    private void loadConfig() {
        config.addDefault("damage-rate", 1.0);
        config.addDefault("damage-interval", 2000);
        config.addDefault("fire-mob-count", 3);
        config.addDefault("fire-damage-multiplier", 1.5);
        config.options().copyDefaults(true);
        saveConfig();
    }
}