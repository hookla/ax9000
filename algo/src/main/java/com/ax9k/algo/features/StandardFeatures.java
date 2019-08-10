package com.ax9k.algo.features;

import com.ax9k.algo.features.set.Average;
import com.ax9k.algo.features.set.Close;
import com.ax9k.algo.features.set.Decay;
import com.ax9k.algo.features.set.ExponentialWeightedMovingAverage;
import com.ax9k.algo.features.set.Maximum;
import com.ax9k.algo.features.set.Minimum;
import com.ax9k.algo.features.set.Open;
import com.ax9k.algo.features.set.Range;
import com.ax9k.algo.features.set.RelativeStrengthIndex;
import com.ax9k.algo.features.set.SetFeature;
import com.ax9k.algo.features.set.StandardDeviation;
import com.ax9k.algo.features.set.Sum;
import com.ax9k.core.marketmodel.Trade;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.marketmodel.orderbook.OrderBook;

public final class StandardFeatures {
    public static final Feature<OrderBook> BID_0 = (book) -> book.getBidPrice(0);
    public static final Feature<OrderBook> ASK_0 = (book) -> book.getAskPrice(0);
    public static final Feature<OrderBook> SPREAD = OrderBook::getSpread;
    public static final Feature<OrderBook> MID = OrderBook::getMid;
    public static final Feature<OrderBook> BOOK_ASK_VALUE = OrderBook::getBookValueAsks;
    public static final Feature<OrderBook> BOOK_BID_VALUE = OrderBook::getBookValueBids;
    public static final Feature<OrderBook> TOTAL_BOOK_VALUE = OrderBook::getBookValue;
    public static final Feature<OrderBook> BID_ASK_VALUE_RATIO = OrderBook::getBookBidAskValueRatio;
    public static final Feature<OrderBook> BID_ASK_QUANTITY_RATIO = OrderBook::getBookBidAskQuantityRatio;
    public static final Feature<OrderBook> CUMULATIVE_QUANTITY = OrderBook::getSumQuantity;
    public static final Feature<OrderBook> WEIGHTED_AVERAGE_PRICE = OrderBook::getWeightedAveragePrice;
    public static final Feature<OrderBook> CROSS_WEIGHTED_AVERAGE_PRICE = OrderBook::getCrossWeightedAveragePrice;
    public static final Feature<OrderBook> TOTAL_BOOK_VALUE_MID_PRICE_RATIO = OrderBook::getTotalBookValueMidPriceRatio;
    public static final Feature<OrderBook> ASK_BOOK_VALUE_PRICE_RATIO = OrderBook::getAskBookValuePriceRatio;
    public static final Feature<OrderBook> BID_BOOK_VALUE_PRICE_RATIO = OrderBook::getBidBookValuePriceRatio;
    public static final Feature<OrderBook> VOLUME_ORDER_IMBALANCE = OrderBook::getVolumeOrderImbalance;

    public static final Feature<Trade> TRADE_PRICE = Trade::getPrice;
    public static final Feature<Trade> TRADE_COUNT = (ignored) -> 1;
    public static final Feature<Trade> TRADE_QUANTITY = Trade::getQuantity;
    public static final Feature<Trade> TRADE_PRICE_X_QUANTITY = Trade::getPriceXQuantity;

    public static final Feature<OhlcvBar> BAR_OPEN = OhlcvBar::getOpen;
    public static final Feature<OhlcvBar> BAR_CLOSE = OhlcvBar::getClose;
    public static final Feature<OhlcvBar> BAR_HIGH = OhlcvBar::getHigh;
    public static final Feature<OhlcvBar> BAR_LOW = OhlcvBar::getLow;
    public static final Feature<OhlcvBar> BAR_VOLUME = OhlcvBar::getVolume;

    public static final SetFeature DECAY = new Decay();
    public static final SetFeature OPEN = new Open();
    public static final SetFeature CLOSE = new Close();
    public static final SetFeature AVERAGE = new Average();
    public static final SetFeature MIN = new Minimum();
    public static final SetFeature MAX = new Maximum();
    public static final SetFeature SUM = new Sum();
    public static final SetFeature RANGE = new Range();
    public static final SetFeature STANDARD_DEVIATION = new StandardDeviation();
    public static final SetFeature EXPONENTIAL_WEIGHTED_MOVING_AVERAGE = new ExponentialWeightedMovingAverage();
    public static final SetFeature RSI = new RelativeStrengthIndex();

    private StandardFeatures() {
        throw new AssertionError("StandardFeatures is not instantiable");
    }
}
