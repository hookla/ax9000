package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.MarketDataReceiver;
import com.ax9k.core.marketmodel.MarketEvent;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.Trade;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.time.Time;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ax9k.core.event.EventType.ADD_ORDER;
import static com.ax9k.core.event.EventType.CALCULATE_OPENING_PRICE;
import static com.ax9k.core.event.EventType.DELETE_ORDER;
import static com.ax9k.core.event.EventType.ORDER_BOOK_CLEAR;
import static com.ax9k.core.event.EventType.TRADE;
import static com.ax9k.core.marketmodel.BidAsk.ASK;
import static com.ax9k.core.marketmodel.BidAsk.BID;

class FileEventProcessor {
    private static final Logger LOGGER = LogManager.getLogger();

    private final MarketDataReceiver receiver;
    private final Map<Long, MarketEvent> activeOrders = new HashMap<>(1000);

    private FileOrderBook orderBook;
    private int tradeErrorCount = 0;
    private int deleteOrderErrorCount = 0;
    private boolean processBookUpdates;
    private boolean processTrades;
    private OrderBook currentBook = OrderBook.EMPTY;

    FileEventProcessor(TradingDay receiver, ProcessingMode mode) {
        this.receiver = receiver;
        orderBook = new FileOrderBook(Instant.EPOCH);

        LOGGER.info("Processing Mode: {}", mode);

        initialiseProcessingMode(mode);
    }

    private void initialiseProcessingMode(ProcessingMode mode) {
        switch (mode) {
            case BOOK_STATES:
                processBookUpdates = true;
                break;
            case TRADES:
                processTrades = true;
                break;
            case ALL:
                processTrades = processBookUpdates = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown processing mode: " + mode);
        }
    }

    void processExchangeMessage(MarketEvent exchangeMessage) {
        LOGGER.debug(".");
        LOGGER.debug("<----START processExchangeMessage---->  {}, Active Orders: {}",
                     exchangeMessage.getMessageType(),
                     activeOrders.keySet().size());
        if (processTrades && exchangeMessage.getMessageType() == TRADE) {
            processTradeEvent(exchangeMessage);
        } else if (processBookUpdates && exchangeMessage.getMessageType() == ADD_ORDER) {
            processAddOrderEvent(exchangeMessage);
        } else if (processBookUpdates && exchangeMessage.getMessageType() == DELETE_ORDER) {
            processDeleteOrder(exchangeMessage);
        } else if (processBookUpdates && exchangeMessage.getMessageType() == ORDER_BOOK_CLEAR) {
            clearOrderBook(exchangeMessage.getEventTimestamp());
            LOGGER.info("Cleared Order Book");
        } else if (exchangeMessage.getMessageType() == CALCULATE_OPENING_PRICE) {
            LOGGER.trace("Nothing to do for CALCULATE_OPENING_PRICE");
        } else {
            if (processBookUpdates && processTrades) {
                LOGGER.error("Unhandled order type '{}'. Phase: {}",
                             exchangeMessage.getMessageType(),
                             Time.currentPhase());
            }
        }
    }

    private void processTradeEvent(MarketEvent exchangeMessage) {
        MarketEvent existingOrder;
        long uniqueOrderID = 0;

        if (exchangeMessage.getOrderId() != 0) {
            existingOrder = lookupExistingOrder(exchangeMessage.getOrderId());

            if (existingOrder == null || existingOrder.getPrice() != exchangeMessage.getPrice()) {
                existingOrder = lookupExistingOrder(-exchangeMessage.getOrderId());
            }

            if (existingOrder == null || existingOrder.getPrice() != exchangeMessage.getPrice()) {
                tradeErrorCount++;
                Phase currentPhase = Time.currentPhase();
                if (currentPhase.isTradingSession() || currentPhase.isAfterMarketClose()) {
                    LOGGER.error("Trade Error #{}. Could not find trade order {}. " +
                                 "Phase: {}, Timestamp: {}, Price: {}, Bid0: {}, Ask0: {}",
                                 tradeErrorCount++,
                                 exchangeMessage.getOrderId(),
                                 Time.currentPhase(),
                                 exchangeMessage.getLocalisedEventTimestamp(),
                                 exchangeMessage.getPrice(),
                                 currentBook.getBid0(),
                                 currentBook.getAsk0());
                }
                return;
            }

            uniqueOrderID = existingOrder.getUniqueOrderId();
            existingOrder.fill(exchangeMessage.getOrderQuantity());

            if (existingOrder.getRemainingQuantity() == 0) {
                activeOrders.remove(existingOrder.getUniqueOrderId());
                LOGGER.trace("Removed order from active orders collection because it has been fully filled.");
            }

            orderBook.processTradeEvent(exchangeMessage, existingOrder);
            currentBook = orderBook.toOrderBook(TRADE);
            BidAsk side = gleanSide(exchangeMessage, currentBook);
            Trade newTrade = new Trade(exchangeMessage.getEventTimestamp(),
                                       exchangeMessage.getPrice(),
                                       exchangeMessage.getOrderQuantity(),
                                       uniqueOrderID,
                                       side);
            receiver.trade(newTrade, currentBook);
        } else {
            BidAsk side = gleanSide(exchangeMessage, currentBook);
            Trade newTrade = new Trade(exchangeMessage.getEventTimestamp(),
                                       exchangeMessage.getPrice(),
                                       exchangeMessage.getOrderQuantity(),
                                       uniqueOrderID,
                                       side);
            receiver.trade(newTrade);
        }
    }

