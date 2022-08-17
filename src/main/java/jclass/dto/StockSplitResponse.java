package jclass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSplitResponse implements Serializable {
    private String status;
    private Long count;
    private List<StockSplit> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockSplit implements Serializable {
        private String ticker;
        private String exDate;
        private String paymentDate;
        private String declaredDate;
        private long ratio;
        private long tofactor;
        private long forfactor;

        public Date getExDate() {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(exDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }

    }
}
