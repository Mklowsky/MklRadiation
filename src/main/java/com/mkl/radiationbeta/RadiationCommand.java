package com.mkl.radiationbeta;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RadiationCommand implements CommandExecutor {

    private final RadiationManager radiationManager;
    private final JavaPlugin plugin;

    public RadiationCommand(RadiationManager radiationManager, JavaPlugin plugin) {
        this.radiationManager = radiationManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy.");
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("mklradiation.radiacja")) {
            sender.sendMessage("Nie masz uprawnień do użycia tej komendy.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Użycie: /radiacja <rozmiar>");
            return true;
        }

        try {
            int size = Integer.parseInt(args[0]);
            Player p = (Player) sender;
            Location center = p.getLocation();
            radiationManager.setSafeZone(center, size);
            radiationManager.saveConfig();
            sender.sendMessage("Ustawiono bezpieczną strefę radiacji o rozmiarze: " + size);
        } catch (NumberFormatException e) {
            sender.sendMessage("Podaj poprawną liczbę.");
        }

        return true;
    }
}
