package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_history")
public class StockDataEntity {

    // Composite key: Symbol + Date ensures uniqueness for caching
    @Id
    private String id; // Format: MSFT_2025-11-26

    private String symbol;

    // We store the date as a string for simplicity, matching the API format
    private String date;

    private BigDecimal price;

    // Default constructor needed by JPA
    public StockDataEntity() {}

    public StockDataEntity(String symbol, String date, BigDecimal price) {
        this.id = symbol + "_" + date;
        this.symbol = symbol;
        this.date = date;
        this.price = price;
    }

    // Getters and Setters (omitted for brevity, but needed in final code)
    // ... (Generate Getters/Setters for id, symbol, date, price) ...

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}