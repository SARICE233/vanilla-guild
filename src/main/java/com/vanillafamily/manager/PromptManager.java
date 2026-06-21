package com.vanillafamily.manager;

import com.vanillafamily.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 通用输入提示系统。
 * GUI 点击 → 提示输入 → 聊天栏直接输入数值/名称 → 自动执行
 */
public class PromptManager {

    private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();
    private Plugin plugin; // 由 VanillaFamily 设置

    /** 注册一个输入提示 */
    public void prompt(Player player, String hint, Consumer<String> callback) {
        prompts.put(player.getUniqueId(), callback);
        player.sendMessage(MessageUtil.format("&e▶ &f" + hint));
        player.sendMessage(MessageUtil.format("&7(输入 &ccancel &7取消)"));
    }

    /** 由 VanillaFamily 在启用时调用，注入插件实例 */
    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    /** 处理玩家聊天输入，消费待处理回调。回调在主线程执行以确保线程安全。 */
    public boolean handleInput(Player player, String input) {
        Consumer<String> callback = prompts.remove(player.getUniqueId());
        if (callback == null) return false;

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(MessageUtil.format("&7已取消"));
            return true;
        }

        String trimmed = input.trim();
        // 聊天事件在异步线程触发，但回调可能需要主线程（如数据库操作），
        // 因此将回调调度到主线程执行。
        if (plugin != null && !Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(trimmed));
        } else {
            callback.accept(trimmed);
        }
        return true;
    }
}
