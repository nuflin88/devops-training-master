package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class StockChartController {

    private final StockService stockService;

    public StockChartController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/chart")
    public String showStockChart(@RequestParam(defaultValue = "PLTR") String symbol, Model model) {

        // --- REAL DATA FETCH ---
        List<StockHistory> history = stockService.getStockHistory(symbol);

        model.addAttribute("symbol", symbol.toUpperCase());
        model.addAttribute("history", history); // Pass the real historical data list

        return "stock-chart";
    }
}