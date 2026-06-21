package com.vanillafamily;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final VanillaFamily plugin;
    private FileConfiguration config;

    public ConfigManager(VanillaFamily plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        setDefaults();
    }

    private void setDefaults() {
        // 公会基础设置
        config.addDefault("guild.create-cost", 5000);
        config.addDefault("guild.min-name-length", 2);
        config.addDefault("guild.max-name-length", 16);
        config.addDefault("guild.tag-max-length", 6);
        config.addDefault("guild.chat-prefix", "&8[&a⚔&8]");
        config.addDefault("guild.default-rank", "member");

        // === 可配置职位系统 ===
        // 管理员可自由增删改职位，每个职位有: display(显示名), weight(权重,越大越高), 权限开关
        config.addDefault("ranks.leader.display", "会长");
        config.addDefault("ranks.leader.weight", 100);
        config.addDefault("ranks.leader.can-disband", true);
        config.addDefault("ranks.leader.can-promote", true);
        config.addDefault("ranks.leader.can-demote", true);
        config.addDefault("ranks.leader.can-kick", true);
        config.addDefault("ranks.leader.can-invite", true);
        config.addDefault("ranks.leader.can-upgrade", true);
        config.addDefault("ranks.leader.can-bank-withdraw", true);
        config.addDefault("ranks.leader.can-bank-deposit", true);

        config.addDefault("ranks.officer.display", "干事");
        config.addDefault("ranks.officer.weight", 50);
        config.addDefault("ranks.officer.can-disband", false);
        config.addDefault("ranks.officer.can-promote", false);
        config.addDefault("ranks.officer.can-demote", false);
        config.addDefault("ranks.officer.can-kick", true);
        config.addDefault("ranks.officer.can-invite", true);
        config.addDefault("ranks.officer.can-upgrade", true);
        config.addDefault("ranks.officer.can-bank-withdraw", true);
        config.addDefault("ranks.officer.can-bank-deposit", true);

        config.addDefault("ranks.member.display", "成员");
        config.addDefault("ranks.member.weight", 10);
        config.addDefault("ranks.member.can-disband", false);
        config.addDefault("ranks.member.can-promote", false);
        config.addDefault("ranks.member.can-demote", false);
        config.addDefault("ranks.member.can-kick", false);
        config.addDefault("ranks.member.can-invite", false);
        config.addDefault("ranks.member.can-upgrade", false);
        config.addDefault("ranks.member.can-bank-withdraw", false);
        config.addDefault("ranks.member.can-bank-deposit", true);

        // === 等级配置 (1~20为渐进, 21+统一规则) ===
        for (int lv = 1; lv <= 20; lv++) {
            config.addDefault("levels." + lv + ".exp", lv <= 5
                    ? new int[]{0, 1000, 3000, 8000, 15000}[lv - 1]
                    : 15000 + (lv - 5) * 2000);
            config.addDefault("levels." + lv + ".max-members", Math.min(10 + (lv - 1) * 2, 50));
            config.addDefault("levels." + lv + ".bank-rows", Math.min(lv, 20));
        }
        config.addDefault("levels.default-exp", 50000);           // 21级起每级经验
        config.addDefault("levels.default-max-members", 50);       // 21级起成员上限
        config.addDefault("levels.default-bank-rows-per-level", 1); // 21级起每级+1行仓库

        // === BUFF 配置 (随公会等级自动解锁, 1~20级逐步到50%) ===
        config.addDefault("buffs.buff-max-level", 20);  // 多少级时达到满BUFF
        config.addDefault("buffs.max-effect", 50);       // 满BUFF百分比
        config.addDefault("buffs.EXP_BOOST.name", "&e经验加成");
        config.addDefault("buffs.EXP_BOOST.enabled", true);
        config.addDefault("buffs.DAMAGE_BOOST.name", "&c伤害加成");
        config.addDefault("buffs.DAMAGE_BOOST.enabled", true);
        config.addDefault("buffs.SPEED.name", "&b速度加成");
        config.addDefault("buffs.SPEED.enabled", true);
        config.addDefault("buffs.DAMAGE_REDUCTION.name", "&9减伤加成");
        config.addDefault("buffs.DAMAGE_REDUCTION.enabled", true);

        // === 每日委托配置 ===
        config.addDefault("daily.quests-per-day", 4);
        config.addDefault("daily.reset-hour", 0);  // 0点重置
        config.addDefault("daily.last-reset", 0L);

        // 委托池 (服务器管理员可自行增删改)
        addDailyQuest(1, "初出茅庐", "击杀50只怪物", "KILL_MOBS", 50, 1000, 1);
        addDailyQuest(2, "小试牛刀", "击杀100只怪物", "KILL_MOBS", 100, 2500, 1);
        addDailyQuest(3, "狩猎开始", "击杀150只怪物", "KILL_MOBS", 150, 4000, 2);
        addDailyQuest(4, "怪物猎人", "击杀250只怪物", "KILL_MOBS", 250, 7000, 2);
        addDailyQuest(5, "屠戮时刻", "击杀400只怪物", "KILL_MOBS", 400, 12000, 3);
        addDailyQuest(6, "百鬼夜行", "击杀600只怪物", "KILL_MOBS", 600, 18000, 3);
        addDailyQuest(7, "公会初献", "累计贡献500点", "CONTRIBUTION", 500, 2000, 1);
        addDailyQuest(8, "鼎力相助", "累计贡献1500点", "CONTRIBUTION", 1500, 6000, 2);
        addDailyQuest(9, "倾囊相助", "累计贡献3000点", "CONTRIBUTION", 3000, 12000, 3);
        addDailyQuest(10, "经验之路", "获得1000点经验", "GAIN_EXP", 1000, 2000, 1);
        addDailyQuest(11, "修行之旅", "获得3000点经验", "GAIN_EXP", 3000, 6000, 2);
        addDailyQuest(12, "登峰造极", "获得8000点经验", "GAIN_EXP", 8000, 15000, 3);
        addDailyQuest(13, "伐木工", "砍伐200个木头", "BREAK_BLOCK", 200, 3000, 1);
        addDailyQuest(14, "矿工日常", "挖掘300个石头/矿石", "BREAK_BLOCK", 300, 5000, 2);
        addDailyQuest(15, "渔获丰收", "钓到50条鱼", "FISH", 50, 3000, 1);
        addDailyQuest(16, "深海猎人", "钓到100条鱼", "FISH", 100, 6000, 2);
        addDailyQuest(17, "签到就好", "公会3人完成签到", "SIGN", 3, 1000, 1);
        addDailyQuest(18, "公会齐心", "公会5人完成签到", "SIGN", 5, 3000, 2);
        addDailyQuest(19, "炼狱征程", "击杀800只怪物", "KILL_MOBS", 800, 25000, 4);
        addDailyQuest(20, "万人斩", "击杀1200只怪物", "KILL_MOBS", 1200, 40000, 4);

        // 贡献设置
        config.addDefault("contributions.gold-ratio", 10.0);  // 1金币 = 10贡献
        config.addDefault("contributions.mob-kill", 0.5);
        config.addDefault("contributions.daily-sign", 50);

        // 数据库
        config.addDefault("database.file", "guilds.db");

        // 消息
        config.addDefault("messages.prefix", "&8[&a香草家族&8]&r ");
        config.addDefault("messages.no-permission", "&c你没有权限使用此命令");
        config.addDefault("messages.guild-created", "&a公会 &e{name} &a创建成功! 花费: &6{cost} 金币");
        config.addDefault("messages.guild-disbanded", "&c公会 &e{name} &c已解散");
        config.addDefault("messages.not-in-guild", "&c你还没有加入任何公会");
        config.addDefault("messages.already-in-guild", "&c你已经加入了公会 &e{guild}");
        config.addDefault("messages.guild-name-taken", "&c公会名称已被使用");
        config.addDefault("messages.guild-name-too-short", "&c公会名称太短 (最少 {min} 个字符)");
        config.addDefault("messages.guild-name-too-long", "&c公会名称太长 (最多 {max} 个字符)");
        config.addDefault("messages.not-enough-money", "&c金币不足, 需要 &6{cost} 金币");
        config.addDefault("messages.player-not-found", "&c玩家不存在或不在线");
        config.addDefault("messages.player-already-in-guild", "&c该玩家已加入其他公会");
        config.addDefault("messages.invite-sent", "&a已向 &e{player} &a发送公会邀请");
        config.addDefault("messages.invite-received", "&e{player} &a邀请你加入公会 &e{guild}&a, 输入 &6/guild join {guild} &a接受");
        config.addDefault("messages.joined-guild", "&e{player} &a加入了公会");
        config.addDefault("messages.left-guild", "&e{player} &c离开了公会");
        config.addDefault("messages.kicked-from-guild", "&c你被踢出了公会 &e{guild}");
        config.addDefault("messages.promoted", "&e{player} &a被提升为 &6{rank}");
        config.addDefault("messages.demoted", "&e{player} &c被降级为 &6{rank}");
        config.addDefault("messages.guild-full", "&c公会已满 ({current}/{max})");
        config.addDefault("messages.no-invite", "&c你没有收到该公会的邀请");
        config.addDefault("messages.not-leader", "&c只有会长才能执行此操作");
        config.addDefault("messages.cannot-kick-self", "&c你不能踢出自己");
        config.addDefault("messages.cannot-kick-higher", "&c你不能踢出职位比你高的成员");
        config.addDefault("messages.cannot-promote-self", "&c你不能提升自己");
        config.addDefault("messages.already-top-rank", "&c该成员已经是最高职位");
        config.addDefault("messages.already-bottom-rank", "&c该成员已经是最低职位");
        config.addDefault("messages.upgraded", "&a公会升级至 &6Lv.{level}");
        config.addDefault("messages.not-enough-exp", "&c公会经验不足");
        config.addDefault("messages.max-level", "&c公会已达最高等级");
        config.addDefault("messages.buff-activated", "&a{cost} &a已激活 {buff} &aLv.{level}");
        config.addDefault("messages.buff-max-level", "&c该BUFF已达最高等级");
        config.addDefault("messages.disband-confirm", "&c确定解散公会? 此操作不可撤销! 输入 &6/guild disband confirm &c确认");
        config.addDefault("messages.chat-enabled", "&a已切换到公会聊天频道");
        config.addDefault("messages.chat-disabled", "&c已退出公会聊天频道");
        config.addDefault("messages.guild-level-low", "&c公会等级不足, 需要 Lv.{required}");

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    // === 公会设置 ===
    public int getCreateCost() { return config.getInt("guild.create-cost"); }
    public int getMinNameLength() { return config.getInt("guild.min-name-length"); }
    public int getMaxNameLength() { return config.getInt("guild.max-name-length"); }
    public int getTagMaxLength() { return config.getInt("guild.tag-max-length"); }
    public String getChatPrefix() { return config.getString("guild.chat-prefix"); }

    // === 等级设置 (无限等级) ===
    /** 无上限, 返回 -1 表示无限 */
    public int getMaxLevel() { return -1; }

    public int getLevelExp(int level) {
        String key = "levels." + level + ".exp";
        if (config.contains(key)) return config.getInt(key);
        return config.getInt("levels.default-exp", 50000); // 21+ 固定
    }

    public int getLevelMaxMembers(int level) {
        String key = "levels." + level + ".max-members";
        if (config.contains(key)) return config.getInt(key);
        return config.getInt("levels.default-max-members", 50);
    }

    public int getLevelBankRows(int level) {
        String key = "levels." + level + ".bank-rows";
        if (config.contains(key)) return config.getInt(key);
        // 21+ 每级扩容
        int baseRows = getLevelBankRows(20);
        int extraPerLevel = config.getInt("levels.default-bank-rows-per-level", 1);
        return baseRows + (level - 20) * extraPerLevel;
    }

    // === BUFF 设置 (自动随等级解锁) ===
    public String getBuffName(String buffType) { return config.getString("buffs." + buffType + ".name"); }
    public boolean isBuffEnabled(String buffType) { return config.getBoolean("buffs." + buffType + ".enabled"); }
    public int getBuffMaxLevel() { return config.getInt("buffs.buff-max-level", 20); }
    public int getBuffMaxEffect() { return config.getInt("buffs.max-effect", 50); }

    /** 根据公会等级计算 BUFF 百分比 (1~20级 2.5%/级 → 最高50%) */
    public double getBuffEffect(int guildLevel, String buffType) {
        if (!isBuffEnabled(buffType)) return 0;
        int maxLevel = getBuffMaxLevel();
        int maxEffect = getBuffMaxEffect();
        if (guildLevel >= maxLevel) return maxEffect;
        return (double) guildLevel / maxLevel * maxEffect;
    }

    private void addDailyQuest(int id, String name, String desc, String type, int target, int reward, int difficulty) {
        String base = "daily.pool." + id + ".";
        config.addDefault(base + "name", name);
        config.addDefault(base + "description", desc);
        config.addDefault(base + "type", type);
        config.addDefault(base + "target", target);
        config.addDefault(base + "reward", reward);
        config.addDefault(base + "difficulty", difficulty);
    }

    // === 每日委托 ===
    public int getDailyQuestsPerDay() { return config.getInt("daily.quests-per-day", 4); }
    public int getDailyResetHour() { return config.getInt("daily.reset-hour", 0); }
    public long getDailyLastReset() { return config.getLong("daily.last-reset", 0L); }
    public void setDailyLastReset(long time) { config.set("daily.last-reset", time); plugin.saveConfig(); }
    public ConfigurationSection getDailyQuestPool() { return config.getConfigurationSection("daily.pool"); }

    // === 贡献设置 ===
    public double getGoldRatio() { return config.getDouble("contributions.gold-ratio"); }
    public double getMobKillContribution() { return config.getDouble("contributions.mob-kill"); }
    public int getDailySignContribution() { return config.getInt("contributions.daily-sign"); }

    // === 数据库 ===
    public String getDatabaseFile() { return config.getString("database.file"); }

    // === 消息 ===
    public String getMessage(String key) {
        return config.getString("messages.prefix", "") + config.getString("messages." + key, "");
    }

    // ==================== 可配置职位系统 ====================

    /**
     * 获取所有职位 key, 按 weight 从高到低排序
     */
    public List<String> getRankKeys() {
        ConfigurationSection section = config.getConfigurationSection("ranks");
        if (section == null) return List.of("leader", "officer", "member");
        return section.getKeys(false).stream()
                .sorted((a, b) -> Integer.compare(
                        getRankWeight(b), getRankWeight(a)))
                .toList();
    }

    /** 获取职位的显示名 */
    public String getRankDisplay(String rankKey) {
        return config.getString("ranks." + rankKey + ".display", rankKey);
    }

    /** 获取职位的权重 (越大越高) */
    public int getRankWeight(String rankKey) {
        return config.getInt("ranks." + rankKey + ".weight", 0);
    }

    /** 检查某职位是否有某权限 */
    public boolean hasRankPermission(String rankKey, String permission) {
        return config.getBoolean("ranks." + rankKey + "." + permission, false);
    }

    /** 获取最高职位 key (weight 最大的) */
    public String getHighestRank() {
        List<String> ranks = getRankKeys();
        return ranks.isEmpty() ? "leader" : ranks.get(0);
    }

    /** 获取最低职位 key (weight 最小的) */
    public String getLowestRank() {
        List<String> ranks = getRankKeys();
        return ranks.isEmpty() ? "member" : ranks.get(ranks.size() - 1);
    }

    /** 新成员默认加入的职位 */
    public String getInitialRank() {
        return config.getString("guild.default-rank", getLowestRank());
    }

    /** 获取当前职位的上一级 (升职) */
    public String getNextRank(String currentRankKey) {
        List<String> ranks = getRankKeys();
        int idx = ranks.indexOf(currentRankKey);
        if (idx > 0) return ranks.get(idx - 1);
        return currentRankKey; // 已最高
    }

    /** 获取当前职位的下一级 (降职) */
    public String getPreviousRank(String currentRankKey) {
        List<String> ranks = getRankKeys();
        int idx = ranks.indexOf(currentRankKey);
        if (idx >= 0 && idx < ranks.size() - 1) return ranks.get(idx + 1);
        return currentRankKey; // 已最低
    }
}
