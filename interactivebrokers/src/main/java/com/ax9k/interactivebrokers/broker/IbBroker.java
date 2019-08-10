package com.ax9k.interactivebrokers.broker;

import com.ax9k.broker.Broker;
import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.broker.OrderRecord;
import com.ax9k.broker.OrderRequest;
import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.time.Time;
import com.ax9k.interactivebrokers.client.IbClient;
import com.ax9k.utils.json.JsonUtils;
import com.ib.client.ContractDetails;
import com.ib.client.Order;
import com.ib.client.OrderState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.ax9k.core.marketmodel.BidAsk.BID;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNullElse;

@SuppressWarnings("unchecked")
public class IbBroker extends IbClient implements Broker {
    private static final Logger EVENT_LOGGER = LogManager.getLogger("brokerLogger");

    private static final String CANCELLED = "Cancelled";
    private static final String FILLED = "Filled";
    private static final String IB_ACCOUNT = "xxxx";  //TODO this needs to goto the command line arguments
    private static final String ACCOUNT_SUMMARY_TAGS = "TotalCashValue, MaintMarginReq, InitMarginReq, AvailableFunds";
    private static final int ACCOUNT_SUMMARY_REQUEST = 300;
    private static final int PNL_SINGLE_REQUEST = 400;
    private static final int CONTRACT_DETAILS_REQUEST = 500;
    private static final int UNINITIALISED = -999;
    private static final int HALF_A_SECOND = 500;
    private static final int TEN_SECONDS = 20;

    private final Set<Integer> pendingOrders = new HashSet<>();
    private final BrokerCallbackReceiver receiver;

    private int lastFillId;
    private double lastFillRemaining;
    private int nextOrderId = UNINITIALISED;

    IbBroker(BrokerCallbackReceiver receiver,
             Contract contractDetails,
             String gatewayHost,
             int gatewayPort,
             String exchange,
             int contractId,
             int clientId) {
        super(gatewayHost, gatewayPort, clientId * 10 + 2, exchange, contractId);
        this.receiver = receiver;
        this.contract = contractDetails;
    }

    @Override
    public OrderRecord place(OrderRequest request) {
        String action = request.getSide() == BID ? "BUY" : "SELL";

        Order order = new Order();
        order.action(action);
        order.orderType("MKT");
        order.totalQuantity(request.getQuantity());
        order.outsideRth(true);
        order.tif("GTC");

        int orderId = nextOrderId++;
        Instant placedTimestamp = Time.now();
        client.placeOrder(orderId, requestContract, order);
        LOGGER.info("Sent {} Market Order for {} to IB. ID: {}", action, request.getQuantity(), orderId);
        pendingOrders.add(orderId);
        return new OrderRecord(placedTimestamp, orderId);
    }

    public int getNextOrderId() {
        return nextOrderId;
    }

