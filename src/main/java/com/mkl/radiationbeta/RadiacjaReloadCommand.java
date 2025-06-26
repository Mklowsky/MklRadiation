package com.mkl.radiationbeta;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class RadiacjaReloadCommand implements CommandExecutor {

    private final RadiationManager radiationManager;
    private final JavaPlugin plugin;

    public RadiacjaReloadCommand(RadiationManager radiationManager, JavaPlugin plugin) {
        this.radiationManager = radiationManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("mklradiation.radiacjareload")) {
            sender.sendMessage("Nie masz uprawnień do użycia tej komendy.");
            return true;
        }

        plugin.reloadConfig();
        radiationManager.loadConfig();
        radiationManager.saveConfig();

        sender.sendMessage("§aKonfiguracja strefy radiacji została przeładowana.");

        return true;
    }
}
