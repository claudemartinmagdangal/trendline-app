package jclass.util;

import jclass.dto.Symbol;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public class ReadExcel {

    private static Set<Symbol> set = new HashSet<>();

    public static List<Symbol> readExcel() {

        List<Symbol> list = new ArrayList<>();

        try {
            FileInputStream file = new FileInputStream(new File("Symbol" + File.separator + "Us_Stock_Symbols_list.xlsx"));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row != null) {
                    String symbol = row.getCell(0).getStringCellValue();
                    String company = row.getCell(1).getStringCellValue();
                    String industry = row.getCell(2).getStringCellValue();
                    //set.add(new Symbol(symbol, company, industry));
                    list.add(new Symbol(symbol, company, industry));
                }
            }
            file.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        list.addAll(set);

        return list;
    }


    public static List<String> headList() {

        Set<String> set = new HashSet<>();
        for (Symbol symbol : readExcel())
            set.add(symbol.getIndustry());

        List<String> industryList = new ArrayList<>(set);
        Collections.sort(industryList);

        //System.out.println(industryList);
        return industryList;
    }


    public static void writeExcell(Map<String, Object[]> data) {
        //Blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet("Demo_Stock_Data");

        //Iterate over data and write to sheet
        Set<String> keyset = data.keySet();
        int rownum = 0;
        for (String key : keyset) {
            Row row = sheet.createRow(rownum++);
            Object[] objArr = data.get(key);
            int cellnum = 0;
            for (Object obj : objArr) {
                Cell cell = row.createCell(cellnum++);
                if (obj instanceof String)
                    cell.setCellValue((String) obj);
                else if (obj instanceof Integer)
                    cell.setCellValue((Integer) obj);
                else if (obj instanceof Date)
                    cell.setCellValue((Date) obj);
                else if (obj instanceof Double)
                    cell.setCellValue((Double) obj);
            }
        }
        try {
            //Write the workbook in file system
            FileOutputStream out = new FileOutputStream(new File("demoStockData.xlsx"));
            workbook.write(out);
            out.close();
            //System.out.println("howtodoinjava_demo.xlsx written successfully on disk.");

        } catch (Exception e) {
            //e.printStackTrace();
        }

    }

}
