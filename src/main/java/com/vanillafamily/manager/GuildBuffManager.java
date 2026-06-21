package com.vanillafamily.manager;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.ConfigManager;
import com.vanillafamily.model.Guild;
import com.vanillafamily.model.GuildMember;
import com.vanillafamily.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * BUFF 随公会等级自动解锁。1~20级逐步增长到50%，无需手动激活。
 */
public class GuildBuffManager {

    private final VanillaFamily plugin;
    private final ConfigManager config;

    public static final String EXP_BOOST = "EXP_BOOST";
    public static final String DAMAGE_BOOST = "DAMAGE_BOOST";
    public static final String SPEED = "SPEED";
    public static final String DAMAGE_REDUCTION = "DAMAGE_REDUCTION";

    public GuildBuffManager(VanillaFamily plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /** 获取某BUFF当前等级的百分比加成 */
    public double getBuffEffect(int guildLevel, String buffType) {
        return config.getBuffEffect(guildLevel, buffType);
    }

    /** 给玩家应用所有已解锁的 BUFF */
    public void applyBuffsToPlayer(Player player) {
        GuildManager gm = plugin.getGuildManager();
        GuildMember member = gm.getMember(player.getUniqueId().toString());
        if (member == null) return;

        Guild guild = gm.getGuildByMember(player.getUniqueId().toString());
        if (guild == null) return;

        int level = guild.getLevel();
        for (String type : new String[]{EXP_BOOST, DAMAGE_BOOST, SPEED, DAMAGE_REDUCTION}) {
            if (!config.isBuffEnabled(type)) continue;
            double effect = getBuffEffect(level, type);
            if (effect <= 0) continue;
            applyBuffEffect(player, type, effect);
        }
    }

    private void applyBuffEffect(Player player, String type, double percent) {
        switch (type) {
            case SPEED -> {
                int amplifier = (int) (percent / 10) - 1;
                if (amplifier >= 0) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SPEED,
                            PotionEffect.INFINITE_DURATION,
                            Math.min(amplifier, 4), false, false, false));
                }
            }
            // EXP_BOOST, DAMAGE_BOOST, DAMAGE_REDUCTION 在事件监听中计算
        }
    }

    /** 给公会所有在线成员应用 BUFF */
    public void applyBuffsToGuildMembers(int guildId) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            GuildManager gm = plugin.getGuildManager();
            GuildMember member = gm.getMember(p.getUniqueId().toString());
            if (member != null && member.getGuildId() == guildId) {
                applyBuffsToPlayer(p);
            }
        }
    }

    public void clearAllBuffs() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    /** 生成 BUFF 状态展示文本 */
    public String getBuffInfo(int guildId) {
        Guild guild = plugin.getDatabaseManager().getGuildDao().findById(guildId);
        if (guild == null) return "";
        int level = guild.getLevel();
        int maxLevel = config.getBuffMaxLevel();
        double maxEffect = config.getBuffMaxEffect();

        StringBuilder sb = new StringBuilder("&6&l=== 公会BUFF &7(&fLv.")
                .append(level).append("&7/&fLv.").append(maxLevel).append(" 满级&7) ===\n");
        for (String type : new String[]{EXP_BOOST, DAMAGE_BOOST, SPEED, DAMAGE_REDUCTION}) {
            if (!config.isBuffEnabled(type)) continue;
            String name = config.getBuffName(type);
            double effect = config.getBuffEffect(level, type);
            sb.append("&7").append(name).append(" &7→ &a")
                    .append(String.format("%.1f", effect)).append("%")
                    .append("&7/&f").append(String.format("%.0f", maxEffect)).append("%\n");
        }
        return sb.toString();
    }

}
