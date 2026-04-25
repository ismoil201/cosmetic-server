# Home Feed Query Performance Analysis

**Purpose:** Database query plan verification for `/api/home/feed` endpoint
**Last Updated:** 2026-04-25

---

## Overview

The home feed executes 5-7 main product candidate queries. This document provides the exact SQL to run EXPLAIN on each query to verify index usage and query plan efficiency.

**Expected Performance:**
- Each query should use indexes (not full table scan)
- Query execution time < 100ms per query
- Total candidate loading time < 200ms

---

## 1. Category Candidates Query

### Query
```sql
EXPLAIN SELECT p.* FROM products p
WHERE p.active = 1
  AND p.category IN ('SERUM', 'CREAM', 'TONER')
  AND (1 = 1 OR p.id NOT IN (123, 456, 789))
ORDER BY (p.sold_count * 3 + p.view_count) DESC, p.created_at DESC
LIMIT 100;
```

### Expected Index Usage
**Primary Index:** `idx_products_category`
- Type: BTREE index on `category` column
- Used for: `WHERE p.category IN (...)`

**Secondary Index:** `idx_products_active_sold_view`
- Type: Composite index on `(active, sold_count, view_count)`
- Used for: Composite filtering and sorting optimization

**Fallback Index:** `idx_products_active`
- Used if composite index not available

### Expected EXPLAIN Output
```
+----+-------------+-------+-------+---------------------------+-----------------------+
| id | select_type | table | type  | possible_keys             | key                   |
+----+-------------+-------+-------+---------------------------+-----------------------+
|  1 | SIMPLE      | p     | range | idx_products_category,    | idx_products_category |
|    |             |       |       | idx_products_active       |                       |
+----+-------------+-------+-------+---------------------------+-----------------------+

key_len: Varies by category enum size
rows: ~50-200 (depends on category distribution)
Extra: Using where; Using filesort (acceptable for ORDER BY expression)
```

### Performance Criteria
- ✅ **PASS:** Uses `idx_products_category` or `idx_products_active_sold_view`
- ✅ **PASS:** `type` is `range` or `ref` (NOT `ALL`)
- ⚠️ **ACCEPTABLE:** `Extra` shows `Using filesort` (complex ORDER BY expression)
- ❌ **FAIL:** Full table scan (`type: ALL`)

---

## 2. Brand Candidates Query

### Query
```sql
EXPLAIN SELECT p.* FROM products p
WHERE p.active = 1
  AND LOWER(p.brand) IN ('cosrx', 'axis-y', 'some by mi')
  AND (1 = 1 OR p.id NOT IN (123, 456, 789))
ORDER BY (p.sold_count * 3 + p.view_count) DESC, p.created_at DESC
LIMIT 100;
```

### Expected Index Usage
**Primary Index:** `idx_products_brand`
- Type: BTREE index on `brand` column
- **Limitation:** `LOWER(brand)` may prevent index usage in some MySQL versions

**Alternative Index:** `idx_products_active`
- Used for: `WHERE p.active = 1`

### Expected EXPLAIN Output
```
+----+-------------+-------+------+---------------------------+---------------------+
| id | select_type | table | type | possible_keys             | key                 |
+----+-------------+-------+------+---------------------------+---------------------+
|  1 | SIMPLE      | p     | ref  | idx_products_brand,       | idx_products_brand  |
|    |             |       |      | idx_products_active       |                     |
+----+-------------+-------+------+---------------------------+---------------------+

OR (if LOWER() prevents index usage):

+----+-------------+-------+------+---------------------------+---------------------+
|  1 | SIMPLE      | p     | ALL  | idx_products_brand,       | NULL                |
|    |             |       |      | idx_products_active       |                     |
+----+-------------+-------+------+---------------------------+---------------------+

Extra: Using where; Using filesort
```

### Performance Criteria
- ✅ **IDEAL:** Uses `idx_products_brand` (if database supports functional index on LOWER)
- ⚠️ **ACCEPTABLE:** Table scan if brand list is small (2-4 brands) and product count < 10k
- ❌ **FAIL:** Table scan with large product catalog (> 50k products)

### Optimization Options (if slow)
1. **Add functional index (MySQL 8.0+):**
   ```sql
   CREATE INDEX idx_products_brand_lower ON products ((LOWER(brand)));
   ```

