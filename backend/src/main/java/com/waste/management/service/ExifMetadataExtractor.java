package com.waste.management.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.waste.management.dto.ExtractedImageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class ExifMetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ExifMetadataExtractor.class);

    public ExtractedImageMetadata extractMetadata(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ExtractedImageMetadata.empty();
        }

        try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(imageBytes))) {
            return extractMetadata(is);
        } catch (Exception ex) {
            logger.warn("Failed to extract EXIF metadata from image bytes: {}", ex.getMessage());
            return ExtractedImageMetadata.empty();
        }
    }

    public ExtractedImageMetadata extractMetadata(InputStream inputStream) {
        BigDecimal exifLat = null;
        BigDecimal exifLon = null;
        OffsetDateTime exifCapturedAt = null;
        String cameraMake = null;
        String cameraModel = null;
        boolean hasGps = false;
        boolean hasDate = false;

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            // 1. Extract GPS Coordinates
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null) {
                GeoLocation geoLocation = gpsDir.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    exifLat = BigDecimal.valueOf(geoLocation.getLatitude()).setScale(7, RoundingMode.HALF_UP);
                    exifLon = BigDecimal.valueOf(geoLocation.getLongitude()).setScale(7, RoundingMode.HALF_UP);
                    hasGps = true;
                }

                Date gpsDate = gpsDir.getGpsDate();
                if (gpsDate != null) {
                    exifCapturedAt = gpsDate.toInstant().atOffset(ZoneOffset.UTC);
                    hasDate = true;
                }
            }

            // 2. Extract SubIFD Capture Date if not found in GPS
            if (exifCapturedAt == null) {
                ExifSubIFDDirectory subIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                if (subIFD != null) {
                    Date dateOriginal = subIFD.getDateOriginal();
                    if (dateOriginal != null) {
                        exifCapturedAt = dateOriginal.toInstant().atOffset(ZoneOffset.UTC);
                        hasDate = true;
                    }
                }
            }

            // 3. Extract Camera Make, Model, Software & Image Description
            String software = null;
            String imageDescription = null;
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                cameraMake = ifd0.getString(ExifIFD0Directory.TAG_MAKE);
                cameraModel = ifd0.getString(ExifIFD0Directory.TAG_MODEL);
                software = ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE);
                imageDescription = ifd0.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
            }

            // 4. Extract User Comment
            String userComment = null;
            ExifSubIFDDirectory subIFDForComment = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIFDForComment != null) {
                userComment = subIFDForComment.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT);
            }

            boolean hasWatermarkIndicator = false;
            String combined = ((software != null ? software : "") + " " +
                               (imageDescription != null ? imageDescription : "") + " " +
                               (userComment != null ? userComment : "")).toLowerCase();
            if (combined.contains("gps map camera") ||
                combined.contains("gpsmapcamera") ||
                combined.contains("timestamp camera") ||
                combined.contains("watermark") ||
                combined.contains("geotag")) {
                hasWatermarkIndicator = true;
            }

            return new ExtractedImageMetadata(
                    exifLat,
                    exifLon,
                    exifCapturedAt,
                    cameraMake,
                    cameraModel,
                    software,
                    imageDescription,
                    userComment,
                    hasGps,
                    hasDate,
                    hasWatermarkIndicator
            );

        } catch (Exception ex) {
            // Missing EXIF or unparseable metadata is normal for stripped images (web, chat uploads)
            logger.debug("Image has no extractable EXIF metadata or stripped format: {}", ex.getMessage());
            return ExtractedImageMetadata.empty();
        }
    }
}
