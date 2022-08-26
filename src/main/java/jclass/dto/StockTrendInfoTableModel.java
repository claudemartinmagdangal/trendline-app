package jclass.dto;

import jclass.io.FileUtils;

import javax.swing.table.AbstractTableModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockTrendInfoTableModel extends AbstractTableModel {

    private static String[] columnNames = {
            "ID",
            "Symbol",
            "Company",
            "Industry",
            "%ChangeUpTrend",
            "%ChangeDownTrend",
            "UpTrendCount",
            "DownTrendCount",
            "UpTrendDistances",
            "Up%SlopeTrendDistance",
            "TotalUpTrendDistance",
            "DownTrendDistances",
            "Down%SlopeTrendDistance",
            "TotalDownTrendDistance",
            "Volume/30",
            "Insider Trading",
            "Price",
            "News Story"
    };

    private List<String> list;
    private Map<String, Integer> mlist;
    private Boolean isMovedColumn = false;

    {
        list = FileUtils.readFile();
        mlist = new HashMap<>();
        if (list.size() > 0) {

            //System.out.println(list.size());

            for (int i = 0; i < list.size(); i++) {
                columnNames[i] = list.get(i);
                mlist.put(list.get(i), i);
                //System.out.println(i +" : "+ list.get(i));
            }
            isMovedColumn = true;
        }
//        else{
//            System.out.println("empty list ::::");
//            System.out.println(list);
//        }
    }

    private List<StockTrendInfo> listStockTrendInfo;

    public StockTrendInfoTableModel(List<StockTrendInfo> listStockTrendInfo) {
        loadTable();
        this.listStockTrendInfo = listStockTrendInfo;
        int indexCount = 1;
        for (StockTrendInfo stockTrendInfo : listStockTrendInfo) {
            stockTrendInfo.setIndex(indexCount++);
        }
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        StockTrendInfo stockTrendInfo = listStockTrendInfo.get(rowIndex);
        Object returnValue = null;

        Long upwardTrendDistances = 0l;
        if (!stockTrendInfo.getUpwardTrendDistances().isEmpty())
            upwardTrendDistances = stockTrendInfo.getUpwardTrendDistances().get(stockTrendInfo.getUpwardTrendDistances().size() - 1);


        Double UpwardTrendDistanceSlopInPercentage = 0.0;
        if (!stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().isEmpty())
            UpwardTrendDistanceSlopInPercentage = stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getUpwardTrendDistanceSlopInPercentage().size() - 1);

        Long DownwardTrendDistances = 0l;
        if (!stockTrendInfo.getDownwardTrendDistances().isEmpty())
            DownwardTrendDistances = stockTrendInfo.getDownwardTrendDistances().get(stockTrendInfo.getDownwardTrendDistances().size() - 1);

        Double DownwardTrendDistanceSlopInPercentage = 0.0;
        if (!stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().isEmpty())
            DownwardTrendDistanceSlopInPercentage = stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().get(stockTrendInfo.getDownwardTrendDistanceSlopInPercentage().size() - 1);

        if (isMovedColumn) {
            String columnName = list.get(columnIndex);
            if (columnName.equals("ID"))
                returnValue = stockTrendInfo.getIndex();
            else if (columnName.equals("Symbol"))
                returnValue = stockTrendInfo.getSymbol();
            else if (columnName.equals("Company"))
                returnValue = stockTrendInfo.getCompany();
            else if (columnName.equals("Industry"))
                returnValue = stockTrendInfo.getIndustry();
            else if (columnName.equals("%ChangeUpTrend"))
                returnValue = stockTrendInfo.getMaxPercentageChangeUpwardTrend();
            else if (columnName.equals("%ChangeDownTrend"))
                returnValue = stockTrendInfo.getMaxPercentageChangeDownWardTrend();
            else if (columnName.equals("UpTrendCount"))
                returnValue = stockTrendInfo.getMaxUpwardTrendCount();
            else if (columnName.equals("DownTrendCount"))
                returnValue = stockTrendInfo.getMaxDownwardTrendCount();
            else if (columnName.equals("UpTrendDistances"))
                returnValue = upwardTrendDistances;
            else if (columnName.equals("Up%SlopeTrendDistance"))
                returnValue = UpwardTrendDistanceSlopInPercentage;
            else if (columnName.equals("TotalUpTrendDistance"))
                returnValue = stockTrendInfo.getTotalUpwardTrendDistance();
            else if (columnName.equals("DownTrendDistances"))
                returnValue = DownwardTrendDistances;
            else if (columnName.equals("Down%SlopeTrendDistance"))
                returnValue = DownwardTrendDistanceSlopInPercentage;
            else if (columnName.equals("TotalDownTrendDistance"))
                returnValue = stockTrendInfo.getTotalDownwardTrendDistance();
            else if (columnName.equals("Volume/30"))
                returnValue = stockTrendInfo.getVolumn();
            else if (columnName.equals("Insider Trading"))
                returnValue = stockTrendInfo.getInsidertrading();
            else if (columnName.equals("Price"))
                returnValue = stockTrendInfo.getPrice();
            else if (columnName.equals("News Story"))
                returnValue = stockTrendInfo.getNewsstory();
            else
                throw new IllegalArgumentException("Invalid column index");


        } else {
            switch (columnIndex) {
                case 0:
                    returnValue = stockTrendInfo.getIndex();
                    break;
                case 1:
                    returnValue = stockTrendInfo.getSymbol();
                    break;
                case 2:
                    returnValue = stockTrendInfo.getCompany();
                    break;
                case 3:
                    returnValue = stockTrendInfo.getIndustry();
                    break;
                case 4:
                    returnValue = stockTrendInfo.getMaxPercentageChangeUpwardTrend();
                    break;
                case 5:
                    returnValue = stockTrendInfo.getMaxPercentageChangeDownWardTrend();
                    break;
                case 6:
                    returnValue = stockTrendInfo.getMaxUpwardTrendCount();
                    break;
                case 7:
                    returnValue = stockTrendInfo.getMaxDownwardTrendCount();
                    break;
                case 8:
                    returnValue = upwardTrendDistances;
                    break;
                case 9:
                    returnValue = UpwardTrendDistanceSlopInPercentage;
                    break;
                case 10:
                    returnValue = stockTrendInfo.getTotalUpwardTrendDistance();
                    break;
                case 11:
                    returnValue = DownwardTrendDistances;
                    break;
                case 12:
                    returnValue = DownwardTrendDistanceSlopInPercentage;
                    break;
                case 13:
                    returnValue = stockTrendInfo.getTotalDownwardTrendDistance();
                    break;
                case 14:
                    returnValue = stockTrendInfo.getVolumn();
                    break;
                case 15:
                    returnValue = stockTrendInfo.getInsidertrading();
                    break;
                case 16:
                    returnValue = stockTrendInfo.getPrice();
                    break;
                case 17:
                    returnValue = stockTrendInfo.getNewsstory();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid column index");
            }
        }

        return returnValue;
    }

    @Override
    public int getRowCount() {
        return listStockTrendInfo.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (listStockTrendInfo.isEmpty()) {
            return Object.class;
        }
        return getValueAt(0, columnIndex).getClass();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        StockTrendInfo stockTrendInfo = listStockTrendInfo.get(rowIndex);
        if (list.get(columnIndex) == "ID") {
            stockTrendInfo.setIndex((int) value);
        }
    }

    public static String[] getColumnNames() {
        return columnNames;
    }


    private void loadTable() {
        list = FileUtils.readFile();
        mlist = new HashMap<>();
        if (list.size() > 0) {

            //System.out.println(list.size());

            for (int i = 0; i < list.size(); i++) {
                columnNames[i] = list.get(i);
                mlist.put(list.get(i), i);
                //System.out.println(i +" : "+ list.get(i));
            }
            isMovedColumn = true;
        }
//            else{
//                System.out.println("empty list");
//                System.out.println(list);
//            }

    }
}
