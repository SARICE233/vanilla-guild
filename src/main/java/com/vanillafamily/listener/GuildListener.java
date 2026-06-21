package com.vanillafamily.listener;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.ConfigManager;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.manager.*;
import com.vanillafamily.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuildListener implements Listener {

    private final VanillaFamily plugin;

    public GuildListener(VanillaFamily plugin) {
        this.plugin = plugin;
    }

    // === 玩家登录: 显示公会信息 & 应用BUFF ===
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GuildManager gm = plugin.getGuildManager();
        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());

        if (guild != null) {
            // 延迟发送, 确保玩家加载完成
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(MessageUtil.format(
                        "&a欢迎回来! 你属于公会: &e" + guild.getName()));
            }, 20L);

            // 应用 BUFF
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getGuildBuffManager().applyBuffsToPlayer(player);
            }, 30L);
        }
    }

    // === 玩家退出: 清理 ===
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGuildBankManager().removePlayer(event.getPlayer());
    }

    // === 聊天: 优先处理输入提示 → 公会频道拦截 ===
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        // 1. 检查是否有待处理的输入提示 (创建/加入/捐献等)
        boolean promptHandled = plugin.getPromptManager().handleInput(player, message);
        if (promptHandled) {
            event.setCancelled(true);
            return;
        }

        GuildChatManager chat = plugin.getGuildChatManager();

        // 2. 检查 ! 快捷消息 (支持 ! 和 ! )
        if (message.startsWith("!") && message.length() > 1) {
            event.setCancelled(true);
            String guildMsg = message.startsWith("! ") ? message.substring(2).trim() : message.substring(1).trim();
            if (!guildMsg.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        chat.sendQuickChat(player, guildMsg));
            }
            return;
        }

        // 检查公会聊天模式
        if (chat.isInGuildChat(player)) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    chat.sendGuildChat(player, message));
        }
    }

    // === 经验加成 (公会等级自动决定) ===
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        GuildManager gm = plugin.getGuildManager();
        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) return;

        double effect = plugin.getGuildBuffManager().getBuffEffect(guild.getLevel(), "EXP_BOOST");
        if (effect > 0) {
            int bonus = (int) (event.getAmount() * (effect / 100.0));
            if (bonus > 0) event.setAmount(event.getAmount() + bonus);
        }
        // 每日委托: GAIN_EXP
        plugin.getDailyQuestManager().addProgress(player, "GAIN_EXP", event.getAmount());
    }

    // === 伤害加成 ===
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Monster)) return;

        GuildManager gm = plugin.getGuildManager();
        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) return;

        double effect = plugin.getGuildBuffManager().getBuffEffect(guild.getLevel(), "DAMAGE_BOOST");
        if (effect > 0) event.setDamage(event.getDamage() * (1 + effect / 100.0));
    }

    // === 减伤 ===
    @EventHandler
    public void onEntityDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        GuildManager gm = plugin.getGuildManager();
        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) return;

        double effect = plugin.getGuildBuffManager().getBuffEffect(guild.getLevel(), "DAMAGE_REDUCTION");
        if (effect > 0) event.setDamage(event.getDamage() * (1 - effect / 100.0));
    }

    // === 打怪贡献 & 公会经验 & 任务进度 ===
    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!(event.getEntity() instanceof Monster)) return;

        plugin.getContributionManager().addMobKillContribution(killer);
        plugin.getGuildTaskManager().addKillProgress(killer);
        plugin.getDailyQuestManager().addProgress(killer, "KILL_MOBS", 1);
    }

    // === 挖掘 (BREAK_BLOCK 委托) ===
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        plugin.getDailyQuestManager().addProgress(event.getPlayer(), "BREAK_BLOCK", 1);
    }

    // === 钓鱼 (FISH 委托) ===
    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() == null) return;
        plugin.getDailyQuestManager().addProgress(event.getPlayer(), "FISH", 1);
    }

    // === 仓库 GUI 交互 + 主面板 GUI ===
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        ItemStack currentItem = event.getCurrentItem();
        Inventory topInv = event.getView().getTopInventory();
        GuildGuiManager guiMgr = plugin.getGuildGuiManager();

        // === 主面板 / 成员列表 GUI ===
        // 多重识别策略: 1) InventoryHolder  2) 物品 PDC 标签
        boolean isOurGui = guiMgr.isOurGui(topInv);
        boolean isOurItem = GuildGuiManager.isOurGuiItem(currentItem);

        if (isOurGui || isOurItem) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= topInv.getSize()) return;
            if (event.getClickedInventory() != topInv) return;

            plugin.getLogger().info("[GuildGUI] 点击处理: player=" + player.getName()
                    + " slot=" + slot + " holder=" + isOurGui + " pdc=" + isOurItem);

            guiMgr.handleMainClick(player, slot, topInv);
            return;
        }

        // === 仓库 GUI ===
        GuildBankManager bm = plugin.getGuildBankManager();
        Integer guildId = bm.getGuildIdForPlayer(player);
        if (guildId == null) return;

        // 如果点击的是自己的背包, 允许 (放入物品)
        if (event.getClickedInventory() == player.getInventory()) {
            return;
        }

        // 点击的是工会仓库 GUI
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) {
            event.setCancelled(true);
            return;
        }

        ConfigManager config = plugin.getConfigManager();
        boolean canTake = config.hasRankPermission(member.getRank(), "can-bank-withdraw");

        // 取物品: 需要 OFFICER+
        // 点击仓库内的物品 → 如果是shift-click取走, 检查权限
        if (!canTake && event.getAction().name().contains("PICKUP")) {
            // 检查是否是取出操作
            if (event.getCursor() == null || event.getCursor().getType().isAir()) {
                // 玩家鼠标上没东西, 正在从仓库取物品
                if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                    event.setCancelled(true);
                    player.sendMessage(MessageUtil.format("&c你的职位没有取出物品的权限!"));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        GuildBankManager bm = plugin.getGuildBankManager();
        Integer guildId = bm.getGuildIdForPlayer(player);
        if (guildId == null) return;

        // 保存仓库
        bm.saveInventory(guildId, event.getInventory());
        bm.removePlayer(player);
    }
}
