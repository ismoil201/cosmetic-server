-- =====================================================
-- Migration: Add composite indexes to event_logs table
-- Purpose: Optimize analytics and recommendation queries
-- Impact: Improves query performance for common access patterns
-- Safety: Index creation is non-blocking in MySQL 5.7+
-- =====================================================

-- ✅ ANALYSIS: Common query patterns for event_logs
--
-- 1. User activity timeline:
--    SELECT * FROM event_logs WHERE user_id = ? ORDER BY created_at DESC
--
-- 2. User's product interactions:
--    SELECT * FROM event_logs WHERE user_id = ? AND event_type IN ('VIEW','CLICK','PURCHASE')
--
-- 3. Recent events by type:
--    SELECT * FROM event_logs WHERE event_type = 'VIEW' AND created_at > ?
--
-- 4. User behavior analysis:
--    SELECT * FROM event_logs WHERE user_id = ? AND event_type = 'PURCHASE' ORDER BY created_at DESC

-- =====================================================
-- Step 1: Check existing indexes
-- =====================================================
SHOW INDEX FROM event_logs;

-- Expected existing indexes (from @Index annotations in EventLog.java):
-- - idx_event_user (user_id)
-- - idx_event_product (product_id)
-- - idx_event_type (event_type)
-- - idx_event_created (created_at)

-- =====================================================
-- Step 2: Add composite indexes for better performance
-- =====================================================

-- Index 1: User activity timeline (most common query)
-- Covers: WHERE user_id = ? ORDER BY created_at DESC
-- Benefit: Eliminates filesort for user activity feeds
CREATE INDEX idx_event_user_created
ON event_logs(user_id, created_at);

-- Index 2: User behavior by event type
-- Covers: WHERE user_id = ? AND event_type = ? ORDER BY created_at
-- Benefit: Fast filtering for specific user actions (e.g., all purchases)
CREATE INDEX idx_event_user_type_created
ON event_logs(user_id, event_type, created_at);

-- Index 3: Event type timeline
-- Covers: WHERE event_type = ? AND created_at > ?
-- Benefit: Fast queries for recent events of specific type
CREATE INDEX idx_event_type_created
ON event_logs(event_type, created_at);

-- Index 4: Product popularity tracking
-- Covers: WHERE product_id = ? AND created_at > ?
-- Benefit: Fast product interaction history queries
CREATE INDEX idx_event_product_created
ON event_logs(product_id, created_at);

-- =====================================================
-- Step 3: Verify new indexes
-- =====================================================
SHOW INDEX FROM event_logs;

-- Expected result: 8 indexes total
-- - PRIMARY (id)
-- - idx_event_user (user_id)
-- - idx_event_product (product_id)
-- - idx_event_type (event_type)
-- - idx_event_created (created_at)
-- - idx_event_user_created (user_id, created_at) ✨ NEW
-- - idx_event_user_type_created (user_id, event_type, created_at) ✨ NEW
-- - idx_event_type_created (event_type, created_at) ✨ NEW
-- - idx_event_product_created (product_id, created_at) ✨ NEW

-- =====================================================
-- Step 4: Test query performance (optional)
-- =====================================================

-- Test 1: User activity timeline (should use idx_event_user_created)
EXPLAIN SELECT * FROM event_logs
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 50;

-- Test 2: User purchases (should use idx_event_user_type_created)
EXPLAIN SELECT * FROM event_logs
WHERE user_id = 1 AND event_type = 'PURCHASE'
ORDER BY created_at DESC;

-- Test 3: Recent views (should use idx_event_type_created)
EXPLAIN SELECT * FROM event_logs
WHERE event_type = 'VIEW' AND created_at > DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ✅ Expected EXPLAIN output:
--    - type: ref or range
--    - key: one of the new composite indexes
--    - Extra: "Using index" or "Using where; Using index"
--
-- ❌ Bad EXPLAIN output:
--    - type: ALL (full table scan)
--    - Extra: "Using filesort" or "Using temporary"
