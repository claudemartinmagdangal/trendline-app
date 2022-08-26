package jclass.dto;

public class Symbol {

    private String symbol;
    private String companyName;
    private String industry;


    public Symbol() {
        super();
    }


    public Symbol(String symbol, String companyName, String industry) {
        super();
        this.symbol = symbol;
        this.companyName = companyName;
        this.industry = industry;
    }


    public String getSymbol() {
        return symbol;
    }


    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }


    public String getCompanyName() {
        return companyName;
    }


    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }


    public String getIndustry() {
        return industry;
    }


    public void setIndustry(String industry) {
        this.industry = industry;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((companyName == null) ? 0 : companyName.hashCode());
        result = prime * result + ((industry == null) ? 0 : industry.hashCode());
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Symbol other = (Symbol) obj;
        if (companyName == null) {
            if (other.companyName != null) return false;
        } else if (!companyName.equals(other.companyName)) return false;
        if (industry == null) {
            if (other.industry != null) return false;
        } else if (!industry.equals(other.industry)) return false;
        if (symbol == null) {
            if (other.symbol != null) return false;
        } else if (!symbol.equals(other.symbol)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Symbol{" + "symbol='" + symbol + '\'' + ", companyName='" + companyName + '\'' + ", industry='" + industry + '\'' + '}';
    }
}
