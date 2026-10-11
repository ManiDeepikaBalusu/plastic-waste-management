package com.waste.management.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.waste.management.dto.WatermarkOcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WatermarkOcrService {

    private static final Logger logger = LoggerFactory.getLogger(WatermarkOcrService.class);

    private final String ocrServiceUrl;
    private final boolean ocrEnabled;
    private final RestTemplate restTemplate;

    public WatermarkOcrService(
            @Value("${app.verification.ocr-service-url:http://localhost:8001}") String ocrServiceUrl,
            @Value("${app.verification.ocr-enabled:true}") boolean ocrEnabled) {
        this.ocrServiceUrl = ocrServiceUrl;
        this.ocrEnabled = ocrEnabled;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Extracts watermark text, coordinates, and timestamp from an image by querying
     * the AI microservice. Fully resilient: failures or timeouts return a safe fallback
     * without blocking or failing the main verification pipeline.
     */
    public WatermarkOcrResult extractWatermark(byte[] imageBytes, String filename) {
        if (!ocrEnabled) {
            logger.debug("[WATERMARK_OCR] OCR service disabled via configuration.");
            return WatermarkOcrResult.empty("OCR_DISABLED");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            return WatermarkOcrResult.empty("EMPTY_IMAGE");
        }

        String safeFilename = (filename != null && !filename.isBlank()) ? filename : "image.jpg";
        String endpoint = ocrServiceUrl.replaceAll("/+$", "") + "/ocr/watermark";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<AiWatermarkOcrDto> response = restTemplate.postForEntity(
                    endpoint,
                    requestEntity,
                    AiWatermarkOcrDto.class
            );

            AiWatermarkOcrDto resBody = response.getBody();
            if (resBody != null && resBody.watermarkDetected()) {
                BigDecimal lat = resBody.ocrLatitude() != null ? BigDecimal.valueOf(resBody.ocrLatitude()) : null;
                BigDecimal lon = resBody.ocrLongitude() != null ? BigDecimal.valueOf(resBody.ocrLongitude()) : null;

                logger.info("[WATERMARK_OCR] Watermark detected for file '{}': Lat={}, Lon={}, Time='{}', Status={}",
                        safeFilename, lat, lon, resBody.ocrTimestamp(), resBody.ocrStatus());

                return new WatermarkOcrResult(
                        true,
                        lat,
                        lon,
                        resBody.ocrTimestamp(),
                        resBody.ocrRawText(),
                        resBody.extractedLines() != null ? resBody.extractedLines() : List.of(),
                        resBody.ocrStatus() != null ? resBody.ocrStatus() : "OCR_WATERMARK_UNVERIFIED",
                        resBody.confidence()
                );
            } else if (resBody != null) {
                logger.debug("[WATERMARK_OCR] No watermark detected in file '{}' (Status: {})", safeFilename, resBody.ocrStatus());
                return WatermarkOcrResult.empty(resBody.ocrStatus() != null ? resBody.ocrStatus() : "NO_WATERMARK_FOUND");
            } else {
                return WatermarkOcrResult.empty("EMPTY_RESPONSE");
            }
        } catch (Exception ex) {
            logger.warn("[WATERMARK_OCR] AI microservice OCR query failed for endpoint '{}': {}. Proceeding without OCR data.",
                    endpoint, ex.getMessage());
            return WatermarkOcrResult.empty("AI_SERVICE_UNAVAILABLE");
        }
    }

    public record AiWatermarkOcrDto(
            @JsonProperty("watermark_detected") boolean watermarkDetected,
            @JsonProperty("ocr_latitude") Double ocrLatitude,
            @JsonProperty("ocr_longitude") Double ocrLongitude,
            @JsonProperty("ocr_timestamp") String ocrTimestamp,
            @JsonProperty("ocr_raw_text") String ocrRawText,
            @JsonProperty("extracted_lines") List<String> extractedLines,
            @JsonProperty("ocr_status") String ocrStatus,
            @JsonProperty("confidence") double confidence
    ) {}
}
