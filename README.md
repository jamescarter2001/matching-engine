# Matching Engine

Low latency order matching engine written in Java.

## Features

This implementation supports the following operations:

- Adding orders for a given Instrument ID.
- Matching orders against the best bids/asks resting on the book.
- Cancelling orders by Order ID.
- Publishing trade + order events to downstream consumers.