package com.mkl.radiationbeta;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public class MklRadiationBeta extends JavaPlugin implements Listener {

    private RadiationManager radiationManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        radiationManager = new RadiationManager(this);
        radiationManager.loadConfig();
        radiationManager.start();

        // Rejestracja komend
        getCommand("radiacja").setExecutor(new RadiationCommand(radiationManager, this));
        getCommand("plyn").setExecutor(new PlynCommand(this));
        getCommand("radiacjareload").setExecutor(new RadiacjaReloadCommand(radiationManager, this));

        Bukkit.getPluginManager().registerEvents(this, this);

        registerLugolRecipe();
    }

    @Override
    public void onDisable() {
        radiationManager.stop();
        radiationManager.saveConfig();
    }

    private void registerLugolRecipe() {
        ItemStack lugol = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) lugol.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.WATER));
        meta.setDisplayName("§aPłyn Lugola");

        // Ustawienie koloru na zielony
        meta.setColor(org.bukkit.Color.LIME);

        // Dodanie lore
        List<String> lore = new ArrayList<>();
        lore.add("Mikstura zapewniająca odporność od Radiacji");
        meta.setLore(lore);

        lugol.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(this, "lugol_potion");

        // Usuwamy ewentualną starą recepturę
        Bukkit.removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, lugol);
        recipe.shape("GWT");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('W', Material.POTION);
        recipe.setIngredient('T', Material.GHAST_TEAR);

        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        radiationManager.addPlayerToBars(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        radiationManager.removePlayerFromBars(event.getPlayer());
    }

    @EventHandler
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.POTION) return;
        if (!event.getItem().hasItemMeta()) return;
        if (!event.getItem().getItemMeta().hasDisplayName()) return;

        String name = event.getItem().getItemMeta().getDisplayName();
        if (name.equals("§aPłyn Lugola")) {
            radiationManager.startProtection(event.getPlayer());
        }
    }
}
