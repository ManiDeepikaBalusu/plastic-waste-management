package com.waste.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record WatermarkOcrResult(
        boolean watermarkDetected,
        BigDecimal ocrLatitude,
        BigDecimal ocrLongitude,
        String ocrTimestamp,
        String ocrRawText,
        List<String> extractedLines,
        String ocrStatus,
        double confidence
) {
    public static WatermarkOcrResult empty(String status) {
        return new WatermarkOcrResult(false, null, null, null, null, List.of(), status, 0.0);
    }
}
