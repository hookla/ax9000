package com.ax9k.core.marketmodel;

import com.ax9k.core.event.Event;
import com.ax9k.core.event.EventType;
import com.ax9k.core.history.BasicHistory;
import com.ax9k.core.history.History;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.ax9k.utils.logging.ImmutableObjectMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static com.ax9k.core.event.EventType.UNKNOWN;
import static java.util.stream.Collectors.toMap;

@JsonPropertyOrder({ "tradingDayDate", "ready", "lastEventTime" })
public class TradingDay extends Observable implements MarketDataReceiver {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Logger DATA_LOGGER = LogManager.getLogger("dataLogger");
    private static final Logger TRADE_LOGGER = LogManager.getLogger("tradeLogger");
    private static final Logger BARS_LOGGER = LogManager.getLogger("barLogger");
    private static final History EMPTY_HISTORY = new BasicHistory();

    private final Lock lock = new ReentrantLock();
    private final History<Trade> trades = new BasicHistory<>();
    private final History<OrderBook> books = new BasicHistory<>();
    private final History<OhlcvBar> bars = new BasicHistory<>();
    private final Map<Class<? extends Event>, History<Event>> extraData = new HashMap<>();
    private EventType lastEventType = UNKNOWN;
    private Instant lastProcessedTimeStamp = Instant.EPOCH;
    private Trade lastTrade;
    private OrderBook currentBook;
    private OhlcvBar lastBar;
    private double dailyTradeHigh = 0;
    private double dailyTradeLow = Double.MAX_VALUE;
    private int dailyTradeCount = 0;
    private LocalDate tradingDayDate;
    private boolean booksDateLogged;
    private boolean tradesDateLogged;
    private final HeartBeat heartBeat = new HeartBeat(Duration.ofSeconds(5), this::beat);

    private BiConsumer<Double, Double> bookUpdateConsumer = (aDouble, aDouble2) -> {

    };

    public TradingDay() {
        lastTrade = Trade.EMPTY;
        currentBook = OrderBook.EMPTY;
        lastBar = OhlcvBar.EMPTY;
    }

    public void setBookUpdateConsumer(BiConsumer<Double, Double> bookUpdateConsumer) {
        this.bookUpdateConsumer = bookUpdateConsumer;
    }

    public HeartBeat getHeartBeat() {
        return heartBeat;
    }

    @JsonIgnore
    public Trade getLastTrade() {
        lock.lock();
        try {
            return lastTrade;
        } finally {
            lock.unlock();
        }
    }

    @JsonProperty("lastEventTime")
    public String getLastEventTimeString() {
        return Time.toTimeString(getLocalisedLastEventTimestamp());
    }

    @JsonIgnore
    private LocalDateTime getLocalisedLastEventTimestamp() {
        return Time.localise(getLastEventTimestamp());
    }

    @JsonIgnore
    private Instant getLastEventTimestamp() {
        return Time.now();
    }

    public EventType getLastEventType() {
        lock.lock();
        try {
            return lastEventType;
        } finally {
            lock.unlock();
        }
    }

    private void beat(Instant timestamp) {
        Time.update(timestamp);
        lock.lock();
        try {
            if (tradingDayDate == null) {
                setTradingDayDate(Time.localiseDate(timestamp));
            }

            lastEventType = EventType.HEARTBEAT;
            lastProcessedTimeStamp = Instant.now();

            setChanged();
        } finally {
            lock.unlock();
        }
        notifyObservers(new HeartBeatEvent(timestamp));
    }

    public void setTradingDayDate(LocalDate tradingDayDate) {
        if (tradingDayDate == null) {
            return;
        }

        lock.lock();
        try {
            this.tradingDayDate = tradingDayDate;

            if (!tradesDateLogged) {
                TRADE_LOGGER.info(tradingDayDate);
                tradesDateLogged = true;
            }

            if (!booksDateLogged) {
                DATA_LOGGER.info(tradingDayDate);
                booksDateLogged = true;
            }
        } finally {
            lock.unlock();
        }
    }

    public long getLag() {
        lock.lock();
        try {
            return lastProcessedTimeStamp == null ? -1 :
                   Duration.between(getLastEventTimestamp(), lastProcessedTimeStamp).toMillis();
        } finally {
            lock.unlock();
        }
    }

    public boolean isReady() {
        return (getAsk0() != 0 && getBid0() != 0);
    }

    public double getBid0() {
        return getCurrentBook().getBidPrice(0);
    }

    public double getAsk0() {
        return getCurrentBook().getAskPrice(0);
    }

    @JsonIgnore
    public OrderBook getCurrentBook() {
        lock.lock();
        try {
            return currentBook;
        } finally {
            lock.unlock();
        }
    }

    @JsonIgnore
    public History<OrderBook> getOrderBookHistory() {
        lock.lock();
        try {
            return books;
        } finally {
            lock.unlock();
        }
    }

    public int getOrderBookHistorySize() {
        lock.lock();
        try {
            return books.getSize();
        } finally {
            lock.unlock();
        }
    }

    @JsonIgnore
    public History<Trade> getTradeHistory() {
        lock.lock();
        try {
            return trades;
        } finally {
            lock.unlock();
        }
    }

