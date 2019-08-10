package com.ax9k.core.marketmodel.orderbook;

import com.ax9k.core.event.Event;
import com.ax9k.core.event.EventType;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.ax9k.utils.math.MathUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.Validate.notNull;

@JsonRootName("orderBook")
@JsonPropertyOrder({ "eventTime", "bids", "asks" })
public final class OrderBook implements Event {
    public static final OrderBook EMPTY = new OrderBook();

    private static final double NOT_CALCULATED = -999;
    private static final int DEFAULT_BOOK_DEPTH = 5;

    private final OrderBookLevel[] askLevels;
    private final OrderBookLevel[] bidLevels;
    private final Instant timestamp;
    private final EventType lastChangeType;

    private double sumAskQuantity = NOT_CALCULATED;
    private double sumBidQuantity = NOT_CALCULATED;
    private double mid = NOT_CALCULATED;
    private double spread = NOT_CALCULATED;
    private double bookValueBids = NOT_CALCULATED;
    private double bookValueAsks = NOT_CALCULATED;
    private double bookBidAskValueRatio = NOT_CALCULATED;
    private double bookBidAskQuantityRatio = NOT_CALCULATED;
    private double weightedAveragePrice = NOT_CALCULATED;
    private double crossWeightedAveragePrice = NOT_CALCULATED;

    private OrderBook() {
        timestamp = Instant.EPOCH;
        lastChangeType = EventType.UNKNOWN;
        askLevels = new OrderBookLevel[DEFAULT_BOOK_DEPTH];
        bidLevels = new OrderBookLevel[DEFAULT_BOOK_DEPTH];
        for (int i = 0; i < DEFAULT_BOOK_DEPTH; i++) {
            askLevels[i] = OrderBookLevel.EMPTY;
            bidLevels[i] = OrderBookLevel.EMPTY;
        }
    }

    public OrderBook(Instant timestamp,
                     EventType lastChangeType,
                     OrderBookLevel[] askLevels,
                     OrderBookLevel[] bidLevels) {
        this.timestamp = notNull(timestamp, "timestamp");
        this.lastChangeType = notNull(lastChangeType, "lastChangeType");

        for (int i = 0; i < askLevels.length; i++) {
            if (askLevels[i] == null) {
                throw new IllegalArgumentException("null level in asks: " + Arrays.toString(askLevels));
            } else if (bidLevels[i] == null) {
                throw new IllegalArgumentException("null level in bids: " + Arrays.toString(bidLevels));
            }
        }
        this.askLevels = askLevels.clone();
        this.bidLevels = bidLevels.clone();
    }

    @SuppressWarnings("unused") /* Jackson */
    public String getEventTime() {
        return Time.toTimeString(getLocalisedTimestamp());
    }

    @Override
    @JsonIgnore
    public LocalDateTime getLocalisedTimestamp() {
        return Time.localise(timestamp);
    }

    @Override
    @JsonIgnore
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public EventType getType() {
        return lastChangeType;
    }

    public double getSpread() {
        if (notCalculated(spread)) {
            spread = round(getAskPrice(0) - getBidPrice(0));
        }
        return spread;
    }

    private static boolean notCalculated(double feature) {
        return Double.compare(feature, NOT_CALCULATED) == 0;
    }

    public double getAskPrice(int depth) {
        return askLevels[depth].getPrice();
    }

    public double getBidPrice(int depth) {
        return bidLevels[depth].getPrice();
    }

    private static double round(double value) {
        return MathUtils.round(value, 5);
    }

    public double getTotalBookValueMidPriceRatio() {
        return (getMid() == 0) ? 0 : round(getBookValue() / getMid());
    }

    public double getMid() {
        if (notCalculated(mid)) {
            mid = (getAskPrice(0) + getBidPrice(0)) / 2.0;
        }
        return mid;
    }

    public double getBookValue() {
        return getBookValueAsks() + getBookValueBids();
    }

