-- ============================================================================
-- V1__init_schema.sql
-- Plastic Waste Reporting and Volunteer Tracking System
-- Flyway Database Migration V1
-- ============================================================================

-- Ensure pgcrypto extension is available for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. USERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(25),
    role VARCHAR(30) NOT NULL CHECK (role IN ('CITIZEN', 'ADMIN', 'VOLUNTEER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- ============================================================================
-- 2. VOLUNTEER_PROFILES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS volunteer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    is_available BOOLEAN NOT NULL DEFAULT FALSE,
    current_latitude DECIMAL(10, 7),
    current_longitude DECIMAL(10, 7),
    last_location_update TIMESTAMP WITH TIME ZONE,
    total_assigned INT NOT NULL DEFAULT 0,
    total_resolved INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_volunteer_avail_loc ON volunteer_profiles(is_available, current_latitude, current_longitude);

-- ============================================================================
-- 3. COMPLAINTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    citizen_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    waste_type VARCHAR(50) NOT NULL DEFAULT 'PLASTIC_MIXED',
    estimated_size VARCHAR(50),
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    address_text TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED' 
        CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED', 'CANCELLED')),
    cancellation_reason TEXT,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_complaints_citizen ON complaints(citizen_id);
CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(status);
CREATE INDEX IF NOT EXISTS idx_complaints_coords ON complaints(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_complaints_created_at ON complaints(created_at DESC);

-- ============================================================================
-- 4. COMPLAINT_IMAGES TABLE (Stores Cloudinary URLs, not binary files)
-- ============================================================================
CREATE TABLE IF NOT EXISTS complaint_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(255),
    image_type VARCHAR(40) NOT NULL DEFAULT 'INITIAL_PROOF' 
        CHECK (image_type IN ('INITIAL_PROOF', 'RESOLUTION_PROOF', 'INSPECTION_PROOF')),
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_complaint_images_complaint ON complaint_images(complaint_id, image_type);

-- ============================================================================
-- 5. ASSIGNMENTS TABLE (Full dispatch & reassignment history)
-- ============================================================================
CREATE TABLE IF NOT EXISTS assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE RESTRICT,
    volunteer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REASSIGNED')),
    notes TEXT,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_assignments_complaint ON assignments(complaint_id);
CREATE INDEX IF NOT EXISTS idx_assignments_volunteer ON assignments(volunteer_id);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON assignments(status);

-- ============================================================================
-- 6. PROGRESS_UPDATES TABLE (Granular lifecycle progress audit log)
-- ============================================================================
CREATE TABLE IF NOT EXISTS progress_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    previous_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_progress_complaint ON progress_updates(complaint_id);
CREATE INDEX IF NOT EXISTS idx_progress_created_at ON progress_updates(created_at DESC);

-- ============================================================================
-- 7. VERIFICATION_RESULTS TABLE (AI & Admin verification metadata)
-- ============================================================================
CREATE TABLE IF NOT EXISTS verification_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    verifier_type VARCHAR(30) NOT NULL CHECK (verifier_type IN ('AI_SERVICE', 'ADMIN_USER')),
    verifier_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_valid_plastic BOOLEAN NOT NULL,
    confidence_score DECIMAL(5, 4),
    detected_classes JSONB,
    analysis_notes TEXT,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_verification_complaint ON verification_results(complaint_id);
CREATE INDEX IF NOT EXISTS idx_verification_type ON verification_results(verifier_type);

-- ============================================================================
-- 8. NOTIFICATIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'COMPLAINT_UPDATE',
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