    public int getTradeHistorySize() {
        lock.lock();
        try {
            return trades.getSize();
        } finally {
            lock.unlock();
        }
    }

    @JsonIgnore
    public History<OhlcvBar> getBarHistory() {
        lock.lock();
        try {
            return bars;
        } finally {
            lock.unlock();
        }
    }

    public int getBarHistorySize() {
        lock.lock();
        try {
            return bars.getSize();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> History<T> getHistory(Class<T> type) {
        lock.lock();
        try {
            Validate.notNull(type);
            return (History<T>) extraData.computeIfAbsent(type, __ -> new BasicHistory<>());
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Integer> getHistorySizes() {
        lock.lock();
        try {
            return extraData.entrySet().stream()
                            .collect(toMap(entry -> entry.getKey().toString(),
                                           entry -> entry.getValue().getSize()));
        } finally {
            lock.unlock();
        }
    }

    @JsonIgnore
    public OhlcvBar getLatestBar() {
        lock.lock();
        try {
            return lastBar;
        } finally {
            lock.unlock();
        }
    }

    public double getDailyTradeHigh() {
        return dailyTradeHigh;
    }

    public double getDailyTradeLow() {
        return dailyTradeLow;
    }

    public int getDailyTradeCount() {
        return dailyTradeCount;
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            return JsonUtils.toPrettyJsonString(this);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void trade(Trade trade) {
        recordTrade(trade);
        sendEventNotification(trade);
    }

    private void sendEventNotification(Event event) {
        lock.lock();
        try {
            setChanged();
        } finally {
            lock.unlock();
        }
        notifyObservers(event);
    }

    private void recordTrade(Trade trade) {
        Time.update(trade.getTimestamp());
        lock.lock();
        try {
            if (tradingDayDate == null) {
                setTradingDayDate(Time.localiseDate(trade.getTimestamp()));
            }
            updateTradeData(trade);
        } finally {
            lock.unlock();
        }
        TRADE_LOGGER.info(new ImmutableObjectMessage(trade));
    }

    private void updateTradeData(Trade trade) {
        trades.record(trade);

        lastTrade = trade;
        if (lastTrade.getPrice() > dailyTradeHigh) {
            dailyTradeHigh = lastTrade.getPrice();
        }

        if (lastTrade.getPrice() < dailyTradeLow) {
            dailyTradeLow = lastTrade.getPrice();
        }
        dailyTradeCount++;

        lastEventType = EventType.TRADE;
        lastProcessedTimeStamp = Instant.now();
    }

    @Override
    public void trade(Trade trade, OrderBook book) {
        recordTrade(trade);
        processBookUpdate(book);
    }

    private void processBookUpdate(OrderBook book) {
        Time.update(book.getTimestamp());

        lock.lock();
        double bid0, ask0;
        try {
            updateBookData(book);
            bid0 = getBid0();
            ask0 = getAsk0();
            reflectOnCurrentState();
        } finally {
            lock.unlock();
        }
        bookUpdateConsumer.accept(bid0, ask0);
        notifyObservers(book);

        DATA_LOGGER.info(new ImmutableObjectMessage(book));
    }

    private void updateBookData(OrderBook book) {
        lock.lock();
        try {
            if (tradingDayDate == null) {
                setTradingDayDate(Time.localiseDate(book.getTimestamp()));
            }

            currentBook = book;
            books.record(currentBook);
            lastEventType = book.getType();
            lastProcessedTimeStamp = Instant.now();
            setChanged();
        } finally {
            lock.unlock();
        }
    }

    private void reflectOnCurrentState() {
        lock.lock();
        try {
            if (getPhase().isMarketOpen() &&
                getBid0() > 0 &&
                getAsk0() > 0 &&
                getBid0() > getAsk0()) {
                LOGGER.warn("phase: {}. timestamp: {}  Bid {} > Ask {}",
                            getPhase(), getLocalisedLastEventTime(), getBid0(), getAsk0());
            }
        } finally {
            lock.unlock();
        }
    }

    public Phase getPhase() {
        return Time.currentPhase();
    }

    @JsonIgnore
    public LocalTime getLocalisedLastEventTime() {
        return Time.localiseTime(getLastEventTimestamp());
    }

    @Override
    public void orderBook(OrderBook book) {
        processBookUpdate(book);
    }

    @Override
    public void bar(OhlcvBar bar) {
        lock.lock();
        try {
            bars.record(bar);
            lastBar = bar;
            lastEventType = EventType.OHLCV_BAR;
            lastProcessedTimeStamp = Instant.now();
            setChanged();
        } finally {
            lock.unlock();
        }
        BARS_LOGGER.info(new ImmutableObjectMessage(bar));
        notifyObservers(bar);
    }

    @Override
    public void extra(Event data) {
        Time.update(data.getTimestamp());

        lock.lock();
        try {
            if (tradingDayDate == null) {
                setTradingDayDate(Time.localiseDate(data.getTimestamp()));
            }

            extraData.computeIfAbsent(data.getClass(), type -> new BasicHistory<>()).record(data);
            lastEventType = data.getType();
            lastProcessedTimeStamp = Instant.now();
            setChanged();
        } finally {
            lock.unlock();
        }
        notifyObservers(data);
    }
}