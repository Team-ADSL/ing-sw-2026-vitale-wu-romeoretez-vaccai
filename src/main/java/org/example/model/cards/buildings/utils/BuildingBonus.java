package org.example.model.cards.buildings.utils;

public class BuildingBonus {
    private int extraStars;
    private int shamanMulitiplierPP;
    private int builderMultiplierPP;
    private int sustenanceDiscount;
    private boolean huntEventBonus;
    private boolean extraMove;
    private boolean noRitualLostPP;
    private boolean artistFood;
    private boolean bonusFoodTile;
    private boolean doubleRitualPP;

    public BuildingBonus(int extraStars, int shamanMulitiplierPP, int builderMultiplierPP, int sustenanceDiscount, boolean huntEventBonus, boolean extraMove, boolean noRitualLostPP, boolean artistFood, boolean bonusFoodTile) {
        this.extraStars = extraStars;
        this.shamanMulitiplierPP = shamanMulitiplierPP;
        this.builderMultiplierPP = builderMultiplierPP;
        this.sustenanceDiscount = sustenanceDiscount;
        this.huntEventBonus = huntEventBonus;
        this.extraMove = extraMove;
        this.noRitualLostPP = noRitualLostPP;
        this.artistFood = artistFood;
        this.bonusFoodTile = bonusFoodTile;
        this.doubleRitualPP = doubleRitualPP;
    }

    public void reset() {
        extraStars = 0;
        shamanMulitiplierPP = 1;
        builderMultiplierPP = 1;
        sustenanceDiscount = 0;
        huntEventBonus = false;
        extraMove = false;
        noRitualLostPP = false;
        artistFood = false;
        bonusFoodTile = false;
        doubleRitualPP = false;
    }

    public boolean isBonusFoodTile() {
        return bonusFoodTile;
    }
    public void setBonusFoodTile(boolean bonusFoodTile) {
        this.bonusFoodTile = bonusFoodTile;
    }

    public int getExtraStars() {
        return extraStars;
    }
    public void setExtraStars(int extraStars) {
        this.extraStars = extraStars;
    }

    public int getShamanMulitiplierPP() {
        return shamanMulitiplierPP;
    }
    public void setShamanMulitiplierPP(int shamanMulitiplierPP) {
        this.shamanMulitiplierPP = shamanMulitiplierPP;
    }

    public int getBuilderMultiplierPP() {
        return builderMultiplierPP;
    }
    public void setBuilderMultiplierPP(int builderMultiplierPP) {
        this.builderMultiplierPP = builderMultiplierPP;
    }

    public int getSustenanceDiscount() {
        return sustenanceDiscount;
    }
    public void setSustenanceDiscount(int sustenanceDiscount) {
        this.sustenanceDiscount += sustenanceDiscount;
    }

    public boolean isHuntEventBonus() {
        return huntEventBonus;
    }
    public void setHuntEventBonus(boolean huntEventBonus) {
        this.huntEventBonus = huntEventBonus;
    }

    public boolean isExtraMove() {
        return extraMove;
    }
    public void setExtraMove(boolean extraMove) {
        this.extraMove = extraMove;
    }

    public boolean isNoRitualLostPP() {
        return noRitualLostPP;
    }
    public void setNoRitualLostPP(boolean noRitualLostPP) {
        this.noRitualLostPP = noRitualLostPP;
    }

    public boolean isArtistFood() {
        return artistFood;
    }
    public void setArtistFood(boolean artistFood) {
        this.artistFood = artistFood;
    }

    public boolean isDoubleRitualPP() { return doubleRitualPP; }
    public void setDoubleRitualPP(boolean doubleRitualPP) { this.doubleRitualPP = doubleRitualPP; }
}
