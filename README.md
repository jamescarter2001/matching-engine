# Matching Engine

Low latency order matching engine written in Java.

## Features

This implementation supports:

- Add/execute orders -  O(1) best case, O(g) worse case, where g is the gap between an exhaused best bid/ask and the next best.
- Cancel orders -  O(1)