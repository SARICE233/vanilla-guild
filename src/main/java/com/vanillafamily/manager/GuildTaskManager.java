package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.model.GuildTask;
import com.vanillafamily.util.MessageUtil;
import com.vanillafamily.util.VaultEconomyHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildTaskManager {

    private final VanillaFamily plugin;
    private final Map<Integer, GuildTask> tasks = new ConcurrentHashMap<>();
    private int nextId = 1;

    public GuildTaskManager(VanillaFamily plugin) {
        this.plugin = plugin;
        loadFromDb();
    }

    // === 创建任务 ===
    public GuildTask createTask(String name, int target, int reward) {
        GuildTask task = new GuildTask(name, target, reward);

        String sql = "INSERT INTO guild_tasks (name, target, reward) VALUES (?, ?, ?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, target);
            ps.setInt(3, reward);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("创建任务失败: " + e.getMessage());
            return null;
        }

        tasks.put(task.getId(), task);
        // 全服广播
        Bukkit.broadcast(MessageUtil.format(
                "&6&l[公会竞赛] &e" + name +
                " &7目标: &f" + target + " 击杀 &7报酬: &6" + reward + " 金币" +
                " &7- 第一名达标的公会获胜!"));
        return task;
    }

    // === 加载进行中的任务 ===
    private void loadFromDb() {
        String sql = "SELECT * FROM guild_tasks WHERE completed = 0";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                GuildTask task = new GuildTask();
                task.setId(rs.getInt("id"));
                task.setName(rs.getString("name"));
                task.setTarget(rs.getInt("target"));
                task.setReward(rs.getInt("reward"));
                task.setCompleted(rs.getInt("completed"));
                task.setWinnerGuildId(rs.getInt("winner_guild_id"));
                task.setCreatedAt(rs.getTimestamp("created_at"));
                tasks.put(task.getId(), task);
                if (task.getId() >= nextId) nextId = task.getId() + 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("加载任务失败: " + e.getMessage());
        }

        // 加载进度
        if (!tasks.isEmpty()) {
            String progressSql = "SELECT * FROM guild_task_progress WHERE task_id IN (" +
                    String.join(",", tasks.keySet().stream().map(String::valueOf).toList()) + ")";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(progressSql)) {
                while (rs.next()) {
                    int taskId = rs.getInt("task_id");
                    int guildId = rs.getInt("guild_id");
                    int progress = rs.getInt("progress");
                    GuildTask task = tasks.get(taskId);
                    if (task != null) task.getProgress().put(guildId, progress);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("加载任务进度失败: " + e.getMessage());
            }
        }
    }

    // === 打怪增加进度 ===
    public void addKillProgress(Player player) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) return;

        int guildId = member.getGuildId();

        for (GuildTask task : tasks.values()) {
            if (task.getCompleted() == 1) continue;

            int current = task.getProgress().getOrDefault(guildId, 0) + 1;
            task.getProgress().put(guildId, current);

            // 持久化进度 (每5次写一次，减少IO)
            if (current % 5 == 0) saveProgress(task.getId(), guildId, current);

            // 检查是否达标
            if (current >= task.getTarget()) {
                completeTask(task, guildId);
                return; // 一个击杀只能完成一个任务
            }
        }
    }

    private void saveProgress(int taskId, int guildId, int progress) {
        String sql = "INSERT OR REPLACE INTO guild_task_progress (task_id, guild_id, progress) VALUES (?, ?, ?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, guildId);
            ps.setInt(3, progress);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // === 完成任务 ===
    private void completeTask(GuildTask task, int winnerGuildId) {
        task.setCompleted(1);
        task.setWinnerGuildId(winnerGuildId);

        // 更新数据库
        String sql = "UPDATE guild_tasks SET completed=1, winner_guild_id=? WHERE id=?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, winnerGuildId);
            ps.setInt(2, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("更新任务状态失败: " + e.getMessage());
        }

        // 发放报酬
        Guild guild = plugin.getDatabaseManager().getGuildDao().findById(winnerGuildId);
        String guildName = guild != null ? guild.getName() : "未知公会";
        String taskName = task.getName();
        int reward = task.getReward();

        // 报酬发给会长
        if (guild != null && VaultEconomyHelper.isAvailable()) {
            Player leader = Bukkit.getPlayer(UUID.fromString(guild.getLeaderUuid()));
            if (leader != null) {
                net.milkbowl.vault.economy.Economy econ = VaultEconomyHelper.getEconomy();
                // 通过反射调用 depositPlayer
                if (econ != null) econ.depositPlayer(leader, reward);
                leader.sendMessage(MessageUtil.format("&6&l[任务完成] &a你的公会 &e" + guildName +
                        " &a率先完成 &e" + taskName + "&a! 获得报酬 &6" + reward + " 金币"));
            }
        }

        // 全服广播
        Bukkit.broadcast(MessageUtil.format("&6&l[公会任务] &e" + taskName +
                " &a已被 &e" + guildName + " &a完成! 报酬: &6" + reward + " 金币"));

        // 清理进度表
        tasks.remove(task.getId());
        String cleanSql = "DELETE FROM guild_task_progress WHERE task_id=?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(cleanSql)) {
            ps.setInt(1, task.getId());
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // === 查询 ===
    public Collection<GuildTask> getActiveTasks() {
        return tasks.values();
    }

    public GuildTask getTask(int id) {
        return tasks.get(id);
    }

    // === 删除任务 ===
    public boolean deleteTask(int taskId) {
        GuildTask task = tasks.remove(taskId);
        if (task == null) return false;

        String sql1 = "DELETE FROM guild_tasks WHERE id=?";
        String sql2 = "DELETE FROM guild_task_progress WHERE task_id=?";
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setInt(1, taskId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                ps.setInt(1, taskId);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {}
        return true;
    }

    // === 任务进度展示 ===
    public String getTasksInfo() {
        if (tasks.isEmpty()) return "&7当前没有进行中的任务";

        StringBuilder sb = new StringBuilder("&6&l====== 公会任务 ======\n");
        for (GuildTask t : tasks.values()) {
            sb.append("&e").append(t.getName())
                    .append(" &7- 目标击杀: &f").append(t.getTarget())
                    .append(" &7报酬: &6").append(t.getReward()).append(" 金币\n");
            // 显示各公会进度
            t.getProgress().forEach((gid, prog) -> {
                Guild g = plugin.getDatabaseManager().getGuildDao().findById(gid);
                String name = g != null ? g.getName() : "#" + gid;
                double pct = Math.min(100.0 * prog / t.getTarget(), 100.0);
                sb.append("  &7").append(name).append(": &f").append(prog)
                        .append("/").append(t.getTarget())
                        .append(" &7(").append(String.format("%.0f", pct)).append("%)\n");
            });
        }
        return sb.toString();
    }
}
