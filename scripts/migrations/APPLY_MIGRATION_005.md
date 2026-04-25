# Migration 005: Fix Schema Validation Errors

## Problem

Application fails to start with `ddl-auto=validate` due to missing columns:

```
Schema-validation: missing column [created_at] in table [user_interest]
Schema-validation: missing column [version] in table [product_variants]
```

## Root Cause

- **UserInterest entity** (`UserInterest.java`) expects: `created_at`, `updated_at`, `last_event_type`
- **ProductVariant entity** (`ProductVariant.java`) expects: `version` (for optimistic locking)
- Database tables are missing these columns

## Solution

Apply migration `005_fix_schema_validation_errors.sql` to add missing columns.

## Prerequisites

1. Database connection details from environment variables:
   - `DB_URL` (e.g., `jdbc:mysql://localhost:3306/cosmetic_db`)
   - `DB_USER`
   - `DB_PASSWORD`

2. MySQL client installed

## Steps to Apply Migration

### Option 1: Using MySQL Command Line (Recommended)

```bash
# Extract database name from DB_URL
# Example: jdbc:mysql://localhost:3306/cosmetic_db → database = cosmetic_db

# Connect to MySQL
mysql -u ${DB_USER} -p${DB_PASSWORD} -h localhost cosmetic_db

# Run migration
source scripts/migrations/005_fix_schema_validation_errors.sql

# Exit MySQL
exit;
```

### Option 2: Using mysql client with file input

```bash
mysql -u ${DB_USER} -p${DB_PASSWORD} -h localhost cosmetic_db < scripts/migrations/005_fix_schema_validation_errors.sql
```

### Option 3: Using IntelliJ Database Console

1. Open Database tool window (View → Tool Windows → Database)
2. Connect to your MySQL database
3. Open `scripts/migrations/005_fix_schema_validation_errors.sql`
4. Select all statements
5. Execute (Ctrl+Enter / Cmd+Enter)
6. Verify STEP 5 output shows all columns exist

## Quick Apply Script (Development Only)

For local development, use the helper script:

```bash
# Make script executable
chmod +x scripts/migrations/apply_005.sh

# Run migration
./scripts/migrations/apply_005.sh
```

## Verification Steps

### 1. Check user_interest table

```sql
DESCRIBE user_interest;
```

Expected columns:
- `id` (bigint, PK)
- `user_id` (bigint)
- `type` (varchar)
- `key_name` (varchar)
- `score` (double)
- `last_event_type` (varchar) ← NEW
- `created_at` (datetime) ← NEW
- `updated_at` (datetime) ← NEW

### 2. Check product_variants table

```sql
DESCRIBE product_variants;
```

Expected columns:
- `id` (bigint, PK)
- `product_id` (bigint)
- `label` (varchar)
- `price` (decimal)
- `discount_price` (decimal)
- `stock` (int)
- `active` (tinyint)
- `sort_order` (int)
- `version` (bigint) ← NEW

### 3. Verify indexes

```sql
SHOW INDEX FROM user_interest;
```

Expected indexes:
- `PRIMARY` (id)
- `UK_*` (user_id, type, key_name) - unique constraint
- `idx_user_interest_user_score` (user_id, score)
- `idx_user_interest_user_type_score` (user_id, type, score)

### 4. Start application

```bash
./gradlew bootRun
```

Expected: Application starts successfully without schema validation errors.

## Rollback (If Needed)

```sql
-- Remove added columns (CAUTION: Data loss)
ALTER TABLE user_interest DROP COLUMN last_event_type;
ALTER TABLE user_interest DROP COLUMN created_at;
ALTER TABLE user_interest DROP COLUMN updated_at;
ALTER TABLE product_variants DROP COLUMN version;

-- Remove indexes
DROP INDEX IF EXISTS idx_user_interest_user_score ON user_interest;
DROP INDEX IF EXISTS idx_user_interest_user_type_score ON user_interest;
```

⚠️ **WARNING**: Rollback will delete `created_at`, `updated_at`, `last_event_type` data. Only use if absolutely necessary.

## Production Safety

- ✅ Backup database before migration
- ✅ Test on staging environment first
- ✅ Run during maintenance window
- ✅ Monitor application after deployment
- ✅ Keep this rollback script ready

## Migration Details

| Table | Column | Type | Default | Purpose |
|-------|--------|------|---------|---------|
| user_interest | created_at | DATETIME | CURRENT_TIMESTAMP | Track when interest was created |
| user_interest | updated_at | DATETIME | CURRENT_TIMESTAMP | Track last update time |
| user_interest | last_event_type | VARCHAR(32) | NULL | Track event that updated interest |
| product_variants | version | BIGINT | 0 | Optimistic locking for stock updates |

## Support

If migration fails:

1. Check error message
2. Verify database connection
3. Ensure user has ALTER TABLE privileges
4. Check if columns already exist: `DESCRIBE user_interest;`
5. Review migration script: `scripts/migrations/005_fix_schema_validation_errors.sql`

## Next Steps After Migration

1. Restart Spring Boot application
2. Verify startup logs show no schema validation errors
3. Run tests: `./gradlew test`
4. Check application health: `curl http://localhost:8080/actuator/health`
