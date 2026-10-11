-- ============================================================================
-- V2__photo_verification.sql
-- GPS-Based Photo Verification & Waste Cleanup Verification Schema Extensions
-- ============================================================================

-- 1. Extend complaint_images with device and EXIF GPS metadata & verification state
ALTER TABLE complaint_images
    ADD COLUMN IF NOT EXISTS device_latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS device_longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS device_captured_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS exif_latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS exif_longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS exif_captured_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS exif_camera_make VARCHAR(100),
    ADD COLUMN IF NOT EXISTS exif_camera_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sha256_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS distance_to_report_meters DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS distance_device_to_exif_meters DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(40) NOT NULL DEFAULT 'VERIFIED',
    ADD COLUMN IF NOT EXISTS verification_flags JSONB,
    ADD COLUMN IF NOT EXISTS capture_source VARCHAR(40) NOT NULL DEFAULT 'CAMERA';

CREATE INDEX IF NOT EXISTS idx_complaint_images_sha256 ON complaint_images(sha256_hash);
CREATE INDEX IF NOT EXISTS idx_complaint_images_verif_status ON complaint_images(verification_status);

-- 2. Extend verification_results for automated GPS check audit logs and Admin reviews
ALTER TABLE verification_results DROP CONSTRAINT IF EXISTS verification_results_verifier_type_check;
ALTER TABLE verification_results ADD CONSTRAINT verification_results_verifier_type_check 
    CHECK (verifier_type IN ('AI_SERVICE', 'ADMIN_USER', 'GPS_VERIFIER', 'ADMIN_REVIEW'));

ALTER TABLE verification_results
    ADD COLUMN IF NOT EXISTS assignment_id UUID REFERENCES assignments(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS image_id UUID REFERENCES complaint_images(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(40) NOT NULL DEFAULT 'VERIFIED',
    ADD COLUMN IF NOT EXISTS check_type VARCHAR(40) NOT NULL DEFAULT 'INITIAL_REPORT',
    ADD COLUMN IF NOT EXISTS device_latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS device_longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS exif_latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS exif_longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS target_latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS target_longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS distance_meters DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS distance_tolerance_meters DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS check_reasons JSONB,
    ADD COLUMN IF NOT EXISTS review_decision VARCHAR(30) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS admin_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_verification_assignment ON verification_results(assignment_id);
CREATE INDEX IF NOT EXISTS idx_verification_status ON verification_results(verification_status);

-- 3. Extend assignments with cleanup verification status
ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(40) DEFAULT 'VERIFIED',
    ADD COLUMN IF NOT EXISTS verification_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_assignments_verif_status ON assignments(verification_status);
