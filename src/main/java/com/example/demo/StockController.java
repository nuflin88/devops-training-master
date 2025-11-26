package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 👈 Annotation needed for Spring to detect it as a REST endpoint
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    @Autowired // Inject the service
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{symbol}")
    public StockDTO getStockData(@PathVariable String symbol) {
        // Log the request to confirm it reached the controller
        System.out.println("Received request for ticker: " + symbol);
        return stockService.getStockData(symbol);
    }
}