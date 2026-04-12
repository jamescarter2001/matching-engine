# Matching Engine

A high-performance, low-latency order matching engine written in Java.

## Prerequisites

- Java 25 or later
- Maven 3.6+

## Features

- **Order Management**: Add, match, and cancel orders for multiple financial instruments
- **Real-time Matching**: Efficient price-time priority matching algorithm
- **Event Publishing**: Publishes trade and order events to downstream consumers

## Architecture

The matching engine is built using modern Java technologies optimised for low latency:

- **Agrona**: High-performance data structures and utilities

### Core Components

- `MatchingEngine`: Main engine managing multiple order books
- `OrderBook`: Individual order book implementation with price levels
- `OrderPool`: Memory pool for order objects to eliminate allocations on the hot path. This is critical for avoiding garbage collection pauses
- Event publishers for trade and order updates