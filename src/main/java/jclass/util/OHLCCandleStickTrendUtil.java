package jclass.util;

import jclass.dto.StockTrendInfo;
import jclass.dto.Symbol;
import jclass.io.StockFileHandling;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.TextAnchor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;

import java.awt.*;
import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OHLCCandleStickTrendUtil {
    private static final long serialVersionUID = 1L;
    private static final int skipNofDaysForTrendLineCalculation = 25;
    private static final Logger log = LoggerFactory.getLogger(OHLCCandleStickTrendUtil.class);

    public static HashMap<Long, List<StockTrendInfo>> nearSlopStockTrend(boolean isAccelerationMatrix) {

        HashMap<Long, List<StockTrendInfo>> stockMap = new HashMap<>();
        // String[] allStockSymbols = StockFileHandling.getAllStockSymbols();

        List<Symbol> allStockSymbols = StockFileHandling.getAllStockSymbolsFromExcel();

        if (!allStockSymbols.isEmpty()) {

            for (Symbol symbol : allStockSymbols) {

                Map.Entry<Stock, List<OHLCDataItem>> entry;

                try {
                    entry = Utils.loadFromFile(symbol.getSymbol().trim());
                } catch (IOException e) {
                    throw new Error("Error occurred while loading data", e);
                }

                List<OHLCDataItem> data = entry.getValue();
                int dataSize = data.size();

                if (dataSize > 0) {

                    IntToDoubleFunction time = i -> data.get(i).getDate().getTime();


                    StockTrendInfo stockTrendInfo = new StockTrendInfo(symbol.getSymbol().trim());
                    stockTrendInfo.setCompany(symbol.getCompanyName());
                    stockTrendInfo.setIndustry(symbol.getIndustry());


                    /* Draw lines between two high points */
                    {
                        IntToDoubleFunction high = i -> data.get(i).getHigh().doubleValue();
                        //calculateAndSetTrendInfo(x, y, dataSize, null, stockTrendInfo, false,true,);
                        calculateAndSetTrendInfo(time, high, dataSize, null, stockTrendInfo, false, true, data);
                    }

                    /* Draw lines between two low points */
                    {
                        IntToDoubleFunction low = i -> data.get(i).getLow().doubleValue();
                        //calculateAndSetTrendInfo(x, y, dataSize, Comparator.reverseOrder(), stockTrendInfo, true,true);
                        calculateAndSetTrendInfo(time, low, dataSize, Comparator.reverseOrder(), stockTrendInfo, true, true, data);
                    }

                    if (stockTrendInfo.getMaxUpwardTrendCount() > 0 && isAccelerationMatrix) {

                        for (int i = 0; i < stockTrendInfo.getUpwardTrendDistances().size(); i++) {

                            if (stockMap.containsKey(stockTrendInfo.getUpwardTrendDistances().get(i))) {
                                List<StockTrendInfo> allStockTrendsFromMap = new ArrayList<>(stockMap.get(stockTrendInfo.getUpwardTrendDistances().get(i)));

                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getUpwardTrendDistances().get(i), i);
//                                stockTrendInfo.setSortingIndex(i);
                                allStockTrendsFromMap.add(stockTrendInfo);
                                stockMap.put(stockTrendInfo.getUpwardTrendDistances().get(i), allStockTrendsFromMap);
                            } else {
                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getUpwardTrendDistances().get(i), i);
//                                stockTrendInfo.setSortingIndex(i);
                                stockMap.put(stockTrendInfo.getUpwardTrendDistances().get(i), List.of(stockTrendInfo));
                            }
                        }
                    }

                    if (stockTrendInfo.getMaxDownwardTrendCount() > 0 && !isAccelerationMatrix) {

                        for (int i = 0; i < stockTrendInfo.getDownwardTrendDistances().size(); i++) {

                            //log.info("Stock : {} with {} Day slope {} ", stockTrendInfo.getSymbol(), stockTrendInfo.getDownwardTrendDistances().get(i), stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(i));

                            if (stockMap.containsKey(stockTrendInfo.getDownwardTrendDistances().get(i))) {
                                List<StockTrendInfo> allStockTrendsFromMap = new ArrayList<>(stockMap.get(stockTrendInfo.getDownwardTrendDistances().get(i)));
//                                stockTrendInfo.setSortingIndex(i);
                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getDownwardTrendDistances().get(i), i);
                                allStockTrendsFromMap.add(stockTrendInfo);
                                stockMap.put(stockTrendInfo.getDownwardTrendDistances().get(i), allStockTrendsFromMap);
                            } else {
//                                stockTrendInfo.setSortingIndex(i);
                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getDownwardTrendDistances().get(i), i);
                                stockMap.put(stockTrendInfo.getDownwardTrendDistances().get(i), List.of(stockTrendInfo));
                            }
                        }
                    }
                    //System.out.println(stockTrendInfo);
                }

            }
        }
        return stockMap;
    }

    public static List<StockTrendInfo> calculateTrendsInfo(boolean includeCurrentDayInSlopCalculation) {
        List<StockTrendInfo> allStockTrends = new ArrayList<>();
        // String[] allStockSymbols = StockFileHandling.getAllStockSymbols();
        List<Symbol> allStockSymbols = StockFileHandling.getAllStockSymbolsFromExcel();

//        int upto=5;
//        int key=2;
//        boolean write=true;
//        Map<String, Object[]> data = new TreeMap<String, Object[]>();
//        data.put("1", new Object[] {"NAME", "O", "H", "L", "C", "DATA"});
        if (!allStockSymbols.isEmpty()) {

            for (Symbol symbol : allStockSymbols) {

                Map.Entry<Stock, List<OHLCDataItem>> entry;

                try {
                    entry = Utils.loadFromFile(symbol.getSymbol().trim());
                } catch (IOException e) {
                    throw new Error("Error occurred while loading data", e);
                }
                List<OHLCDataItem> ohlcDataItemList = entry.getValue();

//                if(upto>0) {
//
//                    for( OHLCDataItem sData : ohlcDataItemList) {
//                        data.put(key + "", new Object[]{symbol.getSymbol(), sData.getOpen(), sData.getHigh(), sData.getLow(), sData.getClose(), sData.getDate()});
//                        key++;
//                    }
//                    upto--;
//                }else{
//                    if(write){
//                        ReadExcel.writeExcell(data);
//                        write=false;
//                    }
//                }

                int dataSize = ohlcDataItemList.size();
                if (dataSize > 0) {

                    IntToDoubleFunction x = i -> ohlcDataItemList.get(i).getDate().getTime();

                    StockTrendInfo stockTrendInfo = new StockTrendInfo(symbol.getSymbol().trim());
                    stockTrendInfo.setCompany(symbol.getCompanyName());
                    stockTrendInfo.setIndustry(symbol.getIndustry());

                    /* Draw lines between two high points */
                    {
                        IntToDoubleFunction y = i -> ohlcDataItemList.get(i).getHigh().doubleValue();
                        calculateAndSetTrendInfo(x, y, dataSize, null, stockTrendInfo, false, includeCurrentDayInSlopCalculation, ohlcDataItemList);
                    }

                    /* Draw lines between two low points */
                    {
                        IntToDoubleFunction y = i -> ohlcDataItemList.get(i).getLow().doubleValue();
                        calculateAndSetTrendInfo(x, y, dataSize, Comparator.reverseOrder(), stockTrendInfo, true, includeCurrentDayInSlopCalculation, ohlcDataItemList);
                    }

                    volumnPercentage(symbol.getSymbol().trim(), ohlcDataItemList, stockTrendInfo);

                    allStockTrends.add(stockTrendInfo);
                }
            }
        }
        return allStockTrends;
    }

    private static void calculateAndSetTrendInfo(IntToDoubleFunction time, IntToDoubleFunction y, int dataSize,
                                                 Comparator<Double> comparator, StockTrendInfo stockTrendInfo, boolean isUpwardTrend,
                                                 boolean includeCurrentDayInSlopCalculation, List<OHLCDataItem> data) {

        Comparator<Integer> cmp = comparator == null ? Comparator.comparingDouble(y::applyAsDouble) :
                (o1, o2) -> comparator.compare(y.applyAsDouble(o1), y.applyAsDouble(o2));

        int start = extractMaxValueIndexWithRange(dataSize, cmp, 0, dataSize, true);

        //while (start < dataSize-1 && start!=dataSize -2) {
        while (start < dataSize - 1) {

            int shift = start;
            IntToDoubleFunction shiftedY = i -> y.applyAsDouble(i + shift);
            int shiftedEnd = Utils.findLinearBound(shiftedY, dataSize - shift, comparator);
            int end = shiftedEnd + shift;

            boolean includeInSlopCalculation = end != dataSize - 1 && !includeCurrentDayInSlopCalculation;

            if (includeCurrentDayInSlopCalculation || includeInSlopCalculation) {
                double endX = time.applyAsDouble(end);
                double endY = y.applyAsDouble(end);
                double startX = time.applyAsDouble(start);
                double startY = y.applyAsDouble(start);

                //double slope = (endY - startY) / (endX - startX);

                LocalDateTime endXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(endX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime startXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(startX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();


                // made change
                long from = startXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
                long to = endXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();


                int countDays = 0;
                for (OHLCDataItem ohlc : data) {
                    if (from < ohlc.getDate().getTime() && ohlc.getDate().getTime() < to) {
                        countDays++;
                    }
                }

                double dailySlope = (endY - startY) / countDays;
                boolean isBrokenTrendLine = isBrokenTrendLine(time, y, dataSize, end, endX, endY, dailySlope);

                if (!isBrokenTrendLine) {

                    double slopeInPercentage = ((endY - startY) / startY) * 100;
                    //double slopeInPercentage = ((endY - startY) / countDay)/ startY;
                    //double slopeInPercentage = ((endY - startY) / countDay)* 100;
                    String rateTxt = String.format("%.2f", slopeInPercentage);
                    slopeInPercentage = Double.parseDouble(rateTxt);

                    if (isUpwardTrend && slopeInPercentage > 0.0) {

                        stockTrendInfo.setMaxUpwardTrendCount(stockTrendInfo.getMaxUpwardTrendCount() + 1);
                        // stockTrendInfo.getUpwardTrendDistances().add(Duration.between(startXDate, LocalDateTime.now()).toDays());
                        //stockTrendInfo.getUpwardTrendDistances().add((long) countDays);
                        stockTrendInfo.getUpwardTrendDistances().add((long) (dataSize - start - 1));
                        stockTrendInfo.setTotalUpwardTrendDistance(stockTrendInfo.getTotalUpwardTrendDistance() + dataSize - start - 1);
                        stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().add(slopeInPercentage);
                        //stockTrendInfo.getUpwardTrendDistanceFromCurrentDate().add(Duration.between(startXDate, LocalDate.now()).toDays());
                    }

                    if (!isUpwardTrend && slopeInPercentage <= 0.0) {

                        stockTrendInfo.setMaxDownwardTrendCount(stockTrendInfo.getMaxDownwardTrendCount() + 1);
//                      stockTrendInfo.getDownwardTrendDistances().add(Duration.between(startXDate, LocalDateTime.now()).toDays());
                        //stockTrendInfo.getDownwardTrendDistances().add((long) (countDays));
                        stockTrendInfo.getDownwardTrendDistances().add((long) (dataSize - start - 1));
                        stockTrendInfo.setTotalDownwardTrendDistance(stockTrendInfo.getTotalUpwardTrendDistance() + dataSize - start - 1);
                        stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().add(slopeInPercentage);
//                      stockTrendInfo.getDownwardTrendDistanceFromCurrentDate().add(Duration.between(startXDate, LocalDate.now()).toDays());
                    }

                    if (isUpwardTrend && slopeInPercentage > stockTrendInfo.getMaxPercentageChangeUpwardTrend())
                        stockTrendInfo.setMaxPercentageChangeUpwardTrend(slopeInPercentage);

                    if (!isUpwardTrend && slopeInPercentage < stockTrendInfo.getMaxPercentageChangeDownWardTrend())
                        stockTrendInfo.setMaxPercentageChangeDownWardTrend(slopeInPercentage);

                }
            }
            start = end;
        }
    }

    public static int drawLinearBounds(IntToDoubleFunction x, IntToDoubleFunction y, int dataSize, int startDatasetIndex,
                                       XYItemRenderer[] renderers, Comparator<Double> comparator, XYPlot xyplot,
                                       String annotationSuffix, Font annotationsFont, boolean includeCurrentDayInSlopeCalculation, java.util.List<OHLCDataItem> data) {

        Comparator<Integer> cmp = comparator == null ? Comparator.comparingDouble(y::applyAsDouble) : (o1, o2) -> comparator.compare(y.applyAsDouble(o1), y.applyAsDouble(o2));

        int start = extractMaxValueIndexWithRange(dataSize, cmp, 0, dataSize, true);//todo make it configurable in params

        double lastX = x.applyAsDouble(dataSize - 1);
        int linesCount = 0;

        while (start < dataSize - 1) {

            int shift = start;
            IntToDoubleFunction shiftedY = i -> y.applyAsDouble(i + shift);
            int shiftedEnd = Utils.findLinearBound(shiftedY, dataSize - shift, comparator);
            int end = shiftedEnd + shift;

            boolean includeInSlopeCalculation = end != dataSize - 1 && !includeCurrentDayInSlopeCalculation;

            if (includeCurrentDayInSlopeCalculation || includeInSlopeCalculation) {
                double endX = x.applyAsDouble(end);
                double endY = y.applyAsDouble(end);
                double startX = x.applyAsDouble(start);
                double startY = y.applyAsDouble(start);

                double slope = (endY - startY) / (endX - startX);

                LocalDateTime endXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(endX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime startXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(startX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                double intercept = startY - (slope * startX);
                LineFunction2D function = new LineFunction2D(intercept, slope);

                int paintIndex = linesCount % renderers.length;
                XYItemRenderer renderer = renderers[paintIndex];

                // made change
                long from = startXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
                long to = endXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();


                int countDay = 0;
                for (OHLCDataItem ohc : data) {
                    if (from < ohc.getDate().getTime() && ohc.getDate().getTime() < to) {
                        countDay++;
                    }
                }

                double dailySlope = (endY - startY) / countDay;
                boolean isBrokenTrendLine = isBrokenTrendLine(x, y, dataSize, end, endX, endY, dailySlope);

                if (!isBrokenTrendLine) {

                    double slopeInPercentage = ((endY - startY) / countDay) / startY;
                    String slopInPercentageTxt = String.format("Y1= %.3f Y2= %.3f Days=%s PCT-SLOPE= %.3f", startY, endY, countDay, slopeInPercentage);

                    //XYTextAnnotation annotation = new XYTextAnnotation(slopInPercentageTxt, startX, startY);
                    XYTextAnnotation annotation = new XYTextAnnotation(slopInPercentageTxt, endX, endY);
                    annotation.setTextAnchor(TextAnchor.TOP_LEFT);
                    annotation.setFont(annotationsFont);

                    Paint paint;
                    if (slopeInPercentage > 0) {
                        paint = renderer.getBasePaint();
                    } else {
                        paint = renderer.getBaseItemLabelPaint();
                    }
                    annotation.setPaint(paint);
                    annotation.setOutlineVisible(true);

                    XYDataset dataset = DatasetUtilities.sampleFunction2D(function, startX, lastX + (lastX - startX), 2, slopInPercentageTxt);

                    xyplot.setDataset(startDatasetIndex + linesCount, dataset);
                    xyplot.addAnnotation(annotation);
                    xyplot.setRenderer(startDatasetIndex + linesCount, renderer);


                    linesCount++;

                }
            }
            start = end;
        }
        return linesCount;
    }

    private static int extractMaxValueIndexWithRange(int dataSize, Comparator<Integer> cmp, int startIndex, int endIndex, boolean skipDefaultNofDaysForTrendLineCalculation) {

        int maxIndex;

        if (skipDefaultNofDaysForTrendLineCalculation && dataSize > skipNofDaysForTrendLineCalculation * 2)
            maxIndex = IntStream.range(skipNofDaysForTrendLineCalculation, dataSize).boxed().max(cmp).orElse(0);
        else
            maxIndex = IntStream.range(startIndex, endIndex).boxed().max(cmp).orElse(0);
        return maxIndex;
    }


    private static boolean isBrokenTrendLine(IntToDoubleFunction x, IntToDoubleFunction y, int dataSize, int end, double endX, double endY, double slop) {

        LocalDateTime startXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(endX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        for (int i = end + 1; i < dataSize; i++) {

            double valueY = y.applyAsDouble(i);
            double valueX = x.applyAsDouble(i);

            LocalDateTime valueXDateTime = Date.from(Instant.ofEpochSecond(Double.valueOf(valueX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            long nofDays = Duration.between(startXDate, valueXDateTime).toDays();

            if (slop > 0.0 && valueY < (endY + (endY * slop * ((double) nofDays)))) {
                return true;
            }
            if (slop < 0.0 && valueY > (endY + (endY * slop * ((double) nofDays)))) {
                return true;
            }
        }
        return false;
    }


    private static void volumnPercentage(String symbol, List<OHLCDataItem> data, StockTrendInfo stockTrendInfo) {

        OHLCDataItem ohlc = data.get(data.size() - 1);
        stockTrendInfo.setOpen((Double) ohlc.getOpen());
        stockTrendInfo.setHigh((Double) ohlc.getHigh());
        stockTrendInfo.setLow((Double) ohlc.getLow());
        stockTrendInfo.setClose((Double) ohlc.getClose());

        // Volumn value setup
        try {

            Double totalVolumn = 0.0d;
            int month = 31;

            if (data.size() <= month) {
                month = 0;
            } else {
                month = (data.size() - month);
            }

            for (int i = data.size() - 1; month < i; i--) {
                ohlc = data.get(i);
                totalVolumn += ((Double) ohlc.getVolume() / 100);
                //System.out.println(symbol+" ==>  "+i );

            }

            String rateTxt = String.format("%.2f", totalVolumn);
            totalVolumn = Double.parseDouble(rateTxt);
            stockTrendInfo.setVolumn(totalVolumn);
        } catch (Exception ex) {
            //System.out.println(symbol + " ==>  " + ohlc.getVolume());
        }
    }

    // Function to sort map by Key
    public static HashMap<Long, List<StockTrendInfo>> sortByKey(HashMap<Long, List<StockTrendInfo>> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

}