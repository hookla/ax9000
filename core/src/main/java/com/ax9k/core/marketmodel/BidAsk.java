package com.ax9k.core.marketmodel;

public enum BidAsk {
    BID(1), ASK(-1), NONE(-999);

    private final int multiplier;

    BidAsk(int multiplier) {
        this.multiplier = multiplier;
    }

    public static BidAsk fromCode(short code) {
        switch (code) {
            case 0:
                return BID;
            case 1:
                return ASK;
            case -999:
                return NONE;
            default:
                throw new IllegalArgumentException("unsupported side: " + code);
        }
    }

    public int getMultiplier() {
        return multiplier;
    }
}
