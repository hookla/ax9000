package com.ax9k.app.contract;

import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.marketmodel.StandardTradingSchedule;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;

public final class JsonContract implements Contract {
    private final TradingSchedule tradingSchedule;
    private final ZoneId timeZone;
    private final String marketName;
    private final String longName;
    private final String localSymbol;
    private final String exchange;
    private final String brokerContractId;
    private final int multiplier;
    private final double costPerTrade;

    @JsonCreator
    private JsonContract(@JsonProperty(value = "phases", required = true) List<JsonPhase> phases,
                         @JsonProperty(value = "timeZoneId", required = true) String timeZoneId,
                         @JsonProperty(value = "marketName", required = true) String marketName,
                         @JsonProperty(value = "longName", required = true) String longName,
                         @JsonProperty(value = "localSymbol", required = true) String localSymbol,
                         @JsonProperty(value = "exchange", required = true) String exchange,
                         @JsonProperty(value = "contractId", required = true) String contractId,
                         @JsonProperty(value = "multiplier", required = true) int multiplier,
                         @JsonProperty(value = "costPerTrade", required = true) double costPerTrade) {
        this(StandardTradingSchedule.wrap(phases, parseZoneId(timeZoneId)),
             parseZoneId(timeZoneId),
             marketName,
             longName,
             localSymbol,
             exchange,
             contractId,
             multiplier,
             costPerTrade);
    }

    private JsonContract(TradingSchedule tradingSchedule,
                         ZoneId timeZone,
                         String marketName,
                         String longName,
                         String localSymbol,
                         String exchange,
                         String brokerContractId,
                         int multiplier,
                         double costPerTrade) {
        this.tradingSchedule = tradingSchedule;
        this.timeZone = timeZone;
        this.marketName = marketName;
        this.longName = longName;
        this.localSymbol = localSymbol;
        this.exchange = exchange;
        this.brokerContractId = brokerContractId;
        this.multiplier = Math.max(1, multiplier);
        this.costPerTrade = costPerTrade;
    }

    private static ZoneId parseZoneId(String timeZoneId) {
        try {
            return ZoneId.of(timeZoneId);
        } catch (ZoneRulesException notFound) {
            String longId = ZoneId.SHORT_IDS.get(timeZoneId);

            if (longId == null) {
                throw notFound;
            }

            return ZoneId.of(longId);
        }
    }

    public static Contract fromFile(Path file) {
        return JsonUtils.readFile(file, JsonContract.class);
    }

    @Override
    public String toString() {
        return JsonUtils.toJsonString(this);
    }

    public String getMarketName() {
        return marketName;
    }

    public String getLongName() {
        return longName;
    }

    @Override
    public String getLocalSymbol() {
        return localSymbol;
    }

    @Override
    public String getExchange() {
        return exchange;
    }

    @Override
    public double getCostPerTrade() {
        return costPerTrade;
    }

    @Override
    public int getMultiplier() { return multiplier;}

    @Override
    public ZoneId getTimeZone() {
        return timeZone;
    }

    @Override
    public TradingSchedule getTradingSchedule() {
        return tradingSchedule;
    }

    public String getBrokerContractId() {
        return brokerContractId;
    }
}
