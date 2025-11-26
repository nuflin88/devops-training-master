package com.example.demo;

import java.util.Objects;
import java.math.BigDecimal; // Recommended for financial data

// This is the simplified model for a single data point on the chart.
public class StockHistory {
    private String date;
    private BigDecimal price; // Use BigDecimal for accuracy with money

    // Constructor
    public StockHistory(String date, BigDecimal price) {
        this.date = date;
        this.price = price;
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}