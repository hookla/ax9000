package com.ax9k.interactivebrokers.broker;

import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.StandardPhase;
import com.ax9k.core.marketmodel.TradingSchedule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;

import static com.ax9k.core.marketmodel.StandardMilestone.wrap;
import static java.time.LocalTime.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IbTradingHoursParserTest {
    private static final Phase[] EXPECTED_HKFE_HOURS = new Phase[] {
            preOpen(of(1, 0), of(9, 15)),
            tradingSession(of(9, 15), of(12, 0)),
            lunch(of(12, 0), of(13, 0)),
            tradingSession(of(13, 0), of(16, 30)),
            close(of(16, 30), of(17, 15)),
            afterHoursSession(of(17, 15), of(1, 0))
    };

    private static final Phase[] EXPECTED_USD_GBP_PHASES = new Phase[] {
            tradingSession(of(17, 15), of(17, 0)),
            close(of(17, 0), of(17, 15))
    };

    private static final String SAMPLE_HKFE_HOURS = "20181206:1715-20181207:0100;20181207:0915-20181207:1200;" +
                                                    "20181207:1300-20181207:1630;20181208:CLOSED;" +
                                                    "20181207:1715-20181208:0100;20181210:0915-20181210:1200;" +
                                                    "20181210:1300-20181210:1630;20181210:1715-20181211:0100;" +
                                                    "20181211:0915-20181211:1200;20181211:1300-20181211:1630;" +
                                                    "20181211:1715-20181212:0100;20181212:0915-20181212:1200;" +
                                                    "20181212:1300-20181212:1630;20181212:1715-20181213:0100;" +
                                                    "20181213:0915-20181213:1200;20181213:1300-20181213:1630;" +
                                                    "20181213:1715-20181214:0100;20181214:0915-20181214:1200;" +
                                                    "20181214:1300-20181214:1630;20181215:CLOSED;20181216:CLOSED;" +
                                                    "20181214:1715-20181215:0100;20181217:0915-20181217:1200;" +
                                                    "20181217:1300-20181217:1630;20181217:1715-20181218:0100;" +
                                                    "20181218:0915-20181218:1200;20181218:1300-20181218:1630;" +
                                                    "20181218:1715-20181219:0100;20181219:0915-20181219:1200;" +
                                                    "20181219:1300-20181219:1630;20181219:1715-20181220:0100;" +
                                                    "20181220:0915-20181220:1200;20181220:1300-20181220:1630;" +
                                                    "20181220:1715-20181221:0100;20181221:0915-20181221:1200;" +
                                                    "20181221:1300-20181221:1630;20181222:CLOSED;20181223:CLOSED;" +
                                                    "20181221:1715-20181222:0100;20181224:0915-20181224:1200;" +
                                                    "20181224:1300-20181224:1630;20181224:1715-20181225:0100;" +
                                                    "20181225:0915-20181225:1200;20181225:1300-20181225:1630;" +
                                                    "20181225:1715-20181226:0100;20181226:0915-20181226:1200;" +
                                                    "20181226:1300-20181226:1630;20181226:1715-20181227:0100;" +
                                                    "20181227:0915-20181227:1200;20181227:1300-20181227:1630;" +
                                                    "20181227:1715-20181228:0100;20181228:0915-20181228:1200;" +
                                                    "20181228:1300-20181228:1600;20181229:CLOSED;20181230:CLOSED";

    private static final String SAMPLE_USD_GBP_HOURS = "20181210:1715-20181211:1700;20181211:1715-20181212:1700;" +
                                                       "20181212:1715-20181213:1700;20181213:1715-20181214:1700;" +
                                                       "20181215:CLOSED;20181216:1715-20181217:1700;" +
                                                       "20181217:1715-20181218:1700;20181218:1715-20181219:1700;" +
                                                       "20181219:1715-20181220:1700;20181220:1715-20181221:1700;" +
                                                       "20181222:CLOSED;20181223:1715-20181224:1700;" +
                                                       "20181224:1715-20181225:1700;20181225:1715-20181226:1700;" +
                                                       "20181226:1715-20181227:1700;20181227:1715-20181228:1700;" +
                                                       "20181229:CLOSED;20181230:1715-20181231:1700;" +
                                                       "20181231:1715-20190101:1700;20190101:1715-20190102:1700;" +
                                                       "20190102:1715-20190103:1700;20190103:1715-20190104:1700;" +
                                                       "20190105:CLOSED;20190106:1715-20190107:1700;" +
                                                       "20190107:1715-20190108:1700;20190108:1715-20190109:1700;" +
                                                       "20190109:1715-20190110:1700;20190110:1715-20190111:1700;" +
                                                       "20190112:CLOSED;20190113:1715-20190114:1700";

    private static Phase preOpen(LocalTime start, LocalTime end) {
        return new StandardPhase(wrap(start),
                                 wrap(end),
                                 "pre_open",
                                 false,
                                 false,
                                 false);
    }

    private static Phase tradingSession(LocalTime start, LocalTime end) {
        return new StandardPhase(wrap(start),
                                 wrap(end),
                                 "session_?",
                                 true,
                                 true,
                                 false);
    }

    private static Phase lunch(LocalTime start, LocalTime end) {
        return new StandardPhase(wrap(start),
                                 wrap(end),
                                 "lunch",
                                 false,
                                 true,
                                 false);
    }

    private static Phase close(LocalTime start, LocalTime end) {
        return new StandardPhase(wrap(start),
                                 wrap(end),
                                 "close",
                                 false,
                                 false,
                                 true);
    }

    private static Phase afterHoursSession(LocalTime start, LocalTime end) {
        return new StandardPhase(wrap(start),
                                 wrap(end),
                                 "session_?",
                                 true,
                                 false,
                                 true);
    }

    @Test
    void shouldParseHkfeHoursProperly() {
        LocalDateTime testTime = LocalDateTime.of(2018, Month.DECEMBER, 7,
                                                  0, 30, 0, 0);
        TradingSchedule schedule = IbTradingHoursParser.createTradingSchedule(SAMPLE_HKFE_HOURS, testTime,
                                                                              ZoneOffset.ofHours(8));

        assertPhasesEqualExpected(EXPECTED_HKFE_HOURS, schedule.getPhases().toArray(new Phase[0]));
    }

    private static void assertPhasesEqualExpected(Phase[] expected, Phase[] actual) {
        assertEquals(expected.length, actual.length);

        for (int i = 0; i < actual.length; i++) {
            assertPhaseEqualsExpected(expected[i], actual[i]);
        }
    }

    private static void assertPhaseEqualsExpected(Phase expected, Phase actual) {
        assertEquals(expected, actual);
    }

    @Test
    void shouldParseUsdGbpHoursProperly() {
        LocalDateTime testTime = LocalDateTime.of(2018, Month.DECEMBER, 11,
                                                  0, 30, 0, 0);
        TradingSchedule schedule = IbTradingHoursParser.createTradingSchedule(SAMPLE_USD_GBP_HOURS, testTime,
                                                                              ZoneOffset.ofHours(-5));

        assertPhasesEqualExpected(EXPECTED_USD_GBP_PHASES, schedule.getPhases().toArray(new Phase[0]));
    }
}