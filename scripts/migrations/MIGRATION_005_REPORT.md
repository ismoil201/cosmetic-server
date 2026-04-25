# Migration 005 Report: Schema Validation Fix

## Executive Summary

**Status**: ✅ Migration Created
**Date**: 2026-04-25
**Impact**: Fixes application startup crash with `ddl-auto=validate`
**Risk Level**: LOW (Safe, backward-compatible schema additions)

## Root Cause Analysis

### Problem Statement

Application fails to start with the following errors:

```
Schema-validation: missing column [created_at] in table [user_interest]
Schema-validation: missing column [version] in table [product_variants]
```

### Root Cause

**Entity-Database Mismatch**: JPA entity classes were updated in STEP 8 to include new columns, but database schema was not migrated.

| Entity | Expected Columns | Database State | Result |
|--------|-----------------|----------------|---------|
| UserInterest.java | created_at, updated_at, last_event_type | Missing | ❌ Validation fails |
| ProductVariant.java | version (optimistic locking) | Missing | ❌ Validation fails |

### Why It Happened

1. **STEP 8 Implementation**: Added user interest tracking with temporal columns
2. **Migration Gap**: Migration script `004_add_user_interest_columns_and_indexes.sql` was created but not applied
3. **Optimistic Locking**: ProductVariant was enhanced with `@Version` for race condition prevention
4. **Validation Mode**: `ddl-auto=validate` strictly enforces entity-schema match

## Technical Details

### Affected Tables

#### 1. user_interest

**Exact Table Name**: `user_interest` (NOT `user_interests`)
**Entity**: `com.example.backend.domain.recommendation.entity.UserInterest`
**Mapped By**: `@Table(name="user_interest")`

**Missing Columns**:

| Column | Type | Nullable | Default | Purpose |
|--------|------|----------|---------|---------|
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | Track interest creation time |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | Track last modification time |
| last_event_type | VARCHAR(32) | NULL | NULL | Track event that updated interest |

**Existing Columns** (already in database):
- id (bigint, PK)
- user_id (bigint, FK)
- type (varchar) - InterestType enum (CATEGORY/BRAND/QUERY)
- key_name (varchar) - Interest key
- score (double) - Interest strength score

**Required Indexes**:
- `idx_user_interest_user_score` (user_id, score DESC)
- `idx_user_interest_user_type_score` (user_id, type, score DESC)

#### 2. product_variants

**Exact Table Name**: `product_variants`
**Entity**: `com.example.backend.domain.product.entity.ProductVariant`
**Mapped By**: `@Table(name="product_variants")`

**Missing Columns**:

| Column | Type | Nullable | Default | Purpose |
|--------|------|----------|---------|---------|
| version | BIGINT | NOT NULL | 0 | Optimistic locking version counter |

**Purpose of version column**:
```java
@Version
private Long version;
```

This prevents race conditions in concurrent stock updates:
1. User A reads variant (version=5)
2. User B reads variant (version=5)
3. User A updates stock, version becomes 6
4. User B tries to update with version=5 → **OptimisticLockException**
5. User B retries with latest version

## Solution

### Migration Strategy

**Approach**: Safe, idempotent schema additions using `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`

**Migration File**: `scripts/migrations/005_fix_schema_validation_errors.sql`

