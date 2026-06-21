package com.vanillafamily.database.dao;

import com.vanillafamily.database.DatabaseManager;
import com.vanillafamily.model.GuildMember;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuildMemberDao {

    private final DatabaseManager db;

    public GuildMemberDao(DatabaseManager db) {
        this.db = db;
    }

    public GuildMember add(GuildMember member) {
        String sql = "INSERT INTO guild_members (guild_id, player_uuid, player_name, rank, contribution) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, member.getGuildId());
            ps.setString(2, member.getPlayerUuid());
            ps.setString(3, member.getPlayerName() != null ? member.getPlayerName() : "");
            ps.setString(4, member.getRank());
            ps.setDouble(5, member.getContribution());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    member.setId(rs.getInt(1));
                    return member;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public GuildMember findByUuid(String playerUuid) {
        String sql = "SELECT * FROM guild_members WHERE player_uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<GuildMember> findByGuildId(int guildId) {
        List<GuildMember> list = new ArrayList<>();
        String sql = "SELECT * FROM guild_members WHERE guild_id = ? ORDER BY joined_at ASC";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countByGuildId(int guildId) {
        String sql = "SELECT COUNT(*) FROM guild_members WHERE guild_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void update(GuildMember member) {
        String sql = "UPDATE guild_members SET rank=?, contribution=?, player_name=? WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, member.getRank());
            ps.setDouble(2, member.getContribution());
            ps.setString(3, member.getPlayerName());
            ps.setInt(4, member.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addContribution(String playerUuid, double amount) {
        String sql = "UPDATE guild_members SET contribution = contribution + ? WHERE player_uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, playerUuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void remove(int id) {
        String sql = "DELETE FROM guild_members WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeByUuid(String playerUuid) {
        String sql = "DELETE FROM guild_members WHERE player_uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<GuildMember> findTopContributors(int guildId, int limit) {
        List<GuildMember> list = new ArrayList<>();
        String sql = "SELECT * FROM guild_members WHERE guild_id = ? ORDER BY contribution DESC LIMIT ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, guildId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<GuildMember> findTopContributorsGlobal(int limit) {
        List<GuildMember> list = new ArrayList<>();
        String sql = "SELECT * FROM guild_members ORDER BY contribution DESC LIMIT ?";
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

    private GuildMember mapRow(ResultSet rs) throws SQLException {
        GuildMember m = new GuildMember();
        m.setId(rs.getInt("id"));
        m.setGuildId(rs.getInt("guild_id"));
        m.setPlayerUuid(rs.getString("player_uuid"));
        m.setPlayerName(rs.getString("player_name"));
        m.setRank(rs.getString("rank"));
        m.setContribution(rs.getDouble("contribution"));
        m.setJoinedAt(rs.getTimestamp("joined_at"));
        return m;
    }
}
