package jclass.chart;

import jclass.dto.StockTrendInfo;
import jclass.enums.CandleChartsEnum;
import jclass.util.OHLCCandleStickTrendUtil;
import jclass.util.Utils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.time.Day;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.*;
import yahoofinance.Stock;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

public class ShowChart extends JFrame {
    private ChartPanel chartPanel;
    private CandlestickRenderer candlestickRenderer;
    private DateAxis domainAxis;
    private NumberAxis rangeAxis;

    public JPanel getChart(String symbol, String title,
                           List<StockTrendInfo> stockTrends,
                           CandleChartsEnum chartName,
                           int slopDaysDifferenceWithCurrentDate,
                           boolean logChart,
                           boolean includeCurrentDayInSlopCalculation) {

        domainAxis = new DateAxis();
        rangeAxis = new NumberAxis();
        //  rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        rangeAxis.setAutoRangeIncludesZero(false);


        //domainAxis.setTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());// getting hang chart panel..

        candlestickRenderer = new CandlestickRenderer();
        candlestickRenderer.setSeriesStroke(0, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
        candlestickRenderer.setSeriesPaint(0, Color.black);
        Color c2 = new Color(0, 255, 128);
        candlestickRenderer.setVolumePaint(c2);
        candlestickRenderer.setDrawVolume(true);

        JPanel mainPanel = new JPanel(new BorderLayout());
        Stroke lineStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);


        XYItemRenderer[] topLinesRenderers = Arrays.stream(new Color[]{Color.DARK_GRAY, Color.MAGENTA, Color.DARK_GRAY}).map(color -> {
            XYItemRenderer r = new SamplingXYLineRenderer();
            r.setSeriesPaint(0, color);
            r.setSeriesStroke(0, lineStroke);
            return r;
        }).toArray(XYItemRenderer[]::new);


        XYItemRenderer[] bottomLinesRenderers = Arrays.stream(new Color[]{Color.BLUE, Color.CYAN, Color.BLUE}).map(color -> {
            XYItemRenderer r = new SamplingXYLineRenderer();
            r.setSeriesPaint(0, color);
            r.setSeriesStroke(0, lineStroke);
            return r;
        }).toArray(XYItemRenderer[]::new);


        XYItemRenderer lineRenderer = new SamplingXYLineRenderer();
        lineRenderer.setSeriesStroke(0, lineStroke);
        lineRenderer.setSeriesPaint(0, Color.RED);


        Font font = new Font(null, Font.PLAIN, 14);
        Map.Entry<Stock, java.util.List<OHLCDataItem>> entry;
        try {
            entry = Utils.loadFromFile(symbol);
        } catch (IOException e) {
            throw new Error("Error occurred while loading data", e);
        }
        Stock stock = entry.getKey();
        java.util.List<OHLCDataItem> data = entry.getValue();


        OHLCDataset dataset = createDataset(data);
        JFreeChart jFreeChart = createChart(dataset, stock, font);

        chartPanel = new ChartPanel(jFreeChart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setMouseZoomable(true, true);
        chartPanel.setDomainZoomable(true);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.ORANGE);
        chartPanel.setZoomAroundAnchor(true);


        XYPlot xyplot = jFreeChart.getXYPlot();
        jFreeChart.getXYPlot().setRangeAxisLocation(AxisLocation.TOP_OR_RIGHT);
        //((DateAxis) xyplot.getDomainAxis()).setTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());


        if (logChart) {
            LogarithmicAxis yAxis = new LogarithmicAxis("Y");
            yAxis.setAutoRangeIncludesZero(false);
            yAxis.setAutoTickUnitSelection(true);
            yAxis.setAllowNegativesFlag(true);
            yAxis.setStrictValuesFlag(true);

            xyplot.setRangeAxis(yAxis);
            //xyplot.setDomainMinorGridlinePaint(Color.YELLOW);
        }

        /* Draw 49-day moving average Line */

        TimeSeries timeSeries = new TimeSeries("54-day moving average");
        for (OHLCDataItem item : entry.getValue()) {
            timeSeries.add(new Day(item.getDate()), item.getClose());
        }

        TimeSeries movingAverage = MovingAverage.createMovingAverage(timeSeries, "LT", 30, 30);
        TimeSeriesCollection movingAverageDataset = new TimeSeriesCollection();
        movingAverageDataset.addSeries(movingAverage);


        // Regression.getOLSRegression();
        // Regression.getPowerRegression()


        xyplot.setDataset(2, movingAverageDataset);
        xyplot.setRenderer(2, lineRenderer);

        /* Draw Fitted Regression (Trend) Line */
        double lowerBound = xyplot.getDomainAxis().getRange().getLowerBound();
        double upperBound = xyplot.getDomainAxis().getRange().getUpperBound();
        double[] regressionParameters = Regression.getOLSRegression(dataset, 0);

        LineFunction2D linefunction2d = new LineFunction2D(regressionParameters[0], regressionParameters[1]);
        XYDataset regressionDataset = DatasetUtilities.sampleFunction2D(linefunction2d, lowerBound, upperBound, 2, "Fitted Regression Line");
        xyplot.setDataset(3, regressionDataset);
        xyplot.setRenderer(3, lineRenderer);

        int dataSize = data.size();
        IntToDoubleFunction x = i -> data.get(i).getDate().getTime();
//            String annotationSuffix = stock.getCurrency() + "/Day";
        String annotationSuffix = "(%/Day)";

        int l = 4;
        /* Draw lines between two high points */
        {
            IntToDoubleFunction y = i -> data.get(i).getHigh().doubleValue();

            l += OHLCCandleStickTrendUtil.drawLinearBounds(x, y, dataSize, l, topLinesRenderers, Comparator.naturalOrder(), xyplot, annotationSuffix, font, includeCurrentDayInSlopCalculation, data);
        }

        /* Draw lines between two low points */
        {
            IntToDoubleFunction y = i -> data.get(i).getLow().doubleValue();

            l += OHLCCandleStickTrendUtil.drawLinearBounds(x, y, dataSize, l, bottomLinesRenderers, Comparator.reverseOrder(), xyplot, annotationSuffix, font, includeCurrentDayInSlopCalculation, data);
        }

        mainPanel.add(chartPanel, BorderLayout.CENTER);

        return mainPanel;
//
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        double width = screenSize.getWidth();
//        double height = screenSize.getHeight();
//
//        JFrame jFrame = new JFrame();
//        jFrame.add(mainPanel);
//        jFrame.setVisible(true);
//        jFrame.setSize((int)width, (int)height);
//        jFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }


    private JFreeChart createChart(final OHLCDataset dataset, Stock stock, Font titleFont) {
        XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, null);
        plot.setDataset(dataset);
        plot.setRenderer(candlestickRenderer);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainMinorGridlinePaint(Color.blue);

        String title = "Symbol: " + stock.getSymbol() + " , " + "Name: " + stock.getName() + " , " + "Currency: "
                + stock.getCurrency() + " , " + "StockExchange: " + stock.getStockExchange();


//
//        //volum bar chart
//        IntervalXYDataset   volumeDataset  = getVolumeDataset(dataset, 24 * 60 * 60 * 1000); // Each bar is 24 hours wide.
//        NumberAxis          volumeAxis     = new NumberAxis("Volume");
//        XYBarRenderer       volumeRenderer = new XYBarRenderer();
//        XYPlot              volumePlot     = new XYPlot(volumeDataset, domainAxis, volumeAxis, volumeRenderer);
//        volumeRenderer.setSeriesPaint(0, Color.BLUE);
//
//        //Build Combined Plot
//        CombinedDomainXYPlot mainPlot = new CombinedDomainXYPlot(domainAxis);
//        mainPlot.add(plot);
//        mainPlot.add(volumePlot);


        return new JFreeChart(title, titleFont, plot, true);
    }


