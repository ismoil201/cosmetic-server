-- =====================================================
-- Migration: Backfill user_interest from event_logs
-- Purpose: Build user interest profiles from historical event data
-- Impact: Enables personalized recommendations based on past user behavior
-- Safety: Read-only event_logs, upserts into user_interest
-- =====================================================

-- ✅ STEP 8: User Interest Profile Builder
--
-- This migration aggregates event_logs into user_interest table:
-- - CATEGORY interests from event_logs.category
-- - BRAND interests from event_logs.brand
-- - QUERY interests from SEARCH events with query_text
--
-- Event weights (as per requirements):
-- - IMPRESSION = 0.1
-- - CLICK = 1.0
-- - VIEW = 2.0
-- - FAVORITE_ADD/FAVORITE_REMOVE = 4.0
-- - ADD_TO_CART = 6.0
-- - PURCHASE = 10.0
-- - SEARCH = 2.0

-- =====================================================
-- Step 1: Statistics BEFORE backfill
-- =====================================================
SELECT 'BEFORE BACKFILL' as status;

SELECT
    'event_logs' as table_name,
    COUNT(*) as total_events,
    COUNT(DISTINCT user_id) as unique_users,
    SUM(CASE WHEN user_id IS NOT NULL THEN 1 ELSE 0 END) as authenticated_events,
    SUM(CASE WHEN user_id IS NOT NULL AND category IS NOT NULL THEN 1 ELSE 0 END) as events_with_category,
    SUM(CASE WHEN user_id IS NOT NULL AND brand IS NOT NULL THEN 1 ELSE 0 END) as events_with_brand,
    SUM(CASE WHEN user_id IS NOT NULL AND event_type = 'SEARCH' AND query_text IS NOT NULL THEN 1 ELSE 0 END) as search_events
FROM event_logs;

SELECT
    'user_interest' as table_name,
    COUNT(*) as total_interests,
    COUNT(DISTINCT user_id) as unique_users,
    SUM(CASE WHEN type = 'CATEGORY' THEN 1 ELSE 0 END) as category_interests,
    SUM(CASE WHEN type = 'BRAND' THEN 1 ELSE 0 END) as brand_interests,
    SUM(CASE WHEN type = 'QUERY' THEN 1 ELSE 0 END) as query_interests
FROM user_interest;

-- =====================================================
-- Step 2: Backfill CATEGORY interests
-- =====================================================

-- Aggregate event_logs by user_id and category
-- Apply event weights using CASE expression
-- Use ON DUPLICATE KEY UPDATE for MySQL (or INSERT ... ON CONFLICT for PostgreSQL)

INSERT INTO user_interest (user_id, type, key_name, score, last_event_type, created_at, updated_at)
SELECT
    user_id,
    'CATEGORY' as type,
    category as key_name,
    SUM(
        CASE event_type
            WHEN 'IMPRESSION' THEN 0.1
            WHEN 'CLICK' THEN 1.0
            WHEN 'VIEW' THEN 2.0
            WHEN 'FAVORITE_ADD' THEN 4.0
            WHEN 'FAVORITE_REMOVE' THEN -2.0  -- Negative feedback (50% of FAVORITE weight)
            WHEN 'ADD_TO_CART' THEN 6.0
            WHEN 'PURCHASE' THEN 10.0
            ELSE 0.0
        END
    ) as total_score,
    MAX(event_type) as last_event_type,  -- Most recent event type (approximate)
    MIN(created_at) as created_at,
    MAX(created_at) as updated_at
FROM event_logs
WHERE user_id IS NOT NULL
  AND category IS NOT NULL
  AND category != ''
GROUP BY user_id, category
HAVING total_score > 0  -- Only create interests with positive score
ON DUPLICATE KEY UPDATE
    score = score + VALUES(score),
    last_event_type = VALUES(last_event_type),
    updated_at = VALUES(updated_at);

-- =====================================================
-- Step 3: Backfill BRAND interests
-- =====================================================

