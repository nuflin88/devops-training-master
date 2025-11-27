package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper; // <-- Import ObjectMapper
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class StockChartController {

    private final StockService stockService;
    private final ObjectMapper objectMapper; // <-- Declare ObjectMapper

    // Inject both service and object mapper
    public StockChartController(StockService stockService, ObjectMapper objectMapper) {
        this.stockService = stockService;
        this.objectMapper = objectMapper; // <-- Initialize ObjectMapper
    }

    @GetMapping("/chart")
    public String showStockChart(@RequestParam(defaultValue = "MSFT") String symbol, Model model) {
        System.out.println(">>> REQUEST RECEIVED for CHART: " + symbol);

        List<StockHistory> history = stockService.getStockHistory(symbol);

        try {
            // Convert the Java List<StockHistory> directly to a clean JSON String
            String historyJson = objectMapper.writeValueAsString(history);
            model.addAttribute("historyJson", historyJson); // <-- Add JSON string to model

            // We no longer need the Java object, but we keep the symbol
            model.addAttribute("symbol", symbol.toUpperCase());

        } catch (Exception e) {
            e.printStackTrace();
            // Handle error, return empty list or error view
            model.addAttribute("historyJson", "[]");
        }

        return "stock-chart";
    }
}