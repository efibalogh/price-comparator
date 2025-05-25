# Price Comparator - Backend

A comprehensive Spring Boot application for comparing grocery prices across multiple supermarket chains, optimizing shopping baskets, tracking price history, and managing price alerts.

## Table of Contents

- [Price Comparator - Backend](#price-comparator---backend)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
    - [Key Capabilities](#key-capabilities)
  - [Features](#features)
    - [🛒 Basket Optimization](#-basket-optimization)
    - [💰 Discount Tracking](#-discount-tracking)
    - [📊 Price History \& Analytics](#-price-history--analytics)
    - [🔔 Price Alerts](#-price-alerts)
    - [📈 Value Comparison](#-value-comparison)
    - [📥 Data Import](#-data-import)
  - [Technology Stack](#technology-stack)
  - [Project Structure](#project-structure)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [API Documentation](#api-documentation)
  - [API Endpoints](#api-endpoints)
    - [Data Import](#data-import)
    - [Products](#products)
    - [Discounts](#discounts)
    - [Basket Optimization](#basket-optimization)
    - [Price Alerts](#price-alerts)
  - [Data Format](#data-format)
    - [Product Data Files](#product-data-files)
    - [Discount Data Files](#discount-data-files)
    - [Supported Units](#supported-units)
  - [Usage Examples](#usage-examples)
    - [1. Import Data](#1-import-data)
    - [2. Optimize Shopping Basket](#2-optimize-shopping-basket)
    - [3. Create Price Alert](#3-create-price-alert)
    - [4. Get Best Discounts](#4-get-best-discounts)
    - [5. Get Price History](#5-get-price-history)
  - [Development](#development)
    - [Code Quality Tools](#code-quality-tools)
    - [Code Standards](#code-standards)
    - [Database Schema](#database-schema)
    - [Logging](#logging)
  - [Testing](#testing)
    - [Running Tests](#running-tests)
    - [Test Data](#test-data)
    - [Test Configuration](#test-configuration)

## Overview

The Price Comparator backend system processes CSV data from multiple supermarket chains to provide intelligent price comparison and shopping optimization services. It helps users make informed purchasing decisions by tracking price history, identifying best deals, optimizing shopping baskets across stores, and providing customizable price alerts.

### Key Capabilities

- **Multi-store Price Comparison**: Compare prices across different supermarket chains
- **Shopping Basket Optimization**: Find the most cost-effective way to purchase items across stores
- **Price History Tracking**: Monitor price trends over time for informed decision-making
- **Discount Management**: Track and identify the best current and new discounts
- **Price Alerts**: Set target prices and get notified when prices drop
- **Value Analysis**: Calculate price per unit for accurate comparison across different package sizes

## Features

### 🛒 Basket Optimization
- Submit shopping lists and receive optimized store recommendations
- Minimize total cost by intelligently splitting purchases across stores
- Consider current discounts in optimization calculations
- Detailed cost breakdown per store

### 💰 Discount Tracking
- Identify products with highest percentage discounts
- Track newly added discounts within configurable time periods
- Support for time-based discount validity periods
- Automatic discount application in price calculations

### 📊 Price History & Analytics
- Comprehensive price history tracking for all products
- Filter by product name, category, brand, or store
- Support for custom date ranges
- Data suitable for trend analysis and visualization

### 🔔 Price Alerts
- Set target prices for specific products and stores
- Automatic monitoring and alert triggering when importing data
- Consideration of discounts in alert calculations
- Alert deactivation after triggering

### 📈 Value Comparison
- Calculate price per unit (per kg, per liter, per piece, etc.)
- Enable accurate comparison across different package sizes
- Support for various unit types and conversions

### 📥 Data Import
- Automated CSV data processing from multiple stores
- Support for both product and discount data
- Robust error handling and data validation
- Duplicate detection and handling

## Technology Stack

- **Java 21** - Modern Java features and performance
- **Spring Boot 3.4.5** - Application framework
- **Spring Data JPA** - Data persistence layer
- **Spring Validation** - Input validation
- **MySQL** - Primary database (H2 for testing)
- **OpenCSV** - CSV file processing
- **Lombok** - Boilerplate code reduction
- **SLF4J + Logback** - Logging framework
- **SpringDoc OpenAPI** - API documentation
- **Gradle** - Build automation

## Project Structure
```bash
src/main/java/com/accesa/pricecomparator/
├── config/
│ └── JpaConfig.java # JPA configuration
├── controller/ # REST API controllers
│ ├── AlertController.java
│ ├── BasketController.java
│ ├── DataIngestionController.java
│ ├── DiscountController.java
│ └── ProductController.java
├── dto/ # Data Transfer Objects
│ ├── request/ # Request DTOs
│ └── response/ # Response DTOs
├── exception/ # Exception handling
│ ├── GlobalExceptionHandler.java
│ └── ResourceNotFoundException.java
├── model/ # Entity classes
│ ├── Alert.java
│ ├── Discount.java
│ ├── Product.java
│ ├── ProductSelection.java
│ ├── ProductWithDiscount.java
│ └── ShoppingList.java
├── repository/ # Data access layer
│ ├── AlertRepository.java
│ ├── DiscountRepository.java
│ └── ProductRepository.java
├── service/ # Business logic interfaces
└── service/impl/ # Business logic implementations
```

## Getting Started

### Prerequisites

- Java 21 or higher
- MySQL 8.0+
- Gradle 7.0+ (or use included wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/efibalogh/price-comparator.git
   cd PriceComparator
   ```

2. **Create an .env file**
   - An example `.env` file is provided in the root of the project.

3. **Build the application**
   ```bash
   ./gradlew build
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080` (or the port specified in the `.env` file).

### API Documentation

Once running, access the interactive API documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## API Endpoints

### Data Import
- `POST /api/data/import?directoryPath={path}` - Import CSV data from directory

### Products
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/history?filter={filter}&value={value}` - Get price history with filters
  - `filter` can be `name`, `category`, `brand`
  - `value` is the value to filter by
  - optional parameters are `store`, `startDate`, `endDate`
- `GET /api/products/value` - Get value per unit analysis
  - optional parameters are `date`, by default it's the latest date

### Discounts
- `GET /api/discounts` - Get all discounts
- `GET /api/discounts/new` - Get new discounts
  - optional parameters are `daysBack`, by default it's 1 day back
- `GET /api/discounts/current` - Get current discounts
  - optional parameters are `date`, by default it's the latest date
- `GET /api/discounts/best` - Get best discounts
  - optional parameters are `date`, by default it's the latest date, `limit`, by default it's 1000

### Basket Optimization
- `POST /api/basket/optimize` - Optimize shopping basket
  - optional parameters are `date`, by default it's the latest date

### Price Alerts
- `POST /api/alerts` - Create price alerts
- `PUT /api/alerts/{id}/activate` - Activate alert based on id
- `PUT /api/alerts/{id}/deactivate` - Deactivate alert based on id

## Data Format

### Product Data Files
**Filename format**: `{store_name}_{YYYY-MM-DD}.csv`

**Example**: `lidl_2025-05-08.csv`

```csv
product_id;product_name;product_category;brand;package_quantity;package_unit;price;currency
P001;lapte zuzu;lactate;Zuzu;1;l;9.80;RON
P002;iaurt grecesc;lactate;Lidl;0.4;kg;11.60;RON
```

### Discount Data Files
**Filename format**: `{store_name}_discounts_{YYYY-MM-DD}.csv`

**Example**: `lidl_discounts_2025-05-08.csv`

```csv
product_id;product_name;brand;package_quantity;package_unit;product_category;from_date;to_date;percentage_of_discount
P001;lapte zuzu;Zuzu;1;l;lactate;2025-05-08;2025-05-14;12
P008;brânză telemea;Pilos;0.3;kg;lactate;2025-05-08;2025-05-14;10
```

### Supported Units
- **Volume**: `l` (liters), `ml` (milliliters)
- **Weight**: `kg` (kilograms), `g` (grams)
- **Count**: `buc` (pieces), `role` (rolls)

## Usage Examples

### 1. Import Data
```bash
curl -X POST "http://localhost:8080/api/data/import?directoryPath=/path/to/csv/files"
```

### 2. Optimize Shopping Basket
```bash
curl -X POST "http://localhost:8080/api/basket/optimize?date=2025-05-08" \
  -H "Content-Type: application/json" \
  -d '[
    {"productName": "lapte zuzu", "quantity": 2},
    {"productName": "pâine albă", "quantity": 1}
  ]'
```

### 3. Create Price Alert
```bash
curl -X POST "http://localhost:8080/api/alerts" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "productName": "lapte zuzu",
      "store": "lidl",
      "targetPrice": 8.50
    }
  ]'
```

### 4. Get Best Discounts
```bash
curl "http://localhost:8080/api/discounts/best?limit=10"
```

### 5. Get Price History
```bash
curl "http://localhost:8080/api/products/history?filter=name&value=lapte%20zuzu&startDate=2025-05-01&endDate=2025-05-31"
```

## Development

### Code Quality Tools
The project includes several code quality tools:

- **Checkstyle** - Code style enforcement
- **PMD** - Static code analysis
- **SpotBugs** - Bug pattern detection

Run quality checks:
```bash
./gradlew check
```

### Code Standards
- Follow Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Write comprehensive JavaDoc for public APIs
- Include unit tests for new functionality
- Maintain logging consistency

### Database Schema
The application uses JPA entities with automatic schema generation. Key entities:

- **Product** - Product information and pricing
- **Discount** - Discount information with validity periods
- **Alert** - User-defined price alerts

### Logging
Comprehensive logging is implemented throughout the application:
- **INFO** - Business operations and API calls
- **DEBUG** - Detailed processing information
- **WARN** - Non-critical issues
- **ERROR** - Critical errors and exceptions

## Testing

### Running Tests
```bash
./gradlew test
```

### Test Data
Sample test data is provided in the `data/` directory:
- `data/products/` - Sample product CSV files
- `data/discounts/` - Sample discount CSV files

### Test Configuration
Tests use H2 in-memory database for isolation and speed.
