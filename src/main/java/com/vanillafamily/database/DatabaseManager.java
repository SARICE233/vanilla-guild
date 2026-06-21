package com.vanillafamily.database;

import com.vanillafamily.VanillaFamily;
import com.vanillafamily.database.dao.GuildDao;
import com.vanillafamily.database.dao.GuildMemberDao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final VanillaFamily plugin;
    private Connection connection;
    private GuildDao guildDao;
    private GuildMemberDao guildMemberDao;

    public DatabaseManager(VanillaFamily plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();

            String dbFile = plugin.getConfigManager().getDatabaseFile();
            String url = "jdbc:sqlite:" + new File(dataFolder, dbFile).getAbsolutePath();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            // 启用 WAL 模式提升并发性能
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();

            plugin.getLogger().info("数据库连接成功: " + dbFile);
        } catch (Exception e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 公会表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS guilds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    tag TEXT,
                    description TEXT DEFAULT '',
                    leader_uuid TEXT NOT NULL,
                    level INTEGER DEFAULT 1,
                    experience INTEGER DEFAULT 0,
                    bank_rows INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 成员表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS guild_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    guild_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT DEFAULT '',
                    rank TEXT DEFAULT 'MEMBER',
                    contribution REAL DEFAULT 0,
                    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                    UNIQUE(player_uuid)
                )
            """);

            // 任务表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS guild_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    target INTEGER NOT NULL,
                    reward INTEGER NOT NULL DEFAULT 0,
                    completed INTEGER DEFAULT 0,
                    winner_guild_id INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS guild_task_progress (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    guild_id INTEGER NOT NULL,
                    progress INTEGER DEFAULT 0,
                    FOREIGN KEY (task_id) REFERENCES guild_tasks(id) ON DELETE CASCADE,
                    UNIQUE(task_id, guild_id)
                )
            """);

            // 仓库表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS guild_bank_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    guild_id INTEGER NOT NULL,
                    slot INTEGER NOT NULL,
                    item_base64 TEXT,
                    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                    UNIQUE(guild_id, slot)
                )
            """);

            // 启用外键
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                String dbFile = plugin.getConfigManager().getDatabaseFile();
                String url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), dbFile).getAbsolutePath();
                connection = DriverManager.getConnection(url);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("无法获取数据库连接: " + e.getMessage());
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("关闭数据库连接失败: " + e.getMessage());
        }
    }

    public GuildDao getGuildDao() {
        if (guildDao == null) guildDao = new GuildDao(this);
        return guildDao;
    }

    public GuildMemberDao getGuildMemberDao() {
        if (guildMemberDao == null) guildMemberDao = new GuildMemberDao(this);
        return guildMemberDao;
    }
}
