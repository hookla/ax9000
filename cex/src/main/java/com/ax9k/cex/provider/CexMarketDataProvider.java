package com.ax9k.cex.provider;

import com.ax9k.cex.client.CexClient;
import com.ax9k.cex.client.CexContract;
import com.ax9k.cex.client.CexRequest;
import com.ax9k.cex.client.CexResponse;
import com.ax9k.cex.client.JsonMapper;
import com.ax9k.cex.client.MessageType;
import com.ax9k.cex.client.Pair;
import com.ax9k.cex.client.ResponseHandler;
import com.ax9k.cex.data.Ticker;
import com.ax9k.cex.provider.orderbook.CexOrderBook;
import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.MarketDataReceiver;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.time.Time;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.utils.logging.ImmutableObjectMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;

public class CexMarketDataProvider implements MarketDataProvider {
    private static final Logger EVENT_LOGGER = LogManager.getLogger("providerLogger");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Logger ERROR_LOG = LogManager.getLogger("error");

    private static final MessageType ORDER_BOOK_SUBSCRIBE = MessageType.of("order-book-subscribe");
    private static final MessageType ORDER_BOOK_UNSUBSCRIBE = MessageType.of("order-book-unsubscribe");
    private static final MessageType ORDER_BOOK_UPDATE = MessageType.of("md_update");

    private static final MessageType ONE_MINUTE_BARS = MessageType.of("ohlcv1m");
    private static final MessageType LATEST_BAR = MessageType.of("ohlcv");
    private static final MessageType TICKER = MessageType.of("tick");
    private static final String ONE_MINUTE_BAR_SUBSCRIBE_REQUEST = "{\n" +
                                                                   "  \"e\": \"init-ohlcv\",\n" +
                                                                   "  \"i\": \"1m\",\n" +
                                                                   "  \"rooms\": [\n" +
                                                                   "    \"pair-%s-%s\"\n" +
                                                                   "  ]\n" +
                                                                   "}";
    private static final String TICKER_REQUEST = "{\n" +
                                                 "  \"e\": \"subscribe\",\n" +
                                                 "  \"rooms\": [\n" +
                                                 "    \"tickers\"\n" +
                                                 "  ]\n" +
                                                 "}";

    private final MarketDataReceiver receiver;
    private final CexClient client;
    private final CexContract contract;

    private final CexRequest bookSubscribeRequest;
    private final CexRequest bookUnsubscribeRequest;
    private final ResponseHandler onFirstBar = this::initialBarSnapshot;
    private final ResponseHandler onBarUpdate = this::barUpdate;
    private final ResponseHandler onTick = this::tick;
    private CexOrderBook orderBook;
    private final ResponseHandler onFirstBook = this::initialBookSnapshot;
    private final ResponseHandler onBookUpdate = this::updateOrderBook;

    CexMarketDataProvider(MarketDataReceiver receiver,
                          CexClient client,
                          CexContract contract) {
        this.receiver = receiver;
        this.client = client;
        this.contract = contract;

        JsonNode pair = contract.getCurrencies().toJsonArray();
        JsonNode bookSubscribeRequestData = JsonMapper.createObjectNode()
                                                      .put("subscribe", true)
                                                      .put("depth", CexOrderBook.DEPTH)
                                                      .set("pair", pair);
        JsonNode bookUnsubscribeRequestData = JsonMapper.createObjectNode().set("pair", pair.deepCopy());

        bookSubscribeRequest = new CexRequest(ORDER_BOOK_SUBSCRIBE, bookSubscribeRequestData, true);
        bookUnsubscribeRequest = new CexRequest(ORDER_BOOK_UNSUBSCRIBE, bookUnsubscribeRequestData, true);
    }

