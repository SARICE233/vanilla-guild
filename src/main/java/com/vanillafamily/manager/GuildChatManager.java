package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.ConfigManager;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildChatManager {

    private static final int MAX_HISTORY = 10;

    private final VanillaFamily plugin;
    private final ConfigManager config;
    private final Set<UUID> guildChatPlayers = ConcurrentHashMap.newKeySet();
    // 每个公会的最近消息缓存: guildId → 消息格式文本队列
    private final Map<Integer, Deque<String>> history = new ConcurrentHashMap<>();

    public GuildChatManager(VanillaFamily plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /** 切换公会聊天模式 (主线程) */
    public boolean toggleChat(Player player) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) {
            player.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return false;
        }

        if (guildChatPlayers.remove(player.getUniqueId())) {
            player.sendMessage(MessageUtil.format(config.getMessage("chat-disabled")));
        } else {
            guildChatPlayers.add(player.getUniqueId());
            player.sendMessage(MessageUtil.format(config.getMessage("chat-enabled")));

            // 进入公会聊天时回放最近消息
            replayHistory(player, member.getGuildId());
        }
        return true;
    }

    /** 是否处于公会聊天模式 (任意线程安全) */
    public boolean isInGuildChat(Player player) {
        return guildChatPlayers.contains(player.getUniqueId());
    }

    /** 发送公会聊天消息 (主线程) */
    public void sendGuildChat(Player sender, String message) {
        GuildManager gm = plugin.getGuildManager();
        Guild guild = gm.getGuildByMember(sender.getUniqueId().toString());
        if (guild == null) {
            guildChatPlayers.remove(sender.getUniqueId());
            sender.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return;
        }

        GuildMember member = gm.getMember(sender.getUniqueId().toString());
        if (member == null) return;

        String format = buildChatFormat(guild.getName(), member, sender.getName(), message);
        storeAndBroadcast(guild.getId(), format);
    }

    /** 发送快捷公会消息 (主线程) */
    public void sendQuickChat(Player sender, String guildMessage) {
        if (guildMessage.isEmpty()) return;

        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(sender.getUniqueId().toString());
        if (member == null) {
            sender.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return;
        }

        Guild guild = gm.getGuildByMember(sender.getUniqueId().toString());
        if (guild == null) {
            sender.sendMessage(MessageUtil.format(config.getMessage("not-in-guild")));
            return;
        }

        String format = buildChatFormat(guild.getName(), member, sender.getName(), guildMessage);
        storeAndBroadcast(guild.getId(), format);
    }

    private String buildChatFormat(String guildName, GuildMember member, String playerName, String msg) {
        String rankDisplay = plugin.getConfigManager().getRankDisplay(member.getRank());
        return config.getChatPrefix() + " &f[" + guildName + "] &7"
                + rankDisplay + " &f" + playerName + "&7: &r" + msg;
    }

    private void storeAndBroadcast(int guildId, String textFormat) {
        // 存入历史
        Deque<String> queue = history.computeIfAbsent(guildId,
                k -> new ArrayDeque<>(MAX_HISTORY));
        if (queue.size() >= MAX_HISTORY) queue.pollFirst();
        queue.addLast(textFormat);

        // 广播给当前在线的公会成员
        Component chatMessage = MessageUtil.format(textFormat);
        plugin.getGuildManager().broadcastToGuild(guildId, chatMessage);
        Bukkit.getConsoleSender().sendMessage(chatMessage);
    }

    /** 回放最近消息 */
    private void replayHistory(Player player, int guildId) {
        Deque<String> queue = history.get(guildId);
        if (queue == null || queue.isEmpty()) return;

        player.sendMessage(MessageUtil.format("&7&o--- 公会最近消息 ---"));
        for (String msg : queue) {
            player.sendMessage(MessageUtil.format(msg));
        }
        player.sendMessage(MessageUtil.format("&7&o--- 以上共 &f" + queue.size() + " &7&o条 ---"));
    }
}
