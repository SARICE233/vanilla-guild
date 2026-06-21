package com.vanillafamily.util;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 经济接入。
 * 使用 Vault API 标准模式，Economy.class 来自服务器 ClassLoader (provided scope)。
 * 首次调用延迟 2 tick 确保 PlayerPoints 等已注册。
 */
public class VaultEconomyHelper {

    private static Economy economy;
    private static boolean checked;

    public static boolean isAvailable() {
        if (!checked) {
            checked = true;
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) return false;
            economy = rsp.getProvider();
        }
        return economy != null;
    }

    public static boolean has(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public static boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.withdrawPlayer(player, amount);
        return r.transactionSuccess();
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.depositPlayer(player, amount);
        return r.transactionSuccess();
    }
}
