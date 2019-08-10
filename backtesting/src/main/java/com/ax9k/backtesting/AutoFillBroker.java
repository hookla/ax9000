package com.ax9k.backtesting;

import com.ax9k.broker.Broker;
import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.broker.OrderRecord;
import com.ax9k.broker.OrderRequest;
import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.time.Time;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static com.ax9k.core.marketmodel.BidAsk.BID;
import static org.apache.commons.lang3.Validate.notNull;

public class AutoFillBroker implements Broker, Observer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Contract contract;
    private final int slippage;
    private final BrokerCallbackReceiver receiver;
    private final List<Runnable> pendingCallbacks = new ArrayList<>();
    private double position;

    private int nextOrderId = 1;
    private boolean initialPositionSet;

    public AutoFillBroker(Contract contract,
                          BrokerCallbackReceiver receiver,
                          int slippage) {
        this.contract = notNull(contract);
        this.receiver = notNull(receiver);
        this.slippage = slippage;
    }

    @Override
    public OrderRecord place(OrderRequest order) {
        return order.getSide() == BID ? buy(order) : sell(order);
    }

    private OrderRecord buy(OrderRequest order) {
        setInitialPosition();

        int orderId = nextOrderId++;
        double price = order.getPrice() + slippage;
        double quantity = order.getQuantity();
        position += quantity;

        double newPosition = position;
        pendingCallbacks.add(() -> {
            LOGGER.info("Auto-filling buy ... ID: {}", orderId);
            receiver.positionUpdate(newPosition, price * contract.getMultiplier());
            receiver.orderFilled(Time.now(), orderId, price, quantity);
        });
        return new OrderRecord(Time.now(), orderId);
    }

    private void setInitialPosition() {
        if (!initialPositionSet) {
            pendingCallbacks.add(() -> receiver.positionUpdate(0d, 0d));
            initialPositionSet = true;
        }
    }

    private OrderRecord sell(OrderRequest order) {
        setInitialPosition();

        int orderId = nextOrderId++;

        final double price = order.getPrice() - slippage;
        double quantity = order.getQuantity();
        position -= quantity;

        double newPosition = position;
        pendingCallbacks.add(() -> {
            LOGGER.info("Auto-filling sell ... ID: {}", orderId);
            receiver.positionUpdate(newPosition, price * contract.getMultiplier());
            receiver.orderFilled(Time.now(), orderId, price, quantity);
        });
        return new OrderRecord(Time.now(), orderId);
    }

    public int getNextOrderId() {
        return nextOrderId;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void connect() {}

    @Override
    public void disconnect() {}

    @Override
    public void requestData() {}

    @Override
    public Contract getContract() {
        return contract;
    }

    @Override
    public void cancelAllPendingOrders() {}

    @Override
    public void update(Observable o, Object arg) {
        pendingCallbacks.forEach(Runnable::run);
        pendingCallbacks.clear();
    }
}