    @Override
    public Contract getContract() {
        if (contract == null) {
            requestContractDetails();
            try {
                int waitCount = 0;
                while (contract == null) {
                    if (++waitCount == TEN_SECONDS) {
                        ERROR_LOG.error("Waiting for contract details from IB ...");
                        waitCount = 0;
                    } else {
                        LOGGER.warn("Waiting for contract details from IB ...");
                    }
                    Thread.sleep(HALF_A_SECOND);
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("Error waiting for client to connect", e);
            }
        }
        return contract;
    }

    private void requestContractDetails() {
        client.reqContractDetails(CONTRACT_DETAILS_REQUEST, requestContract);
    }

    @Override
    public void cancelAllPendingOrders() {
        pendingOrders.forEach(client::cancelOrder);
        pendingOrders.clear();
    }

    @Override
    public void connect() {
        if (!client.isConnected()) {
            super.connect();
            client.reqGlobalCancel();
        }
        makeDataRequests(true);
    }

    @Override
    public void disconnect() {
        if (client.isConnected()) {
            client.reqGlobalCancel();
            client.cancelPositions();
            client.cancelAccountSummary(ACCOUNT_SUMMARY_REQUEST);
            client.cancelPnLSingle(PNL_SINGLE_REQUEST);
            client.reqAccountUpdates(false, IB_ACCOUNT);
        }
        super.disconnect();
    }

    @Override
    public void requestData() {
        //TODO not all these requests are actually doing anything....
        requestContractDetails();
        client.reqPositions();
        client.reqAccountUpdates(true, IB_ACCOUNT);
        client.reqAccountSummary(ACCOUNT_SUMMARY_REQUEST, "All", "$LEDGER:HKD");
        client.reqAccountSummary(ACCOUNT_SUMMARY_REQUEST + 1, "All", ACCOUNT_SUMMARY_TAGS);
        client.reqPnLSingle(PNL_SINGLE_REQUEST, IB_ACCOUNT, "", requestContract.conid());
    }

    @Override
    public void orderStatus(int orderId, String status, double filled,
                            double remaining, double avgFillPrice, int permId, int parentId,
                            double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        LOGGER.info("OrderStatus. ID: {}, Status: {}, Filled: {}, Remaining: {}, AvgFillPrice: {}, " +
                    "PermID: {}, ParentID: {}, Last Fill Price: {}, ClientID: {}, WhyHeld: {}, MktCapPrice: {}",
                    orderId, status, filled, remaining, avgFillPrice,
                    permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
        logEvent("OrderStatus",
                 entry("orderId", orderId),
                 entry("averageFillPrice", avgFillPrice),
                 entry("filledQuantity", filled),
                 entry("status", status),
                 entry("remaining", remaining),
                 entry("permId", permId),
                 entry("parentId", parentId),
                 entry("lastFillPrice", lastFillPrice),
                 entry("clientId", clientId),
                 entry("whyHeld", requireNonNullElse(whyHeld, "UNKNOWN")),
                 entry("mktCapPrice", mktCapPrice));

        Time.update(Instant.now());

        if (status.equals(FILLED)) {
            if (lastFillId == 0) {
                lastFillId = orderId;
                lastFillRemaining = remaining;
            } else if (duplicateFillNotification(orderId, remaining)) {
                lastFillId = 0;
                return;
            }

            receiver.orderFilled(Time.now(), orderId, avgFillPrice, filled);
            if (remaining == 0.0) {
                pendingOrders.remove(orderId);
            }
            pendingOrders.remove(orderId);
        } else if (status.equals(CANCELLED)) {
            receiver.orderCancelled(orderId);
            pendingOrders.remove(orderId);
        }
    }

    private boolean duplicateFillNotification(int orderId, double remaining) {
        return lastFillId == orderId && lastFillRemaining == remaining;
    }

    private static void logEvent(String type, Map.Entry<String, Object>... properties) {
        Map<String, Object> orderedProperties = new LinkedHashMap<>();

        orderedProperties.put("type", type);
        orderedProperties.put("timestamp", Time.now().toString());
        for (Map.Entry<String, Object> property : properties) {
            orderedProperties.put(property.getKey(), property.getValue());
        }

        String json = JsonUtils.toPrettyJsonString(orderedProperties);
        EVENT_LOGGER.info(json);
    }

    @Override
    public void openOrder(int orderId, com.ib.client.Contract contract, Order order, OrderState orderState) {
        LOGGER.info("OpenOrder. ID: {}, {}, {} @ {}: {}, {}, {}",
                    orderId, contract.symbol(), contract.getSecType(), contract.exchange(),
                    order.getAction(), order.getOrderType(), orderState.getStatus());
        logEvent("OpenOrder",
                 entry("orderId", orderId),
                 entry("symbol", contract.localSymbol()),
                 entry("secType", contract.secType()),
                 entry("exchange", contract.exchange()),
                 entry("action", order.getAction()),
                 entry("orderType", order.getOrderType()),
                 entry("status", orderState.getStatus()));
    }

    @Override
    public void nextValidId(int orderId) {
        nextOrderId = orderId;
        logEvent("ConnectionInit", entry("nextOrderId", orderId));
        LOGGER.info("Next valid order ID: {}", orderId);
    }

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {
        LOGGER.info("Contract Details:");
        LOGGER.info(contractDetails.toString());

        try {
            contract = IbContract.fromDetails(contractDetails);
            Time.setTradingSchedule(contract.getTradingSchedule());
        } catch (IllegalArgumentException e) {
            if (contract == null) {
                throw new IllegalStateException("Invalid contract details received, and no contract to fall back on: " +
                                                contractDetails.toString(), e);
            }
            ERROR_LOG.warn("Invalid contract details: " + contractDetails.toString());
        }

        logEvent("Contract", entry("details", this.contract));
    }

    @Override
    public void position(String account, com.ib.client.Contract contract, double pos, double avgCost) {
        if (contract.conid() == this.requestContract.conid()) {
            LOGGER.info("Position Update. " +
                        "Account: {}, Symbol: {}, SecType: {}, Currency: {}, Position: {}, Average Cost: {}",
                        account, contract.localSymbol(), contract.getSecType(), contract.currency(), pos, avgCost);

            logEvent("PositionUpdate",
                     entry("account", account),
                     entry("position", pos),
                     entry("averageCost", avgCost),
                     entry("symbol", contract.localSymbol()),
                     entry("secType", contract.secType()),
                     entry("currency", contract.currency()));

            Time.update(Instant.now());

            receiver.positionUpdate(pos, avgCost);
        } else {
            LOGGER.info("got position info for {} but we are not trading that.", contract.localSymbol());
        }
    }

    @Override
    public void positionEnd() {
        LOGGER.info("PositionEnd");
    }

    @Override
    public void pnlSingle(int reqId, int pos, double dailyPnL, double unrealizedPnL, double realizedPnL,
                          double value) {
        LOGGER.info("PnL Single. Request Id: {}, Pos {}, Daily PnL {}, Unrealized PnL {}, Realized PnL: {}, Value: {}",
                    reqId, pos, dailyPnL, unrealizedPnL, realizedPnL, value);
        logEvent("PnL",
                 entry("position", pos),
                 entry("unrealised", unrealizedPnL),
                 entry("realised", realizedPnL),
                 entry("daily", dailyPnL));

        Time.update(Instant.now());

        receiver.pnlUpdate(unrealizedPnL, realizedPnL, dailyPnL);
    }
}
