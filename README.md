# Agri-Produce Cold-Chain & Mandi Bidding System

A clean, simple, menu-driven desktop-to-terminal Java application for registering crop batches, tracking cold-storage shelf life, accepting mandi bids, and maintaining local records. Developed as a Core Java college Software Engineering mini-project and Object-Oriented Programming (OOP) lab record.

Built purely with Core Java, without external dependencies, frameworks, databases, HTML, JavaScript, or build tools.

---

## Project Overview

The **Agri-Produce Cold-Chain & Mandi Bidding System** manages agricultural batches from registration through storage and bidding.

The application allows users to:

- Register vegetable and grain crop batches
- Store farmer, quantity, MSP, shelf-life, humidity, or moisture details
- Simulate cold-storage temperature changes
- Detect cold-chain temperature breaches
- Reduce shelf life according to crop-specific storage rules
- Place standard and bulk mandi bids
- Reject spoiled batches and bids below MSP
- Track the highest bid and bidder
- Save and load batch records using text-file persistence
- Run background autosave and view application events and logs

The system demonstrates all major core Java and Object-Oriented Programming concepts while remaining small and beginner-friendly.

---

## Features

- **Complete Console/CLI Mode:**
  - Eight-option menu for batch, storage, bidding, and file operations
  - Clear validation messages and custom business-rule exceptions
  - Interactive `Scanner`-based input
  - `finally` resource cleanup for safe scanner closure

- **Crop Batch Management:**
  - Register vegetable and grain batches
  - Automatically assign a crop-specific maximum safe temperature
  - Prevent duplicate batch IDs
  - Validate quantity, MSP, shelf life, humidity, and moisture

- **Cold-Chain Simulation:**
  - Start with a default vault temperature of 6°C
  - Simulate one storage day for every batch
  - Detect batches exceeding their safe temperature
  - Apply subtype-specific shelf-life decay

- **Mandi Bidding:**
  - Place standard bids
  - Place bulk bids with a per-kilogram bonus
  - Enforce MSP, spoilage, and highest-bid rules
  - Maintain the highest bid and bidder name

- **Text-File Persistence:**
  - Save batch records in `data/batches.txt`
  - Load saved records at application startup
  - Support manual save and load operations
  - Automatically create the data directory

- **Background Autosave:**
  - Uses a daemon-safe single-thread executor
  - Runs `AutosaveTask` asynchronously after batch changes
  - Creates `logs/agrichain.log` for status and error records

- **Event Handling:**
  - Publishes registration, temperature, breach, sample-load, and bid events
  - Uses a functional listener interface and lambda event handling
  - Displays events immediately in the CLI

---

## Technologies Used

- **Language:** Core Java
- **Input / Output:** `java.util.Scanner`, `BufferedReader`, and `BufferedWriter`
- **Collections:** `ArrayList`, `List`, `Map`, and `LinkedHashMap`
- **Concurrency:** `ExecutorService` and `Runnable`
- **Storage:** Plain-text pipe-separated UTF-8 records
- **Architecture:** Package-based Model–Service–CLI design
- **External Dependencies:** None

---

## OOP Concepts Demonstrated

| Concept | Implementation |
|---|---|
| Encapsulation | Private fields and validated getters/setters in `CropBatch`, `VegetableBatch`, and `GrainBatch` |
| Inheritance | `VegetableBatch` and `GrainBatch` extend `CropBatch` |
| Abstraction | `CropBatch` is an abstract base class with `getMaxSafeTemp()` |
| Interfaces | `Perishable` and `TradeableCommodity` implemented by `CropBatch` |
| Polymorphism | Subtype overrides of `simulateStorageDay(double)` and overloaded bid methods |
| Packages | Separate contracts, models, services, exceptions, events, logging, persistence, threading, and main packages |
| Exception Handling | Custom checked and unchecked exceptions plus built-in Java exceptions |
| Threading | `ExecutorService` and `AutosaveTask` perform background autosaving |
| File Handling | Text-file loading and saving using `BufferedReader` and `BufferedWriter` |
| Collections | `ArrayList` for ordered storage and `LinkedHashMap` for fast ID lookup |
| Logging | Timestamped `INFO`, `ERROR`, and `EVENT` records |
| Event Handling | Lambda listener receiving service events |

---

## Project Structure

```text
Agri-Produce Cold-Chain & Mandi Bidding System/
├── data/
│   └── batches.txt
├── logs/
│   └── agrichain.log
├── out/                          # Compiled .class files
├── src/
│   └── com/agrichain/
│       ├── contracts/
│       │   ├── Perishable.java
│       │   └── TradeableCommodity.java
│       ├── events/
│       │   ├── AgriChainEvent.java
│       │   └── AgriChainEventListener.java
│       ├── exceptions/
│       │   ├── BatchSpoiledException.java
│       │   ├── BelowMSPBidException.java
│       │   └── ColdChainBreachException.java
│       ├── logging/
│       │   └── ApplicationLogger.java
│       ├── main/
│       │   └── AgriChainCLI.java
│       ├── models/
│       │   ├── CropBatch.java
│       │   ├── VegetableBatch.java
│       │   └── GrainBatch.java
│       ├── persistence/
│       │   └── BatchFileStore.java
│       ├── services/
│       │   └── ColdStorageAndMandiService.java
│       └── threading/
│           └── AutosaveTask.java
└── README.md
```

