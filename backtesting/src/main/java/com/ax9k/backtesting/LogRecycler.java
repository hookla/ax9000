package com.ax9k.backtesting;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.Milestone;
import com.ax9k.core.marketmodel.Trade;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import com.ax9k.core.time.Time;
import com.ax9k.provider.MarketDataProvider;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static com.ax9k.core.marketmodel.BidAsk.ASK;
import static com.ax9k.core.marketmodel.BidAsk.BID;
import static com.ax9k.core.marketmodel.StandardMilestone.wrap;

public class LogRecycler implements MarketDataProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Pattern OBJECT_END = Pattern.compile("^}[,]?[\\s]*$");
    private static final char OBJECT_START = '{';
    private static final int COUNTER_CHECKPOINT = 10_000;

    private final TradingDay tradingDay;
    private final BufferedReader bookLines;
    private final BufferedReader tradeLines;
    private final EventReplay<OhlcvBar> barEvents;
    private final OhlcvBarAggregator aggregator;
    private final LocalDate date;
    private final String source;
    private boolean tradesPrettyPrinted;
    private boolean booksPrettyPrinted;
    private Consumer<OhlcvBar> sendCompleteBarEvent;

    LogRecycler(TradingDay tradingDay,
                Path bookLog,
                Path tradeLog,
                Path barLog) {
        source = bookLog.getFileName().toString();
        this.tradingDay = tradingDay;

        try {
            barEvents = barLog != null ? EventReplay.of(new BarLogRecycler(),
                                                        createBuffer(barLog),
                                                        OhlcvBarAggregator.INPUT_BAR_PERIOD_LENGTH) :
                        null;
            aggregator = new OhlcvBarAggregator();
            sendCompleteBarEvent = barEvent -> {
                OhlcvBar completeBar = aggregator.aggregateData(barEvent);
                if (completeBar != null) {
                    tradingDay.bar(completeBar);
                }
            };

            bookLines = createBuffer(bookLog);
            tradeLines = createBuffer(tradeLog);
            String booksDate = bookLines.readLine();
            String tradesDate = tradeLines.readLine();

            if (booksDate == null) {
                throw new IllegalArgumentException("bookLog file is empty");
            }
            if (tradesDate == null) {
                throw new IllegalArgumentException("tradeLog file is empty");
            }

            if (!booksDate.equals(tradesDate)) {
                throw new IllegalArgumentException("logs are not from the same day");
            }

            date = LocalDate.parse(booksDate);
        } catch (IOException unhandleable) {
            throw new UncheckedIOException("Error opening input file", unhandleable);
        }
    }

    private BufferedReader createBuffer(Path file) throws IOException {
        if (!file.toString().endsWith("gz")) {
            return Files.newBufferedReader(file);
        }

        InputStream stream = Files.newInputStream(file, StandardOpenOption.READ);
        GZIPInputStream unzipper = new GZIPInputStream(stream);
        InputStreamReader reader = new InputStreamReader(unzipper);
        return new BufferedReader(reader);
    }

    private static Instant addDate(Milestone milestone, LocalDate date) {
        return Time.internationalise(LocalDateTime.of(date, milestone.getTime()));
    }

    private void parseLogs() throws IOException {
        Milestone marketOpen = Time.schedule().marketOpen().getStart();
        tradingDay.getHeartBeat().setLastHeartBeat(addDate(marketOpen, date));

        Instant lastEventTimestamp;
        Trade currentTrade = nextTrade();
        OrderBook currentBook;
        Counter eventCounter = new Counter();
        Counter checkpointCounter = new Counter(COUNTER_CHECKPOINT);

        StopWatch stopWatch = StopWatch.createStarted();
        while ((currentBook = nextBook()) != null) {
            lastEventTimestamp = currentBook.getTimestamp();

            if (barEvents != null) {
                barEvents.replayUntil(lastEventTimestamp)
                         .forEach(sendCompleteBarEvent);
            }

            tradingDay.getHeartBeat().massageHeart(lastEventTimestamp);
            if (currentTrade != null && currentTrade.getTimestamp().equals(lastEventTimestamp)) {
                tradingDay.trade(currentTrade, currentBook);
            } else {
                tradingDay.orderBook(currentBook);
            }

            if (checkpointCounter.checkpointReached()) {
                long timeMicros = Math.max(1, stopWatch.getTime(TimeUnit.MICROSECONDS));
                int rate = (int) Math.round(COUNTER_CHECKPOINT / (timeMicros / 1_000_000d));

                double totalMemory = Runtime.getRuntime().totalMemory();
                double usedMemory = totalMemory - Runtime.getRuntime().freeMemory();
                LOGGER.info("Checkpoint. Last event: {}. Events processed: {}@{}/sec. {}/{} MB memory in use.",
                            tradingDay.getLocalisedLastEventTime(),
                            eventCounter.getCount(),
                            rate,
                            toMegabytes(usedMemory),
                            toMegabytes(totalMemory));
                stopWatch.reset();
                stopWatch.start();
            }
            checkpointCounter.increment();
            eventCounter.increment();
        }
        Milestone marketClose = Time.schedule().marketClose().getStart();
        if (marketClose == null) {
            marketClose = wrap(LocalTime.of(23, 59));
        }
        tradingDay.getHeartBeat().massageHeart(addDate(marketClose, date));
    }

    private long toMegabytes(double bytes) {
        return Math.round(bytes / 1024 / 1024);
    }

    private Trade nextTrade() throws IOException {
        JsonNode node = readTradeObject(tradeLines);
        try {
            return node != null ? mapToTrade(node) : null;
        } catch (NullPointerException invalidTrade) {
            throw new IllegalArgumentException("Invalid trade object: " + node, invalidTrade);
        }
    }

    private OrderBook nextBook() throws IOException {
        JsonNode node = readBookObject(bookLines);
        try {
            return node != null ? mapToBook(node) : null;
        } catch (NullPointerException invalidBook) {
            throw new IllegalArgumentException("Invalid book object: " + node, invalidBook);
        }
    }

    private JsonNode readTradeObject(BufferedReader reader) throws IOException {
        if (tradesPrettyPrinted) {
            return readPrettyPrinted(reader);
        } else {
            try {
                return readSingleLine(reader);
            } catch (JsonParseException expected) {
                tradesPrettyPrinted = true;
                return readPrettyPrintedWithStart(reader);
            } catch (IOException unhandleable) {
                throw new UncheckedIOException("Error reading book log", unhandleable);
            }
        }
    }

    private JsonNode readBookObject(BufferedReader reader) throws IOException {
        if (booksPrettyPrinted) {
            return readPrettyPrinted(reader);
        } else {
            try {
                return readSingleLine(reader);
            } catch (JsonParseException expected) {
                booksPrettyPrinted = true;
                return readPrettyPrintedWithStart(reader);
            } catch (IOException unhandleable) {
                throw new UncheckedIOException("Error reading book log", unhandleable);
            }
        }
    }

    private JsonNode readSingleLine(BufferedReader reader) throws IOException {
        String json = reader.readLine();
        return json != null ? JSON_MAPPER.readTree(json) : null;
    }

    private JsonNode readPrettyPrinted(BufferedReader reader) throws IOException {
        return readPrettyPrinted(reader, new StringBuilder(100));
    }

    private JsonNode readPrettyPrintedWithStart(BufferedReader reader) throws IOException {
        return readPrettyPrinted(reader, new StringBuilder(100).append(OBJECT_START));
    }

    private JsonNode readPrettyPrinted(BufferedReader reader, StringBuilder jsonObject) throws IOException {
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            currentLine = currentLine.trim();
            jsonObject.append(currentLine);
            if (OBJECT_END.matcher(currentLine).matches()) {
                return JSON_MAPPER.readTree(jsonObject.toString());
            }
        }

        if (!jsonObject.toString().trim().isEmpty()) {
            LOGGER.warn("Incomplete JSON object: {}", jsonObject);
        }

        return null;
    }

    private Trade mapToTrade(JsonNode root) {
        Instant eventTimestamp = parseEventTimestamp(root);
        double price = root.get("price").asDouble();
        double quantity = root.get("quantity").asDouble();
        long orderId = root.get("uniqueOrderId").asLong();
        BidAsk side = tradingDay.getCurrentBook().getMid() >= price ? BID : ASK;
        return new Trade(eventTimestamp, price, quantity, orderId, side);
    }

    private OrderBook mapToBook(JsonNode root) {
        Instant eventTimestamp = parseEventTimestamp(root);
        EventType type = Enum.valueOf(EventType.class, root.get("type").asText());
        OrderBookLevel[] bids = mapToBookLevels(root.get("bids"));
        OrderBookLevel[] asks = mapToBookLevels(root.get("asks"));

        return new OrderBook(eventTimestamp, type, asks, bids);
    }

    private Instant parseEventTimestamp(JsonNode root) {
        LocalTime eventTime = LocalTime.parse(root.get("eventTime").asText());
        return Time.internationalise(LocalDateTime.of(date, eventTime));
    }

    private OrderBookLevel[] mapToBookLevels(JsonNode levelsNode) {
        List<OrderBookLevel> levels = new ArrayList<>(10);

        Iterator<JsonNode> elements = levelsNode.elements();
        while (elements.hasNext()) {
            JsonNode levelNode = elements.next();
            double price = levelNode.get("price").asDouble();
            double quantity = levelNode.get("quantity").asDouble();
            levels.add(new OrderBookLevel(price, quantity));
        }

        return levels.toArray(new OrderBookLevel[0]);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void startRequest(boolean delayUntilMarketOpen) {
        try {
            parseLogs();
        } catch (IOException unhandleable) {
            throw new UncheckedIOException(unhandleable);
        }
    }

    @Override
    public void stopRequest() {
    }

    @Override
    public String getSource() {
        return source;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getSymbol() {
        return source;
    }
}
