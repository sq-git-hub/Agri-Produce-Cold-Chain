package com.agrichain.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.agrichain.exceptions.BatchSpoiledException;
import com.agrichain.exceptions.BelowMSPBidException;
import com.agrichain.exceptions.ColdChainBreachException;
import com.agrichain.logging.ApplicationLogger;
import com.agrichain.models.CropBatch;
import com.agrichain.models.GrainBatch;
import com.agrichain.models.VegetableBatch;
import com.agrichain.persistence.BatchFileStore;
import com.agrichain.services.ColdStorageAndMandiService;
import com.agrichain.threading.AutosaveTask;

public class AgriChainCLI {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ApplicationLogger logger = new ApplicationLogger();
        ColdStorageAndMandiService service = new ColdStorageAndMandiService(logger);
        BatchFileStore store = new BatchFileStore();
        ExecutorService autosaveExecutor = Executors.newSingleThreadExecutor();

        service.addEventListener(event ->
                System.out.println("[EVENT] " + event.getEventType() + " - " + event.getMessage()));

        try {
            try {
                service.replaceBatches(store.load());
                if (!service.getBatches().isEmpty()) {
                    System.out.println("Saved batches loaded automatically.");
                }
            } catch (IOException exception) {
                logger.error("Could not load saved batches: " + exception.getMessage());
                System.out.println("Could not load saved batches: " + exception.getMessage());
            }

            boolean running = true;
            while (running) {
                printMenu();
                try {
                    int choice = scanner.nextInt();
                    scanner.nextLine();

                    switch (choice) {
                        case 1:
                            service.loadSampleData();
                            scheduleAutosave(autosaveExecutor, store, logger, service);
                            break;
                        case 2:
                            registerBatch(scanner, service);
                            scheduleAutosave(autosaveExecutor, store, logger, service);
                            break;
                        case 3:
                            displayBatches(service);
                            break;
                        case 4:
                            updateTemperature(scanner, service);
                            scheduleAutosave(autosaveExecutor, store, logger, service);
                            break;
                        case 5:
                            placeBid(scanner, service);
                            scheduleAutosave(autosaveExecutor, store, logger, service);
                            break;
                        case 6:
                            saveBatches(store, logger, service);
                            break;
                        case 7:
                            loadBatches(store, logger, service);
                            break;
                        case 8:
                            running = false;
                            saveBatches(store, logger, service);
                            System.out.println("AgriChain session ended.");
                            break;
                        default:
                            System.out.println("Choose a menu option from 1 to 8.");
                    }
                } catch (InputMismatchException exception) {
                    System.out.println("Invalid input: enter a number where requested.");
                    scanner.nextLine();
                } catch (IllegalArgumentException exception) {
                    System.out.println("Invalid value: " + exception.getMessage());
                } catch (ColdChainBreachException exception) {
                    System.out.println("COLD-CHAIN BREACH: " + exception.getMessage());
                } catch (BelowMSPBidException exception) {
                    System.out.println("BID REJECTED: " + exception.getMessage());
                } catch (BatchSpoiledException exception) {
                    System.out.println("BID REJECTED: " + exception.getMessage());
                } finally {
                    System.out.println();
                }
            }
        } finally {
            autosaveExecutor.shutdown();
            scanner.close();
        }
    }

    private static void printMenu() {
        System.out.println("=== Agri-Produce Cold-Chain & Mandi Bidding System ===");
        System.out.println("1. Load Sample Crop Batches (Quick Demo)");
        System.out.println("2. Register New Crop Batch (Vegetable or Grain)");
        System.out.println("3. View All Stored Batches & Shelf-Life Status");
        System.out.println("4. Adjust Cold Storage Temperature (Simulate Decay & Cold-Chain Check)");
        System.out.println("5. Place Mandi Bid on a Batch (Standard or Bulk Bid)");
        System.out.println("6. Save Batches to File");
        System.out.println("7. Load Batches from File");
        System.out.println("8. Exit");
        System.out.print("Select an option: ");
    }

    private static void registerBatch(Scanner scanner, ColdStorageAndMandiService service) {
        System.out.println("Batch type: 1. Vegetable  2. Grain");
        System.out.print("Select type: ");
        int type = scanner.nextInt();
        scanner.nextLine();
        if (type != 1 && type != 2) {
            throw new IllegalArgumentException("Batch type must be 1 or 2.");
        }

        System.out.print("Batch ID: ");
        String batchId = scanner.nextLine();
        System.out.print("Crop name: ");
        String cropName = scanner.nextLine();
        System.out.print("Farmer name: ");
        String farmerName = scanner.nextLine();
        System.out.print("Quantity in kg: ");
        double quantityKg = scanner.nextDouble();
        System.out.print("MSP per kg: ");
        double mspPerKg = scanner.nextDouble();
        System.out.print("Remaining shelf life in days: ");
        int shelfLife = scanner.nextInt();

        CropBatch batch;
        if (type == 1) {
            System.out.print("Humidity percent: ");
            double humidity = scanner.nextDouble();
            batch = new VegetableBatch(batchId, cropName, farmerName, quantityKg,
                    mspPerKg, shelfLife, humidity);
        } else {
            System.out.print("Moisture percent: ");
            double moisture = scanner.nextDouble();
            batch = new GrainBatch(batchId, cropName, farmerName, quantityKg,
                    mspPerKg, shelfLife, moisture);
        }
        scanner.nextLine();
        service.addBatch(batch);
        System.out.println("Batch registered.");
    }

    private static void displayBatches(ColdStorageAndMandiService service) {
        ArrayList<CropBatch> batches = service.getBatches();
        if (batches.isEmpty()) {
            System.out.println("No batches are stored. Load samples or register a batch first.");
            return;
        }

        System.out.printf("Vault temperature: %.1f C%n", service.getVaultTemperature());
        for (int i = 0; i < batches.size(); i++) {
            CropBatch batch = batches.get(i);
            String status = batch.isSpoiled() ? "SPOILED" : "FRESH";
            String highestBidder = batch.getHighestBidderName() == null
                    ? "No bids" : batch.getHighestBidderName();
            System.out.printf("%s | %s | farmer: %s | %.2f kg | MSP: %.2f/kg | "
                            + "shelf life: %d day(s) | %s | highest bid: %.2f/kg (%s)%n",
                    batch.getBatchId(), batch.getCropName(), batch.getFarmerName(),
                    batch.getQuantityKg(), batch.getMinimumSupportPrice(),
                    batch.getRemainingShelfLifeDays(), status, batch.getHighestBidPerKg(),
                    highestBidder);
        }
    }

    private static void updateTemperature(Scanner scanner, ColdStorageAndMandiService service)
            throws ColdChainBreachException {
        System.out.print("New vault temperature in C: ");
        double newTemperature = scanner.nextDouble();
        scanner.nextLine();
        service.updateVaultTemperature(newTemperature);
        System.out.printf("Vault temperature set to %.1f C; one storage day simulated.%n",
                service.getVaultTemperature());
    }

    private static void placeBid(Scanner scanner, ColdStorageAndMandiService service)
            throws BelowMSPBidException {
        System.out.println("Bid type: 1. Standard  2. Bulk buyer");
        System.out.print("Select type: ");
        int bidType = scanner.nextInt();
        scanner.nextLine();
        if (bidType != 1 && bidType != 2) {
            throw new IllegalArgumentException("Bid type must be 1 or 2.");
        }

        System.out.print("Batch ID: ");
        String batchId = scanner.nextLine();
        System.out.print("Trader name: ");
        String traderName = scanner.nextLine();
        System.out.print("Bid per kg: ");
        double bidPerKg = scanner.nextDouble();
        if (bidType == 1) {
            scanner.nextLine();
            service.placeBid(batchId, traderName, bidPerKg);
        } else {
            System.out.print("Bulk bonus per kg: ");
            double bonus = scanner.nextDouble();
            scanner.nextLine();
            service.placeBid(batchId, traderName, bidPerKg, bonus);
        }
        System.out.println("Bid accepted. Effective bid: "
                + service.getBatches().get(findBatchIndex(service, batchId)).getHighestBidPerKg()
                + " per kg.");
    }

    private static int findBatchIndex(ColdStorageAndMandiService service, String batchId) {
        ArrayList<CropBatch> batches = service.getBatches();
        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).getBatchId().equalsIgnoreCase(batchId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("No batch found with ID " + batchId + ".");
    }

    private static void scheduleAutosave(ExecutorService executor, BatchFileStore store,
                                         ApplicationLogger logger,
                                         ColdStorageAndMandiService service) {
        executor.execute(new AutosaveTask(store, logger, service::snapshotBatches));
    }

    private static void saveBatches(BatchFileStore store, ApplicationLogger logger,
                                    ColdStorageAndMandiService service) {
        try {
            store.save(service.snapshotBatches());
            logger.info("Batches saved to file.");
            System.out.println("Batches saved to file.");
        } catch (IOException exception) {
            logger.error("Could not save batches: " + exception.getMessage());
            System.out.println("Could not save batches: " + exception.getMessage());
        }
    }

    private static void loadBatches(BatchFileStore store, ApplicationLogger logger,
                                    ColdStorageAndMandiService service) {
        try {
            service.replaceBatches(store.load());
            logger.info("Batches loaded from file.");
            System.out.println("Batches loaded from file.");
        } catch (IOException | IllegalArgumentException exception) {
            logger.error("Could not load batches: " + exception.getMessage());
            System.out.println("Could not load batches: " + exception.getMessage());
        }
    }
}