package com.vanillafamily.database.dao;

import com.vanillafamily.database.DatabaseManager;
import com.vanillafamily.model.Guild;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuildDao {

    private final DatabaseManager db;

    public GuildDao(DatabaseManager db) {
        this.db = db;
    }

    public Guild create(Guild guild) {
        String sql = "INSERT INTO guilds (name, tag, description, leader_uuid, level, experience, bank_rows) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, guild.getName());
            ps.setString(2, guild.getTag());
            ps.setString(3, guild.getDescription() != null ? guild.getDescription() : "");
            ps.setString(4, guild.getLeaderUuid());
            ps.setInt(5, guild.getLevel());
            ps.setInt(6, guild.getExperience());
            ps.setInt(7, guild.getBankRows());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    guild.setId(rs.getInt(1));
                    return guild;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Guild findById(int id) {
        String sql = "SELECT * FROM guilds WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Guild findByName(String name) {
        String sql = "SELECT * FROM guilds WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Guild> findAll() {
        List<Guild> list = new ArrayList<>();
        String sql = "SELECT * FROM guilds ORDER BY level DESC, experience DESC";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void update(Guild guild) {
        String sql = "UPDATE guilds SET name=?, tag=?, description=?, leader_uuid=?, level=?, experience=?, bank_rows=? WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, guild.getName());
            ps.setString(2, guild.getTag());
            ps.setString(3, guild.getDescription());
            ps.setString(4, guild.getLeaderUuid());
            ps.setInt(5, guild.getLevel());
            ps.setInt(6, guild.getExperience());
            ps.setInt(7, guild.getBankRows());
            ps.setInt(8, guild.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM guilds WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Guild> findTop(int limit) {
        List<Guild> list = new ArrayList<>();
        String sql = "SELECT * FROM guilds ORDER BY level DESC, experience DESC LIMIT ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM guilds";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Guild mapRow(ResultSet rs) throws SQLException {
        Guild g = new Guild();
        g.setId(rs.getInt("id"));
        g.setName(rs.getString("name"));
        g.setTag(rs.getString("tag"));
        g.setDescription(rs.getString("description"));
        g.setLeaderUuid(rs.getString("leader_uuid"));
        g.setLevel(rs.getInt("level"));
        g.setExperience(rs.getInt("experience"));
        g.setBankRows(rs.getInt("bank_rows"));
        g.setCreatedAt(rs.getTimestamp("created_at"));
        return g;
    }
}
