-- =====================================================
-- Migration 005: Fix Schema Validation Errors
-- Purpose: Add missing columns to pass ddl-auto=validate
-- Impact: Adds created_at, updated_at, last_event_type to user_interest
--         Adds version to product_variants for optimistic locking
-- Safety: Uses IF NOT EXISTS, backward compatible
-- =====================================================

-- ✅ Root Cause:
-- Schema-validation: missing column [created_at] in table [user_interest]
-- Schema-validation: missing column [version] in table [product_variants]
--
-- Entity classes expect these columns but database schema is missing them.
-- This migration brings database schema in sync with JPA entity expectations.

-- =====================================================
-- STEP 1: Check current schema before migration
-- =====================================================

-- Check user_interest table
DESCRIBE user_interest;

-- Check product_variants table
DESCRIBE product_variants;

-- =====================================================
-- STEP 2: Fix user_interest table
-- =====================================================

-- Add last_event_type column (tracks what event last updated this interest)
ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS last_event_type VARCHAR(32) NULL
COMMENT 'Last event type that updated this interest (VIEW, CLICK, SEARCH, etc.)';

-- Add created_at column (tracks when interest was first created)
ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
COMMENT 'When this interest was first created';

-- Add updated_at column (tracks when interest was last modified)
ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
COMMENT 'When this interest was last updated';

-- ⚠️ Backfill existing rows if needed (safe if column already exists)
UPDATE user_interest
SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- =====================================================
-- STEP 3: Add user_interest indexes (if not exists)
-- =====================================================

-- Index for user score lookup (top interests per user)
-- Covers: SELECT * FROM user_interest WHERE user_id = ? ORDER BY score DESC
CREATE INDEX IF NOT EXISTS idx_user_interest_user_score
ON user_interest(user_id, score DESC);

-- Index for user type score lookup (top interests by type)
-- Covers: SELECT * FROM user_interest WHERE user_id = ? AND type = ? ORDER BY score DESC
CREATE INDEX IF NOT EXISTS idx_user_interest_user_type_score
ON user_interest(user_id, type, score DESC);

-- =====================================================
-- STEP 4: Fix product_variants table
-- =====================================================

-- Add version column for optimistic locking (prevents race conditions on stock updates)
ALTER TABLE product_variants
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0
COMMENT 'Optimistic locking version for concurrent stock updates';

-- =====================================================
-- STEP 5: Verify schema after migration
-- =====================================================

-- Verify user_interest columns
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_interest'
  AND COLUMN_NAME IN ('created_at', 'updated_at', 'last_event_type')
ORDER BY COLUMN_NAME;

-- Verify product_variants columns
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'product_variants'
  AND COLUMN_NAME = 'version';

-- Verify user_interest indexes
SHOW INDEX FROM user_interest
WHERE Key_name IN ('idx_user_interest_user_score', 'idx_user_interest_user_type_score');

-- =====================================================
-- STEP 6: Test queries to verify indexes work
-- =====================================================

-- Test user_interest queries (should use indexes)
EXPLAIN SELECT * FROM user_interest
WHERE user_id = 1 AND type = 'CATEGORY'
ORDER BY score DESC
LIMIT 20;
-- Expected: Uses idx_user_interest_user_type_score

EXPLAIN SELECT * FROM user_interest
WHERE user_id = 1
ORDER BY score DESC
LIMIT 10;
-- Expected: Uses idx_user_interest_user_score

-- =====================================================
-- STEP 7: Data validation
-- =====================================================

-- Check for NULL values in required columns
SELECT
    COUNT(*) as total_rows,
    SUM(CASE WHEN created_at IS NULL THEN 1 ELSE 0 END) as null_created_at,
    SUM(CASE WHEN updated_at IS NULL THEN 1 ELSE 0 END) as null_updated_at,
    SUM(CASE WHEN created_at > updated_at THEN 1 ELSE 0 END) as invalid_dates
FROM user_interest;

-- Expected: 0 nulls, 0 invalid dates

-- Check product_variants version column
SELECT
    COUNT(*) as total_rows,
    SUM(CASE WHEN version IS NULL THEN 1 ELSE 0 END) as null_version,
    MIN(version) as min_version,
    MAX(version) as max_version
FROM product_variants;

-- Expected: 0 nulls, min_version = 0

-- =====================================================
-- ✅ MIGRATION COMPLETE
-- =====================================================

-- Expected results:
-- 1. user_interest has: created_at, updated_at, last_event_type columns
-- 2. product_variants has: version column
-- 3. Indexes exist: idx_user_interest_user_score, idx_user_interest_user_type_score
-- 4. No NULL values in required columns
-- 5. All existing data preserved
-- 6. Application can start with ddl-auto=validate

-- =====================================================
-- HOW TO APPLY THIS MIGRATION
-- =====================================================

-- Option 1: Manual execution (safest for production)
-- 1. Connect to MySQL: mysql -u your_user -p your_database
-- 2. Run: source /path/to/005_fix_schema_validation_errors.sql
-- 3. Verify: Check STEP 5 output for expected columns
-- 4. Start application: ./gradlew bootRun

-- Option 2: Using mysql client
-- mysql -u your_user -p your_database < scripts/migrations/005_fix_schema_validation_errors.sql

-- Option 3: Copy-paste individual statements (development only)
-- Copy ALTER TABLE statements and run them one by one

-- ⚠️ PRODUCTION SAFETY CHECKLIST:
-- ✅ Backup database before running migration
-- ✅ Test migration on staging environment first
-- ✅ Run during low-traffic maintenance window
-- ✅ Monitor application logs after deployment
-- ✅ Keep rollback plan ready (though these changes are safe)
