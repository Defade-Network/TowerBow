package net.defade.towerbow.bonus;

public enum Bonus {
    EXPLOSIVE_ARROW(new ExplosiveArrowBonusBlock(), "Explosive Arrow"),
    SMOKE_ARROW(new SmokeArrowBonusBlock(), "Smoke Arrow"),
    WALL_ARROW(new WallArrowBonusBlock(), "Wall Arrow"),
    HEAL_BONUS(new HealBonusBlock(), "Heal Bonus"),
    STRIKE_BONUS(new StrikeBonusBlock(), "Strike Bonus");

    private final BonusBlock bonusBlock;
    private final String name;

    Bonus(BonusBlock bonusBlock, String name) {
        this.bonusBlock = bonusBlock;
        this.name = name;
    }

    public BonusBlock getBonusBlock() {
        return bonusBlock;
    }

    public String getName() {
        return name;
    }
}
