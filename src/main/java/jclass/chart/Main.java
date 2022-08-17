
package jclass.chart;

import jclass.dto.StockTrendInfo;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;

public class Main {

    private static List<StockTrendInfo> stockInfo;
    private static HashMap<Long, List<StockTrendInfo>> map;

    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        new StockTable();
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception evt) {
        }
    }
}
