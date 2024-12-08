package org.example;

import java.io.*;
import org.example.StockData;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class ExponentialWeightedMovingAverage {
    public static void main(String[] args) throws IOException {
        System.out.println("Exponential Weighted Moving Average Program");
        // Read the stock data from CSV (saved by Python script)
        List<StockData> stockDataList = readStockData("stock_prices.csv");

        // Calculate moving averages
        List<BigDecimal> ewmaShort = calculateEWMA(stockDataList, 50);  // 50-day EWMA
        List<BigDecimal> ewmaLong = calculateEWMA(stockDataList, 200);  // 200-day EWMA

        // Ensure the lists have the same size before generating signals
        int minSize = Math.min(ewmaShort.size(), ewmaLong.size());
        ewmaShort = ewmaShort.subList(ewmaShort.size() - minSize, ewmaShort.size());
        ewmaLong = ewmaLong.subList(ewmaLong.size() - minSize, ewmaLong.size());

        // Generate buy and sell signals based on crossovers
        List<Boolean> signals = generateSignals(ewmaShort, ewmaLong);

        // Calculate daily returns (log returns)
        List<BigDecimal> returns = calculateReturns(stockDataList);

        // Implement the strategy and calculate performance
        List<BigDecimal> strategyReturns = applyStrategy(returns, signals);

        // Calculate performance metrics
        BigDecimal cumulativeReturns = calculateCumulativeReturns(strategyReturns);
        BigDecimal averageReturn = calculateAverageReturn(strategyReturns);
        BigDecimal volatility = calculateVolatility(strategyReturns);
        BigDecimal sharpeRatio = calculateSharpeRatio(averageReturn, volatility);
        BigDecimal maxDrawdown = calculateMaxDrawdown(strategyReturns);

        // Print the results
        System.out.println("Average Return (Annualized): " + averageReturn);
        System.out.println("Volatility (Annualized): " + volatility);
        System.out.println("Sharpe Ratio: " + sharpeRatio);
        System.out.println("Maximum Drawdown: " + maxDrawdown);
    }

    // Method to read stock data from CSV and return a list of StockData objects
    public static List<StockData> readStockData(String fileName) throws IOException {
        List<StockData> stockDataList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",");
            String date = values[0];
            String priceString = values[1];

            // Validate and parse the price
            BigDecimal price = parseBigDecimal(priceString);
            if (price != null) {
                stockDataList.add(new StockData(date, price));
            } else {
                System.err.println("Skipping invalid price value: " + priceString + " for date: " + date);
            }
        }
        reader.close();
        return stockDataList;
    }

    // Helper method to safely parse BigDecimal and handle invalid values
    public static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;  // Return null for invalid number format
        }
    }

    // Method to calculate the Exponential Weighted Moving Average for a given window size
    public static List<BigDecimal> calculateEWMA(List<StockData> stockData, int window) {
        List<BigDecimal> ewmaValues = new ArrayList<>();
        BigDecimal alpha = BigDecimal.valueOf(2.0 / (window + 1));  // Smoothing factor

        // Initialize with the first data point (could also use SMA for the first value)
        BigDecimal previousEwma = stockData.get(0).getPrice();
        ewmaValues.add(previousEwma);

        for (int i = 1; i < stockData.size(); i++) {
            BigDecimal currentPrice = stockData.get(i).getPrice();
            // Calculate EWMA using the formula: EWMA_t = alpha * price_t + (1 - alpha) * EWMA_(t-1)
            previousEwma = alpha.multiply(currentPrice).add(BigDecimal.ONE.subtract(alpha).multiply(previousEwma));
            ewmaValues.add(previousEwma);
        }
        return ewmaValues;
    }

    // Method to generate buy/sell signals based on crossovers
    public static List<Boolean> generateSignals(List<BigDecimal> ewmaShort, List<BigDecimal> ewmaLong) {
        List<Boolean> signals = new ArrayList<>();
        for (int i = 0; i < ewmaShort.size(); i++) {
            signals.add(ewmaShort.get(i).compareTo(ewmaLong.get(i)) > 0);
        }
        return signals;
    }

    // Method to calculate daily log returns
    public static List<BigDecimal> calculateReturns(List<StockData> stockData) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < stockData.size(); i++) {
            BigDecimal currentPrice = stockData.get(i).getPrice();
            BigDecimal previousPrice = stockData.get(i - 1).getPrice();
            returns.add(BigDecimal.valueOf(Math.log(currentPrice.doubleValue() / previousPrice.doubleValue())));
        }
        return returns;
    }

    // Method to apply the strategy based on signals
    public static List<BigDecimal> applyStrategy(List<BigDecimal> returns, List<Boolean> signals) {
        List<BigDecimal> strategyReturns = new ArrayList<>();
        for (int i = 0; i < signals.size(); i++) {
            if (signals.get(i)) {
                strategyReturns.add(returns.get(i));
            } else {
                strategyReturns.add(BigDecimal.ZERO);
            }
        }
        return strategyReturns;
    }

    // Method to calculate cumulative returns
    public static BigDecimal calculateCumulativeReturns(List<BigDecimal> strategyReturns) {
        BigDecimal cumulative = BigDecimal.ZERO;
        for (BigDecimal dailyReturn : strategyReturns) {
            cumulative = cumulative.add(dailyReturn);
        }
        return cumulative;
    }

    // Method to calculate average return (annualized)
    public static BigDecimal calculateAverageReturn(List<BigDecimal> strategyReturns) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal dailyReturn : strategyReturns) {
            sum = sum.add(dailyReturn);
        }
        BigDecimal averageReturn = sum.divide(BigDecimal.valueOf(strategyReturns.size()), RoundingMode.HALF_UP);
        // Annualize average return by multiplying by 252 (assuming daily returns)
        return averageReturn.multiply(BigDecimal.valueOf(252));
    }

    // Method to calculate volatility (annualized)
    public static BigDecimal calculateVolatility(List<BigDecimal> strategyReturns) {
        BigDecimal mean = calculateAverageReturn(strategyReturns);
        BigDecimal sumOfSquares = BigDecimal.ZERO;
        for (BigDecimal dailyReturn : strategyReturns) {
            sumOfSquares = sumOfSquares.add(dailyReturn.subtract(mean).pow(2));
        }
        BigDecimal variance = sumOfSquares.divide(BigDecimal.valueOf(strategyReturns.size()), RoundingMode.HALF_UP);
        // Annualize volatility by multiplying by sqrt(252)
        return variance.sqrt(new MathContext(5)).multiply(BigDecimal.valueOf(Math.sqrt(252)));
    }

    // Method to calculate Sharpe ratio
    public static BigDecimal calculateSharpeRatio(BigDecimal averageReturn, BigDecimal volatility) {
        return averageReturn.divide(volatility, RoundingMode.HALF_UP);
    }

    // Method to calculate maximum drawdown
    public static BigDecimal calculateMaxDrawdown(List<BigDecimal> strategyReturns) {
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal drawdown = BigDecimal.ZERO;

        // Calculate the maximum drawdown based on cumulative returns
        for (BigDecimal dailyReturn : strategyReturns) {
            peak = peak.max(dailyReturn);  // track the peak value
            drawdown = drawdown.max(peak.subtract(dailyReturn));  // calculate the drawdown from peak
        }
        return drawdown;
    }
}
