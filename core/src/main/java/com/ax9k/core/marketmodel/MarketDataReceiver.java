package com.ax9k.core.marketmodel;

import com.ax9k.core.event.Event;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.marketmodel.orderbook.OrderBook;

public interface MarketDataReceiver {
    void trade(Trade trade);

    void trade(Trade trade, OrderBook book);

    void orderBook(OrderBook book);

    void bar(OhlcvBar bar);

    void extra(Event data);
}
