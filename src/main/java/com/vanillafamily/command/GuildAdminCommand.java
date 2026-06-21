package com.vanillafamily.command;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.model.Guild;
import com.vanillafamily.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GuildAdminCommand implements CommandExecutor {

    private final VanillaFamily plugin;

    public GuildAdminCommand(VanillaFamily plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vanilla.admin")) {
            sender.sendMessage(MessageUtil.format(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "delete"  -> handleDelete(sender, args);
            case "reload"  -> handleReload(sender);
            case "givexp"  -> handleGiveExp(sender, args);
            case "list"    -> handleList(sender);
            case "task"    -> handleTask(sender, args);
            default        -> sendHelp(sender);
        };
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.format("&7用法: /guildadmin delete <公会名>"));
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

        Guild guild = plugin.getDatabaseManager().getGuildDao().findByName(guildName);
        if (guild == null) {
            sender.sendMessage(MessageUtil.format("&c公会不存在"));
            return true;
        }

        plugin.getDatabaseManager().getGuildDao().delete(guild.getId());
        sender.sendMessage(MessageUtil.format("&a公会 &e" + guildName + " &a已被强制删除"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        sender.sendMessage(MessageUtil.format("&a配置已重载"));
        return true;
    }

    private boolean handleGiveExp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.format("&7用法: /guildadmin givexp <公会名> <经验值>"));
            return true;
        }
        String guildName = args[1];
        if (args.length > 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length - 1; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            guildName = sb.toString();
        }

        try {
            int exp = Integer.parseInt(args[args.length - 1]);
            Guild guild = plugin.getDatabaseManager().getGuildDao().findByName(guildName);
            if (guild == null) {
                sender.sendMessage(MessageUtil.format("&c工会不存在"));
                return true;
            }
            plugin.getGuildManager().addGuildExp(guild.getId(), exp);
            sender.sendMessage(MessageUtil.format("&a已给予公会 &e" + guildName +
                    " &6" + exp + " &a经验"));
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.format("&c请输入有效数字"));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        java.util.List<Guild> guilds = plugin.getDatabaseManager().getGuildDao().findAll();
        sender.sendMessage(MessageUtil.format("&6&l=== 所有公会列表 ==="));
        if (guilds.isEmpty()) {
            sender.sendMessage(MessageUtil.format("&7暂无公会"));
        }
        int i = 1;
        for (Guild g : guilds) {
            int memberCount = plugin.getDatabaseManager().getGuildMemberDao().countByGuildId(g.getId());
            sender.sendMessage(MessageUtil.format(
                    "&7" + i + ". &f" + g.getName() +
                            " &7Lv." + g.getLevel() +
                            " &7成员: &f" + memberCount +
                            " &7会长: &f" + plugin.getServer().getOfflinePlayer(
                                    java.util.UUID.fromString(g.getLeaderUuid())).getName()));
            i++;
        }
        return true;
    }

    private boolean handleTask(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.format("&7/guildadmin task create <名称> <目标击杀> <报酬金币>"));
            sender.sendMessage(MessageUtil.format("&7/guildadmin task delete <任务ID>"));
            sender.sendMessage(MessageUtil.format("&7/guildadmin task list"));
            return true;
        }
        return switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 5) {
                    sender.sendMessage(MessageUtil.format("&7用法: /guildadmin task create <名称> <目标击杀数> <报酬金币>"));
                    yield true;
                }
                try {
                    String name = args[2];
                    int target = Integer.parseInt(args[3]);
                    int reward = Integer.parseInt(args[4]);
                    var task = plugin.getGuildTaskManager().createTask(name, target, reward);
                    if (task != null) {
                        sender.sendMessage(MessageUtil.format("&a任务创建成功: &e" + name +
                                " &7目标: &f" + target + " 击杀 &7报酬: &6" + reward + " 金币"));
                    } else {
                        sender.sendMessage(MessageUtil.format("&c任务创建失败"));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.format("&c请输入有效数字"));
                }
                yield true;
            }
            case "delete" -> {
                if (args.length < 3) { sender.sendMessage(MessageUtil.format("&7用法: /guildadmin task delete <任务ID>")); yield true; }
                try {
                    int id = Integer.parseInt(args[2]);
                    boolean ok = plugin.getGuildTaskManager().deleteTask(id);
                    sender.sendMessage(MessageUtil.format(ok ? "&a任务已删除" : "&c任务不存在"));
                } catch (NumberFormatException e) { sender.sendMessage(MessageUtil.format("&c无效ID")); }
                yield true;
            }
            case "list" -> {
                sender.sendMessage(MessageUtil.format(plugin.getGuildTaskManager().getTasksInfo()));
                yield true;
            }
            default -> { sender.sendMessage(MessageUtil.format("&7未知子命令")); yield true; }
        };
    }

    private boolean sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.format("&6&l====== 公会管理 ======"));
        sender.sendMessage(MessageUtil.format("&e/guildadmin delete <公会名>  &7- 强制删除公会"));
        sender.sendMessage(MessageUtil.format("&e/guildadmin givexp <公会名> <数量> &7- 给予经验"));
        sender.sendMessage(MessageUtil.format("&e/guildadmin list           &7- 列出所有公会"));
        sender.sendMessage(MessageUtil.format("&e/guildadmin reload         &7- 重载配置"));
        sender.sendMessage(MessageUtil.format("&e/guildadmin task create <名> <击杀> <金币> &7- 创建任务"));
        sender.sendMessage(MessageUtil.format("&e/guildadmin task delete <ID> &7- 删除任务"));
        sender.sendMessage(MessageUtil.format("&e/guildadmin task list      &7- 查看任务进度"));
        return true;
    }
}
