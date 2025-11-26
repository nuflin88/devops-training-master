package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode; // For JSON parsing
import com.fasterxml.jackson.databind.ObjectMapper; // For JSON parsing
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // NEW FIELD for JSON processing

    @Value("${stock.api.url.current}")
    private String currentApiUrlTemplate;

    @Value("${stock.api.url.history}") // NEW FIELD for history URL
    private String historyApiUrlTemplate;

    @Value("${stock.api.key}")
    private String apiKey;

    public StockService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper(); // Initialize the JSON parser
    }

    // --- Existing getStockData(String symbol) method remains the same,
    //     but ensure you update the property used inside it:
    //     (i.e., replace apiUrlTemplate with currentApiUrlTemplate)

    // ... (Your existing getStockData method) ...
    public StockDTO getStockData(String symbol) {
        String finalUrl = currentApiUrlTemplate // <-- MUST use currentApiUrlTemplate
                .replace("{symbol}", symbol)
                .replace("{key}", apiKey);
        // ... (rest of the method logic) ...
        // ... (ensure you have the updated string parsing logic from before) ...
        return new StockDTO(symbol, "Unknown Error", -1.0);
    }

    // --- NEW METHOD FOR HISTORICAL DATA ---
    public List<StockHistory> getStockHistory(String symbol) {
        List<StockHistory> historyList = new ArrayList<>();
        String finalUrl = historyApiUrlTemplate
                .replace("{symbol}", symbol)
                .replace("{key}", apiKey);

        try {
            // 1. Fetch raw JSON string
            String jsonResponse = restTemplate.getForObject(finalUrl, String.class);

            // Check for common API errors BEFORE parsing
            if (jsonResponse == null || jsonResponse.contains("Error Message") || jsonResponse.contains("Invalid API call") || jsonResponse.contains("Note")) {
                // *** ADDED: Print the full API response to the console ***
                System.err.println("--- Alpha Vantage API Error Response ---");
                System.err.println(jsonResponse);
                System.err.println("--- End API Error Response ---");

                return historyList; // returns empty list
            }

            if (jsonResponse == null || jsonResponse.contains("Error Message") || jsonResponse.contains("Invalid API call")) {
                System.err.println("Alpha Vantage Historical API returned an error or rate limit message.");
                return historyList;
            }

            // 2. Parse the root node
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // The time series data is nested under the key "Time Series (Daily)"
            // This key is static across all requests.
            JsonNode timeSeriesNode = rootNode.get("Time Series (Daily)");

            if (timeSeriesNode != null) {
                // Iterate through the daily entries
                Iterator<Map.Entry<String, JsonNode>> fields = timeSeriesNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String date = entry.getKey(); // The key is the date (e.g., "2025-11-25")
                    JsonNode dayData = entry.getValue(); // The nested object for that day

                    // Extract the "4. close" value and parse it as BigDecimal
                    String closePriceStr = dayData.get("4. close").asText();
                    BigDecimal closePrice = new BigDecimal(closePriceStr);

                    // 3. Add to list
                    historyList.add(new StockHistory(date, closePrice));

                    // Stop after 30 data points to keep the chart clean and respect API limits
                    if (historyList.size() >= 30) {
                        break;
                    }
                }

                // The data is usually returned newest-first, so we reverse it for a proper chart flow (oldest on the left)
                java.util.Collections.reverse(historyList);
            }

        } catch (Exception e) {
            System.err.println("Error fetching historical stock data for " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }

        return historyList;
    }
}