package jclass.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CrawlYahoo {

    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Safari/537.36";
    //public static final String URL = "https://finance.yahoo.com/calendar/splits?";
    //public static final String URL = "https://finance.yahoo.com/quote/TSLA/";
    public static final String URL = "https://finance.yahoo.com/quote/TSLA/";
    // public static final String SELECTOR = "a[data-test$=quotelink]";
    public static final String SELECTOR = "table[class=W(100%)]";

    /*public static void main(String[] args) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMM dd, yyyy");
        LocalDate date = LocalDate.now();
        LocalDate localDate = date.minusDays(1);
        String urlparam = URL + "day=" + localDate + "&offset=0&sf=startdatetime&st=asc";

        List<String> listSymbol = getListStockSplits(localDate, urlparam, SELECTOR);
        for (String symbol : listSymbol) {
            System.out.println(symbol);
        }
        System.out.println("Stock Splits on " + dtf.format(localDate) + " Results : " + listSymbol.size());
        System.out.println(localDate);
    }*/


    public static List<String> getListStockSplits(LocalDate localDate, String url, String selector) throws IOException {
        Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
        List<String> listSymbol = new ArrayList<>();

        for (Element searchResult : doc.select(selector)) {
            listSymbol.add(searchResult.text());
        }
        return listSymbol;
    }

}
