#!/bin/bash

# =====================================================
# Quick Migration Script for Development
# Applies migration 005 to fix schema validation errors
# =====================================================

set -e  # Exit on error

echo "🔧 Migration 005: Fix Schema Validation Errors"
echo "=============================================="
echo ""

# Check if .env file exists (optional)
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env..."
    export $(grep -v '^#' .env | xargs)
fi

# Verify required environment variables
if [ -z "$DB_URL" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: Missing database credentials"
    echo ""
    echo "Required environment variables:"
    echo "  - DB_URL (e.g., jdbc:mysql://localhost:3306/cosmetic_db)"
    echo "  - DB_USER"
    echo "  - DB_PASSWORD"
    echo ""
    echo "Set them in .env file or export them:"
    echo "  export DB_URL='jdbc:mysql://localhost:3306/cosmetic_db'"
    echo "  export DB_USER='your_user'"
    echo "  export DB_PASSWORD='your_password'"
    exit 1
fi

# Extract database connection details from JDBC URL
# Example: jdbc:mysql://localhost:3306/cosmetic_db → host=localhost, port=3306, db=cosmetic_db
DB_HOST=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
DB_PORT=$(echo $DB_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
DB_NAME=$(echo $DB_URL | sed -n 's/.*\/\([^?]*\).*/\1/p')

if [ -z "$DB_HOST" ] || [ -z "$DB_NAME" ]; then
    echo "❌ Error: Could not parse DB_URL"
    echo "DB_URL format should be: jdbc:mysql://host:port/database"
    echo "Current DB_URL: $DB_URL"
    exit 1
fi

echo "📊 Database Connection:"
echo "  Host: $DB_HOST"
echo "  Port: ${DB_PORT:-3306}"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Confirm before proceeding
read -p "Continue with migration? (y/N) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Migration cancelled"
    exit 1
fi

# Check if migration file exists
MIGRATION_FILE="scripts/migrations/005_fix_schema_validation_errors.sql"
if [ ! -f "$MIGRATION_FILE" ]; then
    echo "❌ Error: Migration file not found: $MIGRATION_FILE"
    exit 1
fi

echo ""
echo "🚀 Applying migration..."
echo ""

# Apply migration using mysql client
mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$MIGRATION_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Migration applied successfully!"
    echo ""
    echo "📋 Verification:"
    echo ""

    # Verify columns exist
    echo "Checking user_interest columns..."
    mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e \
        "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
         FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = '$DB_NAME'
           AND TABLE_NAME = 'user_interest'
           AND COLUMN_NAME IN ('created_at', 'updated_at', 'last_event_type');"

    echo ""
    echo "Checking product_variants columns..."
    mysql -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e \
        "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
         FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = '$DB_NAME'
           AND TABLE_NAME = 'product_variants'
           AND COLUMN_NAME = 'version';"

    echo ""
    echo "✅ Migration complete!"
    echo ""
    echo "Next steps:"
    echo "  1. Start application: ./gradlew bootRun"
    echo "  2. Verify no schema validation errors in logs"
    echo "  3. Run tests: ./gradlew test"
else
    echo ""
    echo "❌ Migration failed!"
    echo "Check error messages above for details"
    exit 1
fi
