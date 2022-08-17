package jclass.util;

import jclass.dto.StockTrendInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class SearchinArticleInfo {
    public static void main(String[] args) throws IOException {
//
//        String url = "https://finance.yahoo.com/quote/TSLA/";
//        Document doc = Jsoup.connect(url).get();
//        Element link = doc.select("td.Ta(end) Fw(600) Lh(14px)").first();
//
//        System.out.println(link.text());

        writeFile1();
        readFile1();
    }

    public static void readFile1() throws IOException {
        try {
            File file = new File("RowMoved.txt");    //creates a new file instance

            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            StringBuffer sb = new StringBuffer();    //constructs a string buffer with no characters
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);      //appends line to string buffer
                sb.append("\n");     //line feed
            }
            fr.close();    //closes the stream and release the resources
            //System.out.println("Contents of File: ");
            //System.out.println(sb);   //returns a string that textually represents the object
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeFile1() throws IOException {
        File fout = new File("RowMoved.txt");
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (int i = 0; i < 10; i++) {
            bw.write("something");
            bw.newLine();
        }

        bw.close();
    }

    public static List<StockTrendInfo> removeDuplicates(List<StockTrendInfo> list){

        // Create a new LinkedHashSet
        Set<StockTrendInfo> set = new LinkedHashSet<>();

        // Add the elements to set
        set.addAll(list);

        // Clear the list
        list.clear();

        // add the elements of set
        // with no duplicates to the list
        list.addAll(set);

        // return the list
        return list;
    }
}
