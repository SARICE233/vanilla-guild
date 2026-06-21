package com.vanillafamily.model;

import java.sql.Timestamp;

public class GuildMember {

    private int id;
    private int guildId;
    private String playerUuid;
    private String rank;        // rank key, e.g. "leader", "officer", "member" — defined in config.yml
    private double contribution;
    private Timestamp joinedAt;

    // 非持久化字段
    private String playerName;

    public GuildMember() {}

    public GuildMember(int guildId, String playerUuid, String rank) {
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.rank = rank;
        this.contribution = 0;
    }

    // === Getters & Setters ===
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGuildId() { return guildId; }
    public void setGuildId(int guildId) { this.guildId = guildId; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }

    public double getContribution() { return contribution; }
    public void setContribution(double contribution) { this.contribution = contribution; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}