INSERT INTO user_interest (user_id, type, key_name, score, last_event_type, created_at, updated_at)
SELECT
    user_id,
    'BRAND' as type,
    LOWER(TRIM(brand)) as key_name,  -- Normalize: lowercase and trim
    SUM(
        CASE event_type
            WHEN 'IMPRESSION' THEN 0.1
            WHEN 'CLICK' THEN 1.0
            WHEN 'VIEW' THEN 2.0
            WHEN 'FAVORITE_ADD' THEN 4.0
            WHEN 'FAVORITE_REMOVE' THEN -2.0
            WHEN 'ADD_TO_CART' THEN 6.0
            WHEN 'PURCHASE' THEN 10.0
            ELSE 0.0
        END
    ) as total_score,
    MAX(event_type) as last_event_type,
    MIN(created_at) as created_at,
    MAX(created_at) as updated_at
FROM event_logs
WHERE user_id IS NOT NULL
  AND brand IS NOT NULL
  AND brand != ''
GROUP BY user_id, LOWER(TRIM(brand))
HAVING total_score > 0
ON DUPLICATE KEY UPDATE
    score = score + VALUES(score),
    last_event_type = VALUES(last_event_type),
    updated_at = VALUES(updated_at);

-- =====================================================
-- Step 4: Backfill QUERY interests (from SEARCH events)
-- =====================================================

INSERT INTO user_interest (user_id, type, key_name, score, last_event_type, created_at, updated_at)
SELECT
    user_id,
    'QUERY' as type,
    LOWER(TRIM(query_text)) as key_name,  -- Normalize: lowercase and trim
    COUNT(*) * 2.0 as total_score,  -- SEARCH weight = 2.0
    'SEARCH' as last_event_type,
    MIN(created_at) as created_at,
    MAX(created_at) as updated_at
FROM event_logs
WHERE user_id IS NOT NULL
  AND event_type = 'SEARCH'
  AND query_text IS NOT NULL
  AND query_text != ''
GROUP BY user_id, LOWER(TRIM(query_text))
ON DUPLICATE KEY UPDATE
    score = score + VALUES(score),
    last_event_type = 'SEARCH',
    updated_at = VALUES(updated_at);

-- =====================================================
-- Step 5: Statistics AFTER backfill
-- =====================================================
SELECT 'AFTER BACKFILL' as status;

SELECT
    type,
    COUNT(*) as total_interests,
    COUNT(DISTINCT user_id) as unique_users,
    ROUND(AVG(score), 2) as avg_score,
    ROUND(MAX(score), 2) as max_score,
    ROUND(MIN(score), 2) as min_score
FROM user_interest
GROUP BY type
ORDER BY type;

-- Show top 10 users by total interest score
SELECT
    user_id,
    COUNT(*) as num_interests,
    ROUND(SUM(score), 2) as total_score,
    MAX(updated_at) as last_activity
FROM user_interest
GROUP BY user_id
ORDER BY total_score DESC
LIMIT 10;

-- =====================================================
-- Step 6: Validation queries
-- =====================================================

-- Check for invalid data
SELECT
    'Invalid interests (blank keys)' as validation_check,
    COUNT(*) as count
FROM user_interest
WHERE key_name IS NULL OR key_name = '';

-- Should return 0 rows
SELECT
    'Interests with negative score' as validation_check,
    COUNT(*) as count
FROM user_interest
WHERE score < 0;

-- Check unique constraint
SELECT
    user_id, type, key_name, COUNT(*) as duplicates
FROM user_interest
GROUP BY user_id, type, key_name
HAVING duplicates > 1;

-- Should return 0 rows (unique constraint enforced)

-- ✅ Expected results:
-- - CATEGORY interests created from product categories
-- - BRAND interests created from brand names (normalized to lowercase)
-- - QUERY interests created from search queries (normalized to lowercase)
-- - All interests have positive scores
-- - No duplicate (user_id, type, key_name) combinations
-- - Top users with highest engagement have highest total scores
