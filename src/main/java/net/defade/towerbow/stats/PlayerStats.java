package net.defade.towerbow.stats;

import org.bson.Document;

public class PlayerStats {
    private int kills = 0;
    private int longShots = 0;
    private float damageDealt = 0;
    private int deaths = 0;
    private int bonusBlocks = 0;

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public int getLongShots() {
        return longShots;
    }

    public void addLongShot() {
        longShots++;
    }

    public float getDamageDealt() {
        return damageDealt;
    }

    public void addDamageDealt(float damage) {
        damageDealt += damage;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public int getBonusBlocks() {
        return bonusBlocks;
    }

    public void addBonusBlock() {
        bonusBlocks++;
    }

    public Document getMongoDBDocument() {
        return new Document("kills", kills)
                .append("long_shots", longShots)
                .append("damage_dealt", damageDealt)
                .append("deaths", deaths)
                .append("bonus_blocks", bonusBlocks);
    }
}