2. **Store normalized brand column:**
   ```sql
   ALTER TABLE products ADD COLUMN brand_normalized VARCHAR(255);
   UPDATE products SET brand_normalized = LOWER(TRIM(brand));
   CREATE INDEX idx_products_brand_normalized ON products (brand_normalized);
   ```

3. **Accept current performance:**
   - Brand queries are cached at service layer
   - Typically only 2-4 brands queried per user
   - Result set is limited to 100 products

---

## 3. Discounted Products Query

### Query
```sql
EXPLAIN SELECT p.* FROM products p
WHERE p.active = 1
  AND p.discount_price IS NOT NULL
  AND p.discount_price < p.price
  AND (1 = 1 OR p.id NOT IN (123, 456, 789))
ORDER BY ((p.price - p.discount_price) / p.price) DESC, (p.sold_count * 3 + p.view_count) DESC
LIMIT 80;
```

### Expected Index Usage
**Primary Index:** `idx_products_active`
- Type: BTREE index on `active` column
- Used for: `WHERE p.active = 1`

**Note:** No index on `discount_price` (intentional - minority of products have discounts)

### Expected EXPLAIN Output
```
+----+-------------+-------+------+---------------------------+---------------------+
| id | select_type | table | type | possible_keys             | key                 |
+----+-------------+-------+------+---------------------------+---------------------+
|  1 | SIMPLE      | p     | ref  | idx_products_active       | idx_products_active |
+----+-------------+-------+------+---------------------------+---------------------+

rows: ~1000-5000 (all active products, then filtered)
Extra: Using where; Using filesort
```

### Performance Criteria
- ✅ **PASS:** Uses `idx_products_active`
- ⚠️ **ACCEPTABLE:** `Using where` (filters discount_price after index)
- ⚠️ **ACCEPTABLE:** `Using filesort` (complex ORDER BY expression)
- ❌ **FAIL:** Full table scan without using any index

### Optimization Options (if slow)
1. **Add discount index (if many discounted products):**
   ```sql
   CREATE INDEX idx_products_discount ON products (active, discount_price);
   ```

2. **Add computed discount percentage column:**
   ```sql
   ALTER TABLE products ADD COLUMN discount_pct DECIMAL(5,2);
   UPDATE products SET discount_pct = (price - discount_price) / price WHERE discount_price IS NOT NULL;
   CREATE INDEX idx_products_discount_pct ON products (active, discount_pct DESC);
   ```

---

## 4. Popular Products Query

### Query
```sql
EXPLAIN SELECT * FROM products p
WHERE p.active = 1
ORDER BY p.sold_count DESC
LIMIT 80;
```

### Expected Index Usage
**Optimal Index:** `idx_products_active_sold_view`
- Type: Composite index on `(active, sold_count, view_count)`
- Used for: Both WHERE and ORDER BY optimization

**Fallback Index:** `idx_products_sold_count`
- Used if composite index not available

### Expected EXPLAIN Output
```
+----+-------------+-------+------+---------------------------+-------------------------------+
| id | select_type | table | type | possible_keys             | key                           |
+----+-------------+-------+------+---------------------------+-------------------------------+
|  1 | SIMPLE      | p     | ref  | idx_products_active,      | idx_products_active_sold_view |
|    |             |       |      | idx_products_active_sold  |                               |
+----+-------------+-------+------+---------------------------+-------------------------------+

rows: 80 (LIMIT applied)
Extra: Using index condition
```

### Performance Criteria
- ✅ **IDEAL:** Uses `idx_products_active_sold_view` (composite index)
- ✅ **PASS:** Uses `idx_products_sold_count` + separate active filter
- ❌ **FAIL:** Full table scan or `Using filesort`

---

## 5. New Arrivals Query

### Query
```sql
EXPLAIN SELECT * FROM products p
WHERE p.active = 1
ORDER BY p.created_at DESC
LIMIT 80;
```

### Expected Index Usage
**Primary Index:** `idx_products_created_at`
- Type: BTREE index on `created_at` column
- Used for: ORDER BY optimization

**Secondary Index:** `idx_products_active`
- Used for: WHERE clause