**Key Features**:
- ✅ Idempotent (can run multiple times safely)
- ✅ Non-blocking (adds columns, doesn't lock table)
- ✅ Backward compatible (existing data preserved)
- ✅ Default values prevent NULL violations
- ✅ Indexes added conditionally

### SQL Statements

```sql
-- user_interest: Add temporal columns
ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS last_event_type VARCHAR(32) NULL;

ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE user_interest
ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
ON UPDATE CURRENT_TIMESTAMP;

-- user_interest: Add performance indexes
CREATE INDEX IF NOT EXISTS idx_user_interest_user_score
ON user_interest(user_id, score DESC);

CREATE INDEX IF NOT EXISTS idx_user_interest_user_type_score
ON user_interest(user_id, type, score DESC);

-- product_variants: Add optimistic locking
ALTER TABLE product_variants
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
```

### Safety Measures

1. **IF NOT EXISTS**: Migration is idempotent
2. **DEFAULT VALUES**: Prevent NULL constraint violations
3. **No DROP/TRUNCATE**: Existing data preserved
4. **No Table Locks**: Non-blocking schema changes
5. **Rollback Plan**: Simple DROP COLUMN statements available

## Verification Steps

### Pre-Migration Checks

```sql
-- Check current schema
DESCRIBE user_interest;
DESCRIBE product_variants;

-- Backup data (optional but recommended)
CREATE TABLE user_interest_backup AS SELECT * FROM user_interest;
CREATE TABLE product_variants_backup AS SELECT * FROM product_variants;
```

### Post-Migration Verification

```sql
-- Verify columns exist
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_interest'
  AND COLUMN_NAME IN ('created_at', 'updated_at', 'last_event_type');

SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'product_variants'
  AND COLUMN_NAME = 'version';

-- Verify indexes
SHOW INDEX FROM user_interest;

-- Verify no NULL values
SELECT COUNT(*) as total,
       SUM(CASE WHEN created_at IS NULL THEN 1 ELSE 0 END) as null_created,
       SUM(CASE WHEN updated_at IS NULL THEN 1 ELSE 0 END) as null_updated
FROM user_interest;
```

### Application Startup Test

```bash
# Start application with validation mode
./gradlew bootRun

# Expected: No schema validation errors
# Success indicator: "Started CosmeticApplication in X seconds"
```

## Application Steps

### Method 1: Using Helper Script (Recommended for Development)

```bash
# Make script executable (one-time)
chmod +x scripts/migrations/apply_005.sh

# Set environment variables
export DB_URL='jdbc:mysql://localhost:3306/cosmetic_db'
export DB_USER='your_user'
export DB_PASSWORD='your_password'

# Apply migration
./scripts/migrations/apply_005.sh
```

### Method 2: Manual MySQL Execution (Production)

```bash
# Connect to MySQL
mysql -u your_user -p your_database

# Run migration
source scripts/migrations/005_fix_schema_validation_errors.sql

# Verify
DESCRIBE user_interest;
DESCRIBE product_variants;
exit;
```

### Method 3: IntelliJ Database Console

1. Open Database tool window
2. Connect to MySQL database
3. Open `005_fix_schema_validation_errors.sql`
4. Execute all statements
5. Review output for errors

## Rollback Plan

**Risk Assessment**: LOW (columns are additive, not destructive)

**If Rollback Needed**:

```sql
-- Remove added columns (⚠️ DATA LOSS)
ALTER TABLE user_interest DROP COLUMN last_event_type;
ALTER TABLE user_interest DROP COLUMN created_at;
ALTER TABLE user_interest DROP COLUMN updated_at;
ALTER TABLE product_variants DROP COLUMN version;

-- Remove indexes
DROP INDEX IF EXISTS idx_user_interest_user_score ON user_interest;
DROP INDEX IF EXISTS idx_user_interest_user_type_score ON user_interest;

-- Restore from backup if needed
INSERT INTO user_interest SELECT * FROM user_interest_backup;
INSERT INTO product_variants SELECT * FROM product_variants_backup;
```

**When to Rollback**:
- Migration causes application errors (unlikely)
- Need to revert entity changes
- Database performance issues (unlikely with these simple indexes)

## Production Deployment Checklist

- [ ] **Backup database** before migration
- [ ] **Test on staging** environment first
- [ ] **Schedule maintenance window** (low-traffic period)
- [ ] **Notify team** about deployment
- [ ] **Monitor application logs** during startup
- [ ] **Verify health endpoint**: `curl http://localhost:8080/actuator/health`
- [ ] **Run smoke tests** after deployment
- [ ] **Keep rollback script** ready
- [ ] **Document deployment** (date, time, result)

## Performance Impact

**Expected Impact**: MINIMAL

| Operation | Impact | Duration |
|-----------|--------|----------|
| ALTER TABLE user_interest | LOW | < 1 second (small table) |
| ALTER TABLE product_variants | LOW | < 1 second (small table) |
| CREATE INDEX (2 indexes) | LOW | < 2 seconds |
| Total Downtime | NONE | Non-blocking DDL |

**Index Benefits**:
- `idx_user_interest_user_score`: Speeds up user interest lookups (home feed personalization)
- `idx_user_interest_user_type_score`: Speeds up type-specific interest queries

## Related Changes

### Previous Migrations

- `001_backfill_event_category_brand.sql` - Event tracking enrichment
- `002_add_event_logs_composite_indexes.sql` - Event log performance
- `003_backfill_user_interests_from_event_logs.sql` - User interest initial data
- `004_add_user_interest_columns_and_indexes.sql` - **Should have been applied** (duplicate of this migration)

### Code References

- UserInterest entity: `src/main/java/com/example/backend/domain/recommendation/entity/UserInterest.java:39-43`
- ProductVariant entity: `src/main/java/com/example/backend/domain/product/entity/ProductVariant.java:44-45`
- Application config: `src/main/resources/application.yml:21` (ddl-auto=validate)

## Expected Results

### Before Migration

```
❌ Application Startup: FAILED
Error: Schema-validation: missing column [created_at] in table [user_interest]
Error: Schema-validation: missing column [version] in table [product_variants]
```

### After Migration

```
✅ Application Startup: SUCCESS
✅ user_interest table: Has created_at, updated_at, last_event_type columns
✅ product_variants table: Has version column
✅ Indexes: idx_user_interest_user_score, idx_user_interest_user_type_score exist
✅ Data integrity: All existing data preserved
✅ Performance: Improved query performance for user interests
✅ Optimistic locking: Prevents stock race conditions
```

## Additional Notes

### Table Name Clarification

**Correct table name**: `user_interest` (singular)
**Common mistake**: `user_interests` (plural)

The entity uses `@Table(name="user_interest")`, so all migrations must target `user_interest`.

### Migration 004 vs 005

Migration 004 (`004_add_user_interest_columns_and_indexes.sql`) was created during STEP 8 but may not have been applied. Migration 005 is essentially the same but includes:

1. ProductVariant.version column (additional fix)
2. More comprehensive verification steps
3. Helper scripts for easy application
4. Detailed documentation

If migration 004 was already applied, migration 005 will skip already-existing columns (thanks to `IF NOT EXISTS`).

### Future Migrations

**Best Practice**: Use proper migration tool (Flyway/Liquibase) to avoid manual tracking.

**For now**: Document each migration in this directory with:
- Sequential numbering (001, 002, 003...)
- Clear purpose and impact description
- Idempotent SQL (IF NOT EXISTS)
- Verification queries

## Support

**If migration fails**:

1. Check MySQL user privileges: `SHOW GRANTS FOR CURRENT_USER;`
2. Verify table exists: `SHOW TABLES LIKE 'user_interest';`
3. Check for existing columns: `DESCRIBE user_interest;`
4. Review MySQL error log
5. Try running individual ALTER statements manually

**If application still fails to start**:

1. Check application logs for schema validation errors
2. Verify all columns exist: Run verification queries above
3. Ensure `ddl-auto=validate` in `application.yml`
4. Check for additional missing columns in error message
5. Create migration for any new missing columns

## Conclusion

**Migration Status**: ✅ Ready to Apply
**Risk Level**: LOW
**Recommended Approach**: Apply migration 005 using helper script or manual MySQL execution
**Expected Outcome**: Application starts successfully with `ddl-auto=validate`

**Next Steps**:
1. Apply migration (choose method above)
2. Verify columns exist
3. Start application
4. Run tests: `./gradlew test`
5. Monitor application health
