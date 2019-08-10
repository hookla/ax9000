package com.ax9k.positionmanager;

public interface MarketDataProviderCallbackReceiver {
    void updateBookValues(double bid0, double ask0);
}
