package jclass.chart;

import jclass.dto.StockTrendInfo;
import jclass.enums.CandleChartsEnum;
import jclass.CombinedView.LoggerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class JFrameForMainMenu extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JFrameForMainMenu.class);

    public JFrameForMainMenu(String title, HashMap<Long, List<StockTrendInfo>> map, CandleChartsEnum chartName, int nofTrends, boolean logChart) {
        super(title);
        getContentPane().setLayout(new BorderLayout(0, 0));
        JPanel mainPanel = new JPanel(new BorderLayout());
        Font font = new Font(null, Font.PLAIN, 20);

        ActionListener listener = event -> {
            String key = event.getActionCommand();

            switch (chartName) {
                case UPWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_DESC:
                    createChartForSecondOrderAccelerationSort(map, Long.valueOf(key));
                    break;
                case DOWNWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_ASC:
                    createChartForSecondOrderDecelerationSort(map, Long.valueOf(key));
                    break;
                case RECENT_TWO_UPWARD_TREND_SECOND_ORDER_SORT_DESC:
                    createChartForRecentTwoAccelerationSecondOrderSort(map, Long.valueOf(key));
                    break;
                case RECENT_TWO_DOWNWARD_TREND_SECOND_ORDER_SORT_DESC:
                    createChartForRecentTwoDecelerationSecondOrderSort(map, Long.valueOf(key));
                    break;
            }
        };

        ButtonGroup stockTrendButtonGroup = new ButtonGroup();
        for (Long key : map.keySet()) {
            String radioBtnTitle = createRadioButtonTitle(chartName, key, map.get(key).size());
            JRadioButton radioButton = new JRadioButton(radioBtnTitle);
            radioButton.setFont(font);
            radioButton.setActionCommand(key.toString());
            radioButton.addActionListener(listener);
            stockTrendButtonGroup.add(radioButton);
        }

        JPanel stockTrendRadioPanel = new JPanel(new GridLayout(0, 1));
        stockTrendRadioPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 30));
        for (Iterator<AbstractButton> it = stockTrendButtonGroup.getElements().asIterator(); it.hasNext(); ) {
            stockTrendRadioPanel.add(it.next());
        }

        JScrollPane stockTrendScrollPaneAsc = new JScrollPane(stockTrendRadioPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        stockTrendScrollPaneAsc.getVerticalScrollBar().setUnitIncrement(20);
        mainPanel.add(stockTrendScrollPaneAsc, BorderLayout.EAST);
        getContentPane().add(mainPanel);
    }

    public static void create(String title, HashMap<Long, List<StockTrendInfo>> map, CandleChartsEnum chartName, boolean logChart) {
        initializeJFrame(title, map, chartName, 0, logChart);
    }

    public static void create(String title, HashMap<Long, List<StockTrendInfo>> map, CandleChartsEnum chartName, int slopDaysDifferenceWithCurrentDate, boolean logChart) {
        initializeJFrame(title, map, chartName, slopDaysDifferenceWithCurrentDate, logChart);
    }

    private static void initializeJFrame(String title, HashMap<Long, List<StockTrendInfo>> map, CandleChartsEnum chartName, int nofTrends, boolean logChart) {
        LoggerManager.initializeLoggingContext();
        //log.info("Init Start");
        JFrame jFrame = new JFrameForMainMenu(title, map, chartName, nofTrends, logChart);
        jFrame.setVisible(true);
        jFrame.setSize(1400, 800);
        jFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public static String createRadioButtonTitle(CandleChartsEnum chartName, Long key, int size) {
//        if (chartName == UPWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_DESC || chartName == DOWNWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_ASC)
//        {
        return key + " Day nearest slope has " + size + " stocks";
//        }
//        if (chartName == DOWNWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_ASC)
//        {
//            return key + " Day nearest slope has " + size + " stocks";
//        }
//        return null;
    }

    private void createChartForRecentTwoAccelerationSecondOrderSort(HashMap<Long, List<StockTrendInfo>> map, Long key) {
        OHLCCandleChartJFrame.create("OHLC Acceleration chart second order sort desc", map.get(key), CandleChartsEnum.RECENT_TWO_UPWARD_TREND_SECOND_ORDER_SORT_DESC, key.intValue(), false, true);//todo make configurable
    }

    private void createChartForRecentTwoDecelerationSecondOrderSort(HashMap<Long, List<StockTrendInfo>> map, Long key) {
        OHLCCandleChartJFrame.create("OHLC Deceleration chart second order sort desc", map.get(key), CandleChartsEnum.RECENT_TWO_DOWNWARD_TREND_SECOND_ORDER_SORT_DESC, key.intValue(), false, true);//todo make configurable
    }

    private void createChartForSecondOrderDecelerationSort(HashMap<Long, List<StockTrendInfo>> map, Long key) {
        List<StockTrendInfo> nearestAcceleration = map.get(key).stream()
                .peek(stockTrendInfo -> stockTrendInfo.setSortingIndex(stockTrendInfo.getDaySlopIndexForSort().get(key)))
                .sorted(new StockTrendInfo.SortByDownwardTrendDistanceSlopInPercentageBySortingIndex())
                .collect(Collectors.toList());
        OHLCCandleChartJFrame.create("OHLC Deceleration chart second order sort desc", nearestAcceleration, CandleChartsEnum.DOWNWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_ASC, key.intValue(), false, true);//todo make configurable
    }

    private void createChartForSecondOrderAccelerationSort(HashMap<Long, List<StockTrendInfo>> map, Long key) {
        List<StockTrendInfo> nearestAcceleration = map.get(key).stream()
                .peek(stockTrendInfo -> stockTrendInfo.setSortingIndex(stockTrendInfo.getDaySlopIndexForSort().get(key)))
                .sorted(new StockTrendInfo.SortByUpwardTrendDistanceSlopInPercentageBySortingIndex())
                .collect(Collectors.toList());
        Collections.reverse(nearestAcceleration);
        OHLCCandleChartJFrame.create("OHLC Acceleration chart second order sort desc", nearestAcceleration, CandleChartsEnum.UPWARD_TREND_NEAREST_SLOPE_SECOND_ORDER_SORT_DESC, key.intValue(), false, true);//todo make it configurable
    }
}
