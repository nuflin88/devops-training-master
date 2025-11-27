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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StockHistoryRepository historyRepository; // <-- NEW: Inject Repository

    @Value("${stock.api.url.current}.trim()")
    private String currentApiUrlTemplate;

    @Value("${stock.api.url.history}.trim()")
    private String historyApiUrlTemplate;

    @Value("${stock.api.key}.trim()")
    private String apiKey;

    // Modified Constructor to include the Repository
    public StockService(StockHistoryRepository historyRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.historyRepository = historyRepository; // <-- NEW
    }

    // --- getStockData (Current Price) omitted for brevity; it remains the same ---
    public StockDTO getStockData(String symbol) {
        // ... (Your existing getStockData logic) ...
        String finalUrl = currentApiUrlTemplate
                .replace("{symbol}", symbol)
                .replace("{key}", apiKey);

        try {
            String jsonResponse = restTemplate.getForObject(finalUrl, String.class);

            if (jsonResponse == null || jsonResponse.contains("Error Message") || jsonResponse.contains("Invalid API call") || jsonResponse.contains("Note")) {
                logger.error("Alpha Vantage API returned an error for current price: {}", jsonResponse);
                return new StockDTO(symbol, "API Error or Rate Limit", -1.0);
            }

            if (jsonResponse.contains("Global Quote")) {
                int priceIndex = jsonResponse.indexOf("\"05. price\"");
                if (priceIndex > 0) {
                    int valueStart = jsonResponse.indexOf(":", priceIndex) + 3;
                    int valueEnd = jsonResponse.indexOf("\"", valueStart);

                    String priceString = jsonResponse.substring(valueStart, valueEnd);
                    double price = Double.parseDouble(priceString);

                    return new StockDTO(symbol, "Alpha Vantage Stock", price);
                }
            }

            return new StockDTO(symbol, "Stock Symbol Not Found", -1.0);

        } catch (Exception e) {
            logger.error("Error fetching current stock data for {}: {}", symbol, e.getMessage(), e);
            return new StockDTO(symbol, "System Error during HTTP call", -1.0);
        }
    }


    /**
     * Fetches historical daily stock data, checking the database cache first.
     */
    public List<StockHistory> getStockHistory(String symbol) {
        String uppercaseSymbol = symbol.toUpperCase();

        // 1. CHECK CACHE FIRST
        List<StockDataEntity> cachedData = historyRepository.findBySymbolOrderByDateAsc(uppercaseSymbol);

        if (cachedData != null && !cachedData.isEmpty()) {
            logger.info("Retrieved historical data for {} from cache ({} days).", uppercaseSymbol, cachedData.size());
            // Convert JPA entities back to our lighter DTOs (StockHistory)
            List<StockHistory> historyList = new ArrayList<>();
            for (StockDataEntity entity : cachedData) {
                historyList.add(new StockHistory(entity.getDate(), entity.getPrice()));
            }
            return historyList;
        }

        // 2. CACHE MISS: Fetch from API
        logger.warn("Cache miss for {}. Fetching data from Alpha Vantage API.", uppercaseSymbol);

        List<StockHistory> historyList = new ArrayList<>();
        String finalUrl = historyApiUrlTemplate
                .replace("{symbol}", uppercaseSymbol)
                .replace("{key}", apiKey);

        try {
            String jsonResponse = restTemplate.getForObject(finalUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // --- API ERROR CHECKING ---
            if (rootNode.has("Error Message") || rootNode.has("Note") || rootNode.isEmpty()) {
                String errorMessage = rootNode.has("Error Message") ? rootNode.get("Error Message").asText() :
                        rootNode.has("Note") ? rootNode.get("Note").asText() :
                                "Empty or unknown API response.";

                logger.error("API Response Failure for {}. Reason: {}", uppercaseSymbol, errorMessage);
                return historyList; // Returns empty list on API failure
            }

            // 3. Parse and Cache Data
            JsonNode timeSeriesNode = rootNode.get("Time Series (Daily)");

            if (timeSeriesNode == null || timeSeriesNode.isEmpty()) {
                logger.error("API Data Structure Failure for {}. Key 'Time Series (Daily)' not found. Ticker invalid.", uppercaseSymbol);
                return historyList;
            }

            List<StockDataEntity> entitiesToSave = new ArrayList<>();

            for (Map.Entry<String, JsonNode> entry : timeSeriesNode.properties()) {
                String date = entry.getKey();
                JsonNode dayData = entry.getValue();

                try {
                    String closePriceStr = dayData.get("4. close").asText();
                    BigDecimal closePrice = new BigDecimal(closePriceStr);

                    // Add to both the return list and the cache list
                    historyList.add(new StockHistory(date, closePrice));
                    entitiesToSave.add(new StockDataEntity(uppercaseSymbol, date, closePrice));

                    if (historyList.size() >= 30) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("CRITICAL PARSING ERROR for date: {}. Day Data: {}", date, dayData.toString(), e);
                }
            }

            // 4. SAVE TO CACHE (Database)
            historyRepository.saveAll(entitiesToSave);
            logger.info("Successfully cached {} data points for {}.", entitiesToSave.size(), uppercaseSymbol);

            Collections.reverse(historyList);

        } catch (Exception e) {
            logger.error("Error during HTTP call or JSON read for {}: {}", uppercaseSymbol, e.getMessage(), e);
        }

        return historyList;
    }
}