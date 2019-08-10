package com.ax9k.interactivebrokers.broker;

import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.utils.json.JsonUtils;
import com.ib.client.ContractDetails;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

import static com.ax9k.interactivebrokers.broker.IbTradingHoursParser.createTradingSchedule;

public class IbContract implements Contract {
    private final TradingSchedule tradingSchedule;
    private final ZoneId timeZone;
    private final String marketName;
    private final String longName;
    private final String localSymbol;
    private final String exchange;
    private final String brokerContractId;
    private final int multiplier;
    private final double costPerTrade;

    private IbContract(TradingSchedule tradingSchedule,
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

    static IbContract fromDetails(ContractDetails details) {
        int multiplier = 1;
        if (details.contract().multiplier() != null) {
            multiplier = Integer.valueOf(details.contract().multiplier());
        }

        ZoneId timeZone = parseZoneId(details.timeZoneId());
        // TODO work out how to get this from IB....
        double costPerTrade = 30;
        return new IbContract(createTradingSchedule(details.tradingHours(), LocalDateTime.now(timeZone), timeZone),
                              timeZone,
                              details.marketName(),
                              details.longName(),
                              details.contract().localSymbol(),
                              details.contract().exchange(),
                              String.valueOf(details.conid()),
                              multiplier,
                              costPerTrade);
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
