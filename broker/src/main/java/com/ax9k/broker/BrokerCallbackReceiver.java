package com.ax9k.broker;

import java.time.Instant;

public interface BrokerCallbackReceiver {
    void orderCancelled(int orderId);

    void orderFilled(Instant fillTimestamp, int orderId, double avgFillPrice, double quantity);

    void pnlUpdate(double unrealised, double realised, double daily);

    void positionUpdate(double position, double averageCost);
}
