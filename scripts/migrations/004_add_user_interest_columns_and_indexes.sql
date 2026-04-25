-- =====================================================
-- Migration: Add new columns and indexes to user_interest table
-- Purpose: Support STEP 8 User Interest Profile Builder
-- Impact: Adds last_event_type, created_at columns and performance indexes
-- Safety: Non-blocking schema changes, backward compatible
-- =====================================================

-- ✅ STEP 8: Database schema updates for user interest tracking

-- =====================================================
-- Step 1: Check current schema
-- =====================================================
DESCRIBE user_interest;

-- =====================================================
-- Step 2: Add new columns (if not exists)
-- =====================================================

-- Add last_event_type column to track what event last updated this interest
ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS last_event_type VARCHAR(32) NULL
COMMENT 'Last event type that updated this interest (VIEW, CLICK, SEARCH, etc.)';

-- Add created_at column to track when interest was first created
ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
COMMENT 'When this interest was first created';

-- ⚠️ If created_at column already exists without default, update it:
-- UPDATE user_interest SET created_at = updated_at WHERE created_at IS NULL;

-- =====================================================
-- Step 3: Add performance indexes
-- =====================================================

-- Index 1: User score lookup (for top interests per user)
-- Covers: SELECT * FROM user_interest WHERE user_id = ? ORDER BY score DESC
CREATE INDEX IF NOT EXISTS idx_user_interest_user_score
ON user_interest(user_id, score DESC);

-- Index 2: User type score lookup (for top interests by type)
-- Covers: SELECT * FROM user_interest WHERE user_id = ? AND type = ? ORDER BY score DESC
CREATE INDEX IF NOT EXISTS idx_user_interest_user_type_score
ON user_interest(user_id, type, score DESC);

-- ⚠️ Note: Unique constraint already exists from entity definition:
-- UNIQUE(user_id, type, key_name)

-- =====================================================
-- Step 4: Verify indexes
-- =====================================================
SHOW INDEX FROM user_interest;

-- Expected indexes:
-- 1. PRIMARY (id)
-- 2. UNIQUE(user_id, type, key_name)
-- 3. idx_user_interest_user_score (user_id, score)
-- 4. idx_user_interest_user_type_score (user_id, type, score)

-- =====================================================
-- Step 5: Test query performance
-- =====================================================

-- Test 1: Get top 20 category interests for user
EXPLAIN SELECT * FROM user_interest
WHERE user_id = 1 AND type = 'CATEGORY'
ORDER BY score DESC
LIMIT 20;
-- Expected: Uses idx_user_interest_user_type_score

-- Test 2: Get top 10 overall interests for user
EXPLAIN SELECT * FROM user_interest
WHERE user_id = 1
ORDER BY score DESC
LIMIT 10;
-- Expected: Uses idx_user_interest_user_score

-- Test 3: Check if interest exists
EXPLAIN SELECT * FROM user_interest
WHERE user_id = 1 AND type = 'BRAND' AND key_name = 'cosrx';
-- Expected: Uses unique constraint index

-- =====================================================
-- Step 6: Data validation
-- =====================================================

-- Check if last_event_type values are valid
SELECT DISTINCT last_event_type, COUNT(*) as count
FROM user_interest
GROUP BY last_event_type
ORDER BY count DESC;

-- Check created_at vs updated_at consistency
SELECT
    COUNT(*) as total,
    SUM(CASE WHEN created_at > updated_at THEN 1 ELSE 0 END) as invalid_dates,
    SUM(CASE WHEN created_at IS NULL THEN 1 ELSE 0 END) as null_created_at,
    SUM(CASE WHEN updated_at IS NULL THEN 1 ELSE 0 END) as null_updated_at
FROM user_interest;

-- ✅ Expected results:
-- - All queries use appropriate indexes
-- - No NULL created_at values
-- - created_at <= updated_at for all rows
-- - last_event_type contains valid EventType values or NULL
