package com.ax9k.cex.broker;

import com.ax9k.broker.Broker;
import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.broker.OrderRecord;
import com.ax9k.broker.OrderRequest;
import com.ax9k.cex.client.CexClient;
import com.ax9k.cex.client.CexContract;
import com.ax9k.cex.client.CexRequest;
import com.ax9k.cex.client.CexResponse;
import com.ax9k.cex.client.ConcurrentHolder;
import com.ax9k.cex.client.MessageType;
import com.ax9k.cex.client.Pair;
import com.ax9k.cex.client.ResponseHandler;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.Contract;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ax9k.cex.client.JsonMapper.createObjectNode;
import static com.ax9k.utils.json.JsonUtils.toPrettyJsonString;
import static org.apache.commons.lang3.Validate.notNull;

public class CexBroker implements Broker {
    private static final Logger EVENT_LOGGER = LogManager.getLogger("brokerLogger");

    private static final MessageType GET_BALANCE = MessageType.of("get-balance");
    private static final MessageType PLACE_ORDER = MessageType.of("place-order");
    private static final MessageType TRANSACTION_CREATED = MessageType.of("tx");
    private static final MessageType CANCEL_ORDER = MessageType.of("cancel-order");
    private static final Duration MAX_WAITING_PERIOD = Duration.ofSeconds(20);

    private final CexClient client;
    private final CexContract contract;
    private final BrokerCallbackReceiver receiver;
    private final ConcurrentHolder<JsonNode> placementNotification = new ConcurrentHolder<>();
    private final Map<Integer, Double> openOrders = new ConcurrentHashMap<>();

    private double startingBalance = -999;
    private double position = 0;

    CexBroker(CexClient client, CexContract contract, BrokerCallbackReceiver receiver) {
        this.client = notNull(client, "client");
        this.contract = notNull(contract, "contract");
        this.receiver = notNull(receiver, "receiver");

        client.registerResponseHandler(GET_BALANCE, new ResponseHandler() {
            @Override
            public void ok(CexResponse response) {
                CexBroker.this.onBalanceNotification(response);
            }

            @Override
            public void error(CexResponse response) {
                /*Suppress message*/
            }
        });
        client.registerResponseHandler(TRANSACTION_CREATED, this::onFillNotification);
        client.registerResponseHandler(PLACE_ORDER, response -> placementNotification.hold(response.getData()));

        if (client.isConnected()) {
            requestData();
        }
    }

    private void onBalanceNotification(CexResponse response) {
        JsonNode data = response.getData();
        Pair pair = contract.getCurrencies();
        JsonNode balances = data.get("balance");
        EVENT_LOGGER.info(data);

        double relevantBalance = balances.get(pair.getSymbol1()).asDouble();
        if (startingBalance == -999) {
            startingBalance = relevantBalance;
            return;
        }

        double pnl = relevantBalance - startingBalance;
        receiver.pnlUpdate(-999, pnl, pnl);
    }

    private void onFillNotification(CexResponse response) {
        JsonNode data = response.getData();
        EVENT_LOGGER.info(data);

        int id = data.has("id") ? data.get("id").asInt() : data.get("_id").asInt();
        double quantity = openOrders.remove(id);

        Instant timestamp = Instant.ofEpochMilli(data.get("time").asLong());
        BidAsk side = data.get("type").asText().equals("sell") ? BidAsk.ASK : BidAsk.BID;
        double price = data.get("price").asDouble();

        if (side == BidAsk.ASK) {
            position -= quantity;
        } else {
            position += quantity;
        }

        receiver.positionUpdate(position, -999);
        receiver.orderFilled(timestamp, id, price, quantity);
    }

    @Override
    public void requestData() {
        client.makeRequest(new CexRequest(GET_BALANCE, null, true));
    }

    @Override
    public OrderRecord place(OrderRequest order) {
        client.makeRequest(toCexRequest(order));

        JsonNode data = awaitData(placementNotification, "order placement");
        EVENT_LOGGER.info(data);
        validateNotError(data, "place order");

        int id = data.get("id").asInt();
        openOrders.put(id, order.getQuantity());
        return new OrderRecord(Instant.ofEpochMilli(data.get("time").asLong()), id);
    }

    private static void validateNotError(JsonNode data, String operation) {
        if (data.has("error")) {
            throw new IllegalStateException("could not " + operation + ". Reason: " + data.get("error").asText());
        }
    }

    private CexRequest toCexRequest(OrderRequest order) {
        JsonNode data = createObjectNode()
                .put("amount", order.getQuantity())
                .put("price", String.valueOf(order.getPrice()))
                .put("type", order.getSide() == BidAsk.BID ? "buy" : "sell")
                .set("pair", contract.getCurrencies().toJsonArray());
        return new CexRequest(PLACE_ORDER, data, true);
    }

    private JsonNode awaitData(ConcurrentHolder<JsonNode> holder, String waitReason) {
        try {
            return holder.await(MAX_WAITING_PERIOD)
                         .orElseThrow(() -> new IllegalStateException("Waiting time for " + waitReason +
                                                                      " exceeded " + MAX_WAITING_PERIOD.toString()));
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting for " + waitReason + " interrupted", e);
        }
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void connect() {
        client.connect();
        requestData();
    }

    @Override
    public void disconnect() {
        client.disconnect();
    }

    @Override
    public Contract getContract() {
        return contract;
    }

    @Override
    public void cancelAllPendingOrders() {
        for (Integer id : openOrders.keySet()) {
            CexRequest cancellationRequest = new CexRequest(CANCEL_ORDER,
                                                            createObjectNode().put("order_id", id),
                                                            true);
            client.makeRequest(cancellationRequest);
        }
    }

    @Override
    public String toString() {
        return toPrettyJsonString(this);
    }
}
