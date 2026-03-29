-- ============================================
-- PROFESSIONAL MARKETPLACE SEARCH INDEXES
-- Migration: marketplace_search_indexes
-- Date: 2026-03-29
-- Purpose: Add indexes for tier-based search relevance
-- ============================================

-- Index 1: Active + Stock (for filtering and stock-aware ranking)
-- Supports: WHERE active = 1 AND stock checks
ALTER TABLE products
ADD INDEX idx_products_active_stock (active, stock);

-- Index 2: Name prefix matching (for LIKE 'keyword%' queries)
-- Prefix length 100 chars covers most product names
ALTER TABLE products
ADD INDEX idx_products_name_prefix (active, name(100));

-- Index 3 (Optional): FULLTEXT for natural language search enhancement
-- Uncomment if you want FULLTEXT boost (MySQL 5.6+)
-- ALTER TABLE products
-- ADD FULLTEXT INDEX idx_products_fulltext_search (name, description, search_text);

-- Verify existing indexes are optimal
-- You already have these (keep them):
-- idx_products_search_text (search_text) ✅
-- idx_products_active_sold_view (active, sold_count, view_count) ✅
-- idx_products_category (category) ✅
-- idx_products_brand (brand) ✅

-- Verification query
SHOW INDEX FROM products WHERE Key_name IN (
    'idx_products_active_stock',
    'idx_products_name_prefix'
);

-- Performance test query (should use new indexes)
EXPLAIN SELECT * FROM products
WHERE active = 1
  AND LOWER(name) LIKE 'collagen%'
ORDER BY stock DESC, sold_count DESC;
