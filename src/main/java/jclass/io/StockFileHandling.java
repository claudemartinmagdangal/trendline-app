package jclass.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import jclass.dto.StockHistoricalQuote;
import jclass.dto.StockSplitResponse;
import jclass.dto.Symbol;
import jclass.integration.PolygonStockApi;
import jclass.util.ReadExcel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockFileHandling {
    private static final Logger log = LoggerFactory.getLogger(StockFileHandling.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Integer totalStock = 0;
    private Integer downloadedStock = 0;
    private Integer downloadFailStock = 0;

    public static List<Symbol> getAllStockSymbolsFromExcel() {
        return ReadExcel.readExcel();
    }

    public void downloadStocksHistoryIntoFiles(int historyOfDays) {

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(100);

        try {
            List<Symbol> allStockSymbols = getAllStockSymbolsFromExcel();

            if (allStockSymbols != null) {
//                allStockSymbols = new String[]{"ZME"};
                for (Symbol symbol : allStockSymbols) {
                    File file = new File("downloads");
                    if (!file.exists())
                        file.mkdir();

                    executorService.schedule(() -> {
                        try {
                            Path path = Paths.get("downloads\\" + symbol.getSymbol().trim() + ".txt");

                            if (Files.notExists(path)) {
                                Stock stock = YahooFinance.get(symbol.getSymbol());
                                if (stock != null) {
                                    String stockInfoJson = "{\"symbol\":\"" + stock.getSymbol() + "\",\"name\":\"" + stock.getName() + "\",\"currency\":\"" + stock.getCurrency() + "\",\"stockExchange\":\"" + stock.getStockExchange() + "\",";
                                    //download complete history of days
                                    //create new file to save that stock
                                    String stockHistoricalQuote = PolygonStockApi.getGroupedHistoryResponseJson(symbol.getSymbol(), historyOfDays, 1);
                                    //String stockHistoricalQuote = "{\"ticker\":\"ZME\",\"queryCount\":10,\"resultsCount\":10,\"adjusted\":true,\"results\":[{\"v\":84670,\"vw\":6.2395,\"o\":8.11,\"c\":6.05,\"h\":8.11,\"l\":5.39,\"t\":1640235600000,\"n\":1355},{\"v\":83176,\"vw\":6.754,\"o\":6.28,\"c\":7,\"h\":7.29,\"l\":6.18,\"t\":1640581200000,\"n\":1177},{\"v\":25899,\"vw\":6.4849,\"o\":6.92,\"c\":6.44,\"h\":6.92,\"l\":6.32,\"t\":1640667600000,\"n\":461},{\"v\":62958,\"vw\":5.663,\"o\":6.35,\"c\":5.15,\"h\":6.35,\"l\":5.0756,\"t\":1640754000000,\"n\":580},{\"v\":24413,\"vw\":5.1922,\"o\":5.04,\"c\":5.16,\"h\":5.4499,\"l\":5.02,\"t\":1640840400000,\"n\":299},{\"v\":34269,\"vw\":4.6746,\"o\":5,\"c\":4.52,\"h\":5.07,\"l\":4.52,\"t\":1640926800000,\"n\":422},{\"v\":18704,\"vw\":4.6075,\"o\":4.66,\"c\":4.52,\"h\":4.71,\"l\":4.52,\"t\":1641186000000,\"n\":238},{\"v\":832669,\"vw\":2.7532,\"o\":3.71,\"c\":2.8,\"h\":3.72,\"l\":2.28,\"t\":1641272400000,\"n\":4636},{\"v\":1.692512e+06,\"vw\":2.7945,\"o\":2.54,\"c\":2.83,\"h\":3.2,\"l\":2.2802,\"t\":1641358800000,\"n\":4828},{\"v\":1.652099e+06,\"vw\":2.6562,\"o\":2.73,\"c\":2.38,\"h\":3.03,\"l\":2.26,\"t\":1641445200000,\"n\":4560}],\"status\":\"DELAYED\",\"request_id\":\"3aafeadd299f59fb27f1a458c5a8a0cd\",\"count\":10}";

                                    stockHistoricalQuote = stockInfoJson + stockHistoricalQuote.substring(1);
                                    FileUtils.writeDataOnFile(path, stockHistoricalQuote);
                                    StockHistoricalQuote stockHistoricalQuoteResponse = objectMapper.readValue(stockHistoricalQuote, StockHistoricalQuote.class);

                                    downloadedStock++;
                                } else {
                                    downloadFailStock++;
                                }
                            } else {
                                StockSplitResponse splitResponse = PolygonStockApi.getListStockSplits(symbol.getSymbol());
                                if (splitResponse != null && splitResponse.getResults() != null && !splitResponse.getResults().isEmpty()) {
                                    LocalDate exDate = splitResponse.getResults().get(0).getExDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                                    if (LocalDate.now().compareTo(exDate) == 0) {
                                        //todo just delete the file and download whole data
                                        String previousStoredHistory = FileUtils.readData(path);
                                        if (previousStoredHistory != null && !previousStoredHistory.isBlank()) {
                                            ObjectMapper objectMapper = new ObjectMapper();
                                            StockHistoricalQuote previousStockHistoricalQuoteFromFile = objectMapper.readValue(previousStoredHistory, StockHistoricalQuote.class);
                                            LocalDate historyFromDate = previousStockHistoricalQuoteFromFile.getResults().get(previousStockHistoricalQuoteFromFile.getResults().size() - 1).getUtilDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                                            Stock stock = YahooFinance.get(symbol.getSymbol());
                                            String stockInfoJson = "{\"symbol\":\"" + stock.getSymbol() + "\",\"name\":\"" + stock.getName() + "\",\"currency\":\"" + stock.getCurrency() + "\",\"stockExchange\":\"" + stock.getStockExchange() + "\",";

                                            //as stock split occurred rewrite all history with complete new adjusted history
                                            String stockHistoricalQuote = PolygonStockApi.getGroupedHistoryResponseJson(symbol.getSymbol(), historyFromDate, LocalDate.now(), 1);
                                            if (stockHistoricalQuote != null) {
                                                stockHistoricalQuote = stockInfoJson + stockHistoricalQuote.substring(1);

                                                //String stockHistoricalQuote = "{\"ticker\":\"ZME\",\"queryCount\":10,\"resultsCount\":10,\"adjusted\":true,\"results\":[{\"v\":84670,\"vw\":6.2395,\"o\":8.11,\"c\":6.05,\"h\":8.11,\"l\":5.39,\"t\":1640235600000,\"n\":1355},{\"v\":83176,\"vw\":6.754,\"o\":6.28,\"c\":7,\"h\":7.29,\"l\":6.18,\"t\":1640581200000,\"n\":1177},{\"v\":25899,\"vw\":6.4849,\"o\":6.92,\"c\":6.44,\"h\":6.92,\"l\":6.32,\"t\":1640667600000,\"n\":461},{\"v\":62958,\"vw\":5.663,\"o\":6.35,\"c\":5.15,\"h\":6.35,\"l\":5.0756,\"t\":1640754000000,\"n\":580},{\"v\":24413,\"vw\":5.1922,\"o\":5.04,\"c\":5.16,\"h\":5.4499,\"l\":5.02,\"t\":1640840400000,\"n\":299},{\"v\":34269,\"vw\":4.6746,\"o\":5,\"c\":4.52,\"h\":5.07,\"l\":4.52,\"t\":1640926800000,\"n\":422},{\"v\":18704,\"vw\":4.6075,\"o\":4.66,\"c\":4.52,\"h\":4.71,\"l\":4.52,\"t\":1641186000000,\"n\":238},{\"v\":832669,\"vw\":2.7532,\"o\":3.71,\"c\":2.8,\"h\":3.72,\"l\":2.28,\"t\":1641272400000,\"n\":4636},{\"v\":1.692512e+06,\"vw\":2.7945,\"o\":2.54,\"c\":2.83,\"h\":3.2,\"l\":2.2802,\"t\":1641358800000,\"n\":4828},{\"v\":1.652099e+06,\"vw\":2.6562,\"o\":2.73,\"c\":2.38,\"h\":3.03,\"l\":2.26,\"t\":1641445200000,\"n\":4560}],\"status\":\"DELAYED\",\"request_id\":\"3aafeadd299f59fb27f1a458c5a8a0cd\",\"count\":10}";
                                                FileUtils.writeDataOnFile(path, stockHistoricalQuote);

                                                downloadedStock++;
                                            }
                                        }

                                    } else if (LocalDate.now().compareTo(exDate) > 0) {
                                        //add today stock result the stock history
                                        StockHistoricalQuote stockHistoricalQuote = PolygonStockApi.getGroupedHistory(symbol.getSymbol(), 0, 1);
                                        //String json = "{\"ticker\":\"ZME\",\"queryCount\":10,\"resultsCount\":10,\"adjusted\":true,\"results\":[{\"v\":84670,\"vw\":6.2395,\"o\":8.11,\"c\":6.05,\"h\":8.11,\"l\":5.39,\"t\":1640235600000,\"n\":1355},{\"v\":83176,\"vw\":6.754,\"o\":6.28,\"c\":7,\"h\":7.29,\"l\":6.18,\"t\":1640581200000,\"n\":1177},{\"v\":25899,\"vw\":6.4849,\"o\":6.92,\"c\":6.44,\"h\":6.92,\"l\":6.32,\"t\":1640667600000,\"n\":461},{\"v\":62958,\"vw\":5.663,\"o\":6.35,\"c\":5.15,\"h\":6.35,\"l\":5.0756,\"t\":1640754000000,\"n\":580},{\"v\":24413,\"vw\":5.1922,\"o\":5.04,\"c\":5.16,\"h\":5.4499,\"l\":5.02,\"t\":1640840400000,\"n\":299},{\"v\":34269,\"vw\":4.6746,\"o\":5,\"c\":4.52,\"h\":5.07,\"l\":4.52,\"t\":1640926800000,\"n\":422},{\"v\":18704,\"vw\":4.6075,\"o\":4.66,\"c\":4.52,\"h\":4.71,\"l\":4.52,\"t\":1641186000000,\"n\":238},{\"v\":832669,\"vw\":2.7532,\"o\":3.71,\"c\":2.8,\"h\":3.72,\"l\":2.28,\"t\":1641272400000,\"n\":4636},{\"v\":1.692512e+06,\"vw\":2.7945,\"o\":2.54,\"c\":2.83,\"h\":3.2,\"l\":2.2802,\"t\":1641358800000,\"n\":4828},{\"v\":1.652099e+06,\"vw\":2.6562,\"o\":2.73,\"c\":2.38,\"h\":3.03,\"l\":2.26,\"t\":1641445200000,\"n\":4560}],\"status\":\"DELAYED\",\"request_id\":\"3aafeadd299f59fb27f1a458c5a8a0cd\",\"count\":10}";
                                        //ObjectMapper objectMapperr = new ObjectMapper();
                                        //StockHistoricalQuote stockHistoricalQuote = objectMapperr.readValue(json, StockHistoricalQuote.class);

                                        if (stockHistoricalQuote != null && stockHistoricalQuote.getResults() != null && !stockHistoricalQuote.getResults().isEmpty()) {
                                            String previousStoredHistory = FileUtils.readData(path);
                                            if (previousStoredHistory != null && !previousStoredHistory.isBlank()) {
                                                ObjectMapper objectMapper = new ObjectMapper();
                                                StockHistoricalQuote previousStockHistoricalQuoteFromFile = objectMapper.readValue(previousStoredHistory, StockHistoricalQuote.class);
                                                stockHistoricalQuote.getResults().addAll(previousStockHistoricalQuoteFromFile.getResults());
                                                previousStockHistoricalQuoteFromFile.setResults(stockHistoricalQuote.getResults());
                                                previousStockHistoricalQuoteFromFile.setResultsCount(previousStockHistoricalQuoteFromFile.getResultsCount() + stockHistoricalQuote.getResultsCount());
                                                previousStockHistoricalQuoteFromFile.setCount(previousStockHistoricalQuoteFromFile.getCount() + stockHistoricalQuote.getCount());

                                                //save updated history from file
                                                String updatedStockHistoricalQuoteForFile = objectMapper.writeValueAsString(previousStockHistoricalQuoteFromFile);
                                                FileUtils.writeDataOnFile(path, updatedStockHistoricalQuoteForFile);

                                                downloadedStock++;
                                            }
                                        }
                                    }

                                }
                                //today data need to be appended in file
                                //check if today is stock split date if today is stock split date then download complete history of days and create new file
                            }
                        } catch (Exception e) {
                            downloadFailStock++;
                            //e.printStackTrace();
                        }
                    }, 500, TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public Integer getTotalStock() {
        totalStock = getAllStockSymbolsFromExcel().size() - 1;
        return totalStock;
    }

    public Integer getDownloadFailStock() {
        return downloadFailStock;
    }

}
