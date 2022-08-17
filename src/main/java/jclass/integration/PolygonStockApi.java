package jclass.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jclass.dto.StockHistoricalQuote;
import jclass.dto.StockSplitResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;

public class PolygonStockApi {
    private static final Logger log = LoggerFactory.getLogger(PolygonStockApi.class);

    private static final String baseURL = "https://api.polygon.io";
    private static final String apiKey = "FyOyupaDcv4U7nbcZdc4tlPqp7lIyE7J";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static StockSplitResponse getListStockSplits(String symbol) {

        int retryApiCallCount = 0;
        while (retryApiCallCount < 5) {
            try {
                Document stockSplitApiResponse = Jsoup.connect("https://api.polygon.io/v2/reference/splits/" + symbol + "?apiKey=" + apiKey)
                        .ignoreContentType(true).get();
                String json = stockSplitApiResponse.body().text();
//        String json = "{\"status\":\"OK\",\"count\":1,\"results\":[{\"ticker\":\"ZME\",\"exDate\":\"2021-12-23\",\"paymentDate\":\"2021-12-23\",\"declaredDate\":\"2021-12-23\",\"ratio\":8,\"tofactor\":8,\"forfactor\":1}]}\n";
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(json, StockSplitResponse.class);
            } catch (IOException e) {
                ++retryApiCallCount;
                //log.error("Api Call: {} with symbol {}, Failed with Error: {}", "StockSplitResponse", symbol, e);
            }
        }
        return null;
    }

    public static StockHistoricalQuote getGroupedHistory(String stockSymbol, int historyOfDays, int rangeOfAggHistory) {
        try {
            String json = getGroupedHistoryResponseJson(stockSymbol, historyOfDays, rangeOfAggHistory);
            return objectMapper.readValue(json, StockHistoricalQuote.class);
        } catch (IOException e) {
            //log.error("Error while parsing json data Api Call: {} with symbol {}, Failed with Error: {}", "StockHistoricalQuote", stockSymbol, e);
        }
        return null;
    }

    public static StockHistoricalQuote getGroupedHistory(String stockSymbol, LocalDate fromDate, LocalDate toDate, int rangeOfAggHistory) {
        try {
            String json = getGroupedHistoryResponseJson(stockSymbol, fromDate, toDate, rangeOfAggHistory);
            return objectMapper.readValue(json, StockHistoricalQuote.class);
        } catch (IOException e) {
            //log.error("Error while parsing json data Api Call: {} with symbol {}, Failed with Error: {}", "StockHistoricalQuote", stockSymbol, e);
        }
        return null;
    }

    public static String getGroupedHistoryResponseJson(String stockSymbol, int historyOfDays, int rangeOfAggHistory) {
        return getGroupedHistoryResponseJson(stockSymbol, LocalDate.now().minusDays(historyOfDays), LocalDate.now(), rangeOfAggHistory);
    }

    public static String getGroupedHistoryResponseJson(String stockSymbol, LocalDate fromDate, LocalDate toDate, int rangeOfAggHistory) {

        String resourcePath = baseURL + "/v2/aggs/ticker/" + stockSymbol + "/range/" + rangeOfAggHistory + "/day/"
                + fromDate + "/" + toDate + "?apiKey=" + apiKey;

        int retryApiCallCount = 0;

        while (retryApiCallCount < 5) {
            try {
                Document stockHistoricalQuoteJsonResponse = Jsoup.connect(resourcePath).ignoreContentType(true).get();
                String json = stockHistoricalQuoteJsonResponse.body().text();
                //log.info(json);
                return json;
            } catch (IOException e) {
                ++retryApiCallCount;
                //log.error("Api Call: {} with symbol {}, Failed with Error: {}", "StockHistoricalQuote", stockSymbol, e);
            }
        }
        return null;
    }
}
