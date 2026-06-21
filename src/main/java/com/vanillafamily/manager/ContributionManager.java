package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.ConfigManager;
import com.vanillafamily.database.dao.GuildMemberDao;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.util.MessageUtil;
import com.vanillafamily.util.VaultEconomyHelper;
import org.bukkit.entity.Player;

import java.util.*;

public class ContributionManager {

    private final VanillaFamily plugin;
    private final ConfigManager config;
    private final GuildMemberDao memberDao;
    // 每日签到: playerUuid -> 上次签到日期 (epoch day)
    private final Map<String, Long> dailySignCache = new HashMap<>();

    public ContributionManager(VanillaFamily plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.memberDao = plugin.getDatabaseManager().getGuildMemberDao();
    }

    // === 捐献金币 ===
    public boolean donateGold(Player player, int amount) {
        if (amount <= 0) return false;

        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) {
            player.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return false;
        }

        // Vault 经济检查
        if (VaultEconomyHelper.isAvailable()) {
            if (!VaultEconomyHelper.has(player, amount)) {
                player.sendMessage(MessageUtil.format(
                        config.getMessage("not-enough-money"),
                        "{cost}", String.valueOf(amount)));
                return false;
            }
            VaultEconomyHelper.withdraw(player, amount);
        } else {
            player.sendMessage(MessageUtil.format("&c服务器未安装经济插件"));
            return false;
        }

        // 计算贡献
        double ratio = config.getGoldRatio();
        double contribution = amount * ratio;
        memberDao.addContribution(player.getUniqueId().toString(), contribution);

        // 每日委托: CONTRIBUTION
        plugin.getDailyQuestManager().addProgress(player, "CONTRIBUTION", (int) contribution);

        // 公会经验
        plugin.getGuildManager().addGuildExpByPlayerUuid(
                player.getUniqueId().toString(), (int) (amount / 10));

        player.sendMessage(MessageUtil.format(
                "&a你捐献了 &6" + amount + " 金币 &a获得 &6" + String.format("%.0f", contribution) + " 贡献点"));

        return true;
    }

    // === 打怪贡献 ===
    public void addMobKillContribution(Player player) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) return;

        double amount = config.getMobKillContribution();
        if (amount <= 0) return;

        memberDao.addContribution(player.getUniqueId().toString(), amount);
        plugin.getGuildManager().addGuildExpByPlayerUuid(
                player.getUniqueId().toString(), 1);
    }

    // === 每日签到 ===
    public void dailySign(Player player) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) {
            player.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return;
        }

        String uuid = player.getUniqueId().toString();
        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

        Long lastSign = dailySignCache.get(uuid);
        if (lastSign != null && lastSign >= today) {
            player.sendMessage(MessageUtil.format("&c你今天已经签到过了!"));
            return;
        }

        dailySignCache.put(uuid, today);

        int contribution = config.getDailySignContribution();
        memberDao.addContribution(uuid, contribution);

        // 每日委托: SIGN
        plugin.getDailyQuestManager().addProgress(player, "SIGN", 1);

        plugin.getGuildManager().addGuildExpByPlayerUuid(uuid, 10);

        player.sendMessage(MessageUtil.format(
                "&a签到成功! 获得 &6" + contribution + " &a贡献点, 公会 +10 经验"));
    }

    // === 贡献排行 ===
    public void showTopContributors(Player player, int guildId) {
        GuildManager gm = plugin.getGuildManager();
        if (guildId == -1) {
            // 全局排行
            List<GuildMember> top = gm.getTopContributorsGlobal(10);
            sendTopList(player, top, "&6&l=== 全服贡献排行 ===");
        } else {
            List<GuildMember> top = gm.getTopContributors(guildId, 10);
            sendTopList(player, top, "&6&l=== 公会贡献排行 ===");
        }
    }

    private void sendTopList(Player player, List<GuildMember> top, String title) {
        player.sendMessage(MessageUtil.format(title));
        int rank = 1;
        for (GuildMember m : top) {
            player.sendMessage(MessageUtil.format(
                    "&7" + rank + ". &f" + m.getPlayerName() +
                            " &7- &6" + String.format("%.0f", m.getContribution()) + " 贡献点"));
            rank++;
        }
    }
}
