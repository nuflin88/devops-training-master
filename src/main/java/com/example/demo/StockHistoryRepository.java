package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockDataEntity, String> {

    // Custom method to find all data points for a given symbol, ordered by date
    List<StockDataEntity> findBySymbolOrderByDateAsc(String symbol);
}