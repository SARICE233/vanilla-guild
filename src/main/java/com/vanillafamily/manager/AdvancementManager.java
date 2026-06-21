package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AdvancementManager {

    private final VanillaFamily plugin;
    private final NamespacedKey firstJoinKey;
    private final NamespacedKey gloryJourneyKey;

    public AdvancementManager(VanillaFamily plugin) {
        this.plugin = plugin;
        this.firstJoinKey = new NamespacedKey(plugin, "guild/first_join");
        this.gloryJourneyKey = new NamespacedKey(plugin, "guild/glory_journey");
        loadAdvancements();
    }

    private void loadAdvancements() {
        try {
            // 加载 root
            loadAdvancement("guild/root");
            // 加载成就
            loadAdvancement("guild/first_join");
            loadAdvancement("guild/glory_journey");
            plugin.getLogger().info("自定义成就已注册");
        } catch (Exception e) {
            plugin.getLogger().warning("成就注册失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void loadAdvancement(String path) {
        try {
            NamespacedKey key = new NamespacedKey(plugin, path);
            if (Bukkit.getAdvancement(key) != null) return; // 已存在

            InputStream is = plugin.getResource("data/vanillafamily/advancement/" + path + ".json");
            if (is == null) {
                plugin.getLogger().warning("成就文件缺失: " + path);
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            is.close();

            Bukkit.getUnsafe().loadAdvancement(key, json);
        } catch (Exception e) {
            plugin.getLogger().warning("加载成就失败 " + path + ": " + e.getMessage());
        }
    }

    /** 加入公会时授予 */
    public void grantFirstJoin(Player player) {
        grantAdvancement(player, firstJoinKey);
    }

    /** 公会达到20级时授予该公会所有在线成员 */
    public void grantGloryJourney(Player player) {
        grantAdvancement(player, gloryJourneyKey);
    }

    private void grantAdvancement(Player player, NamespacedKey key) {
        try {
            Advancement adv = Bukkit.getAdvancement(key);
            if (adv == null) return;
            AdvancementProgress progress = player.getAdvancementProgress(adv);
            if (progress.isDone()) return; // 已有
            for (String criteria : progress.getRemainingCriteria()) {
                progress.awardCriteria(criteria);
            }
        } catch (Exception ignored) {}
    }
}
