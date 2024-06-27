
# Karbitrage

Karbitrage, which stands for "arbitrage finder," is a Kotlin-based project designed to extract cryptocurrency prices from various exchanges using the Ktor framework. This project focuses on exploring Ktor in addition to designing an application with maximum changeability and extensibility. The application periodically persists data to a PostgreSQL database and provides APIs to retrieve the price data.

## Technologies Used

- **Ktor**: For building asynchronous servers and web applications in Kotlin.
- **Exposed**: A lightweight SQL library on top of JDBC for Kotlin.
- **PostgreSQL**: For persisting cryptocurrency price data.
- **Caffeine Cache**: For in-memory caching to improve performance.
- **Reflection**: For dynamic class loading and manipulation.
- **Koin**: A pragmatic lightweight dependency injection framework for Kotlin.

## Getting Started

### Prerequisites

- Kotlin 1.8+
- Gradle 7.0+
- Java 11+
- PostgreSQL
- H2 Database (for testing)

### Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/FirstArtaxer/karbitrage.git
    ```
2. Navigate to the project directory:
    ```sh
    cd karbitrage
    ```
3. Build the project:
    ```sh
    ./gradlew build
    ```
4. Set up PostgreSQL and configure the connection settings in your application.

### Running the Application

1. Start the server:
    ```sh
    ./gradlew run
    ```
2. The application will be available at `http://localhost:8080`.

### API Endpoints

- **Get Cryptocurrency Prices:**
  ```http
  GET /cryptos/prices?from={fromDateTime}&to={toDateTime}&symbol={symbol}
  ```
  Retrieves the prices of a specific cryptocurrency symbol within a date range from all implemented exchanges.

- **Get Latest Prices:**
  ```http
  GET /cryptos/last-prices
  ```
  Retrieves the latest prices of all cryptocurrencies from all implemented exchanges.

### Extending with New Exchanges

To add a new exchange, implement the `PriceExtractor` and `RequestMaker` interfaces:

#### Implementing `RequestMaker`

```kotlin
/**
 * Interface for creating HTTP requests to a cryptocurrency exchange API.
 */
interface RequestMaker {

    /**
     * Constructs an HTTP request to retrieve data from the exchange.
     *
     * @return A [HttpRequestBuilder] configured to make the request to the exchange's API.
     */
    fun getRequest(): HttpRequestBuilder
}
```

#### Implementing `PriceExtractor`

```kotlin
/**
 * Abstract class for extracting price data from a cryptocurrency exchange.
 */
abstract class PriceExtractor {

    /**
     * Gets a function that processes the API response from the exchange.
     *
     * @return A function that takes a string response from the exchange's API and returns
     *         a map where the key is the cryptocurrency symbol and the value is the price.
     *         This function extracts the relevant data from the API response string.
     */
    abstract fun getExtractor(): (String) -> Map<String, Double>
}
```

#### Example

1. Create a new class in the `exchange` package.
2. Implement the required interfaces and abstract methods.

```kotlin
package exchange

import io.ktor.client.request.*
import io.ktor.client.request.HttpRequestBuilder

class NewExchangeRequestMaker : RequestMaker {
    override fun getRequest(): HttpRequestBuilder {
        // Define the HTTP request for the new exchange
    }
}

class NewExchangePriceExtractor : PriceExtractor() {
    override fun getExtractor(): (String) -> Map<String, Double> {
        // Define the extraction logic for the new exchange
    }
}
```

### Testing

1. Run the tests:
    ```sh
    ./gradlew test
    ```

## Contributing

Contributions are welcome! Please fork the repository and create a pull request.