    private void tick(CexResponse response) {
        JsonNode data = response.getData();
        Pair pair = contract.getCurrencies();

        LOGGER.info("Tick: {}", data.toString());
        if (pair.getSymbol1().equalsIgnoreCase(data.get("symbol1").asText()) &&
            pair.getSymbol2().equalsIgnoreCase(data.get("symbol2").asText())) {
            receiver.extra(new Ticker(data.get("price").asDouble()));
        }
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void startRequest(boolean delayUntilMarketOpen) {
        client.connect();

        client.registerResponseHandler(ORDER_BOOK_SUBSCRIBE, onFirstBook);
        client.registerResponseHandler(ORDER_BOOK_UPDATE, onBookUpdate);
        client.registerResponseHandler(LATEST_BAR, onFirstBar);
        client.registerResponseHandler(ONE_MINUTE_BARS, onBarUpdate);
        client.registerResponseHandler(TICKER, onTick);

        Pair pair = contract.getCurrencies();
        String barSubscribe = format(ONE_MINUTE_BAR_SUBSCRIBE_REQUEST,
                                     pair.getSymbol1(),
                                     pair.getSymbol2());

        Consumer<CexClient> requestMarketData = client -> {
            ERROR_LOG.info("Requesting order book subscriptions ...");
            client.makeRequest(bookSubscribeRequest);
            ERROR_LOG.info("Requesting OHLCV bar subscriptions ...");
            client.makeRawRequest(barSubscribe);
            client.makeRawRequest(TICKER_REQUEST);
        };

        requestMarketData.accept(client);
        client.onDisconnectionRecovery(getClass(), requestMarketData);
    }

    @Override
    public void stopRequest() {
        ERROR_LOG.info("Cancelling subscriptions...");
        client.cancelDisconnectionRecovery(getClass());
        client.makeRequest(bookUnsubscribeRequest);
    }

    @Override
    public String getSource() {
        return "CEX.io";
    }

    @Override
    public LocalDate getDate() {
        return Time.localiseDate(Instant.now());
    }

    @Override
    public String getSymbol() {
        return contract.getLocalSymbol();
    }

    @Override
    public Collection<Class<?>> getExtraDataTypes() {
        return List.of(Ticker.class);
    }

    private void initialBookSnapshot(CexResponse response) {
        EVENT_LOGGER.info(new ImmutableObjectMessage(response));
        JsonNode data = response.getData();
        LOGGER.info("Received first book: " + data);
        orderBook = CexOrderBook.of(data);
        sendBookEvent();
    }

    private void sendBookEvent() {
        receiver.orderBook(orderBook.toImmutableBook(EventType.ADD_ORDER));
    }

    private void updateOrderBook(CexResponse response) {
        EVENT_LOGGER.info(new ImmutableObjectMessage(response));
        JsonNode data = response.getData();
        orderBook.update(data);
        sendBookEvent();
    }

    private void initialBarSnapshot(CexResponse response) {
        EVENT_LOGGER.info(new ImmutableObjectMessage(response));
        JsonNode data = response.getData();
        ArrayNode latest = (ArrayNode) data.get(0);

        if (latest == null) {
            return;
        }

        Instant timestamp = Instant.ofEpochSecond(latest.get(0).asLong());
        double open = latest.get(1).asDouble();
        double high = latest.get(2).asDouble();
        double low = latest.get(3).asDouble();
        double close = latest.get(4).asDouble();
        double volume = latest.get(5).asDouble();

        sendBarEvent(OhlcvBar.of(timestamp, open, high, low, close, volume));
    }

    private void sendBarEvent(OhlcvBar bar) {
        receiver.bar(bar);
    }

    private void barUpdate(CexResponse response) {
        EVENT_LOGGER.info(new ImmutableObjectMessage(response));
        JsonNode data = response.getData();

        Instant timestamp = Instant.ofEpochSecond(data.get("time").asLong());
        double open = data.get("o").asDouble();
        double high = data.get("h").asDouble();
        double low = data.get("l").asDouble();
        double close = data.get("c").asDouble();
        double volume = data.get("v").asDouble();

        sendBarEvent(OhlcvBar.of(timestamp, open, high, low, close, volume));
    }
}
