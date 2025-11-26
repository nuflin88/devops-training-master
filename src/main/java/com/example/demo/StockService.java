package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class StockService {

    private final RestTemplate restTemplate;

    // Injected from application.properties
    @Value("${stock.api.url}")
    private String apiUrlTemplate;

    @Value("${stock.api.key}")
    private String apiKey;

    public StockService() {
        // Initialize RestTemplate for making HTTP calls
        this.restTemplate = new RestTemplate();
    }

    public StockDTO getStockData(String symbol) {
        // ... (URL construction remains the same)

        String finalUrl = apiUrlTemplate
                .replace("{symbol}", symbol)
                .replace("{key}", apiKey);

        try {
            // 1. Make the HTTP GET request
            String jsonResponse = restTemplate.getForObject(finalUrl, String.class);

            // --- IMPROVED ERROR CHECKING ---
            if (jsonResponse == null || jsonResponse.contains("Error Message") || jsonResponse.contains("Invalid API call")) {
                System.err.println("Alpha Vantage API returned an error or rate limit message.");
                return new StockDTO(symbol, "Alpha Vantage API Error (Check key, symbol, or rate limit)", -1.0);
            }

            // 2. Check for the expected structure before attempting to parse
            if (jsonResponse.contains("Global Quote")) {

                // Your original brittle parsing logic that needs refinement:
                int priceIndex = jsonResponse.indexOf("\"05. price\"");
                if (priceIndex > 0) {
                    // Find start of the actual value after the key/colon
                    int valueStart = jsonResponse.indexOf(":", priceIndex) + 3;
                    // Find the end quote
                    int valueEnd = jsonResponse.indexOf("\"", valueStart);

                    String priceString = jsonResponse.substring(valueStart, valueEnd);
                    double price = Double.parseDouble(priceString);

                    return new StockDTO(symbol, "Alpha Vantage Stock ", price);
                }
            }

            // If it was a valid response but didn't contain "Global Quote" (e.g., symbol not found)
            return new StockDTO(symbol, "Stock Symbol Not Found", -1.0);

        } catch (Exception e) {
            System.err.println("Error fetching stock data for " + symbol + ": " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for debugging purposes
            return new StockDTO(symbol, "System Error during HTTP call", -1.0);
        }
    }
}