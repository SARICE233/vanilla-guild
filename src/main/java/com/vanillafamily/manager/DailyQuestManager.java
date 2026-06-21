package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.ConfigManager;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.util.MessageUtil;
import com.vanillafamily.util.VaultEconomyHelper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DailyQuestManager {

    private final VanillaFamily plugin;
    private final ConfigManager config;
    // 当天委托: questId -> QuestDef
    private final Map<Integer, QuestDef> todayQuests = new LinkedHashMap<>();
    // 进度: guildId -> (questId -> progress)
    private final Map<Integer, Map<Integer, Integer>> progress = new ConcurrentHashMap<>();
    // 已完成的: guildId -> Set<questId>
    private final Map<Integer, Set<Integer>> completed = new ConcurrentHashMap<>();

    public DailyQuestManager(VanillaFamily plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        checkReset();
    }

    /** 检查是否需要每日重置 */
    public void checkReset() {
        long now = System.currentTimeMillis();
        long lastReset = config.getDailyLastReset();
        // 计算今天0点的时间戳
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, config.getDailyResetHour());
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayReset = today.getTimeInMillis();

        if (now >= todayReset && lastReset < todayReset) {
            resetDaily();
            config.setDailyLastReset(todayReset);
        }
    }

    /** 每日重置 — 随机抽取4个委托 */
    private void resetDaily() {
        todayQuests.clear();
        progress.clear();
        completed.clear();

        ConfigurationSection pool = config.getDailyQuestPool();
        if (pool == null) return;

        // 收集所有委托ID
        List<Integer> allIds = pool.getKeys(false).stream()
                .map(Integer::parseInt).toList();
        if (allIds.isEmpty()) return;

        // 随机抽取 N 个
        int count = config.getDailyQuestsPerDay();
        List<Integer> shuffled = new ArrayList<>(allIds);
        Collections.shuffle(shuffled);

        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            int id = shuffled.get(i);
            String base = "daily.pool." + id + ".";
            QuestDef q = new QuestDef(
                    id,
                    pool.getString(base + ".name", "?"),
                    pool.getString(base + ".description", ""),
                    pool.getString(base + ".type", "KILL_MOBS"),
                    pool.getInt(base + ".target", 100),
                    pool.getInt(base + ".reward", 1000),
                    pool.getInt(base + ".difficulty", 1)
            );
            todayQuests.put(id, q);
        }

        // 全服通知
        Bukkit.broadcast(MessageUtil.format("&6&l✦ 每日委托已刷新! &7共 &f" + todayQuests.size() + " &7个任务"));
        Bukkit.broadcast(MessageUtil.format("&7输入 &e/guild daily &7查看详情"));
    }

    // === 进度跟踪 ===

    public void addProgress(Player player, String questType, int amount) {
        if (todayQuests.isEmpty()) return;

        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) return;
        int gid = member.getGuildId();

        for (QuestDef q : todayQuests.values()) {
            if (!q.type.equals(questType)) continue;
            if (isCompleted(gid, q.id)) continue;

            int cur = progress.computeIfAbsent(gid, k -> new ConcurrentHashMap<>())
                    .merge(q.id, amount, Integer::sum);

            if (cur >= q.target) {
                completeQuest(gid, q);
            }
        }
    }

    private void completeQuest(int gid, QuestDef q) {
        completed.computeIfAbsent(gid, k -> ConcurrentHashMap.newKeySet()).add(q.id);

        Guild guild = plugin.getDatabaseManager().getGuildDao().findById(gid);
        String guildName = guild != null ? guild.getName() : "未知公会";
        int reward = q.reward;
        String diffStr = switch (q.difficulty) {
            case 1 -> "&a★";
            case 2 -> "&e★★";
            case 3 -> "&6★★★";
            default -> "&c★★★★";
        };

        // 发放报酬（会长）
        if (guild != null && VaultEconomyHelper.isAvailable()) {
            Player leader = Bukkit.getPlayer(UUID.fromString(guild.getLeaderUuid()));
            if (leader != null) VaultEconomyHelper.deposit(leader, reward);
        }

        // 公会内通知
        String msg = "&6[每日委托] " + diffStr + " &e" + q.name + " &a完成! 报酬 &6" + reward + " 金币";
        if (guild != null) {
            plugin.getGuildManager().broadcastToGuild(gid, MessageUtil.format(msg));
        }
    }

    public boolean isCompleted(int gid, int questId) {
        Set<Integer> set = completed.get(gid);
        return set != null && set.contains(questId);
    }

    public int getProgress(int gid, int questId) {
        Map<Integer, Integer> map = progress.get(gid);
        return map != null ? map.getOrDefault(questId, 0) : 0;
    }

    // === 展示 ===

    public String getDailyInfo(int gid) {
        if (todayQuests.isEmpty()) {
            checkReset();
            if (todayQuests.isEmpty()) return "&7今日委托尚未生成，请联系管理员配置委托池";
        }

        StringBuilder sb = new StringBuilder("&6&l====== 每日委托 ======\n");
        int done = 0;
        for (QuestDef q : todayQuests.values()) {
            boolean isDone = isCompleted(gid, q.id);
            if (isDone) done++;
            int prog = getProgress(gid, q.id);
            double pct = Math.min(100.0 * prog / q.target, 100.0);
            String diffStr = switch (q.difficulty) {
                case 1 -> "&a★";
                case 2 -> "&e★★";
                case 3 -> "&6★★★";
                default -> "&c★★★★";
            };
            String status = isDone ? "&a✔ 已完成" : "&7" + prog + "/" + q.target + " (" + String.format("%.0f", pct) + "%)";
            sb.append(diffStr).append(" &f").append(q.name)
                    .append(" &7- ").append(q.description).append("\n")
                    .append("  &7进度: ").append(status)
                    .append(" &7| 报酬: &6").append(q.reward).append(" 金币\n");
        }
        sb.append("&7完成: &f").append(done).append("/").append(todayQuests.size());
        return sb.toString();
    }

    public Collection<QuestDef> getTodayQuests() {
        return todayQuests.values();
    }

    /** 委托定义 */
    public record QuestDef(int id, String name, String description, String type,
                           int target, int reward, int difficulty) {}
}
