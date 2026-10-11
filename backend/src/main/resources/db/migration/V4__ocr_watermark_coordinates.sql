-- ============================================================================
-- V4__ocr_watermark_coordinates.sql
-- Separate OCR watermark coordinates and timestamps, plus browser upload
-- coordinates distinct from shutter capture GPS.
-- ============================================================================

ALTER TABLE verification_results
    ADD COLUMN IF NOT EXISTS ocr_latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS ocr_longitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS ocr_timestamp VARCHAR(100),
    ADD COLUMN IF NOT EXISTS browser_upload_latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS browser_upload_longitude NUMERIC(10, 7);

ALTER TABLE complaint_images
    ADD COLUMN IF NOT EXISTS ocr_latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS ocr_longitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS ocr_timestamp VARCHAR(100),
    ADD COLUMN IF NOT EXISTS browser_upload_latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS browser_upload_longitude NUMERIC(10, 7);
