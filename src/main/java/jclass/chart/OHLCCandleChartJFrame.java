package jclass.chart;

import jclass.dto.StockTrendInfo;
import jclass.dto.StockTrendInfoTableModel;
import jclass.enums.CandleChartsEnum;
import jclass.util.OHLCCandleStickTrendUtil;
import jclass.CombinedView.LoggerManager;
import jclass.util.Utils;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
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
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.IntToDoubleFunction;

public class OHLCCandleChartJFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(OHLCCandleChartJFrame.class);
    private ChartPanel chartPanel;
    private final CandlestickRenderer candlestickRenderer;
    private final DateAxis domainAxis;
    private final NumberAxis rangeAxis;
    JButton button = new JButton();

    // title= symble of stock
    // stokTrends = summary information
    // cartName= to generating menu
    // slopDaysDifferenceWithCurrentDate
    public OHLCCandleChartJFrame(String title,
                                 List<StockTrendInfo> stockTrends,
                                 CandleChartsEnum chartName,
                                 int slopDaysDifferenceWithCurrentDate,
                                 boolean logChart,
                                 boolean includeCurrentDayInSlopCalculation) {
        super(title);
        getContentPane().setLayout(new BorderLayout(0, 0));

        domainAxis = new DateAxis();
        rangeAxis = new NumberAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

        candlestickRenderer = new CandlestickRenderer();
        candlestickRenderer.setSeriesStroke(0, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));

        candlestickRenderer.setSeriesPaint(0, Color.black);
        candlestickRenderer.setDrawVolume(true);
        Color c2 = new Color(0,255,128);
        candlestickRenderer.setVolumePaint(c2);

        JPanel mainPanel = new JPanel(new BorderLayout());
        Stroke lineStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        XYItemRenderer[] topLinesRenderers = Arrays.stream(new Color[]{Color.DARK_GRAY, Color.MAGENTA }).map(color -> {
            XYItemRenderer r = new SamplingXYLineRenderer();
            r.setSeriesPaint(0, color);
            r.setSeriesStroke(0, lineStroke);
            return r;
        }).toArray(XYItemRenderer[]::new);


        XYItemRenderer[] bottomLinesRenderers = Arrays.stream(new Color[]{Color.BLUE, Color.CYAN}).map(color -> {
            XYItemRenderer r = new SamplingXYLineRenderer();
            r.setSeriesPaint(0, color);
            r.setSeriesStroke(0, lineStroke);
            return r;
        }).toArray(XYItemRenderer[]::new);



        XYItemRenderer lineRenderer = new SamplingXYLineRenderer();
        lineRenderer.setSeriesStroke(0, lineStroke);
        lineRenderer.setSeriesPaint(0, Color.RED);

        Font font = new Font(null, Font.PLAIN, 16);

        ActionListener listener = event -> {
            String symbol = event.getActionCommand();

            Map.Entry<Stock, java.util.List<OHLCDataItem>> entry;
            try {
                entry = Utils.loadFromFile(symbol);
            } catch (IOException e) {
                throw new Error("Error occurred while loading data", e);
            }
            Stock stock = entry.getKey();
            java.util.List<OHLCDataItem> data = entry.getValue();
            OHLCDataset dataset = createDataset(data);

            if (chartPanel != null) {
                mainPanel.remove(chartPanel);
            }

            JFreeChart jFreeChart = createChart(dataset, stock, font);

            chartPanel = new ChartPanel(jFreeChart);
            chartPanel.setMouseWheelEnabled(true);
            chartPanel.setMouseZoomable(true, true);
            chartPanel.setDomainZoomable(true);
            chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            chartPanel.setBackground(Color.ORANGE);
            chartPanel.setZoomAroundAnchor(true);

            mainPanel.add(chartPanel, BorderLayout.CENTER);

            XYPlot xyplot = (XYPlot) jFreeChart.getPlot();

            if (logChart) {
                LogarithmicAxis yAxis = new LogarithmicAxis("Y");
                yAxis.setAutoRange(true);
                yAxis.setAllowNegativesFlag(true);
                yAxis.setAutoRangeNextLogFlag(true);
                xyplot.setRangeAxis(yAxis);
            }

            /* Draw 49-day moving average Line */

            TimeSeries timeSeries = new TimeSeries("49-day moving average");
            for (OHLCDataItem item : entry.getValue()) {
                timeSeries.add(new Day(item.getDate()), item.getClose());

            }

            TimeSeries movingAverage = MovingAverage.createMovingAverage(timeSeries, "LT", 30, 30);
            TimeSeriesCollection movingAverageDataset = new TimeSeriesCollection();
            movingAverageDataset.addSeries(movingAverage);

            xyplot.setDataset(2, movingAverageDataset);
            xyplot.setRenderer(2, lineRenderer);

            /* Draw Fitted Regression (Trend) Line */
            double lowerBound = xyplot.getDomainAxis().getRange().getLowerBound();
            double upperBound = xyplot.getDomainAxis().getRange().getUpperBound();
            double[] regressionParameters = Regression.getOLSRegression(dataset, 0);

            LineFunction2D linefunction2d = new LineFunction2D(regressionParameters[0], regressionParameters[1]);
            XYDataset regressionDataset = DatasetUtilities.sampleFunction2D(linefunction2d, lowerBound, upperBound,2, "Fitted Regression Line");
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

                l += OHLCCandleStickTrendUtil.drawLinearBounds(x,y,dataSize,l,topLinesRenderers,null,xyplot,annotationSuffix,font,includeCurrentDayInSlopCalculation, data);
            }

            /* Draw lines between two low points */
            {
                IntToDoubleFunction y = i -> data.get(i).getLow().doubleValue();

                l += OHLCCandleStickTrendUtil.drawLinearBounds(x, y, dataSize, l, bottomLinesRenderers, Comparator.reverseOrder(), xyplot,annotationSuffix, font, includeCurrentDayInSlopCalculation, data);
            }

            mainPanel.updateUI();
        };

        ButtonGroup stockTrendButtonGroup = new ButtonGroup();


        for (StockTrendInfo stockTrendInfo : stockTrends) {
            String radioBtnTitle = createRadioButtonTitle(chartName, stockTrendInfo, slopDaysDifferenceWithCurrentDate);
            JRadioButton radioButton = new JRadioButton(radioBtnTitle);
            radioButton.setFont(font);
            radioButton.setActionCommand(stockTrendInfo.getSymbol());
            radioButton.addActionListener(listener);
            stockTrendButtonGroup.add(radioButton);
        }

        JPanel stockTrendRadioPanel = new JPanel(new GridLayout(0, 1));
        stockTrendRadioPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 30));

        for (Iterator<AbstractButton> it = stockTrendButtonGroup.getElements().asIterator(); it.hasNext(); ) {
            stockTrendRadioPanel.add(it.next());
        }

         JScrollPane stockTrendScrollPaneAsc= new JScrollPane(stockTrendRadioPanel,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
         stockTrendScrollPaneAsc.getVerticalScrollBar().setUnitIncrement(20);
         TextField tf1 =new TextField(15);
         TextField tf2 =new TextField(15);



         ActionListener  btn = event -> {
            ButtonGroup  gb = new ButtonGroup();

            stockTrendRadioPanel.removeAll();
            String except= tf1.getText();
            String searchSymble= tf2.getText();

            for (StockTrendInfo stockTrendInfo : stockTrends) {

                String radioBtnTitle = createRadioButtonTitle(chartName, stockTrendInfo, slopDaysDifferenceWithCurrentDate);

                JRadioButton radioButton = new JRadioButton(radioBtnTitle);
                radioButton.setFont(font);
                radioButton.setActionCommand(stockTrendInfo.getSymbol());
                radioButton.addActionListener(listener);

                if (!searchSymble.isEmpty()) {
                    if (radioBtnTitle.contains(searchSymble)) {
                        gb.add(radioButton);
                    }
                }
                if (!except.isEmpty()) {
                    if (!radioBtnTitle.contains(except)) {
                        gb.add(radioButton);
                    }
                }
                if (except.isEmpty() && searchSymble.isEmpty()) {
                    gb.add(radioButton);
                }
            }

            for (Iterator<AbstractButton> it = gb.getElements().asIterator(); it.hasNext(); ) {
                stockTrendRadioPanel.add(it.next());
            }

            stockTrendRadioPanel.updateUI();
            stockTrendScrollPaneAsc.updateUI();
            mainPanel.updateUI();
        };


        ActionListener  btnShow = event -> {


        };


        JPanel searching = new JPanel(new GridLayout(0, 1));

        Button bt2 =new Button("Filter");
        Button btn3 =new Button("Show Table");
        bt2.addActionListener(btn);
        btn3.addActionListener(btnShow);
        searching.add(tf1);
        searching.add(tf2);
        searching.add(bt2);
        searching.add(btn3);


        JPanel menu= new JPanel();
        menu.add(searching);

        mainPanel.add(menu, BorderLayout.NORTH);
        mainPanel.add(stockTrendScrollPaneAsc, BorderLayout.EAST);

        getContentPane().add(mainPanel);
    }

    public static void create(String title, List<StockTrendInfo> stockTrends, CandleChartsEnum chartName, boolean logChart, boolean includeCurrentDayInSlopCalculation) {
        initializeJFrame(title, stockTrends, chartName, 0, logChart, includeCurrentDayInSlopCalculation);
    }

    public static void create(String title, List<StockTrendInfo> stockTrends, CandleChartsEnum chartName, int slopDaysDifferenceWithCurrentDate, boolean logChart, boolean includeCurrentDayInSlopCalculation) {
        initializeJFrame(title, stockTrends, chartName, slopDaysDifferenceWithCurrentDate, logChart, includeCurrentDayInSlopCalculation);
    }

    private static void initializeJFrame(String title, List<StockTrendInfo> slopDaysDifferenceWithCurrentDate, CandleChartsEnum chartName, int nofTrends, boolean logChart, boolean includeCurrentDayInSlopCalculation) {
        LoggerManager.initializeLoggingContext();
        //log.info("Init Start");
        JFrame jFrame = new OHLCCandleChartJFrame(title, slopDaysDifferenceWithCurrentDate, chartName, nofTrends, logChart, includeCurrentDayInSlopCalculation);
        jFrame.setVisible(true);
        jFrame.setSize(1400, 800);
        jFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private static OHLCDataset createDataset(List<OHLCDataItem> data) {
        OHLCDataItem[] res = data.toArray(OHLCDataItem[]::new);
        return new DefaultOHLCDataset("Stock data", res);
    }

    public static String createRadioButtonTitle(CandleChartsEnum chartName, StockTrendInfo stockTrendInfo, int slopDaysDifferenceWithCurrentDate) {
        switch (chartName) {
            case DOWNWARD_TREND_ASC:
            case DOWNWARD_TREND_DESC:
                return stockTrendInfo.getSymbol() + ": " + stockTrendInfo.getMaxPercentageChangeDownWardTrend() + "% change in downward trend";
            case DOWNWARD_TREND_COUNT_ASC:
            case DOWNWARD_TREND_COUNT_DESC:
                return stockTrendInfo.getSymbol() + ": max " + stockTrendInfo.getMaxDownwardTrendCount() + " Deceleration";
            case UPWARD_TREND_ASC:
            case UPWARD_TREND_DESC:
                return stockTrendInfo.getSymbol() + ": " + stockTrendInfo.getMaxPercentageChangeUpwardTrend() + "% change in upward trend";
            case UPWARD_TREND_COUNT_ASC:
            case UPWARD_TREND_COUNT_DESC:
                return stockTrendInfo.getSymbol() + ": max " + stockTrendInfo.getMaxUpwardTrendCount() + " Acceleration";
            case UPWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_DESC:
                return stockTrendInfo.getSymbol() + ": " + slopDaysDifferenceWithCurrentDate + " Day fastest trend with slop " + stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getSortingIndex()) + "%";
            case DOWNWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_ASC:
                return stockTrendInfo.getSymbol() + ": " + slopDaysDifferenceWithCurrentDate + " Day fastest trend with slop " + stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getSortingIndex()) + "%";
            case RECENT_TWO_UPWARD_TREND_SECOND_ORDER_SORT_DESC: {
                if (stockTrendInfo.getSortingIndex() > 0)
                    return stockTrendInfo.getSymbol() + ": " + slopDaysDifferenceWithCurrentDate + " Day slop : " + stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getSortingIndex()) + "% have neighbour slop at " + (stockTrendInfo.getUpwardTrendDistances().get(stockTrendInfo.getSortingIndex() - 1) - stockTrendInfo.getUpwardTrendDistances().get(stockTrendInfo.getSortingIndex())) + " Day";
                else
                    return stockTrendInfo.getSymbol() + ": " + slopDaysDifferenceWithCurrentDate + " Day slop : " + stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getSortingIndex()) + "% have no neighbour slop";
            }
            case RECENT_TWO_DOWNWARD_TREND_SECOND_ORDER_SORT_DESC: {
                if (stockTrendInfo.getSortingIndex() > 0)
                    return stockTrendInfo.getSymbol() + ": " + slopDaysDifferenceWithCurrentDate + " Day slop : " + stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getSortingIndex()) + "% have neighbour slop at " + (stockTrendInfo.getDownwardTrendDistances().get(stockTrendInfo.getSortingIndex() - 1) - stockTrendInfo.getDownwardTrendDistances().get(stockTrendInfo.getSortingIndex())) + " Day";
                else
                    return stockTrendInfo.getSymbol() + ": " + slopDaysDifferenceWithCurrentDate + " Day slop : " + stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getSortingIndex()) + "% have no neighbour slop";
            }
        }
        return null;
    }

    private JFreeChart createChart(final OHLCDataset dataset, Stock stock, Font titleFont) {


        XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, null);
        plot.setDataset(dataset);
        plot.setRenderer(candlestickRenderer);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainMinorGridlinePaint(Color.blue);

        String title = "Symbol: " + stock.getSymbol() + " , " + "Name: " + stock.getName() + " , " + "Currency: "
                + stock.getCurrency() + " , " + "StockExchange: " + stock.getStockExchange();
        return new JFreeChart(title, titleFont, plot, true);
    }
}
