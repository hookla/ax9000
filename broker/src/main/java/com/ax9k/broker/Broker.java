package com.ax9k.broker;

import com.ax9k.core.marketmodel.Contract;

public interface Broker {
    OrderRecord place(OrderRequest order);

    boolean isConnected();

    void connect();

    void disconnect();

    void requestData();

    Contract getContract();

    void cancelAllPendingOrders();
}

