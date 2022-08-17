package jclass.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jclass.dto.StockHistoricalQuote;
import jclass.integration.PolygonStockApi;
import jclass.io.FileUtils;
import org.jfree.data.xy.OHLCDataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;
import yahoofinance.histquotes.Interval;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;
import java.util.zip.CRC32;

import static jclass.CombinedView.LoggerManager.initializeLoggingContext;

public class Utils {

    public static final int STOCK_HISTORY_OF_DAY = 600;
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final String TEST_TOKEN = "token=";
    private static final String TOKEN = "token=";

    private static final String BASE_URL = "https://cloud.iexapis.com";
    private static final String SANDBOX_BASE_URL = "https://sandbox.iexapis.com";
    private static final String VERSION = "stable";

    private static final String TIME_SERIES_ENDPOINT = "time-series/HISTORICAL_PRICES/%s";
    private static final String BATCH_ENDPOINT = "stock/market/batch";

    private static final String BATCH_TYPES = "types=quote,chart";
    private static final String BATCH_RANGE = "range=2y";
    private static final String BATCH_SYMBOLS = "symbols=%s";

    private static final String TIME_SERIES_FROM = "from=YYYY-MM-DD";
    private static final String TIME_SERIES_FILTER = "filter=date,fopen,fclose,fhigh,flow,volume";

    private static final String SORT_ASC = "sort=ASC";
    private static final String SORT_DESC = "sort=DESC";

    private static final String BATCH_URL_EX = "https://sandbox.iexapis.com/stable/stock/market/batch?symbols=aapl,fb,tsla&types=quote,chart&range=2y&sort=ASC&token=Tpk_5f01f755af474429a66a407f69eefdb0";
    private static final String TIME_SERIES_URL_EX = "https://sandbox.iexapis.com/stable/time-series/HISTORICAL_PRICES/AAPL?filter=date,fopen,fclose,fhigh,flow,volume&from=2019-01-01&token=Tpk_5f01f755af474429a66a407f69eefdb0";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getBatchUrl(String[] symbols, boolean sandbox) {
        StringBuilder sb = new StringBuilder();
        if (sandbox) {
            sb.append(SANDBOX_BASE_URL);
        } else {
            sb.append(BASE_URL);
        }
        sb.append('/').append(VERSION);
        sb.append('/').append(BATCH_ENDPOINT);
        sb.append('?');
        if (sandbox) {
            sb.append(TEST_TOKEN);
        } else {
            sb.append(TOKEN);
        }
        sb.append('&').append(BATCH_TYPES);
        sb.append('&').append(BATCH_RANGE);
        sb.append('&').append(SORT_ASC);
        sb.append('&').append(String.format(BATCH_SYMBOLS, String.join(",", symbols)));

        return sb.toString();
    }

