-- ============================================================================
-- V3__photo_verification_enhancements.sql
-- Granular metadata fields for GPS accuracy, separate timestamp source,
-- file last-modified time, and OCR watermark verification.
-- ============================================================================

ALTER TABLE complaint_images
    ADD COLUMN IF NOT EXISTS file_last_modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS device_gps_accuracy DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS device_gps_timestamp TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS coordinate_source VARCHAR(50),
    ADD COLUMN IF NOT EXISTS timestamp_source VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ocr_watermark_text TEXT,
    ADD COLUMN IF NOT EXISTS ocr_watermark_status VARCHAR(50);

ALTER TABLE verification_results
    ADD COLUMN IF NOT EXISTS coordinate_source VARCHAR(50),
    ADD COLUMN IF NOT EXISTS timestamp_source VARCHAR(50),
    ADD COLUMN IF NOT EXISTS device_gps_accuracy DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ocr_watermark_text TEXT,
    ADD COLUMN IF NOT EXISTS ocr_watermark_status VARCHAR(50);
