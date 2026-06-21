package com.vanillafamily;

import com.vanillafamily.database.DatabaseManager;
import com.vanillafamily.manager.*;
import com.vanillafamily.command.GuildCommand;
import com.vanillafamily.command.GuildAdminCommand;
import com.vanillafamily.listener.GuildListener;
import com.vanillafamily.util.VaultEconomyHelper;
import org.bukkit.plugin.java.JavaPlugin;

public class VanillaFamily extends JavaPlugin {

    private static VanillaFamily instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GuildManager guildManager;
    private GuildChatManager guildChatManager;
    private GuildBankManager guildBankManager;
    private GuildBuffManager guildBuffManager;
    private ContributionManager contributionManager;
    private GuildGuiManager guildGuiManager;
    private PromptManager promptManager;
    private GuildTaskManager guildTaskManager;
    private DailyQuestManager dailyQuestManager;
    private AdvancementManager advancementManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 加载配置
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // 2. 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        // 3. 初始化管理器
        guildManager = new GuildManager(this);
        guildChatManager = new GuildChatManager(this);
        guildBankManager = new GuildBankManager(this);
        guildBuffManager = new GuildBuffManager(this);
        contributionManager = new ContributionManager(this);
        guildGuiManager = new GuildGuiManager(this);
        promptManager = new PromptManager();
        promptManager.setPlugin(this);
        guildTaskManager = new GuildTaskManager(this);
        dailyQuestManager = new DailyQuestManager(this);
        advancementManager = new AdvancementManager(this);

        // 4. 注册命令
        getCommand("guild").setExecutor(new GuildCommand(this));
        getCommand("guildadmin").setExecutor(new GuildAdminCommand(this));

        // 5. 注册监听器
        getServer().getPluginManager().registerEvents(new GuildListener(this), this);

        // 6. 延迟检测 Vault 经济 (等其他插件注册完毕)
        getServer().getScheduler().runTaskLater(this, () -> {
            boolean vaultOk = VaultEconomyHelper.isAvailable();
            getLogger().info("经济系统: " + (vaultOk ? "Vault ✓" : "Vault 未检测到, 将跳过扣费"));
        }, 2L);

        getLogger().info("香草家族插件已启用 v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (guildManager != null) guildManager.flushAll();
        if (guildBuffManager != null) {
            guildBuffManager.clearAllBuffs();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("香草家族插件已卸载");
    }

    public static VanillaFamily getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public GuildChatManager getGuildChatManager() {
        return guildChatManager;
    }

    public GuildBankManager getGuildBankManager() {
        return guildBankManager;
    }

    public GuildBuffManager getGuildBuffManager() {
        return guildBuffManager;
    }

    public ContributionManager getContributionManager() {
        return contributionManager;
    }

    public GuildGuiManager getGuildGuiManager() {
        return guildGuiManager;
    }

    public PromptManager getPromptManager() {
        return promptManager;
    }

    public GuildTaskManager getGuildTaskManager() {
        return guildTaskManager;
    }

    public DailyQuestManager getDailyQuestManager() {
        return dailyQuestManager;
    }

    public AdvancementManager getAdvancementManager() {
        return advancementManager;
    }
}
