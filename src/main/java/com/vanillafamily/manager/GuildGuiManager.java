package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.ConfigManager;
import com.vanillafamily.manager.PromptManager;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GuildGuiManager {

    private final VanillaFamily plugin;
    /** PDC 标签键 — 每个 GUI 物品都带有此标签，点击时可靠识别 */
    public static NamespacedKey GUI_KEY;

    /** 自定义 InventoryHolder — 用于可靠识别我们的 GUI */
    public static class GuildGuiHolder implements InventoryHolder {
        private final String type; // "main" | "member-list"

        public GuildGuiHolder(String type) {
            this.type = type;
        }

        public String getGuiType() {
            return type;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null; // 不在此处提供 Inventory
        }
    }

    public GuildGuiManager(VanillaFamily plugin) {
        this.plugin = plugin;
        if (GUI_KEY == null) {
            GUI_KEY = new NamespacedKey(plugin, "guild-gui");
        }
    }

    /** 是否是我们的 GUI — 多重识别：InventoryHolder + 物品 PDC */
    public boolean isOurGui(Inventory inv) {
        // 方案1: InventoryHolder (标准做法)
        if (inv.getHolder() instanceof GuildGuiHolder) {
            return true;
        }
        // 方案2: 检查第一个有物品的槽位是否带 PDC 标签 (兜底)
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.hasItemMeta()) {
                String tag = item.getItemMeta().getPersistentDataContainer()
                        .get(GUI_KEY, PersistentDataType.STRING);
                if (tag != null) return true;
            }
        }
        return false;
    }

    /** 检查单个物品是否属于我们的 GUI (通过 PDC 标签) */
    public static boolean isOurGuiItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(GUI_KEY, PersistentDataType.STRING);
    }

    /** 打开主面板 — 根据是否在公会显示不同界面 */
    public void openMainGui(Player player) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());

        if (member == null) {
            openNonMemberGui(player);
        } else {
            openMemberGui(player);
        }
    }

    // ==================== 未加入公会 ====================

    private void openNonMemberGui(Player player) {
        Inventory inv = Bukkit.createInventory(new GuildGuiHolder("main"), 9,
                MessageUtil.format("&8✦ 香草家族 — 公会大厅"));

        inv.setItem(1, makeItem(Material.NETHER_STAR, "&e&l创建公会",
                "&7花费: &6" + plugin.getConfigManager().getCreateCost() + " 金币",
                "&7点击后在聊天栏输入公会名"));
        inv.setItem(3, makeItem(Material.WRITABLE_BOOK, "&a&l加入公会",
                "&7点击后在聊天栏输入公会名",
                "&7需要先收到邀请"));
        inv.setItem(5, makeItem(Material.BOOK, "&b&l查看公会",
                "&7点击后在聊天栏输入公会名",
                "&7查看任意公会的信息"));
        inv.setItem(7, makeItem(Material.DIAMOND, "&6&l贡献排行",
                "&7查看全服贡献排行榜"));
        inv.setItem(8, makeItem(Material.KNOWLEDGE_BOOK, "&a&l帮助",
                "&7命令 / 快捷操作 / 提示"));

        player.openInventory(inv);
    }

    // ==================== 已加入公会 ====================

    private void openMemberGui(Player player) {
        GuildManager gm = plugin.getGuildManager();
        ConfigManager config = plugin.getConfigManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) {
            // 数据不一致：成员记录存在但公会不存在，回退到非成员界面
            openNonMemberGui(player);
            return;
        }

        int memberCount = gm.getGuildMembers(guild.getId()).size();
        int maxMembers = config.getLevelMaxMembers(guild.getLevel());

        Inventory inv = Bukkit.createInventory(new GuildGuiHolder("main"), 27,
                MessageUtil.format("&8✦ " + guild.getName() + " — Lv." + guild.getLevel()));

        // 第1行: 功能按钮
        inv.setItem(0,  makeItem(Material.BOOK, "&b&l公会信息",
                "&7等级: &fLv." + guild.getLevel(),
                "&7经验: &f" + guild.getExperience(),
                "&7成员: &f" + memberCount + "/" + maxMembers));
        inv.setItem(1,  makeItem(Material.PLAYER_HEAD, "&a&l成员列表",
                "&7点击查看所有成员"));
        inv.setItem(2,  makeItem(Material.PAPER, "&d&l公会聊天",
                "&7状态: " + (plugin.getGuildChatManager().isInGuildChat(player) ? "&a开启" : "&7关闭"),
                "&7点击切换"));
        inv.setItem(3,  makeItem(Material.CHEST, "&6&l公会仓库",
                "&7行数: &f" + guild.getBankRows() + " 行"));
        inv.setItem(4,  makeItem(Material.EXPERIENCE_BOTTLE, "&e&l升级公会",
                "&7当前: Lv." + guild.getLevel(),
                "&7升级消耗经验"));
        inv.setItem(5,  makeItem(Material.POTION, "&c&l公会BUFF",
                "&7随公会等级自动解锁",
                "&7Lv.1→20 逐步增长至50%加成"));
        inv.setItem(6,  makeItem(Material.NAME_TAG, "&a&l邀请玩家",
                "&7点击后在聊天栏输入玩家名"));
        inv.setItem(7,  makeItem(Material.GOLD_INGOT, "&6&l捐献贡献",
                "&7你的贡献: &6" + String.format("%.0f", member.getContribution()),
                "&7点击后在聊天栏输入金币数量"));
        inv.setItem(8,  makeItem(Material.CLOCK, "&e&l每日签到",
                "&7签到获得贡献点"));

        // 第2行: 辅助功能
        inv.setItem(18, makeItem(Material.BEACON, "&b&l贡献排行",
                "&7查看公会内贡献排行"));
        inv.setItem(19, makeItem(Material.ENDER_PEARL, "&c&l离开公会",
                "&7注意: 离开后贡献清零"));
        inv.setItem(26, makeItem(Material.KNOWLEDGE_BOOK, "&a&l帮助",
                "&7命令/快捷键/提示"));

        // 只有最高职位才显示解散按钮
        if (member.getRank().equals(config.getHighestRank())) {
            inv.setItem(20, makeItem(Material.TNT, "&4&l解散公会",
                    "&c⚠ 此操作不可撤销!",
                    "&c点击后需输入确认"));
        }

        player.openInventory(inv);
    }

    /** 处理主面板点击 */
    public boolean handleMainClick(Player player, int slot, Inventory inventory) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());

        plugin.getLogger().info("[GuildGUI] handleMainClick: player=" + player.getName()
                + " slot=" + slot + " isMember=" + (member != null));

        if (member == null) {
            return handleNonMemberClick(player, slot);
        }

        // 成员 GUI：确认公会确实存在，不存在则回退到非成员处理
        // （与 openMemberGui 的回退逻辑保持一致）
        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) {
            plugin.getLogger().warning("[GuildGUI] 公会数据不一致，回退非成员处理: player="
                    + player.getName());
            return handleNonMemberClick(player, slot);
        }

        return handleMemberClick(player, slot, member, guild);
    }

    private boolean handleNonMemberClick(Player player, int slot) {
        PromptManager pm = plugin.getPromptManager();
        plugin.getLogger().info("[GuildGUI] handleNonMemberClick: player="
                + player.getName() + " slot=" + slot);
        switch (slot) {
            case 1 -> // 创建公会 (关闭GUI方便输入)
                withClose(player, () ->
                    pm.prompt(player, "请输入公会名称", name ->
                            plugin.getGuildManager().createGuild(player, name)));
            case 3 -> // 加入公会
                withClose(player, () ->
                    pm.prompt(player, "请输入要加入的公会名称", name ->
                            plugin.getGuildManager().joinGuild(player, name)));
            case 5 -> // 查看公会
                withClose(player, () ->
                    pm.prompt(player, "请输入要查看的公会名称", name -> {
                        var info = plugin.getGuildManager().getGuildInfo(name);
                        player.sendMessage(info != null ? info : MessageUtil.format("&c公会不存在"));
                    }));
            case 7 -> // 排行 — 关闭GUI让玩家看到结果
                withClose(player, () ->
                    plugin.getContributionManager().showTopContributors(player, -1));
            case 8 -> // 帮助 — 关闭GUI让玩家看到结果
                withClose(player, () -> showHelp(player));
        }
        return true;
    }

    private boolean handleMemberClick(Player player, int slot, GuildMember member, Guild guild) {
        GuildManager gm = plugin.getGuildManager();
        ConfigManager config = plugin.getConfigManager();

        plugin.getLogger().info("[GuildGUI] handleMemberClick: player=" + player.getName()
                + " slot=" + slot + " guild=" + guild.getName() + " rank=" + member.getRank());

        switch (slot) {
            case 0 -> { // 公会信息 — 先关 GUI 再显示文本
                plugin.getLogger().info("[GuildGUI] member-slot0: 公会信息");
                withClose(player, () ->
                    player.sendMessage(gm.getGuildInfo(guild.getId())));
            }
            case 1 -> { // 成员列表 (延迟一 tick 避免 InventoryClickEvent 内 openInventory)
                plugin.getLogger().info("[GuildGUI] member-slot1: 成员列表");
                final int gid = guild.getId();
                Bukkit.getScheduler().runTask(plugin,
                        () -> showMemberList(player, gid));
            }
            case 2 -> { // 切换聊天
                plugin.getLogger().info("[GuildGUI] member-slot2: 切换聊天");
                withClose(player, () ->
                    plugin.getGuildChatManager().toggleChat(player));
            }
            case 3 -> { // 仓库 — 延迟一 tick 打开新 GUI
                plugin.getLogger().info("[GuildGUI] member-slot3: 仓库");
                Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGuildBankManager().openBank(player));
            }
            case 4 -> { // 升级
                plugin.getLogger().info("[GuildGUI] member-slot4: 升级");
                withClose(player, () ->
                    plugin.getGuildManager().upgradeGuild(player));
            }
            case 5 -> { // BUFF 展示
                plugin.getLogger().info("[GuildGUI] member-slot5: BUFF");
                withClose(player, () -> {
                    String buffInfo = plugin.getGuildBuffManager().getBuffInfo(guild.getId());
                    player.sendMessage(MessageUtil.format(buffInfo));
                });
            }
            case 6 -> { // 邀请
                plugin.getLogger().info("[GuildGUI] member-slot6: 邀请");
                withClose(player, () ->
                    plugin.getPromptManager().prompt(player, "请输入要邀请的玩家名", name ->
                            plugin.getGuildManager().invitePlayer(player, name)));
            }
            case 7 -> { // 捐献
                plugin.getLogger().info("[GuildGUI] member-slot7: 捐献");
                withClose(player, () ->
                    plugin.getPromptManager().prompt(player, "请输入捐献金币数量", input -> {
                        try {
                            int amount = Integer.parseInt(input);
                            plugin.getContributionManager().donateGold(player, amount);
                        } catch (NumberFormatException e) {
                            player.sendMessage(MessageUtil.format("&c请输入有效数字"));
                        }
                    }));
            }
            case 8 -> { // 签到
                plugin.getLogger().info("[GuildGUI] member-slot8: 签到");
                withClose(player, () ->
                    plugin.getContributionManager().dailySign(player));
            }
            case 18 -> { // 排行
                plugin.getLogger().info("[GuildGUI] member-slot18: 排行");
                withClose(player, () ->
                    plugin.getContributionManager().showTopContributors(player, guild.getId()));
            }
            case 19 -> { // 离开
                plugin.getLogger().info("[GuildGUI] member-slot19: 离开");
                withClose(player, () ->
                    plugin.getGuildManager().leaveGuild(player));
            }
            case 20 -> { // 解散 (仅最高职位可见)
                plugin.getLogger().info("[GuildGUI] member-slot20: 解散");
                withClose(player, () -> {
                    if (member.getRank().equals(config.getHighestRank())) {
                        plugin.getPromptManager().prompt(player,
                            "⚠ 确定解散公会? 输入 &cconfirm &f确认", input -> {
                                if ("confirm".equalsIgnoreCase(input.trim())) {
                                    plugin.getGuildManager().disbandGuild(player, "confirm");
                                } else {
                                    player.sendMessage(MessageUtil.format("&7已取消解散"));
                                }
                            });
                    }
                });
            }
            case 26 -> { // 帮助
                plugin.getLogger().info("[GuildGUI] member-slot26: 帮助");
                withClose(player, () -> showHelp(player));
            }
            default -> {
                // 未处理的槽位（空槽或装饰物品）
                plugin.getLogger().info("[GuildGUI] member-unhandled: slot=" + slot);
            }
        }
        return true;
    }

    /** 显示成员列表 (GUI 内) */
    private void showMemberList(Player player, int guildId) {
        GuildManager gm = plugin.getGuildManager();
        ConfigManager config = plugin.getConfigManager();
        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) return;

        List<GuildMember> members = gm.getGuildMembers(guildId);
        int size = ((members.size() - 1) / 9 + 1) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(new GuildGuiHolder("member-list"), size,
                MessageUtil.format("&8✦ " + guild.getName() + " — 成员列表"));

        for (int i = 0; i < members.size(); i++) {
            GuildMember m = members.get(i);
            ItemStack head = makeItem(Material.PLAYER_HEAD,
                    "&f" + m.getPlayerName(),
                    "&7职位: &6" + config.getRankDisplay(m.getRank()),
                    "&7贡献: &6" + String.format("%.0f", m.getContribution()));
            inv.setItem(i, head);
        }

        player.openInventory(inv);
    }

    // ==================== 工具方法 ====================

    /**
     * 关闭 GUI 并延迟执行动作。
     * 依据 Paper 1.21.4 InventoryClickEvent Javadoc:
     *   HumanEntity#closeInventory() 绝不能在 InventoryClickEvent 回调内直接调用，
     *   必须通过 BukkitScheduler.runTask() 延迟到下一 tick。
     *
     * 时序：
     *   tick N:   InventoryClickEvent → withClose() → 调度任务
     *   tick N+1: player.closeInventory() 安全执行
     *   tick N+2: action.run() 执行（GUI 已完全关闭，玩家可看到消息）
     */
    private void withClose(Player player, Runnable action) {
        final String playerName = player.getName();
        // 第1步: 下一 tick 关闭 GUI
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                player.closeInventory();
                plugin.getLogger().info("[GuildGUI] withClose-关闭: " + playerName);
            } catch (Exception e) {
                plugin.getLogger().severe("[GuildGUI] withClose-关闭异常: " + e.getMessage());
                e.printStackTrace();
            }
            // 第2步: 再下一 tick 执行实际动作（确保 close 完全生效）
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    action.run();
                    plugin.getLogger().info("[GuildGUI] withClose-动作: " + playerName);
                } catch (Exception e) {
                    plugin.getLogger().severe("[GuildGUI] withClose-动作异常: " + e.getMessage());
                    e.printStackTrace();
                    // 动作执行失败时通知玩家
                    try {
                        player.sendMessage(MessageUtil.format("&c操作执行出错: " + e.getMessage()));
                    } catch (Exception ignored) {}
                }
            }, 1L);
        }, 1L);
    }

    private void showHelp(Player player) {
        player.sendMessage(MessageUtil.format("&6&l====== 香草家族 · 帮助 ======"));
        player.sendMessage(MessageUtil.format("&e/guild        &7- 打开功能面板 (GUI)"));
        player.sendMessage(MessageUtil.format("&e/guild help   &7- 显示命令列表"));
        player.sendMessage(MessageUtil.format("&e!消息         &7- 快捷发送公会消息 (任何频道)"));
        player.sendMessage(MessageUtil.format("&e/guild chat   &7- 切换公会聊天模式"));
        player.sendMessage(MessageUtil.format("&7"));
        player.sendMessage(MessageUtil.format("&6公会聊天技巧:"));
        player.sendMessage(MessageUtil.format("&7• 输入 &e/guild chat &7进入公会专属聊天"));
        player.sendMessage(MessageUtil.format("&7• 在公共频道直接打 &e!大家好 &7即可发送到公会"));
        player.sendMessage(MessageUtil.format("&7• 再输 &e/guild chat &7退出, 回到公共聊天"));
        player.sendMessage(MessageUtil.format("&7"));
        player.sendMessage(MessageUtil.format("&6贡献获取:"));
        player.sendMessage(MessageUtil.format("&7• 打怪自动获得 &e" + plugin.getConfigManager().getMobKillContribution() + " 贡献"));
        player.sendMessage(MessageUtil.format("&7• 每日签到获得 &e" + plugin.getConfigManager().getDailySignContribution() + " 贡献"));
        player.sendMessage(MessageUtil.format("&7• 捐献金币按比例转换为贡献"));
        player.sendMessage(MessageUtil.format("&7"));
        player.sendMessage(MessageUtil.format("&6BUFF说明:"));
        player.sendMessage(MessageUtil.format("&7• 随公会等级自动解锁, 无需手动操作"));
        player.sendMessage(MessageUtil.format("&7• " + plugin.getConfigManager().getBuffMaxLevel() + "级满BUFF, 最高 &c+" + plugin.getConfigManager().getBuffMaxEffect() + "%"));
        player.sendMessage(MessageUtil.format("&7"));
        player.sendMessage(MessageUtil.format("&6管理命令: &e/guildadmin &7(OP专用)"));
    }

    private ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtil.format(name));
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(MessageUtil.format(line));
        }
        meta.lore(loreList);
        // 打上 PDC 标签 — 即使 InventoryHolder 失效也能识别
        meta.getPersistentDataContainer().set(GUI_KEY, PersistentDataType.STRING, "1");
        item.setItemMeta(meta);
        return item;
    }

}
