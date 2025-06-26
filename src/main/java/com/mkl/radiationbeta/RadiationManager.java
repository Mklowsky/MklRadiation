package com.mkl.radiationbeta;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class RadiationManager {

    private final JavaPlugin plugin;
    private BossBar radiationBar;          // czerwony boss bar
    private BossBar protectionBar;         // zielony boss bar
    private Location safeCenter = null;
    private int safeRadius = 0;
    private BukkitRunnable radiationTask;
    private int potionDurationTicks = 600; // 30 sekund domyślnie (20 ticków = 1 sekunda)
    private Set<UUID> protectedPlayers = new HashSet<>();
    private Map<UUID, BukkitRunnable> protectionTasks = new HashMap<>();
    private Map<UUID, Integer> protectionTimes = new HashMap<>();

    // <-- DODANE: set do śledzenia kto jest w strefie radiacji
    private Set<UUID> playersInRadiationZone = new HashSet<>();
    // <-- DODANE: przechowuje wiadomość z configu
    private String enterRadiationMessage;

    public RadiationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        radiationBar = Bukkit.createBossBar("§cStrefa Radiacji", BarColor.RED, BarStyle.SOLID);
        protectionBar = Bukkit.createBossBar("§aOchrona od Radiacji", BarColor.GREEN, BarStyle.SOLID);

        radiationBar.setVisible(false);
        protectionBar.setVisible(false);

        for (Player p : Bukkit.getOnlinePlayers()) {
            radiationBar.addPlayer(p);
            protectionBar.addPlayer(p);
        }
    }

    public void start() {
        radiationTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateRadiation();
            }
        };
        radiationTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void stop() {
        if (radiationTask != null) {
            radiationTask.cancel();
        }
        radiationBar.removeAll();
        protectionBar.removeAll();
        for (BukkitRunnable task : protectionTasks.values()) {
            task.cancel();
        }
        protectionTasks.clear();
        protectedPlayers.clear();
        protectionTimes.clear();
        playersInRadiationZone.clear();
    }

    public void setSafeZone(Location center, int radius) {
        this.safeCenter = center;
        this.safeRadius = radius;
        saveConfig();
    }

    public void loadConfig() {
        double x = plugin.getConfig().getDouble("safeZone.x", 0);
        double y = plugin.getConfig().getDouble("safeZone.y", 0);
        double z = plugin.getConfig().getDouble("safeZone.z", 0);
        int radius = plugin.getConfig().getInt("safeZone.radius", 0);
        int potionDurationSeconds = plugin.getConfig().getInt("potionDurationSeconds", 30);
        potionDurationTicks = potionDurationSeconds * 20;

        // <-- DODANE: wczytanie tekstu wiadomości z configu
        enterRadiationMessage = plugin.getConfig().getString(
            "messages.enterRadiationZone",
            "&c[Radiacja] &7Gracz &a%player% &7weszedł do strefy radiacji!"
        );

        if (radius > 0) {
            World world = Bukkit.getWorlds().get(0);
            safeCenter = new Location(world, x, y, z);
            safeRadius = radius;
        }
    }

    public void saveConfig() {
        if (safeCenter != null) {
            plugin.getConfig().set("safeZone.x", safeCenter.getX());
            plugin.getConfig().set("safeZone.y", safeCenter.getY());
            plugin.getConfig().set("safeZone.z", safeCenter.getZ());
            plugin.getConfig().set("safeZone.radius", safeRadius);
            plugin.getConfig().set("potionDurationSeconds", potionDurationTicks / 20);
            plugin.saveConfig();
        }
    }

    public void startProtection(Player player) {
        UUID uuid = player.getUniqueId();

        if (protectedPlayers.contains(uuid)) {
            // Resetuj timer i pasek jeśli już chroniony
            protectionTimes.put(uuid, potionDurationTicks);
            protectionBar.setProgress(1.0);
            return;
        }

        protectedPlayers.add(uuid);
        protectionBar.addPlayer(player);
        protectionBar.setVisible(true);
        protectionTimes.put(uuid, potionDurationTicks);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = protectionTimes.getOrDefault(uuid, 0);
                if (timeLeft <= 0 || !protectedPlayers.contains(uuid)) {
                    protectedPlayers.remove(uuid);
                    protectionTimes.remove(uuid);
                    protectionBar.removePlayer(player);
                    if (protectionBar.getPlayers().isEmpty()) {
                        protectionBar.setVisible(false);
                    }
                    protectionTasks.remove(uuid);
                    cancel();
                    return;
                }
                protectionBar.setProgress((double) timeLeft / potionDurationTicks);
                protectionTimes.put(uuid, timeLeft - 20);
            }
        };

        protectionTasks.put(uuid, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    private void updateRadiation() {
        if (safeCenter == null) return;
        World world = safeCenter.getWorld();
        if (world == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                radiationBar.removePlayer(player);
                protectionBar.removePlayer(player);
                playersInRadiationZone.remove(player.getUniqueId()); // usuwamy, bo inny świat
                continue;
            }

            radiationBar.addPlayer(player);

            Location loc = player.getLocation();
            double dx = Math.abs(loc.getX() - safeCenter.getX());
            double dz = Math.abs(loc.getZ() - safeCenter.getZ());

            boolean inSafeZone = (dx <= safeRadius && dz <= safeRadius);

            if (inSafeZone) {
                radiationBar.removePlayer(player);
                radiationBar.setVisible(false);

                if (protectedPlayers.contains(player.getUniqueId())) {
                    protectionBar.addPlayer(player);
                    protectionBar.setVisible(true);
                } else {
                    protectionBar.removePlayer(player);
                    protectionBar.setVisible(false);
                }

                player.removePotionEffect(PotionEffectType.HUNGER);
                player.removePotionEffect(PotionEffectType.WITHER);

                // Jeśli był wcześniej w radiacji, a wyszedł - usuwamy go z setu
                playersInRadiationZone.remove(player.getUniqueId());
            } else {
                radiationBar.setVisible(true);
                radiationBar.setProgress(1.0);

                if (protectedPlayers.contains(player.getUniqueId())) {
                    protectionBar.addPlayer(player);
                    protectionBar.setVisible(true);

                    player.removePotionEffect(PotionEffectType.HUNGER);
                    player.removePotionEffect(PotionEffectType.WITHER);
                } else {
                    protectionBar.removePlayer(player);
                    if (protectionBar.getPlayers().isEmpty()) {
                        protectionBar.setVisible(false);
                    }

                    int amplifier = 5;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, amplifier, true, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, amplifier, true, false, false));
                }

                // Tutaj wysyłamy wiadomość jeśli dopiero wszedł do radiacji
                UUID uuid = player.getUniqueId();
                if (!playersInRadiationZone.contains(uuid)) {
                    playersInRadiationZone.add(uuid);

                    // Zamiana %player% i kolorów & na §
                    String msg = enterRadiationMessage.replace("%player%", player.getName());
                    msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);

                    // Wysyłamy wiadomość wszystkim online
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(msg);
                    }
                }
            }
        }
    }

    public void addPlayerToBars(Player player) {
        radiationBar.addPlayer(player);
        protectionBar.addPlayer(player);
    }

    public void removePlayerFromBars(Player player) {
        radiationBar.removePlayer(player);
        protectionBar.removePlayer(player);
    }
}
