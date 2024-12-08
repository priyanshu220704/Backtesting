package org.example;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class SimpleMovingAverage {
    public static void main(String[] args) throws IOException {
        System.out.println("Simple Moving Average Program");
        // Read the stock data from CSV (saved by Python script)
        List<StockData> stockDataList = readStockData("stock_prices.csv");

        // Calculate moving averages
        List<BigDecimal> smaShort = calculateMovingAverage(stockDataList, 50);  // 50-day SMA
        List<BigDecimal> smaLong = calculateMovingAverage(stockDataList, 200);  // 200-day SMA

        // Ensure the lists have the same size before generating signals
        int minSize = Math.min(smaShort.size(), smaLong.size());
        smaShort = smaShort.subList(smaShort.size() - minSize, smaShort.size());
        smaLong = smaLong.subList(smaLong.size() - minSize, smaLong.size());

        // Generate buy and sell signals based on crossovers
        List<Boolean> signals = generateSignals(smaShort, smaLong);
        List<BigDecimal> returns = calculateReturns(stockDataList);
        List<BigDecimal> strategyReturns = applyStrategy(returns, signals);

        // Calculate performance metrics
        BigDecimal cumulativeReturns = calculateCumulativeReturns(strategyReturns);
        BigDecimal averageReturn = calculateAverageReturn(strategyReturns);
        BigDecimal volatility = calculateVolatility(strategyReturns);
        BigDecimal sharpeRatio = calculateSharpeRatio(averageReturn, volatility);
        BigDecimal maxDrawdown = calculateMaxDrawdown(cumulativeReturns);

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

    // method to safely parse BigDecimal and handle invalid values
    public static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;  // Return null for invalid number format
        }
    }

    // calculate the moving average for a given window size
    public static List<BigDecimal> calculateMovingAverage(List<StockData> stockData, int window) {
        List<BigDecimal> movingAverages = new ArrayList<>();
        for (int i = window - 1; i < stockData.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - window + 1; j <= i; j++) {
                sum = sum.add(stockData.get(j).getPrice());
            }
            movingAverages.add(sum.divide(BigDecimal.valueOf(window), RoundingMode.HALF_UP));
        }
        return movingAverages;
    }

    // generate buy/sell signals based on crossovers
    public static List<Boolean> generateSignals(List<BigDecimal> smaShort, List<BigDecimal> smaLong) {
        List<Boolean> signals = new ArrayList<>();
        for (int i = 0; i < smaShort.size(); i++) {
            signals.add(smaShort.get(i).compareTo(smaLong.get(i)) > 0);  // True if short-term > long-term
        }
        return signals;
    }

    // calculate daily log returns
    public static List<BigDecimal> calculateReturns(List<StockData> stockData) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < stockData.size(); i++) {
            BigDecimal currentPrice = stockData.get(i).getPrice();
            BigDecimal previousPrice = stockData.get(i - 1).getPrice();
            returns.add(BigDecimal.valueOf(Math.log(currentPrice.doubleValue() / previousPrice.doubleValue())));
        }
        return returns;
    }

    // strategy based on signals
    public static List<BigDecimal> applyStrategy(List<BigDecimal> returns, List<Boolean> signals) {
        List<BigDecimal> strategyReturns = new ArrayList<>();
        for (int i = 0; i < signals.size(); i++) {
            if (signals.get(i)) {
                strategyReturns.add(returns.get(i));  // Take return if signal is true
            } else {
                strategyReturns.add(BigDecimal.ZERO);  // No return if signal is false
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

    // average return (annualized)
    public static BigDecimal calculateAverageReturn(List<BigDecimal> strategyReturns) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal dailyReturn : strategyReturns) {
            sum = sum.add(dailyReturn);
        }
        return sum.divide(BigDecimal.valueOf(strategyReturns.size()), RoundingMode.HALF_UP);
    }

    // calculate volatility (annualized)
    public static BigDecimal calculateVolatility(List<BigDecimal> strategyReturns) {
        BigDecimal mean = calculateAverageReturn(strategyReturns);
        BigDecimal sumOfSquares = BigDecimal.ZERO;
        for (BigDecimal dailyReturn : strategyReturns) {
            sumOfSquares = sumOfSquares.add(dailyReturn.subtract(mean).pow(2));
        }
        BigDecimal variance = sumOfSquares.divide(BigDecimal.valueOf(strategyReturns.size()), RoundingMode.HALF_UP);
        return variance.sqrt(new MathContext(5));
    }

    // Sharpe ratio
    public static BigDecimal calculateSharpeRatio(BigDecimal averageReturn, BigDecimal volatility) {
        return averageReturn.divide(volatility, RoundingMode.HALF_UP);
    }

    // maximum drawdown
    public static BigDecimal calculateMaxDrawdown(BigDecimal cumulativeReturns) {
        return cumulativeReturns.negate();
    }
}

// StockData class to hold stock price data
class StockData {
    private String date;
    private BigDecimal price;

    public StockData(String date, BigDecimal price) {
        this.date = date;
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
