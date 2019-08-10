package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.MarketDataReceiver;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.time.Time;
import com.ax9k.interactivebrokers.client.IbClient;
import com.ax9k.provider.MarketDataProvider;
import com.ib.client.TickAttr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDate;

import static com.ax9k.core.marketmodel.BidAsk.ASK;
import static com.ax9k.core.marketmodel.BidAsk.BID;
import static com.ax9k.core.marketmodel.BidAsk.NONE;
import static com.ax9k.interactivebrokers.provider.TickType.fromCode;

public class IbMarketDataProvider extends IbClient implements MarketDataProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int BAR_PERIOD_SECONDS = 5;
    private static final int CONNECTION_INITIALISED_CODE = 317;
    private static final int MARKET_DATA_REQUEST = 100;
    private static final int MARKET_DEPTH_REQUEST = 200;
    private static final int BAR_REQUEST = 30_001;
    private static final int BOOK_DEPTH = 5;
    private static final int CLEAR_ORDER = 2;
    private static final int KNOWN_LAG = 0; //TODO record time offset between when we get stuff from IB

    private final MarketDataReceiver receiver;
    private final IbOrderBook orderBook;
    private final TransactionMessageProcessor transactionMessageProcessor;
    private final DepthMessageProcessor depthMessageProcessor;
    private final OhlcvBarAggregator aggregator;

    private OrderBook lastCompleteBook;
    private Phase lastPhase;

    IbMarketDataProvider(MarketDataReceiver receiver,
                         String gatewayHost,
                         int gatewayPort,
                         int contractId,
                         String exchange,
                         int clientId) {
        super(gatewayHost, gatewayPort, clientId * 10 + 1, exchange, contractId);
        this.receiver = receiver;
        orderBook = new IbOrderBook(BOOK_DEPTH);
        transactionMessageProcessor = new TransactionMessageProcessor(this.receiver::trade);
        depthMessageProcessor = new DepthMessageProcessor(this.orderBook::directBookUpdate,
                                                          this::updateTradingDay);
        aggregator = new OhlcvBarAggregator();
    }

    private void updateTradingDay() {
        lastCompleteBook = orderBook.toImmutableOrderBook(EventType.ADD_ORDER);
        receiver.orderBook(lastCompleteBook);
    }

    @Override
    public void startRequest(boolean delayUntilMarketOpen) {
        if (!isConnected()) {
            connect();
        }
        makeDataRequests(delayUntilMarketOpen);
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        super.error(id, errorCode, errorMsg);
        if (id == CONNECTION_INITIALISED_CODE) {
            requestData();
        }
    }

    @Override
    protected void requestData() {
        /*for the trade messages*/
        client.reqMktData(MARKET_DATA_REQUEST, requestContract, "", false, false, null);
        /*for the direct book level updates*/
        client.reqMktDepth(MARKET_DEPTH_REQUEST, requestContract, BOOK_DEPTH, null);
        ERROR_LOG.info("Requested market data subscriptions...");

        LOGGER.info("Requesting real-time bar subscription ... {}", BAR_REQUEST);
        client.reqRealTimeBars(BAR_REQUEST, requestContract, BAR_PERIOD_SECONDS, "TRADES", false, null);
    }

    @Override
    public void stopRequest() {
        if (client.isConnected()) {
            ERROR_LOG.info("Cancelling subscriptions...");
            client.cancelMktData(MARKET_DATA_REQUEST);
            client.cancelMktDepth(MARKET_DEPTH_REQUEST);
            client.cancelRealTimeBars(BAR_REQUEST);
            disconnect();

            ERROR_LOG.info("IB data subscription cancelled and connection terminated");
        }
    }

    @Override
    public String getSource() {
        return "INTERACTIVE_BROKERS";
    }

    @Override
    public LocalDate getDate() {
        return Time.localiseDate(Instant.now());
    }

    public String getSymbol() {
        return requestContract.localSymbol();
    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume,
                            double WAP, int count) {
        Instant timestamp = Instant.ofEpochSecond(time);
        OhlcvBar latestData = OhlcvBar.of(timestamp, open, high, low, close, volume);

        LOGGER.info("Received bar: {}", latestData);

        OhlcvBar completeBar = aggregator.aggregateData(latestData);
        if (completeBar != null) {
            sendUpdate(completeBar);
        }
    }

    private void sendUpdate(OhlcvBar bar) {
        LOGGER.info("Sending {} ...", bar);
        receiver.bar(bar);
    }

    @Override
    public void tickPrice(int tickerId, int tickType, double price, TickAttr attribs) {
        LOGGER.trace("Tick Price. ID: {}, Field: {}, Price: {}, CanAutoExecute: {} Past Limit: {} Pre-Open: {}",
                     tickerId, fromCode(tickType), price, attribs.canAutoExecute(),
                     attribs.pastLimit(), attribs.preOpen());
        transactionMessageProcessor.process(fromCode(tickType), price, lastCompleteBook.getMid());
    }

    @Override
    public void tickSize(int tickerId, int tickType, int size) {
        LOGGER.trace("Tick Size. ID: {}, Field: {}, Size: {}", tickerId, fromCode(tickType), size);
        transactionMessageProcessor.process(fromCode(tickType), size, lastCompleteBook.getMid());
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        LOGGER.trace("Tick Generic. ID: {}, Field: {}, Value: {}", tickerId, fromCode(tickType), value);
        transactionMessageProcessor.process(fromCode(tickType), value, lastCompleteBook.getMid());
    }

    @Override
    public void tickString(int id, int tickType, String value) {
        LOGGER.trace("Tick String. ID: {}, Field: {}, Value: {}", id, fromCode(tickType), value);
        if (fromCode(tickType) == TickType.LAST_TIME_STAMP) {
            transactionMessageProcessor.process(fromCode(tickType),
                                                Double.valueOf(value),
                                                lastCompleteBook.getMid());
        }
    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size) {
        LOGGER.trace("Market Depth Update. Side: {}, Level: {}, Operation: {}, Price: {}, Size: {}",
                     side, position, operation, price, size);
        Instant timestamp = Instant.now().plusMillis(KNOWN_LAG);
        processBookUpdate(timestamp, position, operation, side, price, size);
    }

    private void processBookUpdate(Instant timestamp,
                                   int position,
                                   int operation,
                                   int side,
                                   double price,
                                   double size) {
        if (isNewTradingSession(Time.currentPhase())) {
            refreshBarSubscription();
        }

        //insert (0), update (1) or remove (2).
        //TODO this isnt quite right.. 0 means shuffle everything down.  2 means delete JUST that row and shuffle
        // others up.
        if (operation == CLEAR_ORDER) {
            LOGGER.trace("Clear Orders");
            orderBook.clearLevel(position, decodeSide(side));
            lastCompleteBook = orderBook.toImmutableOrderBook(EventType.DELETE_ORDER);
            receiver.orderBook(lastCompleteBook);
        } else {
            LOGGER.trace("TradingDay Update. Level: {}, Side: {}, Values: {}@{}", position, decodeSide(side), size,
                         price
            );
            depthMessageProcessor.processUpdate(timestamp, position, decodeSide(side), price, size);
        }
    }

    private void refreshBarSubscription() {
        LOGGER.info("Refreshing real-time bar subscription ... {}", BAR_REQUEST);
        client.cancelRealTimeBars(BAR_REQUEST);
        client.reqRealTimeBars(BAR_REQUEST, requestContract, BAR_PERIOD_SECONDS, "TRADES", false, null);
    }

    private boolean isNewTradingSession(Phase currentPhase) {
        boolean result = lastPhase != null &&
                         !currentPhase.equals(lastPhase) &&
                         currentPhase.isTradingSession();
        lastPhase = currentPhase;
        return result;
    }

    private static BidAsk decodeSide(int side) {
        //note this is NOT the same way round as the market data from files
        switch (side) {
            case 0:
                return ASK;
            case 1:
                return BID;
            case -999:
                return NONE;
            default:
                throw new IllegalArgumentException("unsupported side: " + side);
        }
    }

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price,
                                 int size) {
        LOGGER.trace("Level 2 Market Depth Update. Side: {}, Level: {}, Operation: {}, Price: {}, Size: {}",
                     side, position, operation, price, size);
        Instant timestamp = Instant.now().plusMillis(KNOWN_LAG);
        processBookUpdate(timestamp, position, operation, side, price, size);
    }
}
