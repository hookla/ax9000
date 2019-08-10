package com.ax9k.positionmanager;

import com.ax9k.broker.Broker;
import com.ax9k.core.event.Event;

public interface OrderReceiver {

    int buy(String source, Event triggeringEvent, double quantity, double stopPrice);

    int sell(String source, Event triggeringEvent, double quantity, double stopPrice);

    void cancelAllPendingOrders(String source);

    void exitPosition(String source);

    void initialiseBroker(Broker broker);

    //  boolean isExitingPosition();

    String getLastRiskManagerRejectReason();

    String getContractLocalSymbol();

    String getContractExchange();

    double getCostPerTrade();

    int getContractMultiplier();
}
