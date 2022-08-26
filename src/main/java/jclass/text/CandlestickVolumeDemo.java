package jclass.text;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class CandlestickVolumeDemo {
    /*public static void main(String[] args) {
        ChartFrame chartFrame = new ChartFrame("Candlestick + Volume Demo", buildChart());
        chartFrame.setSize(600, 600);
        chartFrame.setVisible(true);
    }*/

    public static JFreeChart buildChart() {
        //Shared date axis
        DateAxis domainAxis = new DateAxis("Date");


        //Build Candlestick Chart based on stock price OHLC
        OHLCDataset priceDataset = getPriceDataSet("MSFT");
        NumberAxis priceAxis = new NumberAxis("Price");

        CandlestickRenderer priceRenderer = new CandlestickRenderer();

        XYPlot pricePlot = new XYPlot(priceDataset, domainAxis, priceAxis, priceRenderer);
        priceRenderer.setSeriesPaint(0, Color.BLACK);
        priceRenderer.setDrawVolume(false);
        priceAxis.setAutoRangeIncludesZero(false);


        //Build Bar Chart for volume by wrapping price dataset with an IntervalXYDataset
        IntervalXYDataset volumeDataset = getVolumeDataset(priceDataset, 24 * 60 * 60 * 1000); // Each bar is 24 hours wide.
        NumberAxis volumeAxis = new NumberAxis("Volume");
        XYBarRenderer volumeRenderer = new XYBarRenderer();
        XYPlot volumePlot = new XYPlot(volumeDataset, domainAxis, volumeAxis, volumeRenderer);
        volumeRenderer.setSeriesPaint(0, Color.BLUE);

        //Build Combined Plot
        CombinedDomainXYPlot mainPlot = new CombinedDomainXYPlot(domainAxis);
        mainPlot.add(pricePlot);
        mainPlot.add(volumePlot);

        return new JFreeChart("Microsoft", null, mainPlot, false);
    }

    protected static OHLCDataset getPriceDataSet(String symbol) {
        List<OHLCDataItem> dataItems = new ArrayList<OHLCDataItem>();
        try {
            String strUrl = "http://ichart.finance.yahoo.com/table.csv?s=" + symbol + "&a=0&b=1&c=2008&d=3&e=30&f=2008&ignore=.csv";
            URL url = new URL(strUrl);
            System.out.println("************************* URL : " + url.toString() + "*************************");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            DateFormat df = new SimpleDateFormat("y-M-d");
            String inputLine;
            in.readLine();
            while ((inputLine = in.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(inputLine, ",");
                System.out.println("************************* st : " + st.toString() + "*************************");
                Date date = df.parse(st.nextToken());
                double open = Double.parseDouble(st.nextToken());
                double high = Double.parseDouble(st.nextToken());
                double low = Double.parseDouble(st.nextToken());
                double close = Double.parseDouble(st.nextToken());
                double volume = Double.parseDouble(st.nextToken());
                double adjClose = Double.parseDouble(st.nextToken());
                OHLCDataItem item = new OHLCDataItem(date, open, high, low, close, volume);
                dataItems.add(item);
            }
            in.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        Collections.reverse(dataItems);
        OHLCDataItem[] data = dataItems.toArray(new OHLCDataItem[dataItems.size()]);
        return new DefaultOHLCDataset(symbol, data);
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
