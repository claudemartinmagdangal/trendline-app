package com.jfreechart.trendline.thomas.util;

import jclass.dto.StockTrendInfo;
import jclass.dto.Symbol;
import jclass.io.StockFileHandling;
import jclass.util.Utils;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.XYDataset;
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

import static jclass.CombinedView.LoggerManager.initializeLoggingContext;
import static jclass.util.Utils.findLinearBound;

public class OHLCCandleStickTrendUtil
{
    private static final long serialVersionUID = 1L;
    private static final int skipNofDaysForTrendLineCalculation = 25;
    private static final Logger log = LoggerFactory.getLogger(jclass.util.OHLCCandleStickTrendUtil.class);

    public static void main(String[] args)
    {
        initializeLoggingContext();
        jclass.util.OHLCCandleStickTrendUtil.calculateTrendsInfo(true);
    }

    public static HashMap<Long,List<StockTrendInfo>> nearSlopStockTrend(boolean isAccelerationMatrix){
        HashMap<Long,List<StockTrendInfo>> stockMap= new HashMap<>();
//        List<StockTrendInfo> allStockTrends = new ArrayList<>();
        //String[] allStockSymbols = StockFileHandling.getAllStockSymbols();

        List<Symbol> allStockSymbols =StockFileHandling.getAllStockSymbolsFromExcel();
        if(!allStockSymbols.isEmpty()){
            for (Symbol symbol : allStockSymbols){

                Map.Entry<Stock, List<OHLCDataItem>> entry;

                try{
                    entry = Utils.loadFromFile(symbol.getSymbol().trim());
                }
                catch (IOException e){
                    throw new Error("Error occurred while loading data", e);
                }
                List<OHLCDataItem> data = entry.getValue();

                int dataSize = data.size();

                if (dataSize > 0){
                    IntToDoubleFunction x = i -> data.get(i).getDate().getTime();

                    StockTrendInfo stockTrendInfo = new StockTrendInfo(symbol.getSymbol().trim());
                    stockTrendInfo.setCompany(symbol.getCompanyName());
                    stockTrendInfo.setIndustry(symbol.getIndustry());

                    /* Draw lines between two high points */
                    {
                        IntToDoubleFunction y = i -> data.get(i).getHigh().doubleValue();

                        //calculateAndSetTrendInfo(x, y, dataSize, null, stockTrendInfo, false,true,);
                        calculateAndSetTrendInfo(x, y, dataSize, null, stockTrendInfo, false,true,data);
                    }

                    /* Draw lines between two low points */
                    {
                        IntToDoubleFunction y = i -> data.get(i).getLow().doubleValue();

                        //calculateAndSetTrendInfo(x, y, dataSize, Comparator.reverseOrder(), stockTrendInfo, true,true);
                        calculateAndSetTrendInfo(x, y, dataSize, Comparator.reverseOrder(), stockTrendInfo, true,true,data);
                    }

                    if(stockTrendInfo.getMaxUpwardTrendCount()>0 && isAccelerationMatrix)
                    {
                        for(int i = 0; i<stockTrendInfo.getUpwardTrendDistances().size(); i++)
                        {
                            //log.info("Stock : {} with {} Day slope {} ",stockTrendInfo.getSymbol(),
                                    stockTrendInfo.getUpwardTrendDistances().get(i),
                                    stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().get(i));
                            if(stockMap.containsKey(stockTrendInfo.getUpwardTrendDistances().get(i)))
                            {
                                List<StockTrendInfo> allStockTrendsFromMap =
                                        new ArrayList<>(stockMap.get(stockTrendInfo.getUpwardTrendDistances().get(i)));
                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getUpwardTrendDistances().get(i),i);
//                                stockTrendInfo.setSortingIndex(i);
                                allStockTrendsFromMap.add(stockTrendInfo);
                                stockMap.put(stockTrendInfo.getUpwardTrendDistances().get(i), allStockTrendsFromMap);
                            } else {
                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getUpwardTrendDistances().get(i),i);
//                                stockTrendInfo.setSortingIndex(i);
                                stockMap.put(stockTrendInfo.getUpwardTrendDistances().get(i), List.of(stockTrendInfo));
                            }
                        }
                    }

                    if(stockTrendInfo.getMaxDownwardTrendCount()>0 && !isAccelerationMatrix)
                    {
                        for(int i = 0; i<stockTrendInfo.getDownwardTrendDistances().size(); i++)
                        {
                            //log.info("Stock : {} with {} Day slope {} ",stockTrendInfo.getSymbol(),
                                    stockTrendInfo.getDownwardTrendDistances().get(i),
                                    stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(i));
                            if(stockMap.containsKey(stockTrendInfo.getDownwardTrendDistances().get(i)))
                            {
                                List<StockTrendInfo> allStockTrendsFromMap =
                                        new ArrayList<>(stockMap.get(stockTrendInfo.getDownwardTrendDistances().get(i)));
//                                stockTrendInfo.setSortingIndex(i);
                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getDownwardTrendDistances().get(i),i);
                                allStockTrendsFromMap.add(stockTrendInfo);
                                stockMap.put(stockTrendInfo.getDownwardTrendDistances().get(i), allStockTrendsFromMap);
                            } else {
//                                stockTrendInfo.setSortingIndex(i);
                                stockTrendInfo.getDaySlopIndexForSort().put(stockTrendInfo.getDownwardTrendDistances().get(i),i);
                                stockMap.put(stockTrendInfo.getDownwardTrendDistances().get(i), List.of(stockTrendInfo));
                            }
                        }
                    }
                }
            }
        }
        return stockMap;
    }

    public static List<StockTrendInfo> calculateTrendsInfo(boolean includeCurrentDayInSlopCalculation)
    {
        List<StockTrendInfo> allStockTrends = new ArrayList<>();
        //String[] allStockSymbols = StockFileHandling.getAllStockSymbols();

        List<Symbol> allStockSymbols =StockFileHandling.getAllStockSymbolsFromExcel();

        if(!allStockSymbols.isEmpty()){

            for (Symbol symbol : allStockSymbols){

                Map.Entry<Stock, List<OHLCDataItem>> entry;
                try
                {
                    entry = Utils.loadFromFile(symbol.getSymbol().trim());
                }
                catch (IOException e)
                {
                    throw new Error("Error occurred while loading data", e);
                }
                List<OHLCDataItem> data = entry.getValue();

                int dataSize = data.size();
                if (dataSize > 0){

                    IntToDoubleFunction x = i -> data.get(i).getDate().getTime();

                    StockTrendInfo stockTrendInfo = new StockTrendInfo(symbol.getSymbol().trim());
                    stockTrendInfo.setCompany(symbol.getCompanyName());
                    stockTrendInfo.setIndustry(symbol.getIndustry());

                    /* Draw lines between two high points */
                    {
                        IntToDoubleFunction y = i -> data.get(i).getHigh().doubleValue();

//                        calculateAndSetTrendInfo(x, y, dataSize, null, stockTrendInfo, false,includeCurrentDayInSlopCalculation);
                        calculateAndSetTrendInfo(x, y, dataSize, null, stockTrendInfo, false,includeCurrentDayInSlopCalculation,data);
                    }

                    /* Draw lines between two low points */
                    {
                        IntToDoubleFunction y = i -> data.get(i).getLow().doubleValue();

//                        calculateAndSetTrendInfo(x, y, dataSize, Comparator.reverseOrder(), stockTrendInfo, true,includeCurrentDayInSlopCalculation);
                        calculateAndSetTrendInfo(x, y, dataSize, Comparator.reverseOrder(), stockTrendInfo, true,includeCurrentDayInSlopCalculation,data);
                    }

                    allStockTrends.add(stockTrendInfo);
                }
            }
        }
        return allStockTrends;
    }

    private static void calculateAndSetTrendInfo(IntToDoubleFunction x, IntToDoubleFunction y, int dataSize,
                                                 Comparator<Double> comparator, StockTrendInfo stockTrendInfo, boolean isUpwardTrend,
                                                 boolean includeCurrentDayInSlopCalculation,List<OHLCDataItem> data)
    {

        Comparator<Integer> cmp = comparator == null ? Comparator.comparingDouble(y::applyAsDouble) :
                (o1, o2) -> comparator.compare(y.applyAsDouble(o1), y.applyAsDouble(o2));

        int start = extractMaxValueIndexWithRange(dataSize, cmp, 0, dataSize,true);

        while (start < dataSize-1 && start!=dataSize -2)
        {
            int shift = start;
            IntToDoubleFunction shiftedY = i -> y.applyAsDouble(i + shift);
            int shiftedEnd = findLinearBound(shiftedY, dataSize - shift, comparator);
            int end = shiftedEnd + shift;

            boolean includeInSlopCalculation = end != dataSize - 1 && !includeCurrentDayInSlopCalculation;
            if (includeCurrentDayInSlopCalculation ||includeInSlopCalculation)
            {
                double endX = x.applyAsDouble(end);
                double endY = y.applyAsDouble(end);
                double startX = x.applyAsDouble(start);
                double startY = y.applyAsDouble(start);

                //double slope = (endY - startY) / (endX - startX);

                LocalDateTime endXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(endX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime startXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(startX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();


                // made change
                long from=startXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
                long to=endXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();


                int countDay=0;
                for (OHLCDataItem ohlc:data){
                    if(from< ohlc.getDate().getTime() && ohlc.getDate().getTime()<to){
                        countDay++;
                    }
                }


                //double dailySlop = ((endY - startY) / startY) / ((double) Duration.between(startXDate, endXDate).toDays());
                double dailySlope = (endY - startY) / countDay ;

                boolean isBrokenTrendLine = isBrokenTrendLine(x, y, dataSize, end, endX, endY, dailySlope);
                if (!isBrokenTrendLine)
                {
                    //double slopInPercentage = ((endY - startY) / startY) * 100;
                    double slopeInPercentage = ((endY - startY) / countDay)/ startY;
                    String rateTxt = String.format("%.2f", slopeInPercentage);
                    slopeInPercentage = Double.parseDouble(rateTxt);

                    if (isUpwardTrend && slopeInPercentage > 0.0)
                    {
                        stockTrendInfo.setMaxUpwardTrendCount(stockTrendInfo.getMaxUpwardTrendCount() + 1);
//                    stockTrendInfo.getUpwardTrendDistances().add(Duration.between(startXDate, LocalDateTime.now()).toDays());
                        stockTrendInfo.getUpwardTrendDistances().add((long) (dataSize - start - 1));
                        stockTrendInfo.setTotalUpwardTrendDistance(stockTrendInfo.getTotalUpwardTrendDistance() + dataSize - start - 1);
                        stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().add(slopeInPercentage);
//                    stockTrendInfo.getUpwardTrendDistanceFromCurrentDate().add(Duration.between(startXDate, LocalDate.now()).toDays());
                    }

                    if (!isUpwardTrend && slopeInPercentage < 0.0)
                    {
                        stockTrendInfo.setMaxDownwardTrendCount(stockTrendInfo.getMaxDownwardTrendCount() + 1);
//                    stockTrendInfo.getDownwardTrendDistances().add(Duration.between(startXDate, LocalDateTime.now()).toDays());
                        stockTrendInfo.getDownwardTrendDistances().add((long) (dataSize - start - 1));
                        stockTrendInfo.setTotalDownwardTrendDistance(stockTrendInfo.getTotalUpwardTrendDistance() + dataSize - start - 1);
                        stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().add(slopeInPercentage);
//                    stockTrendInfo.getDownwardTrendDistanceFromCurrentDate().add(Duration.between(startXDate, LocalDate.now()).toDays());
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

    private static int extractMaxValueIndexWithRange(int dataSize, Comparator<Integer> cmp, int startIndex, int endIndex, boolean skipDefaultNofDaysForTrendLineCalculation)
    {
        int maxIndex = 0;
        if (skipDefaultNofDaysForTrendLineCalculation && dataSize > skipNofDaysForTrendLineCalculation * 2)
            maxIndex = IntStream.range(skipNofDaysForTrendLineCalculation, dataSize).boxed().max(cmp).orElse(0);
        else
            maxIndex = IntStream.range(startIndex, endIndex).boxed().max(cmp).orElse(0);
        return maxIndex;
    }

    public static int drawLinearBounds(IntToDoubleFunction x, IntToDoubleFunction y, int dataSize, int startDatasetIndex,
                                       XYItemRenderer[] renderers, Comparator<Double> comparator, XYPlot xyplot,
                                       String annotationSuffix, Font annotationsFont, boolean includeCurrentDayInSlopCalculation) {

        Comparator<Integer> cmp = comparator == null ? Comparator.comparingDouble(y::applyAsDouble) :
                (o1, o2) -> comparator.compare(y.applyAsDouble(o1), y.applyAsDouble(o2));

        int start = extractMaxValueIndexWithRange(dataSize, cmp, 0, dataSize,true);//todo make it configurable in params

        double lastX = x.applyAsDouble(dataSize-1);

        int linesCount = 0;

        while (start < dataSize-1)
        {
            int shift = start;
            IntToDoubleFunction shiftedY = i -> y.applyAsDouble(i + shift);
            int shiftedEnd = findLinearBound(shiftedY, dataSize - shift, comparator);
            int end = shiftedEnd + shift;

            boolean includeInSlopCalculation = end != dataSize - 1 && !includeCurrentDayInSlopCalculation;
            if (includeCurrentDayInSlopCalculation ||includeInSlopCalculation)
            {
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
                //startXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli() it will give millisecond
                double dailySlop = ((endY - startY) / startY) / ((double) Duration.between(startXDate, endXDate).toDays());
                boolean isBrokenTrendLine = isBrokenTrendLine(x, y, dataSize, end, endX, endY, dailySlop);

                if (!isBrokenTrendLine)
                {
                    double slopInPercentage = ((endY - startY) / startY) * 100;
                    String slopInPercentageTxt = String.format("SLOPE:%.3f %s & RC: %.3f %s", slopInPercentage,"%",slopInPercentage/ (double) (end - start), annotationSuffix);
                    XYTextAnnotation annotation = new XYTextAnnotation(slopInPercentageTxt, startX, startY);
                    annotation.setFont(annotationsFont);

                    Paint paint;
                    if (slopInPercentage > 0)
                        paint = renderer.getBasePaint();
                    else
                    {
                        paint = renderer.getBaseItemLabelPaint();
                    }
                    annotation.setPaint(paint);

                    XYDataset dataset = DatasetUtilities.sampleFunction2D(function, startX, lastX + (lastX - startX), 2, String.format("Slope:%.3f%s & RC:%.3f%s,", slopInPercentage, "%",slopInPercentage/ (double) (end - start),"%/Day"));

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

    private static boolean isBrokenTrendLine(IntToDoubleFunction x, IntToDoubleFunction y, int dataSize, int end, double endX, double endY, double slop)
    {
        LocalDateTime startXDate = Date.from(Instant.ofEpochSecond(Double.valueOf(endX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        for (int i = end+1; i < dataSize; i++)
        {
            double valueY = y.applyAsDouble(i);
            double valueX = x.applyAsDouble(i);

            LocalDateTime valueXDateTime = Date.from(Instant.ofEpochSecond(Double.valueOf(valueX / 1000).longValue())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            long nofDays = Duration.between(startXDate, valueXDateTime).toDays();

            if (slop > 0.0 && valueY < (endY + ( endY * slop * ((double) nofDays))))
            {
                return true;
            }
            if (slop < 0.0 && valueY >  (endY + (endY * slop * ((double) nofDays))))
            {
                return true;
            }
        }
        return false;
    }

    // Function to sort map by Key
    public static HashMap<Long,List<StockTrendInfo>> sortByKey(HashMap<Long,List<StockTrendInfo>> map)
    {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }



    public static int drawLinearBounds(IntToDoubleFunction x, IntToDoubleFunction y, int dataSize, int startDatasetIndex,
                                       XYItemRenderer[] renderers, Comparator<Double> comparator, XYPlot xyplot,
                                       String annotationSuffix, Font annotationsFont, boolean includeCurrentDayInSlopCalculation, java.util.List<OHLCDataItem> data) {

        Comparator<Integer> cmp = comparator == null ? Comparator.comparingDouble(y::applyAsDouble) : (o1, o2) -> comparator.compare(y.applyAsDouble(o1), y.applyAsDouble(o2));

        int start = extractMaxValueIndexWithRange(dataSize, cmp, 0, dataSize,true);//todo make it configurable in params

        double lastX = x.applyAsDouble(dataSize-1);

        int linesCount = 0;

        while (start < dataSize-1)
        {
            int shift = start;
            IntToDoubleFunction shiftedY = i -> y.applyAsDouble(i + shift);
            int shiftedEnd = findLinearBound(shiftedY, dataSize - shift, comparator);
            int end = shiftedEnd + shift;

            boolean includeInSlopCalculation = end != dataSize - 1 && !includeCurrentDayInSlopCalculation;
            if (includeCurrentDayInSlopCalculation ||includeInSlopCalculation)
            {
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


                //startXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli() it will give millisecond

                // made change
                long from=startXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
                long to=endXDate.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();


                int countDay=0;
                for (OHLCDataItem ohc:data){
                    if(from< ohc.getDate().getTime() && ohc.getDate().getTime()<to){
                        countDay++;
                    }
                }


               // double dailySlop = ((endY - startY) / startY) / ((double) Duration.between(startXDate, endXDate).toDays());
                double dailySlope = (endY - startY) / countDay;


                boolean isBrokenTrendLine = isBrokenTrendLine(x, y, dataSize, end, endX, endY, dailySlope);

                if (!isBrokenTrendLine)
                {
                    //double slopInPercentage = ((endY - startY) / startY) * 100;

                    double slopeInPercentage = ((endY - startY) / countDay)/ startY;

                  //  String slopInPercentageTxt = String.format("SLOPE:%.3f %s & RC: %.3f %s", slopInPercentage,"%",slopInPercentage/ (double) (end - start), "%"+coundDay+"Day(s)");

                    String slopInPercentageTxt = String.format("Y1= %.3f Y2= %.3f Days=%s PCT-SLOPE= %.3f ",startY,endY, countDay,slopeInPercentage);

                    XYTextAnnotation annotation = new XYTextAnnotation(slopInPercentageTxt, startX, startY);
                    annotation.setFont(annotationsFont);

                    Paint paint;
                    if (slopeInPercentage > 0)
                        paint = renderer.getBasePaint();
                    else
                    {
                        paint = renderer.getBaseItemLabelPaint();
                    }
                    annotation.setPaint(paint);

                   // XYDataset dataset = DatasetUtilities.sampleFunction2D(function, startX, lastX + (lastX - startX), 2, String.format("Slope:%.3f%s & RC:%.3f%s,", slopInPercentage, "%",slopInPercentage/ (double) (end - start), "%"+coundDay+"Day(s)"));
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

}