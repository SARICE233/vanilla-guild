package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuildBankManager {

    private final VanillaFamily plugin;
    // 打开的仓库映射: inventory -> guildId
    private final Map<UUID, Integer> openBanks = new HashMap<>();

    public GuildBankManager(VanillaFamily plugin) {
        this.plugin = plugin;
    }

    public void openBank(Player player) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) {
            player.sendMessage(MessageUtil.format(
                    plugin.getConfigManager().getMessage("not-in-guild")));
            return;
        }

        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) return;

        int rows = plugin.getConfigManager().getLevelBankRows(guild.getLevel());
        int size = rows * 9;
        // 至少1行
        if (size < 9) size = 9;

        Component title = MessageUtil.format("&8" + guild.getName() + " 公会仓库");
        Inventory inv = Bukkit.createInventory(null, size, title);

        // 加载已有物品
        loadItems(guild.getId(), inv);

        openBanks.put(player.getUniqueId(), guild.getId());
        player.openInventory(inv);
    }

    public void saveInventory(int guildId, Inventory inventory) {
        // 先清空该公会仓库
        String deleteSql = "DELETE FROM guild_bank_items WHERE guild_id = ?";
        String insertSql = "INSERT OR REPLACE INTO guild_bank_items (guild_id, slot, item_base64) VALUES (?, ?, ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setInt(1, guildId);
            deleteStmt.executeUpdate();

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack item = inventory.getItem(slot);
                    if (item != null && !item.getType().isAir()) {
                        insertStmt.setInt(1, guildId);
                        insertStmt.setInt(2, slot);
                        insertStmt.setString(3, serializeItem(item));
                        insertStmt.addBatch();
                    }
                }
                insertStmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("保存公会仓库失败: " + e.getMessage());
        }
    }

    public void loadItems(int guildId, Inventory inventory) {
        String sql = "SELECT slot, item_base64 FROM guild_bank_items WHERE guild_id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int slot = rs.getInt("slot");
                    String base64 = rs.getString("item_base64");
                    ItemStack item = deserializeItem(base64);
                    if (item != null && slot < inventory.getSize()) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("加载公会仓库失败: " + e.getMessage());
        }
    }

    private String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos);
            oos.writeObject(item);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("序列化物品失败: " + e.getMessage());
            return "";
        }
    }

    private ItemStack deserializeItem(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            BukkitObjectInputStream ois = new BukkitObjectInputStream(
                    new ByteArrayInputStream(data));
            return (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("反序列化物品失败: " + e.getMessage());
            return null;
        }
    }

    public Integer getGuildIdForPlayer(Player player) {
        return openBanks.get(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        openBanks.remove(player.getUniqueId());
    }
}
