package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map; // <-- FIX: Added missing import for Map
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${stock.api.url.current:}.trim()")
    private String currentApiUrlTemplate;

    @Value("${stock.api.url.history:}.trim()")
    private String historyApiUrlTemplate;

    @Value("${stock.api.key:}.trim()")
    private String apiKey;

    public StockService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches the current stock price using the GLOBAL_QUOTE function.
     */
    public StockDTO getStockData(String symbol) {
        String finalUrl = currentApiUrlTemplate
                .replace("{symbol}", symbol)
                .replace("{key}", apiKey);

        try {
            String jsonResponse = restTemplate.getForObject(finalUrl, String.class);

            // 1. Check for API errors (rate limit, invalid key)
            if (jsonResponse == null || jsonResponse.contains("Error Message") || jsonResponse.contains("Invalid API call") || jsonResponse.contains("Note")) {
                logger.error("Alpha Vantage API returned an error for current price: {}", jsonResponse);
                return new StockDTO(symbol, "API Error or Rate Limit", -1.0);
            }

            // 2. Check for the expected Global Quote structure
            if (jsonResponse.contains("Global Quote")) {
                // Brittle string parsing for current price
                int priceIndex = jsonResponse.indexOf("\"05. price\"");
                if (priceIndex > 0) {
                    int valueStart = jsonResponse.indexOf(":", priceIndex) + 3;
                    int valueEnd = jsonResponse.indexOf("\"", valueStart);

                    String priceString = jsonResponse.substring(valueStart, valueEnd);
                    double price = Double.parseDouble(priceString);

                    return new StockDTO(symbol, "Alpha Vantage Stock", price);
                }
            }

            // Symbol not found or data structure unexpected
            return new StockDTO(symbol, "Stock Symbol Not Found", -1.0);

        } catch (Exception e) {
            logger.error("Error fetching current stock data for {}: {}", symbol, e.getMessage(), e);
            return new StockDTO(symbol, "System Error during HTTP call", -1.0);
        }
    }

    /**
     * Fetches historical daily stock data using the TIME_SERIES_DAILY function.
     */
    /**
     * Fetches historical daily stock data using the TIME_SERIES_DAILY function.
     */
    public List<StockHistory> getStockHistory(String symbol) {
        List<StockHistory> historyList = new ArrayList<>();

        // 🚨 FIX: Force symbol to uppercase before constructing the URL
        String uppercaseSymbol = symbol.toUpperCase();

        String finalUrl = historyApiUrlTemplate
                .replace("{symbol}", uppercaseSymbol) // Use the forced uppercase symbol
                .replace("{key}", apiKey);

        try {
            String jsonResponse = restTemplate.getForObject(finalUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // --- ROBUST API ERROR CHECKING ---
            if (rootNode.has("Error Message") || rootNode.has("Note") || rootNode.isEmpty()) {
                String errorMessage = rootNode.has("Error Message") ? rootNode.get("Error Message").asText() :
                        rootNode.has("Note") ? rootNode.get("Note").asText() :
                                "Empty or unknown API response.";

                logger.error("API Response Failure for {}. Reason: {}", uppercaseSymbol, errorMessage);
                return historyList;
            }

            // 3. Get the nested time series data
            JsonNode timeSeriesNode = rootNode.get("Time Series (Daily)");

            if (timeSeriesNode == null || timeSeriesNode.isEmpty()) {
                logger.error("API Data Structure Failure for {}. Key 'Time Series (Daily)' not found or is empty. Ticker likely invalid.", uppercaseSymbol);
                return historyList;
            }

            // 4. Iterate through the daily entries
            for (Map.Entry<String, JsonNode> entry : timeSeriesNode.properties()) {
                String date = entry.getKey();
                JsonNode dayData = entry.getValue();

                try {
                    String closePriceStr = dayData.get("4. close").asText();
                    BigDecimal closePrice = new BigDecimal(closePriceStr);

                    historyList.add(new StockHistory(date, closePrice));

                    if (historyList.size() >= 30) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("CRITICAL PARSING ERROR for date: {}. Day Data: {}", date, dayData.toString(), e);
                }
            }

            Collections.reverse(historyList);

        } catch (Exception e) {
            logger.error("Error during HTTP call or JSON read for {}: {}", uppercaseSymbol, e.getMessage(), e);
        }

        return historyList;
    }
}