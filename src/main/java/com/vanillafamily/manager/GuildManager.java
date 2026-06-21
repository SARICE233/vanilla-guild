package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.ConfigManager;
import com.vanillafamily.database.dao.GuildDao;
import com.vanillafamily.database.dao.GuildMemberDao;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.util.MessageUtil;
import com.vanillafamily.util.VaultEconomyHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildManager {

    private final VanillaFamily plugin;
    private final ConfigManager config;
    private final GuildDao guildDao;
    private final GuildMemberDao memberDao;

    // 待处理的公会邀请: playerUuid -> guildName
    private final Map<String, String> pendingInvites = new HashMap<>();
    // 需要二次确认解散的玩家
    private final Set<String> disbandConfirm = new HashSet<>();
    // 缓存: playerUuid -> guildId (避免频繁 DB 查询)
    private final Map<String, Integer> guildIdCache = new ConcurrentHashMap<>();
    // 脏标记: 需要持久化经验的公会
    private final Set<Integer> dirtyGuilds = ConcurrentHashMap.newKeySet();

    public GuildManager(VanillaFamily plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.guildDao = plugin.getDatabaseManager().getGuildDao();
        this.memberDao = plugin.getDatabaseManager().getGuildMemberDao();
        // 每30秒将脏公会经验写入数据库
        plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::flushDirtyGuilds, 600L, 600L);
    }

    // === 创建公会 ===
    public boolean createGuild(Player leader, String name) {
        // 名称校验
        if (name.length() < config.getMinNameLength()) {
            leader.sendMessage(MessageUtil.format(
                    config.getMessage("guild-name-too-short"),
                    "{min}", String.valueOf(config.getMinNameLength())));
            return false;
        }
        if (name.length() > config.getMaxNameLength()) {
            leader.sendMessage(MessageUtil.format(
                    config.getMessage("guild-name-too-long"),
                    "{max}", String.valueOf(config.getMaxNameLength())));
            return false;
        }

        // 检查是否已加入公会（通过 getMember 自动清理孤儿记录）
        String uuid = leader.getUniqueId().toString();
        if (getMember(uuid) != null) {
            leader.sendMessage(MessageUtil.format(config.getMessage("already-in-guild"),
                    "{guild}", ""));
            return false;
        }

        // 检查名称唯一
        if (guildDao.findByName(name) != null) {
            leader.sendMessage(MessageUtil.format(config.getMessage("guild-name-taken")));
            return false;
        }

        // 扣费 (如果有 Vault 经济)
        int cost = config.getCreateCost();
        if (VaultEconomyHelper.isAvailable()) {
            if (!VaultEconomyHelper.has(leader, cost)) {
                leader.sendMessage(MessageUtil.format(config.getMessage("not-enough-money"),
                        "{cost}", String.valueOf(cost)));
                return false;
            }
            VaultEconomyHelper.withdraw(leader, cost);
        }

        // 创建公会
        Guild guild = new Guild(name, uuid);
        guild = guildDao.create(guild);
        if (guild == null) {
            leader.sendMessage(MessageUtil.format("&c创建公会失败, 请联系管理员"));
            return false;
        }

        // 添加会长为成员 (使用最高职位)
        GuildMember member = new GuildMember(guild.getId(), uuid, config.getHighestRank());
        member.setPlayerName(leader.getName());
        memberDao.add(member);

        leader.sendMessage(MessageUtil.format(config.getMessage("guild-created"),
                "{name}", name, "{cost}", String.valueOf(cost)));
        return true;
    }

    // === 解散公会 ===
    public boolean disbandGuild(Player leader, String confirm) {
        String uuid = leader.getUniqueId().toString();
        GuildMember member = memberDao.findByUuid(uuid);
        if (member == null || !member.getRank().equals(config.getHighestRank())) {
            leader.sendMessage(MessageUtil.format(config.getMessage("not-leader")));
            return false;
        }

        // 二次确认：首次调用提示，二次调用带 confirm 执行
        if (!"confirm".equalsIgnoreCase(confirm)) {
            disbandConfirm.add(uuid);
            leader.sendMessage(MessageUtil.format(config.getMessage("disband-confirm")));
            return false;
        }
        disbandConfirm.remove(uuid); // 清理标记

        Guild guild = guildDao.findById(member.getGuildId());
        if (guild == null) return false;

        String guildName = guild.getName();
        int guildId = guild.getId();

        // 先收集在线成员 UUID，再删除 (删除会级联清除成员记录)
        Set<String> memberUuids = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            GuildMember gm = memberDao.findByUuid(p.getUniqueId().toString());
            if (gm != null && gm.getGuildId() == guildId) {
                memberUuids.add(p.getUniqueId().toString());
            }
        }

        guildDao.delete(guildId);

        // 通知已收集的成员
        Component disbandMsg = MessageUtil.format(config.getMessage("guild-disbanded"),
                "{name}", guildName);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (memberUuids.contains(p.getUniqueId().toString())) {
                p.sendMessage(disbandMsg);
            }
        }

        return true;
    }

    // === 邀请玩家 ===
    public boolean invitePlayer(Player inviter, String targetName) {
        GuildMember inviterMember = memberDao.findByUuid(inviter.getUniqueId().toString());
        if (inviterMember == null) {
            inviter.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return false;
        }

        // 权限检查: can-invite
        if (!config.hasRankPermission(inviterMember.getRank(), "can-invite")) {
            inviter.sendMessage(MessageUtil.format(config.getMessage("no-permission")));
            return false;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            inviter.sendMessage(MessageUtil.format(config.getMessage("player-not-found")));
            return false;
        }

        if (getMember(target.getUniqueId().toString()) != null) {
            inviter.sendMessage(MessageUtil.format(config.getMessage("player-already-in-guild")));
            return false;
        }

        // 检查公会是否满员
        Guild guild = guildDao.findById(inviterMember.getGuildId());
        if (guild == null) return false;

        int memberCount = memberDao.countByGuildId(guild.getId());
        int maxMembers = config.getLevelMaxMembers(guild.getLevel());
        if (memberCount >= maxMembers) {
            inviter.sendMessage(MessageUtil.format(config.getMessage("guild-full"),
                    "{current}", String.valueOf(memberCount),
                    "{max}", String.valueOf(maxMembers)));
            return false;
        }

        // 发送邀请
        pendingInvites.put(target.getUniqueId().toString(), guild.getName());

        inviter.sendMessage(MessageUtil.format(config.getMessage("invite-sent"),
                "{player}", target.getName()));
        target.sendMessage(MessageUtil.format(config.getMessage("invite-received"),
                "{player}", inviter.getName(),
                "{guild}", guild.getName()));

        return true;
    }

    // === 加入公会 ===
    public boolean joinGuild(Player player, String guildName) {
        String uuid = player.getUniqueId().toString();

        // 检查是否已有公会
        if (memberDao.findByUuid(uuid) != null) {
            player.sendMessage(MessageUtil.format(config.getMessage("already-in-guild"),
                    "{guild}", ""));
            return false;
        }

        // 检查是否有有效邀请
        String invitedGuild = pendingInvites.get(uuid);
        if (invitedGuild == null || !invitedGuild.equalsIgnoreCase(guildName)) {
            player.sendMessage(MessageUtil.format(config.getMessage("no-invite")));
            return false;
        }

        Guild guild = guildDao.findByName(guildName);
        if (guild == null) return false;

        // 检查人数上限
        int memberCount = memberDao.countByGuildId(guild.getId());
        int maxMembers = config.getLevelMaxMembers(guild.getLevel());
        if (memberCount >= maxMembers) {
            player.sendMessage(MessageUtil.format(config.getMessage("guild-full"),
                    "{current}", String.valueOf(memberCount),
                    "{max}", String.valueOf(maxMembers)));
            return false;
        }

        // 加入
        GuildMember member = new GuildMember(guild.getId(), uuid, config.getInitialRank());
        member.setPlayerName(player.getName());
        memberDao.add(member);

        pendingInvites.remove(uuid);

        // 广播
        player.sendMessage(MessageUtil.format("&a你已加入公会 &e" + guild.getName()));
        plugin.getAdvancementManager().grantFirstJoin(player);
        broadcastToGuild(guild.getId(), MessageUtil.format(config.getMessage("joined-guild"),
                "{player}", player.getName()));

        return true;
    }

    // === 离开公会 ===
    public boolean leaveGuild(Player player) {
        String uuid = player.getUniqueId().toString();
        GuildMember member = memberDao.findByUuid(uuid);
        if (member == null) {
            player.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return false;
        }

        // 最高职位(会长)不能直接离开
        if (member.getRank().equals(config.getHighestRank())) {
            player.sendMessage(MessageUtil.format("&c最高职位不能直接离开, 请先转让或解散公会"));
            return false;
        }

        int guildId = member.getGuildId();
        memberDao.removeByUuid(uuid);
        guildIdCache.remove(uuid);

        player.sendMessage(MessageUtil.format(config.getMessage("left-guild"),
                "{player}", player.getName()));
        broadcastToGuild(guildId, MessageUtil.format(config.getMessage("left-guild"),
                "{player}", player.getName()));

        return true;
    }

    // === 踢出成员 ===
    public boolean kickMember(Player kicker, String targetName) {
        GuildMember kickerMember = memberDao.findByUuid(kicker.getUniqueId().toString());
        if (kickerMember == null) {
            kicker.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return false;
        }

        if (!config.hasRankPermission(kickerMember.getRank(), "can-kick")) {
            kicker.sendMessage(MessageUtil.format(config.getMessage("no-permission")));
            return false;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target != null && target.getUniqueId().equals(kicker.getUniqueId())) {
            kicker.sendMessage(MessageUtil.format(config.getMessage("cannot-kick-self")));
            return false;
        }

        // 通过名称查找目标
        GuildMember targetMember = null;
        if (target != null) {
            targetMember = memberDao.findByUuid(target.getUniqueId().toString());
        } else {
            // 离线玩家: 遍历成员列表匹配名称
            for (GuildMember gm : memberDao.findByGuildId(kickerMember.getGuildId())) {
                if (gm.getPlayerName().equalsIgnoreCase(targetName)) {
                    targetMember = gm;
                    break;
                }
            }
        }

        if (targetMember == null || targetMember.getGuildId() != kickerMember.getGuildId()) {
            kicker.sendMessage(MessageUtil.format(config.getMessage("player-not-found")));
            return false;
        }

        int kickerWeight = config.getRankWeight(kickerMember.getRank());
        int targetWeight = config.getRankWeight(targetMember.getRank());
        if (targetWeight >= kickerWeight && !kickerMember.getRank().equals(config.getHighestRank())) {
            kicker.sendMessage(MessageUtil.format(config.getMessage("cannot-kick-higher")));
            return false;
        }

        memberDao.remove(targetMember.getId());
        guildIdCache.remove(targetMember.getPlayerUuid());

        kicker.sendMessage(MessageUtil.format("&a已将 &e" + targetName + " &a踢出公会"));
        if (target != null) {
            target.sendMessage(MessageUtil.format(config.getMessage("kicked-from-guild"),
                    "{guild}", guildDao.findById(kickerMember.getGuildId()).getName()));
        }

        broadcastToGuild(kickerMember.getGuildId(), MessageUtil.format("&e" + targetName + " &c被踢出了公会"));

        return true;
    }

    // === 提升职位 ===
    public boolean promoteMember(Player leader, String targetName) {
        return changeRank(leader, targetName, true);
    }

    // === 降级 ===
    public boolean demoteMember(Player leader, String targetName) {
        return changeRank(leader, targetName, false);
    }

    private boolean changeRank(Player operator, String targetName, boolean promote) {
        GuildMember opMember = memberDao.findByUuid(operator.getUniqueId().toString());
        if (opMember == null || !opMember.getRank().equals(config.getHighestRank())) {
            operator.sendMessage(MessageUtil.format(config.getMessage("not-leader")));
            return false;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target != null && target.getUniqueId().equals(operator.getUniqueId())) {
            operator.sendMessage(MessageUtil.format(config.getMessage("cannot-promote-self")));
            return false;
        }

        GuildMember targetMember = null;
        if (target != null) {
            targetMember = memberDao.findByUuid(target.getUniqueId().toString());
        } else {
            for (GuildMember gm : memberDao.findByGuildId(opMember.getGuildId())) {
                if (gm.getPlayerName().equalsIgnoreCase(targetName)) {
                    targetMember = gm;
                    break;
                }
            }
        }

        if (targetMember == null || targetMember.getGuildId() != opMember.getGuildId()) {
            operator.sendMessage(MessageUtil.format(config.getMessage("player-not-found")));
            return false;
        }

        String newRank = promote
                ? config.getNextRank(targetMember.getRank())
                : config.getPreviousRank(targetMember.getRank());

        if (newRank.equals(targetMember.getRank())) {
            operator.sendMessage(MessageUtil.format(promote
                    ? config.getMessage("already-top-rank")
                    : config.getMessage("already-bottom-rank")));
            return false;
        }

        targetMember.setRank(newRank);
        memberDao.update(targetMember);

        String msgKey = promote ? "promoted" : "demoted";
        operator.sendMessage(MessageUtil.format(config.getMessage(msgKey),
                "{player}", targetName,
                "{rank}", config.getRankDisplay(newRank)));

        if (target != null) {
            target.sendMessage(MessageUtil.format("&a你的公会职位已变为: &6" +
                    config.getRankDisplay(newRank)));
        }

        return true;
    }

    // === 公会升级 ===
    public boolean upgradeGuild(Player player) {
        GuildMember member = memberDao.findByUuid(player.getUniqueId().toString());
        if (member == null) {
            player.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return false;
        }

        if (!config.hasRankPermission(member.getRank(), "can-upgrade")) {
            player.sendMessage(MessageUtil.format(config.getMessage("no-permission")));
            return false;
        }

        Guild guild = guildDao.findById(member.getGuildId());
        if (guild == null) return false;

        int currentLevel = guild.getLevel();
        // 无等级上限, maxLevel = -1 表示无限
        int maxLevel = config.getMaxLevel();
        if (maxLevel > 0 && currentLevel >= maxLevel) {
            player.sendMessage(MessageUtil.format(config.getMessage("max-level")));
            return false;
        }

        int requiredExp = config.getLevelExp(currentLevel + 1);
        if (guild.getExperience() < requiredExp) {
            player.sendMessage(MessageUtil.format(config.getMessage("not-enough-exp"),
                    "{need}", String.valueOf(requiredExp),
                    "{have}", String.valueOf(guild.getExperience())));
            return false;
        }

        // 扣除经验并升级
        guild.setExperience(guild.getExperience() - requiredExp);
        guild.setLevel(currentLevel + 1);
        guild.setBankRows(config.getLevelBankRows(guild.getLevel()));
        guildDao.update(guild);

        broadcastToGuild(guild.getId(), MessageUtil.format(config.getMessage("upgraded"),
                "{level}", String.valueOf(guild.getLevel())));

        // 达成20级时，该公会所有在线成员获得"辉煌旅程"
        if (guild.getLevel() == 20) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                GuildMember gm = memberDao.findByUuid(p.getUniqueId().toString());
                if (gm != null && gm.getGuildId() == guild.getId()) {
                    plugin.getAdvancementManager().grantGloryJourney(p);
                }
            }
        }

        return true;
    }

    // === 添加公会经验 (标记脏，延迟写DB) ===
    public void addGuildExp(int guildId, int exp) {
        Guild guild = guildDao.findById(guildId);
        if (guild == null) return;
        guild.setExperience(guild.getExperience() + exp);
        dirtyGuilds.add(guildId); // 标记脏，30秒后批量写入

        // 自动升级：循环检测，支持连升多级 (最多100次防止死循环)
        int safety = 0;
        while (safety++ < 100) {
            int maxLevel = config.getMaxLevel();
            if (maxLevel > 0 && guild.getLevel() >= maxLevel) break;

            int nextLevel = guild.getLevel() + 1;
            int requiredExp = config.getLevelExp(nextLevel);
            if (guild.getExperience() < requiredExp) break;

            // 扣除经验并升级
            guild.setExperience(guild.getExperience() - requiredExp);
            guild.setLevel(nextLevel);
            guild.setBankRows(config.getLevelBankRows(guild.getLevel()));
            guildDao.update(guild);

            broadcastToGuild(guildId, MessageUtil.format(
                    "&6&l[公会升级] &e" + guild.getName() + " &a已自动升级至 &6Lv." + nextLevel + "&a!"));

            // 20级全员获得辉煌旅程成就
            if (nextLevel == 20) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    GuildMember gm = memberDao.findByUuid(p.getUniqueId().toString());
                    if (gm != null && gm.getGuildId() == guildId) {
                        plugin.getAdvancementManager().grantGloryJourney(p);
                    }
                }
            }
        }
    }

    // === 获取公会信息文本 ===
    public Component getGuildInfo(String guildName) {
        Guild guild = guildDao.findByName(guildName);
        if (guild == null) return null;
        return buildGuildInfoComponent(guild);
    }

    public Component getGuildInfo(int guildId) {
        Guild guild = guildDao.findById(guildId);
        if (guild == null) return null;
        return buildGuildInfoComponent(guild);
    }

    private Component buildGuildInfoComponent(Guild guild) {
        List<GuildMember> members = memberDao.findByGuildId(guild.getId());
        int memberCount = members.size();
        int maxMembers = config.getLevelMaxMembers(guild.getLevel());

        StringBuilder sb = new StringBuilder();
        sb.append("&6&l======= &e&l").append(guild.getName()).append(" &6&l=======\n");
        sb.append("&7等级: &fLv.").append(guild.getLevel())
                .append(" &7| 经验: &f").append(guild.getExperience())
                .append("/").append(config.getLevelExp(guild.getLevel() + 1)).append("\n");
        sb.append("&7会长: &f").append(getPlayerNameByUuid(guild.getLeaderUuid())).append("\n");
        sb.append("&7成员: &f").append(memberCount).append("/").append(maxMembers).append("\n");

        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            sb.append("&7简介: &f").append(guild.getDescription()).append("\n");
        }

        sb.append("&7成员列表:\n");
        for (GuildMember m : members) {
            sb.append("  &7").append(config.getRankDisplay(m.getRank()))
                    .append(": &f").append(m.getPlayerName())
                    .append(" &7[贡献: &6").append(String.format("%.0f", m.getContribution()))
                    .append("&7]\n");
        }

        return MessageUtil.format(sb.toString());
    }

    public void addGuildExpByPlayerUuid(String playerUuid, int exp) {
        GuildMember member = memberDao.findByUuid(playerUuid);
        if (member == null) return;
        addGuildExp(member.getGuildId(), exp);
    }

    public void broadcastToGuild(int guildId, Component message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 优先从缓存查 guildId，未命中再查 DB
            Integer cachedId = guildIdCache.get(p.getUniqueId().toString());
            if (cachedId != null) {
                if (cachedId == guildId) p.sendMessage(message);
            } else {
                GuildMember gm = memberDao.findByUuid(p.getUniqueId().toString());
                if (gm != null) {
                    guildIdCache.put(p.getUniqueId().toString(), gm.getGuildId());
                    if (gm.getGuildId() == guildId) p.sendMessage(message);
                }
            }
        }
    }

    public GuildMember getMember(String playerUuid) {
        GuildMember m = memberDao.findByUuid(playerUuid);
        if (m != null) {
            // 验证公会是否仍然存在，防止孤儿记录
            if (guildDao.findById(m.getGuildId()) == null) {
                // 公会已被删除但成员记录未清理：删除孤儿记录
                plugin.getLogger().warning("清理孤儿成员记录: uuid=" + playerUuid
                        + " guildId=" + m.getGuildId());
                memberDao.removeByUuid(playerUuid);
                guildIdCache.remove(playerUuid);
                return null;
            }
            guildIdCache.put(playerUuid, m.getGuildId());
        } else {
            guildIdCache.remove(playerUuid);
        }
        return m;
    }

    public Guild getGuildByMember(String playerUuid) {
        GuildMember member = getMember(playerUuid); // 通过 getMember 自动清理孤儿
        if (member == null) return null;
        return guildDao.findById(member.getGuildId());
    }

    public List<GuildMember> getGuildMembers(int guildId) {
        return memberDao.findByGuildId(guildId);
    }

    public List<GuildMember> getTopContributors(int guildId, int limit) {
        return memberDao.findTopContributors(guildId, limit);
    }

    public List<GuildMember> getTopContributorsGlobal(int limit) {
        return memberDao.findTopContributorsGlobal(limit);
    }

    private String getPlayerNameByUuid(String uuid) {
        Player p = Bukkit.getPlayer(UUID.fromString(uuid));
        if (p != null) return p.getName();

        // 尝试从成员表查找名称
        GuildMember member = memberDao.findByUuid(uuid);
        if (member != null && member.getPlayerName() != null && !member.getPlayerName().isEmpty()) {
            return member.getPlayerName();
        }
        return uuid.substring(0, 8);
    }

    public String getGuildNameByPlayer(String playerUuid) {
        GuildMember member = memberDao.findByUuid(playerUuid);
        if (member == null) return null;
        Guild guild = guildDao.findById(member.getGuildId());
        return guild != null ? guild.getName() : null;
    }

    public int getGuildIdByPlayer(String playerUuid) {
        GuildMember member = memberDao.findByUuid(playerUuid);
        return member != null ? member.getGuildId() : -1;
    }

    /** 定期将脏公会经验写入数据库 (主线程) */
    private void flushDirtyGuilds() {
        for (int gid : dirtyGuilds) {
            dirtyGuilds.remove(gid);
            Guild guild = guildDao.findById(gid);
            if (guild != null) guildDao.update(guild);
        }
    }

    /** 强制刷新所有脏数据 (插件卸载时调用) */
    public void flushAll() {
        for (int gid : dirtyGuilds) {
            dirtyGuilds.remove(gid);
            Guild guild = guildDao.findById(gid);
            if (guild != null) guildDao.update(guild);
        }
    }
}