---

## Architecture and Data Flow

```text
User Input
    |
    v
AgriChainCLI
    |
    v
ColdStorageAndMandiService
    |
    +--> ArrayList<CropBatch>
    +--> LinkedHashMap<String, CropBatch>
    |
    +--> VegetableBatch
    +--> GrainBatch
    |
    +--> Event listeners
    +--> Application logger
    |
    v
Text-file persistence and background autosave
```

1. `AgriChainCLI` reads a menu choice and required values.
2. The CLI calls `ColdStorageAndMandiService`.
3. The service validates, stores, updates, or bids on batch objects.
4. The appropriate subtype implements storage-day behavior.
5. Events and logs report successful or failed operations.
6. A background task writes changed batches to the text file.

---

## Batch Rules

### Vegetable Batches

- Maximum safe temperature: **8°C**
- At or below 8°C: lose **1** shelf-life day
- Above 8°C: lose **3** shelf-life days
- Humidity must be between **0%** and **100%**

### Grain Batches

- Maximum safe temperature: **25°C**
- Lose **1** shelf-life day if temperature exceeds 25°C
- Lose **1** shelf-life day if moisture exceeds 14%
- Shelf life remains unchanged if both conditions are safe
- Moisture must be between **0%** and **100%**

Shelf life never falls below zero. A batch is considered spoiled when remaining shelf life reaches zero.

---

## Mandi Bidding Rules

- The batch ID must exist.
- The trader name cannot be blank.
- A spoiled batch cannot receive a bid.
- The effective bid must be at least the batch MSP.
- The effective bid must exceed the current highest bid.
- A bulk bid uses: `effective bid = bid per kg + bulk bonus per kg`.

---

## CLI Menu

1. **Load Sample Crop Batches** — Replace the current batch list with demonstration data.
2. **Register New Crop Batch** — Register a vegetable or grain batch.
3. **View Stored Batches** — Display batch details, freshness, and highest bid.
4. **Adjust Cold-Storage Temperature** — Simulate one storage day and detect breaches.
5. **Place Mandi Bid** — Submit a standard or bulk bid.
6. **Save Batches to File** — Save records to `data/batches.txt`.
7. **Load Batches from File** — Replace memory contents with saved records.
8. **Exit** — Perform a final save and close the application.

Saved batches are loaded automatically when the application starts.

---

## Sample Data

| Batch ID | Crop | Type | Quantity | MSP/kg | Shelf Life | Extra Property |
|---|---|---|---:|---:|---:|---:|
| `TOM-101` | Tomatoes | Vegetable | 250 kg | 18 | 5 days | 88% humidity |
| `SPI-102` | Spinach | Vegetable | 120 kg | 22 | 3 days | 92% humidity |
| `RICE-201` | Basmati Rice | Grain | 1,000 kg | 42 | 30 days | 12.5% moisture |

---

## Validation Rules

- Batch IDs, crop names, farmer names, and trader names cannot be blank.
- Quantity and MSP must be finite values above zero.
- Shelf life cannot be negative.
- Humidity and moisture must be from 0 to 100 percent.
- Batch IDs cannot be registered more than once.
- A bid cannot be placed on a spoiled batch.
- Temperatures and bulk bonuses must be finite.
- Bulk bonuses cannot be negative.

---

## Exception Handling

- **`ColdChainBreachException`**: Checked exception for unsafe temperature conditions.
- **`BelowMSPBidException`**: Checked exception for bids below the minimum support price.
- **`BatchSpoiledException`**: Unchecked exception for bidding on spoiled produce.
- **`IllegalArgumentException`**: Invalid values and business-rule violations.
- **`InputMismatchException`**: Incorrect menu or numeric input types.
- **`IOException`**: Saved-data read/write failures.

The CLI catches these exceptions and continues running instead of terminating the session.

---

## Logging and Events

- `ApplicationLogger` writes timestamped records to `logs/agrichain.log`.
- Registration, sample loading, temperature updates, cold-chain breaches, and accepted bids publish events.
- The CLI attaches a lambda event listener and prints event notifications.

---

## How to Compile and Run

### Requirements

- JDK with `javac` and `java` available on `PATH`
- PowerShell or Command Prompt

### Compile

Open PowerShell in the project directory and compile all source files into the sibling `out` directory:

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object FullName)
```

### Run

```powershell
java -cp out com.agrichain.main.AgriChainCLI
```

Both commands should be run from the project root directory:

```text
Agri-Produce Cold-Chain & Mandi Bidding System
```
