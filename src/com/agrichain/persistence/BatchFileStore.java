package com.agrichain.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.agrichain.models.CropBatch;
import com.agrichain.models.GrainBatch;
import com.agrichain.models.VegetableBatch;

public class BatchFileStore {
    private final Path dataFile;

    public BatchFileStore() {
        this(Paths.get("data", "batches.txt"));
    }

    public BatchFileStore(Path dataFile) {
        this.dataFile = dataFile;
    }

    public synchronized void save(List<CropBatch> batches) throws IOException {
        if (dataFile.getParent() != null) {
            Files.createDirectories(dataFile.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(dataFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            for (CropBatch batch : batches) {
                writer.write(toRecord(batch));
                writer.newLine();
            }
        }
    }

    public synchronized List<CropBatch> load() throws IOException {
        List<CropBatch> batches = new ArrayList<CropBatch>();
        if (!Files.exists(dataFile)) {
            return batches;
        }

        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    batches.add(fromRecord(line));
                } catch (IllegalArgumentException exception) {
                    throw new IOException("Invalid batch record on line " + lineNumber
                            + ": " + exception.getMessage(), exception);
                }
            }
        }
        return batches;
    }

    private String toRecord(CropBatch batch) {
        String extra;
        if (batch instanceof VegetableBatch) {
            extra = "HUMIDITY=" + ((VegetableBatch) batch).getHumidityLevel();
            return String.join("|", "VEGETABLE", escape(batch.getBatchId()),
                    escape(batch.getCropName()), escape(batch.getFarmerName()),
                    String.valueOf(batch.getQuantityKg()),
                    String.valueOf(batch.getBaseMspPerKg()),
                    String.valueOf(batch.getRemainingShelfLifeDays()),
                    String.valueOf(batch.getHighestBidPerKg()),
                    escape(batch.getHighestBidderName() == null ? "" : batch.getHighestBidderName()),
                    extra);
        }
        GrainBatch grain = (GrainBatch) batch;
        extra = "MOISTURE=" + grain.getMoisturePercent();
        return String.join("|", "GRAIN", escape(batch.getBatchId()),
                escape(batch.getCropName()), escape(batch.getFarmerName()),
                String.valueOf(batch.getQuantityKg()),
                String.valueOf(batch.getBaseMspPerKg()),
                String.valueOf(batch.getRemainingShelfLifeDays()),
                String.valueOf(batch.getHighestBidPerKg()),
                escape(batch.getHighestBidderName() == null ? "" : batch.getHighestBidderName()),
                extra);
    }

    private CropBatch fromRecord(String record) {
        String[] fields = record.split("\\|", -1);
        if (fields.length != 10) {
            throw new IllegalArgumentException("A batch record must contain 10 fields.");
        }

        String type = fields[0];
        String batchId = unescape(fields[1]);
        String cropName = unescape(fields[2]);
        String farmerName = unescape(fields[3]);
        double quantity = Double.parseDouble(fields[4]);
        double msp = Double.parseDouble(fields[5]);
        int shelfLife = Integer.parseInt(fields[6]);
        double highestBid = Double.parseDouble(fields[7]);
        String bidder = unescape(fields[8]);

        double extra = parseExtra(fields[9]);
        CropBatch batch;
        if ("VEGETABLE".equals(type)) {
            batch = new VegetableBatch(batchId, cropName, farmerName,
                    quantity, msp, shelfLife, extra);
        } else if ("GRAIN".equals(type)) {
            batch = new GrainBatch(batchId, cropName, farmerName,
                    quantity, msp, shelfLife, extra);
        } else {
            throw new IllegalArgumentException("Unknown batch type: " + type);
        }

        batch.setHighestBidPerKg(highestBid);
        if (!bidder.isEmpty()) {
            batch.setHighestBidderName(bidder);
        }
        return batch;
    }

    private double parseExtra(String field) {
        int equalsIndex = field.indexOf('=');
        if (equalsIndex < 0) {
            throw new IllegalArgumentException("Invalid extra-property field.");
        }
        return Double.parseDouble(field.substring(equalsIndex + 1));
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaped) {
                result.append(character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else {
                result.append(character);
            }
        }
        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }
}
