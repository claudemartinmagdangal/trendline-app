package jclass.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockHistoricalQuote implements Serializable {
    private boolean adjusted;
    private Long queryCount;

    @JsonProperty("request_id")
    private String requestId;

    private List<HistoricalQuote> results;
    private String status;
    private Long resultsCount;
    private Long count;
    private String ticker;

    private String symbol;
    private String name;
    private String currency;
    private String stockExchange;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalQuote implements Serializable {
        @JsonProperty("c")
        private Double closPrice;
        @JsonProperty("h")
        private Double highestPrice;
        @JsonProperty("l")
        private Double lowestPrice;
        @JsonProperty("n")
        private Long nofTransactions;
        @JsonProperty("o")
        private Double openPrice;
        @JsonProperty("t")
        private Long dateTime;
        @JsonProperty("v")
        private Long volume;
        @JsonProperty("vw")
        private Double volumeWeightedAveragePrice;

        @JsonIgnore
        public Date getUtilDateTime() {
            Instant instant = Instant.ofEpochSecond(dateTime / 1000);
            Date date = Date.from(instant);
            return date;
        }

    }
}