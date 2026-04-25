-- =====================================================
-- Migration: Backfill NULL category and brand in event_logs
-- Purpose: Populate missing category/brand from products table
-- Impact: Updates existing event_logs with NULL category/brand
-- Safety: Read-only joins, updates only NULL values
-- =====================================================

-- Step 1: Show statistics BEFORE backfill
SELECT
    'BEFORE' as status,
    COUNT(*) as total_events,
    SUM(CASE WHEN category IS NULL THEN 1 ELSE 0 END) as null_category,
    SUM(CASE WHEN brand IS NULL THEN 1 ELSE 0 END) as null_brand,
    SUM(CASE WHEN product_id IS NOT NULL AND category IS NULL THEN 1 ELSE 0 END) as fixable_category,
    SUM(CASE WHEN product_id IS NOT NULL AND brand IS NULL THEN 1 ELSE 0 END) as fixable_brand
FROM event_logs;

-- Step 2: Backfill category from products.category
-- Only update rows where:
--   1. event_logs.category IS NULL
--   2. event_logs.product_id IS NOT NULL
--   3. products.category IS NOT NULL
UPDATE event_logs e
INNER JOIN products p ON e.product_id = p.id
SET e.category = p.category
WHERE e.category IS NULL
  AND e.product_id IS NOT NULL
  AND p.category IS NOT NULL;

-- Step 3: Backfill brand from products.brand
-- Only update rows where:
--   1. event_logs.brand IS NULL
--   2. event_logs.product_id IS NOT NULL
--   3. products.brand IS NOT NULL
UPDATE event_logs e
INNER JOIN products p ON e.product_id = p.id
SET e.brand = p.brand
WHERE e.brand IS NULL
  AND e.product_id IS NOT NULL
  AND p.brand IS NOT NULL;

-- Step 4: Show statistics AFTER backfill
SELECT
    'AFTER' as status,
    COUNT(*) as total_events,
    SUM(CASE WHEN category IS NULL THEN 1 ELSE 0 END) as null_category,
    SUM(CASE WHEN brand IS NULL THEN 1 ELSE 0 END) as null_brand,
    SUM(CASE WHEN product_id IS NOT NULL AND category IS NULL THEN 1 ELSE 0 END) as orphaned_category,
    SUM(CASE WHEN product_id IS NOT NULL AND brand IS NULL THEN 1 ELSE 0 END) as orphaned_brand
FROM event_logs;

-- ✅ Expected result:
--    - null_category and null_brand should be reduced
--    - orphaned_category/orphaned_brand = events with deleted products (cannot be fixed)
--
-- ⚠️ Note: Events with NULL product_id (e.g., SEARCH events) will still have NULL category/brand
--          This is expected and correct behavior.
