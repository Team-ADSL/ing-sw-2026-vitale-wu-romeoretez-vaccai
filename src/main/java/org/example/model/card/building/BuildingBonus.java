package org.example.model.card.building;

public class BuildingBonus {
    private int extraStars;
    private int shamanMulitiplierPP;
    private int sustenanceDiscount;
    private boolean huntEventBonus;
    private boolean extraMove;
    private boolean noRitualLostPP;
    private boolean artistFood;

    public BuildingBonus(int extraStars, int shamanMulitiplierPP, int sustenanceDiscount,
                         boolean huntEventBonus, boolean extraMove, boolean noRitualLostPP,
                         boolean artistFood) {
        this.extraStars = extraStars;
        this.shamanMulitiplierPP = shamanMulitiplierPP;
        this.sustenanceDiscount = sustenanceDiscount;
        this.huntEventBonus = huntEventBonus;
        this.extraMove = extraMove;
        this.noRitualLostPP = noRitualLostPP;
        this.artistFood = artistFood;
    }

    public void reset(){
        extraStars = 0;
        shamanMulitiplierPP = 1;
        sustenanceDiscount = 0;
        huntEventBonus = false;
        extraMove = false;
        noRitualLostPP = false;
        artistFood = false;
    }

    public void setHuntEventBonus(boolean huntEventBonus) {
        this.huntEventBonus = huntEventBonus;
    }

    public void setExtraMove(boolean extraMove) {
        this.extraMove = extraMove;
    }

    public void setArtistFood(boolean artistFood) {
        this.artistFood = artistFood;
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

    public boolean isHuntEventBonus() {
        return huntEventBonus;
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