    private static BidAsk gleanSide(MarketEvent exchangeMessage, OrderBook currentBook) {
        return currentBook.getMid() >= exchangeMessage.getPrice() ? BID : ASK;
    }

    private MarketEvent lookupExistingOrder(long uniqueOrderId) {
        StopWatch stopWatch = StopWatch.createStarted();
        MarketEvent existingOrder = null;
        if (activeOrders.containsKey(uniqueOrderId)) {
            existingOrder = activeOrders.get(uniqueOrderId);
            LOGGER.debug("Found order in {} microseconds. Side: {}, Price: {}, Remaining Quantity: {}",
                         stopWatch.getTime(TimeUnit.MICROSECONDS),
                         existingOrder.getSide(),
                         existingOrder.getPrice(),
                         existingOrder.getRemainingQuantity());
        }
        return existingOrder;
    }

    private void processAddOrderEvent(MarketEvent exchangeMessage) {
        StopWatch stopWatch = StopWatch.createStarted();
        activeOrders.put(exchangeMessage.getUniqueOrderId(), exchangeMessage);
        LOGGER.trace("Added new order to active orders collection in {} micro secs",
                     stopWatch.getTime(TimeUnit.MICROSECONDS));
        stopWatch.reset();
        stopWatch.start();

        orderBook.processAddOrderEvent(exchangeMessage);
        receiver.orderBook(orderBook.toOrderBook(ADD_ORDER));
        LOGGER.debug("Processed new order {} in {} microseconds. Price: {}, Quantity: {}",
                     exchangeMessage.getUniqueOrderId(),
                     stopWatch.getTime(TimeUnit.MICROSECONDS),
                     exchangeMessage.getPrice(),
                     exchangeMessage.getOrderQuantity());
    }

    private void processDeleteOrder(final MarketEvent exchangeMessage) {
        StopWatch stopWatch = StopWatch.createStarted();
        MarketEvent existingOrder = lookupExistingOrder(exchangeMessage.getUniqueOrderId());
        if (existingOrder != null) {
            activeOrders.remove(existingOrder.getUniqueOrderId());
            LOGGER.trace("Removed order from active orders collection");

            orderBook.processDeleteOrder(existingOrder, exchangeMessage);
            receiver.orderBook(orderBook.toOrderBook(DELETE_ORDER));
            LOGGER.debug("Deleted order {} in {} microseconds",
                         exchangeMessage.getUniqueOrderId(),
                         stopWatch.getTime(TimeUnit.MICROSECONDS));
        } else if (Time.currentPhase().isMarketOpen()) {
            deleteOrderErrorCount++;
            LOGGER.error("Delete order error #{}. Couldn't find order to delete. Order ID: {}, Phase: {}",
                         deleteOrderErrorCount,
                         exchangeMessage.getUniqueOrderId(),
                         Time.currentPhase());
        }
    }

    private void clearOrderBook(Instant eventTimestamp) {
        orderBook = new FileOrderBook(eventTimestamp);
        activeOrders.clear();
        receiver.orderBook(orderBook.toOrderBook(ORDER_BOOK_CLEAR));
    }
}
