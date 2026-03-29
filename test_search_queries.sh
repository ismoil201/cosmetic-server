#!/bin/bash

# ============================================
# PROFESSIONAL MARKETPLACE SEARCH TEST SUITE
# ============================================

BASE_URL="http://localhost:8080"
API_ENDPOINT="/api/products/search"

echo "🔍 Testing Professional Marketplace Search..."
echo "============================================"

# Test 1: Exact name match (should rank #1)
echo ""
echo "TEST 1: Exact match - 'collagen serum'"
curl -s "${BASE_URL}${API_ENDPOINT}?q=collagen%20serum&page=0&size=5" | jq '.content[0].name'

# Test 2: Prefix match
echo ""
echo "TEST 2: Prefix match - 'collagen'"
curl -s "${BASE_URL}${API_ENDPOINT}?q=collagen&page=0&size=5" | jq '.content[] | {name, stock}'

# Test 3: Multi-language (Uzbek/Russian typo)
echo ""
echo "TEST 3: Synonym/transliteration - 'kallagen' (should find 'collagen')"
curl -s "${BASE_URL}${API_ENDPOINT}?q=kallagen&page=0&size=5" | jq '.content[0].name'

# Test 4: Russian search
echo ""
echo "TEST 4: Russian keyword - 'крем' (cream)"
curl -s "${BASE_URL}${API_ENDPOINT}?q=%D0%BA%D1%80%D0%B5%D0%BC&page=0&size=5" | jq '.content[] | .name'

# Test 5: Pagination test
echo ""
echo "TEST 5: Pagination - page 0 vs page 1"
echo "Page 0:"
curl -s "${BASE_URL}${API_ENDPOINT}?q=serum&page=0&size=3" | jq '.content[] | .name'
echo "Page 1:"
curl -s "${BASE_URL}${API_ENDPOINT}?q=serum&page=1&size=3" | jq '.content[] | .name'

# Test 6: Empty query (should return empty)
echo ""
echo "TEST 6: Empty query (should return no results)"
curl -s "${BASE_URL}${API_ENDPOINT}?q=&page=0&size=5" | jq '.totalElements'

# Test 7: Performance test (check response time)
echo ""
echo "TEST 7: Performance test - measuring response time"
time curl -s "${BASE_URL}${API_ENDPOINT}?q=cream&page=0&size=20" > /dev/null

echo ""
echo "============================================"
echo "✅ Search tests completed!"
echo "Review results above to verify:"
echo "  - Exact matches rank first"
echo "  - In-stock products prioritized"
echo "  - Pagination works correctly"
echo "  - Multi-language support active"
echo "  - Response time < 500ms"