    private static void retrieveNewDataByBatches(List<String> symbols, boolean sandbox) {
        List<OHLCDataItem> data = new ArrayList<>(600);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectMapper objectMapper = new ObjectMapper();
        for (int i = 0; i < symbols.size(); i += 100) {
            List<String> chunk = symbols.subList(i, Math.min(i + 100, symbols.size()));
            URL url;
            try {
                url = new URL(getBatchUrl(chunk.toArray(String[]::new), sandbox));
            } catch (MalformedURLException e) {
                throw new Error(e);
            }
            JsonNode root;
            try {
                root = objectMapper.readTree(url);
            } catch (IOException e) {
                throw new Error(e);
            }

            for (Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
                JsonNode item = it.next();
                JsonNode quote = item.get("quote");
                JsonNode chart = item.get("chart");
                String symbol;
                String companyName;
                String currency;
                String primaryExchange;
                try {
                    symbol = quote.get("symbol").asText();
                    companyName = quote.get("companyName").asText();
                    currency = quote.get("currency").asText();
                    primaryExchange = quote.get("primaryExchange").asText();
                } catch (NullPointerException e) {
                    log.error("error parsing quote data", e);
                    continue;
                }
                /*System.out.println("symbol: " + symbol);
                System.out.println("companyName: " + companyName);
                System.out.println("currency: " + currency);
                System.out.println("primaryExchange: " + primaryExchange);*/
                Stock stock = new Stock(symbol);
                stock.setName(companyName);
                stock.setCurrency(currency);
                stock.setStockExchange(primaryExchange);
                data.clear();
                try {
                    for (Iterator<JsonNode> chartIt = chart.elements(); chartIt.hasNext(); ) {
                        JsonNode chartItem = chartIt.next();
                        String date = chartItem.get("date").asText();
                        double fOpen = chartItem.get("fOpen").asDouble();
                        double fClose = chartItem.get("fClose").asDouble();
                        double fHigh = chartItem.get("fHigh").asDouble();
                        double fLow = chartItem.get("fLow").asDouble();
                        double fVolume = chartItem.get("fVolume").asDouble();
                        OHLCDataItem dataItem = new OHLCDataItem(dateFormat.parse(date), fOpen, fHigh, fLow, fClose, fVolume);
                        data.add(dataItem);
                        /*System.out.println("date: " + date);
                        System.out.println("fOpen: " + fOpen);
                        System.out.println("fClose: " + fClose);
                        System.out.println("fHigh: " + fHigh);
                        System.out.println("fLow: " + fLow);
                        System.out.println("fVolume: " + fVolume);*/

                    }
                } catch (NullPointerException | ParseException e) {
                    log.error("error parsing historical data", e);
                    continue;
                }
                updateFile(new AbstractMap.SimpleEntry<>(stock, data));
                data.clear();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }
    }

    public static void main(String[] args) {
        initializeLoggingContext();
        Path symbolsPath = Paths.get("src", "main", "resources", "symbols.txt");
        List<String> symbols = new ArrayList<>(5000);

        try (Scanner scanner = new Scanner(new FileInputStream(symbolsPath.toFile()))) {
            while (scanner.hasNextLine()) {
                String symbol = scanner.nextLine();
                symbols.add(symbol);
            }
        } catch (IOException e) {
            throw new Error(e);
        }

        retrieveNewDataByBatches(symbols, false);
    }

    /**
     * - Let f a function from [0, {@code size}] to {@code double}.
     * - Let g a <strong>linear</strong> function from [0, {@code size}] to {@code double} (ie. g(x) = ax + b) such that:
     * - g(0) = f(0)
     * Return the integer i such that g(i) = f(i) and f is bounded from above by g
     * (ie. for all integer x in [0, {@code size}] f(x) <= g(x)).
     *
     * @param func       The function f.
     * @param size       the upper bound of the functions' domain.
     * @param comparator The comparator that will be used to order the values of f. If null, the natural ordering of the
     *                   values will be used.
     * @return The integer i such that g(i) = f(i) and f is bounded from above by g.
     * @throws IllegalArgumentException if {@code size} is less than or equal to zero.
     */
    public static int findLinearBound(final IntToDoubleFunction func, int size, final Comparator<Double> comparator) {
        if (size <= 0) throw new IllegalArgumentException("Size should be greater than zero, found: " + size);
        if (size == 1) return 0;
        IntToDoubleFunction slope = i -> (func.applyAsDouble(i) - func.applyAsDouble(0)) / i;
        final Comparator<Integer> cmp = comparator == null ? Comparator.comparingDouble(slope::applyAsDouble) :
                (o1, o2) -> comparator.compare(slope.applyAsDouble(o1), slope.applyAsDouble(o2));
        return IntStream.range(1, size).boxed().max(cmp).orElse(0);
    }

    static double highestBottomLineRate(List<OHLCDataItem> data) {

        IntToDoubleFunction f = i -> data.get(i).getLow().doubleValue();
        int start = IntStream.range(0, data.size()).boxed().min(Comparator.comparingDouble(f::applyAsDouble)).orElse(0);
        double maxRate = Double.NEGATIVE_INFINITY;

        while (start < data.size() - 1) {
            int shift = start;
            IntToDoubleFunction shiftedF = k -> f.applyAsDouble(k + shift);
            int shiftedEnd = findLinearBound(shiftedF, data.size() - shift, Comparator.reverseOrder());
            int end = shiftedEnd + shift;
            double rate = 100 * (f.applyAsDouble(end) - f.applyAsDouble(start)) / f.applyAsDouble(start);
            maxRate = Math.max(rate, maxRate);
            start = end;
        }
        return maxRate;
    }

    static double lastBottomLineRate(List<OHLCDataItem> data) {

        IntToDoubleFunction f = i -> data.get(i).getLow().doubleValue();
        int start = IntStream.range(0, data.size()).boxed().min(Comparator.comparingDouble(f::applyAsDouble)).orElse(0);
        double rate = Double.NEGATIVE_INFINITY;

        while (start < data.size() - 1) {
            int shift = start;
            IntToDoubleFunction shiftedF = k -> f.applyAsDouble(k + shift);
            int shiftedEnd = findLinearBound(shiftedF, data.size() - shift, Comparator.reverseOrder());
            int end = shiftedEnd + shift;
            rate = 100 * (f.applyAsDouble(end) - f.applyAsDouble(start)) / f.applyAsDouble(start);
            start = end;
        }
        return rate;
    }

    static Map<String, Double> lastBottomLinesRates(List<Future<Map.Entry<Stock, List<OHLCDataItem>>>> futureData) {

        Map<String, Double> res = new HashMap<>();
        long startTime = System.nanoTime();

        for (int i = 0; i < futureData.size(); i++) {

            Future<Map.Entry<Stock, List<OHLCDataItem>>> future = futureData.get(i);
            Map.Entry<Stock, List<OHLCDataItem>> entry;

            try {
                entry = future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error occurred while waiting for threads to terminate", e);
                continue;
            }

            Stock stock = entry.getKey();
            String symbol = stock.getSymbol();
            List<OHLCDataItem> data = entry.getValue();

            double rate = highestBottomLineRate(data);
            res.put(symbol, rate);

            System.out.println("=========== " + symbol);
            System.out.println("Last line rate:  " + rate);
            System.out.printf("Progress: %.02f%% done (%d/%d)" + System.lineSeparator(), 100. * (i + 1.) / futureData.size(), i + 1, futureData.size());

            long endTime = System.nanoTime();
            long diff = endTime - startTime;
            Duration duration = Duration.ofNanos(diff);

            System.out.printf("Elapsed time: %dh%02dm%02d.%03ds" + System.lineSeparator(),
                    duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(),
                    duration.toMillisPart());
            long avg = diff / (i + 1);
            Duration avgDuration = Duration.ofNanos(avg);

            System.out.printf("Average: %02dm%02d.%03ds per stock" + System.lineSeparator(), avgDuration.toMinutes(), avgDuration.toSecondsPart(), avgDuration.toMillisPart());

            long estimated = avg * futureData.size();
            Duration estimatedDuration = Duration.ofNanos(estimated);

            System.out.printf("Estimated total time: %dh%02dm%02d.%03ds" + System.lineSeparator(),
                    estimatedDuration.toHours(), estimatedDuration.toMinutesPart(),
                    estimatedDuration.toSecondsPart(), estimatedDuration.toMillisPart());
            long eta = avg * (futureData.size() - i - 1);

            Duration etaDuration = Duration.ofNanos(eta);

            System.out.printf("ETA: %dh%02dm%02d.%03ds" + System.lineSeparator(),
                    etaDuration.toHours(), etaDuration.toMinutesPart(), etaDuration.toSecondsPart(),
                    etaDuration.toMillisPart());
            System.out.println("===========");
        }
        return res;
    }

    static Map<String, Double> sortByValues(Map<String, Double> data) {
        Map<String, Double> res = new TreeMap<>(Comparator.comparingDouble(data::get).reversed());
        res.putAll(data);
        return res;
    }

    public static Map.Entry<Stock, List<OHLCDataItem>> loadFromFile(String symbol) throws IOException {
//        Path file = Paths.get("src", "main", "resources", "stocks", symbol + ".bin"); //todo revert

        Path file = Paths.get("downloads", symbol + ".txt");
        log.debug("Loading file: {}", file);

        List<OHLCDataItem> res = new ArrayList<>();

        // pass the stock  name into the Stock class
        Stock stock = new Stock(symbol);

        if (Files.exists(file)) {

            String fileJsonData = FileUtils.readData(file);
            //System.out.println(fileJsonData);
            if (fileJsonData != null && !fileJsonData.isBlank()) {
                objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                StockHistoricalQuote stockHistoricalQuote = objectMapper.readValue(fileJsonData, StockHistoricalQuote.class);
                stock.setName(stockHistoricalQuote.getName());
                stock.setCurrency(stockHistoricalQuote.getCurrency());
                stock.setStockExchange(stockHistoricalQuote.getStockExchange());

                if (stockHistoricalQuote.getResults() != null) {

                    ListIterator<StockHistoricalQuote.HistoricalQuote> historicalQuoteIterator = stockHistoricalQuote.getResults().listIterator();
                    while (historicalQuoteIterator.hasNext()) {

                        StockHistoricalQuote.HistoricalQuote historicalQuote = historicalQuoteIterator.next();

                        Date date = historicalQuote.getUtilDateTime();
                        double open = historicalQuote.getOpenPrice();
                        double high = historicalQuote.getHighestPrice();
                        double low = historicalQuote.getLowestPrice();
                        double close = historicalQuote.getClosPrice();
                        double volume = historicalQuote.getVolume();
                        //double adjClose = historicalQuote.getVolumeWeightedAveragePrice();
                        //OHLCDataItem item = new OHLCDataItem(date, open, high, low, adjClose, volume);
                        OHLCDataItem item = new OHLCDataItem(date, open, high, low, close, volume);
                        res.add(item);

                        log.trace("date: {}, open: {}, high: {}, low: {}, close: {}, volume: {}", date, open, high, low, close, volume);
                    }
                }

            }
        }
        return new AbstractMap.SimpleEntry<>(stock, res);
    }


    static Future<Map.Entry<Stock, List<OHLCDataItem>>> loadFromFile(ExecutorService executor, String symbol) {
        return executor.submit(() -> loadFromFile(symbol));
    }

    private static Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        cleanupCalendar(calendar);
        return calendar;
    }

