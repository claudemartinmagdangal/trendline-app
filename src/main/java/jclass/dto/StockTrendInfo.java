package jclass.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

@Data
@NoArgsConstructor
public class StockTrendInfo implements Serializable {

    private int index;
    private String symbol;
    private String company;
    private String industry;
    private double maxPercentageChangeUpwardTrend;
    private double maxPercentageChangeDownWardTrend;
    private int maxUpwardTrendCount;
    private int maxDownwardTrendCount;
    private List<Long> upwardTrendDistances;//trend distance from last trading date
    private List<Double> upwardTrendDistanceSlopInPercentage;
    private int totalUpwardTrendDistance;
    private List<Long> downwardTrendDistances; //trend distance from last trading date
    private List<Double> downwardTrendDistanceSlopInPercentage;
    private int totalDownwardTrendDistance;
    private int sortingIndex;

    private double open;
    private double high;
    private double low;
    private double close;
    private double volumn;

    private int insidertrading;
    private int price;
    private int newsstory;
    private HashMap<Long, Integer> daySlopIndexForSort;

    public StockTrendInfo(String symbol) {
        this.symbol = symbol;
        this.company = "";
        this.industry = "";
        this.maxPercentageChangeUpwardTrend = 0;
        this.maxPercentageChangeDownWardTrend = 0;
        this.maxUpwardTrendCount = 0;
        this.maxDownwardTrendCount = 0;
        this.upwardTrendDistances = new ArrayList<>();
        this.upwardTrendDistanceSlopInPercentage = new ArrayList<>();
        this.downwardTrendDistanceSlopInPercentage = new ArrayList<>();
        this.totalUpwardTrendDistance = 0;
        this.totalDownwardTrendDistance = 0;
        this.downwardTrendDistances = new ArrayList<>();
        this.daySlopIndexForSort = new HashMap<>();
        this.sortingIndex = 0;
        this.insidertrading = 0;
        this.price = 0;
        this.newsstory = 0;
        this.open = 0;
        this.high = 0;
        this.low = 0;
        this.close = 0;
        this.volumn = 0;
    }

    public static class SortByUpwardTrendDistanceSlopInPercentageBySortingIndex implements Comparator<StockTrendInfo> {
        public int compare(StockTrendInfo firstStockTrendInfo, StockTrendInfo secondStockTrendInfo) {
            return Double.compare(firstStockTrendInfo.getUpwardTrendDistanceSlopInPercentage().get(firstStockTrendInfo.getSortingIndex()), secondStockTrendInfo.getUpwardTrendDistanceSlopInPercentage().get(secondStockTrendInfo.getSortingIndex()));
        }
    }

    public static class SortByDownwardTrendDistanceSlopInPercentageBySortingIndex implements Comparator<StockTrendInfo> {
        public int compare(StockTrendInfo firstStockTrendInfo, StockTrendInfo secondStockTrendInfo) {
            return Double.compare(firstStockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(firstStockTrendInfo.getSortingIndex()), secondStockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(secondStockTrendInfo.getSortingIndex()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockTrendInfo that = (StockTrendInfo) o;
        return symbol.equals(that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return "StockTrendInfo{" +
                "symbol='" + symbol + '\'' +
                '}';
    }
}
