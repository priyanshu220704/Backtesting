import yfinance as yf
import numpy as np
import pandas as pd

# Define a diverse set of stocks across sectors and price ranges
tickers = [
    'AAPL',  
    'MSFT',  
    'GOOGL', 
    'JNJ',   
    'XOM',   # Energy
    'JPM',   # Financials
    'NVDA',  # Technology
    'AMZN',  # Consumer Discretionary
    'PFE',   # Healthcare
    'F',     # Low price, Consumer Discretionary
    'GE',    # Medium price, Industrials
    'BRK-B', # High price, Financials
    'CVX',   # Energy
    'WMT',   # Consumer Staples
    'DUK',
    'SO',
    'CAT',
    'BA',
    'PG',
    'KO',
]

# Time horizon
start_date = '2010-01-01'
end_date = '2018-12-31'

# Download adjusted close price data
data = yf.download(tickers, start=start_date, end=end_date, progress=False)['Adj Close']

# Calculate daily returns
returns = np.log(data / data.shift(1))

# Reflect on stock data
print("Selected Stocks:", tickers)
print(f"Time Horizon: {start_date} to {end_date}")
print("Stock Price Ranges:")
print(data.iloc[-1].sort_values())  # Display final adjusted prices for stocks
print("\nMissing Data Summary:")
print(data.isna().sum())  # Check for missing data

# Save data to CSV for further analysis
data.to_csv('stock_prices.csv')
returns.to_csv('stock_returns.csv')