    private static void cleanupCalendar(Calendar calendar) {
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 0);
    }

    static List<OHLCDataItem> loadFromAPI(Stock stock, int field, int amount, Interval interval) throws IOException {
        Calendar start = getCalendar();
        start.add(field, -amount);
        Calendar end = getCalendar();
        return loadFromAPI(stock, start, end, interval);
    }

    private static List<OHLCDataItem> loadFromAPI(Stock stock, Calendar start, Calendar end, Interval interval)
            throws IOException {
        log.debug("Loading data form API for: {}", stock.getSymbol());
        List<OHLCDataItem> res = new ArrayList<>();
//        List<HistoricalQuote> historicalQuotes = stock.getHistory(start, end, interval);//todo update changes
        LocalDate fromDate = start.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate toDate = end.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        StockHistoricalQuote stockHistoricalQuote = PolygonStockApi.getGroupedHistory(stock.getSymbol(), fromDate, toDate, 1);


        if (stockHistoricalQuote != null && stockHistoricalQuote.getResults() != null) {
            ListIterator<StockHistoricalQuote.HistoricalQuote> historicalQuoteIterator = stockHistoricalQuote.getResults().listIterator();
            while (historicalQuoteIterator.hasNext()) {
                StockHistoricalQuote.HistoricalQuote historicalQuote = historicalQuoteIterator.next();
                Date date = historicalQuote.getUtilDateTime();
                double open = historicalQuote.getOpenPrice();
                double high = historicalQuote.getHighestPrice();
                double low = historicalQuote.getLowestPrice();
                double close = historicalQuote.getClosPrice();
                double volume = historicalQuote.getVolume();
                double adjClose = historicalQuote.getVolumeWeightedAveragePrice();

                // adjust data:
                System.out.println("close: " + close);
                System.out.println("adjClose: " + adjClose);
                open = open * adjClose / close;
                high = high * adjClose / close;
                low = low * adjClose / close;

                OHLCDataItem item = new OHLCDataItem(date, open, high, low, adjClose, volume);
                res.add(item);
            }
        }
        return res;
    }

    private static void updateFile(Map.Entry<Stock, List<OHLCDataItem>> entry) {
        Stock stock = entry.getKey();
        List<OHLCDataItem> data = entry.getValue();
        Path file = Paths.get("src", "main", "resources", "stocks", stock.getSymbol() + ".bin");
        log.debug("Updating file: {}", file);

        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        CRC32 crc32 = new CRC32();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file.toFile()))) {
            dos.writeUTF(stock.getName());
            dos.writeUTF(stock.getCurrency());
            dos.writeUTF(stock.getStockExchange());

            crc32.update(stock.getName().getBytes());
            crc32.update(stock.getCurrency().getBytes());
            crc32.update(stock.getStockExchange().getBytes());

            dos.writeLong(crc32.getValue());
            for (OHLCDataItem item : data) {
                dos.writeLong(item.getDate().getTime());
                dos.writeDouble(item.getOpen().doubleValue());
                dos.writeDouble(item.getHigh().doubleValue());
                dos.writeDouble(item.getLow().doubleValue());
                dos.writeDouble(item.getClose().doubleValue());
                dos.writeDouble(item.getVolume().doubleValue());

                buffer.putLong(0, item.getDate().getTime());
                crc32.update(buffer.array());
                buffer.putDouble(0, item.getOpen().doubleValue());
                crc32.update(buffer.array());
                buffer.putDouble(0, item.getHigh().doubleValue());
                crc32.update(buffer.array());
                buffer.putDouble(0, item.getLow().doubleValue());
                crc32.update(buffer.array());
                buffer.putDouble(0, item.getClose().doubleValue());
                crc32.update(buffer.array());
                buffer.putDouble(0, item.getVolume().doubleValue());
                crc32.update(buffer.array());

                dos.writeLong(crc32.getValue());
            }
            log.debug("File {} updated with {} entries", file, data.size());
        } catch (IOException e) {
            log.error("Error occurred while updating file: {}", file, e);
        }
    }

    private static void updateFile(ExecutorService executor, Future<Map.Entry<Stock, List<OHLCDataItem>>> futureData) {
        executor.submit(() -> {
            try {
                updateFile(futureData.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error occurred while waiting for task to finish", e);
            }
        });
    }
}
