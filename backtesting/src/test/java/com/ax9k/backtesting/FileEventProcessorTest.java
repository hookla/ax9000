package com.ax9k.backtesting;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.MarketEvent;
import com.ax9k.core.marketmodel.StandardTradingSchedule;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.time.Time;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

class FileEventProcessorTest {
    private static final Instant TIMESTAMP = ZonedDateTime.of(LocalDate.of(2016, Month.NOVEMBER, 9),
                                                              LocalTime.of(5,
                                                                     6,
                                                                     30,
                                                                     653_000),
                                                              ZoneOffset.ofHours(8)).toInstant();

    private static final MarketEvent AddAsk = new MarketEvent(TIMESTAMP,
                                                              EventType.ADD_ORDER,
                                                              12345,
                                                              500,
                                                              10,
                                                              BidAsk.BID,
                                                              MarketEvent.Type.UNKNOWN_ORDER_TYPE);

    private static final MarketEvent AddBid =
            MarketEventParser.parse("20161109100000002,330,8458146,112,490,1,0,2,0,18,,,,,,,,,,HSIX6");
    private static final MarketEvent AddTrade =
            MarketEventParser
                    .parse("20161109100000003,350,8458146,12345,500,1,,,,,xxx,0,3,3,0,0,20161109091400020,,,HSIX6");
    private static final MarketEvent DeleteOrder =
            MarketEventParser.parse("20161109100000004,332,8458146,112,,,0,,,,,,,,,,,,,HSIX6");

    private TradingDay tradingDay;
    private FileEventProcessor eventProcessor;

    @BeforeAll
    static void initialiseTimeZone() {
        Time.setTradingSchedule(StandardTradingSchedule.wrap(List.of(), ZoneOffset.ofHours(8)));
    }

    @BeforeEach
    void init() {
        tradingDay = new TradingDay();
        eventProcessor = new FileEventProcessor(tradingDay, ProcessingMode.ALL);
    }

    @Test
    void testProcessExchangeMessage() {
        Time.setTradingSchedule(StandardTradingSchedule.wrap(List.of(), ZoneOffset.ofHours(8)));

        eventProcessor.processExchangeMessage(copy(AddAsk));
        final String result1 = tradingDay.getCurrentBook().toString();

        eventProcessor.processExchangeMessage(copy(AddBid));
        final String result2 = tradingDay.getCurrentBook().toString();

        eventProcessor.processExchangeMessage(copy(AddTrade));
        final String result3 = tradingDay.getCurrentBook().toString();

        eventProcessor.processExchangeMessage(copy(DeleteOrder));
        final String result4 = tradingDay.getCurrentBook().toString();
        System.out.println(AddAsk);
        System.out.println(AddBid);

        System.out.println(result1);
        System.out.println(result2);
        System.out.println(result3);
        System.out.println(result4);
    }

    private static MarketEvent copy(MarketEvent event) {
        return new MarketEvent(event.getEventTimestamp(),
                               event.getMessageType(),
                               event.getOrderId(),
                               event.getPrice(),
                               event.getOrderQuantity(),
                               event.getSide(),
                               event.getOrderType());
    }
}