    public double getBookValueAsks() {
        if (notCalculated(bookValueAsks)) {
            var asks = stream(getAsks());
            bookValueAsks = asks
                    .mapToDouble(OrderBookLevel::getValue)
                    .sum();
        }
        return bookValueAsks;
    }

    public OrderBookLevel[] getAsks() {
        return askLevels.clone();
    }

    public double getBookValueBids() {
        if (notCalculated(bookValueBids)) {
            var bids = stream(getBids());
            bookValueBids = bids
                    .mapToDouble(OrderBookLevel::getValue)
                    .sum();
        }
        return bookValueBids;
    }

    public OrderBookLevel[] getBids() {
        return bidLevels.clone();
    }

    public double getAskBookValuePriceRatio() {
        return (getAsk0() == 0) ? 0 : round(getBookValueAsks() / getAsk0());
    }

    public double getAsk0() {
        return getAskPrice(0);
    }

    public double getBidBookValuePriceRatio() {
        return (getBid0() == 0) ? 0 : round(getBookValueBids() / getBid0());
    }

    public double getBid0() {
        return getBidPrice(0);
    }

    public double getBookBidAskValueRatio() {
        if (notCalculated(bookBidAskValueRatio)) {
            bookBidAskValueRatio = (getBookValueBids() == 0 || getBookValueAsks() == 0) ? 0 :
                                   round(getBookValueAsks() / getBookValueBids());
        }
        return bookBidAskValueRatio;
    }

    public double getBookBidAskQuantityRatio() {
        if (notCalculated(bookBidAskQuantityRatio)) {
            bookBidAskQuantityRatio = (getBookValueBids() == 0 || getBookValueAsks() == 0) ? 0 :
                                      round(getSumAskQuantity() / getSumBidQuantity());
        }
        return bookBidAskValueRatio;
    }

    public double getSumAskQuantity() {
        if (notCalculated(sumAskQuantity)) {
            var asks = stream(getAsks());
            sumAskQuantity = asks
                    .mapToDouble(OrderBookLevel::getQuantity)
                    .sum();
        }
        return sumAskQuantity;
    }

    public double getSumBidQuantity() {
        if (notCalculated(sumBidQuantity)) {
            var bids = stream(getBids());
            sumBidQuantity = bids
                    .mapToDouble(OrderBookLevel::getQuantity)
                    .sum();
        }
        return sumBidQuantity;
    }

    public double getWeightedAveragePrice() {
        if (notCalculated(weightedAveragePrice)) {
            weightedAveragePrice =
                    (getSumQuantity() == 0 || getBookValue() == 0) ? 0 : round(getBookValue() / getSumQuantity());
        }
        return weightedAveragePrice;
    }

    public double getSumQuantity() {
        return getSumAskQuantity() + getSumBidQuantity();
    }

    public double getCrossWeightedAveragePrice() {
        if (notCalculated(crossWeightedAveragePrice)) {
            double askCrossWeightedPrice = 0;
            double bidCrossWeightedPrice = 0;
            for (int i = 0; i < askLevels.length; i++) {
                askCrossWeightedPrice += askLevels[i].getValue();
                bidCrossWeightedPrice += bidLevels[i].getValue();
            }
            crossWeightedAveragePrice =
                    round((askCrossWeightedPrice + bidCrossWeightedPrice) /
                          getSumQuantity());
        }
        return crossWeightedAveragePrice;
    }

    public double getVolumeOrderImbalance() {
        double bidQuantity = getSumBidQuantity();
        double askQuantity = getSumAskQuantity();

        if (bidQuantity > 0) {
            return round(bidQuantity / (bidQuantity + askQuantity));
        } else {
            return 0;
        }
    }

    public boolean isEmpty() {
        return bidLevels.length == 0;
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        int result = Arrays.hashCode(askLevels);
        result = 31 * result + Arrays.hashCode(bidLevels);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        OrderBook orderBook = (OrderBook) o;
        return Arrays.equals(askLevels, orderBook.askLevels) &&
               Arrays.equals(bidLevels, orderBook.bidLevels);
    }

    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}