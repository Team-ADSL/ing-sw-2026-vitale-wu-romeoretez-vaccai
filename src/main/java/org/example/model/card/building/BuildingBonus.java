package org.example.model.card.building;

public class BuildingBonus {
    private int extraStars;
    private int shamanMulitiplierPP;
    private int sustenanceDiscount;
    private int hunterPP;
    private int hunterFood;
    private boolean extraMove;
    private boolean noRitualLostPP;
    private boolean artistFood;

    public BuildingBonus(int extraStars, int shamanMulitiplierPP, int sustenanceDiscount,
                         int hunterPP, int hunterFood, boolean extraMove, boolean noRitualLostPP,
                         boolean artistFood) {
        this.extraStars = extraStars;
        this.shamanMulitiplierPP = shamanMulitiplierPP;
        this.sustenanceDiscount = sustenanceDiscount;
        this.hunterPP = hunterPP;
        this.hunterFood = hunterFood;
        this.extraMove = extraMove;
        this.noRitualLostPP = noRitualLostPP;
        this.artistFood = artistFood;
    }

    public void reset(){
        extraStars = 0;
        shamanMulitiplierPP = 1;
        sustenanceDiscount = 0;
        extraMove = false;
        noRitualLostPP = false;
        artistFood = false;
    }

    public void setSustenanceDiscount(int sustenanceDiscount) {
        this.sustenanceDiscount = sustenanceDiscount;
    }

    public void setNoRitualLostPP(boolean noRitualLostPP) {
        this.noRitualLostPP = noRitualLostPP;
    }

    public int getExtraStars() {
        return extraStars;
    }

    public int getShamanMulitiplierPP() {
        return shamanMulitiplierPP;
    }

    public int getSustenanceDiscount() {
        return sustenanceDiscount;
    }

    public int getHunterPP() {
        return hunterPP;
    }

    public int getHunterFood() {
        return hunterFood;
    }

    public boolean isExtraMove() {
        return extraMove;
    }

    public boolean isNoRitualLostPP() {
        return noRitualLostPP;
    }

    public boolean isArtistFood() {
        return artistFood;
    }

    public void setExtraStars(int extraStars) {
        this.extraStars = extraStars;
    }

    public void setShamanMulitiplierPP(int shamanMulitiplierPP) {
        this.shamanMulitiplierPP = shamanMulitiplierPP;
    }
}