### Expected EXPLAIN Output
```
+----+-------------+-------+------+---------------------------+--------------------------+
| id | select_type | table | type | possible_keys             | key                      |
+----+-------------+-------+------+---------------------------+--------------------------+
|  1 | SIMPLE      | p     | ref  | idx_products_active,      | idx_products_created_at  |
|    |             |       |      | idx_products_created_at   |                          |
+----+-------------+-------+------+---------------------------+--------------------------+

rows: 80 (LIMIT applied)
Extra: Using where (for active filter)
```

### Performance Criteria
- ✅ **IDEAL:** Uses `idx_products_created_at` for ordering
- ⚠️ **ACCEPTABLE:** `Using where` to filter active=1
- ❌ **FAIL:** `Using filesort` (indicates index not used for ORDER BY)

---

## 6. Batch Favorite Lookup Query

### Query
```sql
EXPLAIN SELECT f.product_id FROM favorites f
WHERE f.user_id = 123
  AND f.product_id IN (1, 2, 3, 4, 5, ..., 30);
```

### Expected Index Usage
**Required Index:** Composite index on `(user_id, product_id)`
- Index name: `idx_favorites_user_product` or similar
- Used for: Both WHERE conditions

### Expected EXPLAIN Output
```
+----+-------------+-------+-------+---------------------------+---------------------------+
| id | select_type | table | type  | possible_keys             | key                       |
+----+-------------+-------+-------+---------------------------+---------------------------+
|  1 | SIMPLE      | f     | range | idx_favorites_user_product| idx_favorites_user_product|
+----+-------------+-------+-------+---------------------------+---------------------------+

rows: ~0-30 (matches user's favorites in the list)
Extra: Using where; Using index (covering index - best case)
```

### Performance Criteria
- ✅ **IDEAL:** `Using index` (covering index, no table access needed)
- ✅ **PASS:** Uses composite index on `(user_id, product_id)`
- ❌ **FAIL:** Full table scan on favorites table

### Required Index
```sql
CREATE INDEX idx_favorites_user_product ON favorites (user_id, product_id);
```

---

## 7. Batch Product Images Query

### Query
```sql
EXPLAIN SELECT pi.product_id, pi.image_url, pi.main
FROM product_images pi
WHERE pi.product_id IN (1, 2, 3, 4, 5, ..., 30)
ORDER BY pi.main DESC, pi.id ASC;
```

### Expected Index Usage
**Required Index:** Index on `product_id`
- Index name: `idx_product_images_product_id` or foreign key index
- Used for: WHERE IN clause

### Expected EXPLAIN Output
```
+----+-------------+-------+-------+---------------------------+--------------------------------+
| id | select_type | table | type  | possible_keys             | key                            |
+----+-------------+-------+-------+---------------------------+--------------------------------+
|  1 | SIMPLE      | pi    | range | idx_product_images_product| idx_product_images_product_id  |
+----+-------------+-------+-------+---------------------------+--------------------------------+

rows: ~30-150 (depends on images per product)
Extra: Using where; Using filesort (acceptable for small result set)
```

### Performance Criteria
- ✅ **PASS:** Uses index on `product_id`
- ⚠️ **ACCEPTABLE:** `Using filesort` (ORDER BY on small result set)
- ❌ **FAIL:** Full table scan on product_images

### Required Index
```sql
CREATE INDEX idx_product_images_product_id ON product_images (product_id);
```

---

## Running EXPLAIN Analysis

### 1. Connect to Database
```bash
mysql -u ${DB_USER} -p${DB_PASSWORD} -h ${DB_HOST} ${DB_NAME}
```

### 2. Enable Extended EXPLAIN
```sql
-- Show detailed query plan with costs
EXPLAIN FORMAT=JSON SELECT ...;

-- Show actual execution stats (requires running query)
EXPLAIN ANALYZE SELECT ...;
```

### 3. Check Index Usage
```sql
-- List all indexes on products table
SHOW INDEX FROM products;

-- Check index cardinality (higher is better)
SHOW INDEX FROM products WHERE Key_name LIKE 'idx_%';
```

### 4. Analyze Table Statistics
```sql
-- Update table statistics for better query planning
ANALYZE TABLE products;
ANALYZE TABLE favorites;
ANALYZE TABLE product_images;
```

---

## Performance Benchmarks

