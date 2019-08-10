package com.ax9k.interactivebrokers.broker;

import com.ax9k.core.marketmodel.Milestone;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.StandardPhase;
import com.ax9k.core.marketmodel.StandardTradingSchedule;
import com.ax9k.core.marketmodel.TradingSchedule;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.ax9k.core.marketmodel.StandardMilestone.wrap;
import static com.ax9k.utils.compare.ComparableUtils.greaterThanOrEqual;

class IbTradingHoursParser {
    public static final String SESSION_SEPARATOR = ";";
    private static final DateTimeFormatter MILESTONE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd:HHmm");
    private static final String CLOSE_INDICATOR = "CLOSED";
    private static final String START_END_SEPARATOR = "-";

    private IbTradingHoursParser() {
        throw new AssertionError("IbTradingHoursParser is not instantiable");
    }

    static TradingSchedule createTradingSchedule(String tradingHours, LocalDateTime now, ZoneId timeZone) {
        Validate.notBlank(tradingHours, "No trading hours information");
        Validate.notNull(now);
        Validate.notNull(timeZone);

        List<Phase> phases;

        try {
            phases = parseTradingSessions(tradingHours, now.toLocalDate());
            phases = fillInGapsBetweenSessions(phases, now.toLocalTime());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Error parsing trading hours: " + tradingHours, e);
        }

        return StandardTradingSchedule.wrap(phases, timeZone);
    }

    private static List<Phase> parseTradingSessions(String tradingHours, LocalDate today) {
        tradingHours = StringUtils.removeStart(tradingHours, SESSION_SEPARATOR);

        List<Phase> tradingSessions = new ArrayList<>();
        boolean afterClose = false;
        String[] sessions = tradingHours.split(SESSION_SEPARATOR);
        for (String session : sessions) {
            if (isCloseIndicator(session)) {
                afterClose = true;
                continue;
            }

            LocalDateTime[] milestones = parseMilestones(session);

            if (milestones[0].toLocalDate().isAfter(today)) {
                return tradingSessions;
            } else if (milestones[1].toLocalDate().isAfter(today)) {
                tradingSessions.add(createTradingSession(milestones, afterClose, tradingSessions.size()));
                return tradingSessions;
            } else {
                tradingSessions.add(createTradingSession(milestones, afterClose, tradingSessions.size()));
            }
        }

        return tradingSessions;
    }

    private static Phase createTradingSession(LocalDateTime[] milestones, boolean afterClose, int index) {
        return new StandardPhase(wrap(milestones[0].toLocalTime()),
                                 wrap(milestones[1].toLocalTime()),
                                 "session_" + index,
                                 true,
                                 !afterClose,
                                 afterClose);
    }

    private static boolean isCloseIndicator(String milestone) {
        return milestone.contains(CLOSE_INDICATOR);
    }

    private static LocalDateTime[] parseMilestones(String milestones) {
        return Arrays.stream(milestones.split(START_END_SEPARATOR))
                     .map(milestone -> LocalDateTime.parse(milestone, MILESTONE_FORMAT))
                     .toArray(LocalDateTime[]::new);
    }

    private static List<Phase> fillInGapsBetweenSessions(List<Phase> tradingSessions, LocalTime now) {
        Phase originalFirstSession = tradingSessions.get(0);

        Phase preOpenPeriod = null;
        if (phaseWrapsIntoNewDay(originalFirstSession)) {
            if (greaterThanOrEqual(now, originalFirstSession.getStart().getTime())) {
                /*Trading day is already ending*/
                Phase restOfDay = createCloseSession(originalFirstSession.getEnd(),
                                                     originalFirstSession.getStart(),
                                                     "trading_stopped");
                return List.of(restOfDay, originalFirstSession);
            } else if (tradingSessions.size() == 2 || tradingSessions.size() == 1) {
                Phase restOfDay = createCloseSession(originalFirstSession.getEnd(),
                                                     originalFirstSession.getStart(),
                                                     "close");
                return List.of(originalFirstSession, restOfDay);
            }

            Phase lastSession = tradingSessions.get(tradingSessions.size() - 1);
            Validate.isTrue(originalFirstSession.getStart().equals(lastSession.getStart()) &&
                            originalFirstSession.getEnd().equals(lastSession.getEnd()),
                            "Trading sessions wrap into new day but do not match up to complete the cycle. " +
                            "First: %s, Last: %s", originalFirstSession, lastSession);

            tradingSessions.remove(originalFirstSession);
            Phase newFirstSession = tradingSessions.get(0);

            preOpenPeriod = createPreOpenSession(originalFirstSession.getEnd(), newFirstSession.getStart());
        } else if (originalFirstSession.getStart().getTime().isAfter(LocalTime.MIDNIGHT)) {
            preOpenPeriod = createPreOpenSession(wrap(LocalTime.MIDNIGHT), originalFirstSession.getStart());
        }

        List<Phase> breaks = createBreakPeriods(tradingSessions);
        Phase closePeriod = createClosePeriod(tradingSessions);

        List<Phase> allPhases = new ArrayList<>();

        if (preOpenPeriod != null) {
            allPhases.add(preOpenPeriod);
        }

        allPhases.addAll(tradingSessions);
        allPhases.addAll(breaks);

        if (closePeriod != null) {
            allPhases.add(closePeriod);
        }

        allPhases.sort(Comparator.comparing(phase -> phase.getStart().getTime()));

        return allPhases;
    }

    private static List<Phase> createBreakPeriods(List<Phase> tradingSessions) {
        List<Phase> breaks = new ArrayList<>();
        for (int i = 1; i < tradingSessions.size(); i++) {
            Phase current = tradingSessions.get(i);
            Phase previous = tradingSessions.get(i - 1);

            Phase breakPeriod = createBreakSession(previous.getEnd(),
                                                   current.getStart(),
                                                   breaks.size(),
                                                   current.isAfterMarketClose());

            breaks.add(breakPeriod);
        }
        return breaks;
    }

    private static Phase createBreakSession(Milestone start, Milestone end, int index, boolean afterClose) {
        return new StandardPhase(start, end,
                                 "break" + index,
                                 false,
                                 !afterClose,
                                 afterClose);
    }

    private static Phase createClosePeriod(List<Phase> tradingSessions) {
        Phase lastSession = tradingSessions.get(tradingSessions.size() - 1);
        Phase closePeriod = null;
        if (!phaseWrapsIntoNewDay(lastSession)) {
            closePeriod = createCloseSession(lastSession.getEnd(), wrap(LocalTime.MIDNIGHT), "close");
        }
        return closePeriod;
    }

    private static Phase createPreOpenSession(Milestone start, Milestone end) {
        return new StandardPhase(start, end,
                                 "pre_open",
                                 false,
                                 false,
                                 false);
    }

    private static Phase createCloseSession(Milestone start, Milestone end, String name) {
        return new StandardPhase(start,
                                 end,
                                 name,
                                 false,
                                 false,
                                 true);
    }

    private static boolean phaseWrapsIntoNewDay(Phase phase) {
        LocalTime start = phase.getStart().getTime();
        LocalTime end = phase.getEnd().getTime();

        return !end.equals(LocalTime.MIDNIGHT) && start.isAfter(end);
    }
}
