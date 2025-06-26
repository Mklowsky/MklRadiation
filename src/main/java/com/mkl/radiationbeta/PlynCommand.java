package com.mkl.radiationbeta;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.plugin.java.JavaPlugin;

public class PlynCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public PlynCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy.");
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("mklradiation.plyn")) {
            sender.sendMessage("Nie masz uprawnień do użycia tej komendy.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack lugol = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) lugol.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.WATER));
        meta.setDisplayName("§aPłyn Lugola");
        meta.setColor(org.bukkit.Color.LIME);

        // Dodanie lore
        List<String> lore = new ArrayList<>();
        lore.add("Mikstura zapewniająca odporność od Radiacji");
        meta.setLore(lore);

        lugol.setItemMeta(meta);

        player.getInventory().addItem(lugol);
        player.sendMessage("Otrzymałeś §aPłyn Lugola§r!");

        return true;
    }
}