    private static OHLCDataset createDataset(List<OHLCDataItem> data) {
        OHLCDataItem[] res = data.toArray(OHLCDataItem[]::new);
        return new DefaultOHLCDataset("Stock data", res);
    }


    private static void stockNews() {
    }


    protected static IntervalXYDataset getVolumeDataset(final OHLCDataset priceDataset, final long barWidthInMilliseconds) {
        return new AbstractIntervalXYDataset() {
            public int getSeriesCount() {
                return priceDataset.getSeriesCount();
            }

            public Comparable getSeriesKey(int series) {
                return priceDataset.getSeriesKey(series) + "-Volume";
            }

            public int getItemCount(int series) {
                return priceDataset.getItemCount(series);
            }

            public Number getX(int series, int item) {
                return priceDataset.getX(series, item);
            }

            public Number getY(int series, int item) {
                return priceDataset.getVolume(series, item);
            }

            public Number getStartX(int series, int item) {
                return priceDataset.getX(series, item).doubleValue() - barWidthInMilliseconds / 2;
            }

            public Number getEndX(int series, int item) {
                return priceDataset.getX(series, item).doubleValue() + barWidthInMilliseconds / 2;
            }

            public Number getStartY(int series, int item) {
                return new Double(0.0);
            }

            public Number getEndY(int series, int item) {
                return priceDataset.getVolume(series, item);
            }
        };
    }
}