### Expected Query Times (Local Development)
- Category candidates: < 20ms
- Brand candidates: < 30ms
- Discounted products: < 50ms
- Popular products: < 10ms
- New arrivals: < 10ms
- Favorite lookup: < 5ms
- Image lookup: < 10ms

**Total candidate loading:** < 150ms

### Expected Query Times (Production)
- Category candidates: < 50ms
- Brand candidates: < 80ms
- Discounted products: < 100ms
- Popular products: < 30ms
- New arrivals: < 30ms
- Favorite lookup: < 20ms
- Image lookup: < 30ms

**Total candidate loading:** < 300ms

---

## Troubleshooting Slow Queries

### If candidatesMs > 200ms

1. **Check index usage:**
   ```sql
   EXPLAIN SELECT ...;
   ```
   Look for: `type: ALL` (full table scan)

2. **Check table size:**
   ```sql
   SELECT COUNT(*) FROM products WHERE active = 1;
   ```
   If > 100k products, indexes are critical

3. **Check query cache (MySQL 5.x):**
   ```sql
   SHOW VARIABLES LIKE 'query_cache%';
   ```

4. **Check connection pool:**
   - HikariCP metrics in Spring Boot
   - Look for connection wait times

### If toCardsMs > 100ms

1. **Check for N+1 queries:**
   - Enable SQL logging: `spring.jpa.show-sql=true`
   - Look for repeated favorite/image queries

2. **Verify batch query is used:**
   - Should see 2 total queries:
     ```sql
     SELECT ... FROM favorites WHERE user_id = ? AND product_id IN (?, ?, ...)
     SELECT ... FROM product_images WHERE product_id IN (?, ?, ...)
     ```

3. **Check result set size:**
   - If loading > 30 products, check why limit is not working

---

## Monitoring Query Performance

### Enable Slow Query Log (Production)
```sql
-- MySQL configuration
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.1;  -- Log queries > 100ms
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow-query.log';
```

### Application Metrics
Monitor these in Prometheus/Grafana:
- `home_feed_candidates_ms` - Candidate loading time
- `home_feed_to_cards_ms` - Card conversion time
- `home_feed_total_ms` - Total feed generation time
- `home_feed_candidate_count` - Number of candidates loaded

### Alert Thresholds
- **Warning:** `home_feed_total_ms` p95 > 500ms
- **Critical:** `home_feed_total_ms` p95 > 1000ms
- **Alert:** `home_feed_candidate_count` > 350 (indicates limit bug)

---

## Index Maintenance

### Required Indexes (Must Exist)

#### products table
```sql
CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_brand ON products (brand);
CREATE INDEX idx_products_sold_count ON products (sold_count);
CREATE INDEX idx_products_created_at ON products (created_at);
CREATE INDEX idx_products_active ON products (active);
CREATE INDEX idx_products_active_sold_view ON products (active, sold_count, view_count);
```

#### favorites table
```sql
CREATE INDEX idx_favorites_user_product ON favorites (user_id, product_id);
```

#### product_images table
```sql
CREATE INDEX idx_product_images_product_id ON product_images (product_id);
CREATE INDEX idx_product_images_main ON product_images (product_id, main);
```

### Verify Indexes Exist
```sql
SELECT
  TABLE_NAME,
  INDEX_NAME,
  GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
  INDEX_TYPE,
  CARDINALITY
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('products', 'favorites', 'product_images')
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, CARDINALITY
ORDER BY TABLE_NAME, INDEX_NAME;
```

---

## Summary

### Critical Success Factors
1. ✅ All candidate queries use indexes (no full table scans)
2. ✅ Favorite/image lookups use composite/foreign key indexes
3. ✅ Query execution time < 100ms per query
4. ✅ Total candidate loading < 200ms (local) / 500ms (prod)

### Acceptable Trade-offs
- `Using filesort` for complex ORDER BY expressions
- `Using where` for additional filter conditions after index
- Table scan on brand query if product count < 10k

### Red Flags
- ❌ Full table scan (`type: ALL`) on products table with > 10k rows
- ❌ N+1 queries in toCardsPublic (should be exactly 2 queries)
- ❌ `candidatesMs` > 500ms consistently
- ❌ Missing indexes on favorites or product_images

---

**Last Reviewed:** 2026-04-25
**Next Review:** After significant data growth or schema changes
