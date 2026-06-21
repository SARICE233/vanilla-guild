package com.vanillafamily.command;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.manager.*;
import com.vanillafamily.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuildCommand implements CommandExecutor {

    private final VanillaFamily plugin;

    public GuildCommand(VanillaFamily plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.format("&c此命令只能由玩家执行"));
            return true;
        }

        if (args.length == 0) {
            // 无参数 → 打开主 GUI 面板
            plugin.getGuildGuiManager().openMainGui(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        try {
            return switch (subCommand) {
                case "create"   -> handleCreate(player, args);
                case "disband"  -> handleDisband(player, args);
                case "info"     -> handleInfo(player, args);
                case "invite"   -> handleInvite(player, args);
                case "join"     -> handleJoin(player, args);
                case "leave"    -> handleLeave(player);
                case "kick"     -> handleKick(player, args);
                case "promote"  -> handlePromote(player, args);
                case "demote"   -> handleDemote(player, args);
                case "chat", "c" -> handleChat(player);
                case "upgrade"  -> handleUpgrade(player);
                case "bank"     -> handleBank(player);
                case "buff"     -> { showBuffInfo(player); yield true; }
                case "top"      -> handleTop(player, args);
                case "contribute", "donate" -> handleContribute(player, args);
                case "sign"     -> handleSign(player);
                case "daily"    -> { showDaily(player); yield true; }
                case "task"     -> { showTasks(player); yield true; }
                case "help"     -> sendHelp(player);
                default         -> { sendHelp(player); yield true; }
            };
        } catch (Exception e) {
            player.sendMessage(MessageUtil.format("&c命令执行出错: " + e.getMessage()));
            e.printStackTrace();
            return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("vanilla.guild.create")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.format("&7用法: /guild create <公会名>"));
            return true;
        }
        String name = args[1];
        // 支持中文名，用空格连接
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            name = sb.toString();
        }
        plugin.getGuildManager().createGuild(player, name);
        return true;
    }

    private boolean handleDisband(Player player, String[] args) {
        if (!player.hasPermission("vanilla.guild.disband")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        String confirm = args.length > 1 ? args[1] : "";
        plugin.getGuildManager().disbandGuild(player, confirm);
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String guildName;
        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            guildName = sb.toString();
        } else {
            String uuid = player.getUniqueId().toString();
            guildName = plugin.getGuildManager().getGuildNameByPlayer(uuid);
            if (guildName == null) {
                player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("not-in-guild")));
                return true;
            }
        }
        Component info = plugin.getGuildManager().getGuildInfo(guildName);
        if (info == null) {
            player.sendMessage(MessageUtil.format("&c公会不存在"));
        } else {
            player.sendMessage(info);
        }
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (!player.hasPermission("vanilla.guild.invite")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.format("&7用法: /guild invite <玩家>"));
            return true;
        }
        plugin.getGuildManager().invitePlayer(player, args[1]);
        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtil.format("&7用法: /guild join <公会名>"));
            return true;
        }
        String guildName = args[1];
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            guildName = sb.toString();
        }
        plugin.getGuildManager().joinGuild(player, guildName);
        return true;
    }

    private boolean handleLeave(Player player) {
        plugin.getGuildManager().leaveGuild(player);
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (!player.hasPermission("vanilla.guild.kick")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.format("&7用法: /guild kick <玩家>"));
            return true;
        }
        plugin.getGuildManager().kickMember(player, args[1]);
        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        if (!player.hasPermission("vanilla.guild.promote")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.format("&7用法: /guild promote <玩家>"));
            return true;
        }
        plugin.getGuildManager().promoteMember(player, args[1]);
        return true;
    }

    private boolean handleDemote(Player player, String[] args) {
        if (!player.hasPermission("vanilla.guild.demote")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.format("&7用法: /guild demote <玩家>"));
            return true;
        }
        plugin.getGuildManager().demoteMember(player, args[1]);
        return true;
    }

    private boolean handleChat(Player player) {
        if (!player.hasPermission("vanilla.guild.chat")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        plugin.getGuildChatManager().toggleChat(player);
        return true;
    }

    private boolean handleUpgrade(Player player) {
        if (!player.hasPermission("vanilla.guild.upgrade")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        plugin.getGuildManager().upgradeGuild(player);
        return true;
    }

    private boolean handleBank(Player player) {
        if (!player.hasPermission("vanilla.guild.bank")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        plugin.getGuildBankManager().openBank(player);
        return true;
    }

    private boolean showBuffInfo(Player player) {
        GuildManager gm = plugin.getGuildManager();
        int guildId = gm.getGuildIdByPlayer(player.getUniqueId().toString());
        if (guildId == -1) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("not-in-guild")));
            return true;
        }
        player.sendMessage(MessageUtil.format(plugin.getGuildBuffManager().getBuffInfo(guildId)));
        return true;
    }

    private boolean handleTop(Player player, String[] args) {
        GuildManager gm = plugin.getGuildManager();
        int guildId = gm.getGuildIdByPlayer(player.getUniqueId().toString());
        plugin.getContributionManager().showTopContributors(player, guildId);
        return true;
    }

    private boolean handleContribute(Player player, String[] args) {
        if (!player.hasPermission("vanilla.guild.contribute")) {
            player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.format("&7用法: /guild contribute <金币数量>"));
            return true;
        }
        try {
            int amount = Integer.parseInt(args[1]);
            plugin.getContributionManager().donateGold(player, amount);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.format("&c请输入有效数字"));
        }
        return true;
    }

    private boolean handleSign(Player player) {
        plugin.getContributionManager().dailySign(player);
        return true;
    }

    private boolean showDaily(Player player) {
        int gid = plugin.getGuildManager().getGuildIdByPlayer(player.getUniqueId().toString());
        if (gid == -1) { player.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("not-in-guild"))); return true; }
        player.sendMessage(MessageUtil.format(plugin.getDailyQuestManager().getDailyInfo(gid)));
        return true;
    }

    private boolean showTasks(Player player) {
        player.sendMessage(MessageUtil.format(plugin.getGuildTaskManager().getTasksInfo()));
        return true;
    }

    private boolean sendHelp(Player player) {
        var config = plugin.getConfigManager();
        String topRank = config.getRankDisplay(config.getHighestRank());
        String inviteRank = config.getRankDisplay(config.getRankKeys().size() >= 2 ? config.getRankKeys().get(1) : config.getHighestRank());

        player.sendMessage(MessageUtil.format("&6&l====== 香草家族 - 公会系统 ======"));
        player.sendMessage(MessageUtil.format("&e/guild create <名字>   &7- 创建公会 (&6" + config.getCreateCost() + " 金币)"));
        player.sendMessage(MessageUtil.format("&e/guild disband          &7- 解散公会 (" + topRank + ")"));
        player.sendMessage(MessageUtil.format("&e/guild info [公会名]    &7- 查看公会信息"));
        player.sendMessage(MessageUtil.format("&e/guild invite <玩家>    &7- 邀请玩家 (" + inviteRank + "+)"));
        player.sendMessage(MessageUtil.format("&e/guild join <公会名>    &7- 接受邀请加入"));
        player.sendMessage(MessageUtil.format("&e/guild leave            &7- 离开公会"));
        player.sendMessage(MessageUtil.format("&e/guild kick <玩家>      &7- 踢出成员 (" + inviteRank + "+)"));
        player.sendMessage(MessageUtil.format("&e/guild promote <玩家>   &7- 提升职位 (" + topRank + ")"));
        player.sendMessage(MessageUtil.format("&e/guild demote <玩家>    &7- 降级 (" + topRank + ")"));
        player.sendMessage(MessageUtil.format("&e/guild chat             &7- 切换公会聊天"));
        player.sendMessage(MessageUtil.format("&e/guild upgrade          &7- 升级公会 (" + inviteRank + "+)"));
        player.sendMessage(MessageUtil.format("&e/guild bank             &7- 打开公会仓库"));
        player.sendMessage(MessageUtil.format("&e/guild buff [类型]      &7- 查看/激活BUFF"));
        player.sendMessage(MessageUtil.format("&e/guild contribute <钱>  &7- 捐献金币换贡献"));
        player.sendMessage(MessageUtil.format("&e/guild sign             &7- 每日签到"));
        player.sendMessage(MessageUtil.format("&e/guild top              &7- 贡献排行"));
        player.sendMessage(MessageUtil.format("&7提示: 聊天中可使用 &e! 消息 &7快捷发送公会消息"));
        return true;
    }
}
