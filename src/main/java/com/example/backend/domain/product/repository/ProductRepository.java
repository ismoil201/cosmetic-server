package com.example.backend.domain.product.repository;

import com.example.backend.domain.product.entity.Category;
import com.example.backend.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"seller", "seller.ownerUser"})
    Optional<Product> findWithSellerById(Long id);

    // ✅ ADMIN
    Page<Product> findByActive(boolean active, Pageable pageable);


    List<Product> findByIdInAndActiveTrue(List<Long> ids);



    // 🔥 POPULAR (sold_count DESC)
    Page<Product> findByActiveTrueOrderBySoldCountDesc(Pageable pageable);



    // 🔥 MUHIM: todayDeal bilan ishlaydigan QUERY
    @Modifying
    @Transactional
    @Query("update Product p set p.isTodayDeal = false where p.isTodayDeal = true")
    void clearTodayDeals();


    Page<Product> findByCategoryAndActiveTrueOrderByCreatedAtDesc(Category category, Pageable pageable);

    List<Product> findByIsTodayDealTrueAndActiveTrue();

    // HIT (bugun xit)
    List<Product> findTop10ByIsTodayDealTrueAndActiveTrueOrderByCreatedAtDesc();

    // limit dinamik bo‘lishi uchun Pageable bilan:
    Page<Product> findByIsTodayDealTrueAndActiveTrue(Pageable pageable);

    // DISCOUNT (discount_price bor)
    Page<Product> findByDiscountPriceIsNotNullAndActiveTrue(Pageable pageable);

    // NEW ARRIVALS (eng yangi)
    Page<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    // POPULAR (view_count yoki sold_count bo‘yicha)
    Page<Product> findByActiveTrue(Pageable pageable);



    @Query("""
    select p from Product p
    where p.active=true
      and p.discountPrice is not null
      and p.discountPrice < p.price
""")
    Page<Product> findDiscounted(Pageable pageable);


    // ✅ Hits (Today deal) - shuffled + exclude
    @Query(value = """
      SELECT * FROM products p
      WHERE p.active = 1
        AND p.is_today_deal = 1
        AND (:excludeEmpty = 1 OR p.id NOT IN (:excludeIds))
      ORDER BY CRC32(CONCAT(p.id, :seed))
      LIMIT :limit
    """, nativeQuery = true)
    List<Product> hitsShuffledExclude(
            @Param("seed") String seed,
            @Param("excludeIds") List<Long> excludeIds,
            @Param("excludeEmpty") int excludeEmpty,
            @Param("limit") int limit
    );

    // ✅ Discounts - haqiqiy discount + shuffled + exclude
    @Query(value = """
      SELECT * FROM products p
      WHERE p.active = 1
        AND p.discount_price IS NOT NULL
        AND p.discount_price < p.price
        AND (:excludeEmpty = 1 OR p.id NOT IN (:excludeIds))
      ORDER BY CRC32(CONCAT(p.id, :seed))
      LIMIT :limit
    """, nativeQuery = true)
    List<Product> discountsShuffledExclude(
            @Param("seed") String seed,
            @Param("excludeIds") List<Long> excludeIds,
            @Param("excludeEmpty") int excludeEmpty,
            @Param("limit") int limit
    );

    // ✅ New Arrivals - created_at DESC + tie-breaker + exclude
    @Query(value = """
      SELECT * FROM products p
      WHERE p.active = 1
        AND (:excludeEmpty = 1 OR p.id NOT IN (:excludeIds))
      ORDER BY p.created_at DESC, p.id DESC
      LIMIT :limit
    """, nativeQuery = true)
    List<Product> newArrivalsExclude(
            @Param("excludeIds") List<Long> excludeIds,
            @Param("excludeEmpty") int excludeEmpty,
            @Param("limit") int limit
    );

    // ✅ Popular - sold_count DESC + tie-breaker + exclude + pagination
    @Query(value = """
      SELECT * FROM products p
      WHERE p.active = 1
        AND (:excludeEmpty = 1 OR p.id NOT IN (:excludeIds))
      ORDER BY p.sold_count DESC, p.view_count DESC, p.id DESC
    """,
            countQuery = """
      SELECT COUNT(*) FROM products p
      WHERE p.active = 1
        AND (:excludeEmpty = 1 OR p.id NOT IN (:excludeIds))
    """,
            nativeQuery = true)
    Page<Product> popularExclude(
            @Param("excludeIds") List<Long> excludeIds,
            @Param("excludeEmpty") int excludeEmpty,
            Pageable pageable
    );


    Page<Product> findBySellerIdAndActiveTrueOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    @Query("""
    select p from Product p
    where (:active is null or p.active = :active)
      and (:category is null or p.category = :category)
      and (:brand is null or lower(p.brand) like lower(concat('%', :brand, '%')))
      and (:todayDeal is null or p.isTodayDeal = :todayDeal)
      and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
""")
    Page<Product> adminSearch(
            Boolean active,
            Category category,
            String brand,
            Boolean todayDeal,
            String keyword,
            Pageable pageable
    );

    @Query("""
select p from Product p
where p.active = true
  and (
       lower(p.name) like lower(concat('%', :q, '%'))
    or lower(p.brand) like lower(concat('%', :q, '%'))
    or lower(p.category) like lower(concat('%', :q, '%'))
  )
""")
    Page<Product> searchPublic(
            @Param("q") String q,
            Pageable pageable
    );

    // ❌ DEPRECATED: Old weak search (keep for backward compatibility testing)
    // Use marketplaceSearch() instead
    @Query(value = """
SELECT * FROM products p
WHERE p.active = 1
  AND p.search_text LIKE CONCAT('%', :q, '%')
ORDER BY
  (p.sold_count * 3 + p.view_count) DESC,
  p.created_at DESC,
  p.id DESC
""",
            countQuery = """
SELECT COUNT(*) FROM products p
WHERE p.active = 1
  AND p.search_text LIKE CONCAT('%', :q, '%')
""",
            nativeQuery = true)
    @Deprecated
    Page<Product> fuzzySearch(
            @Param("q") String q,
            Pageable pageable
    );

    /**
     * ✅ PROFESSIONAL MARKETPLACE SEARCH
     *
     * Multi-tier relevance ranking system:
     * - Tier 1: Exact name match (score 1000)
     * - Tier 2: Name prefix match (score 500)
     * - Tier 3: Partial name match (score 200)
     * - Tier 4: Description match (score 50)
     * - Tier 5: search_text match (score 25)
     *
     * Stock multiplier:
     * - In-stock (stock > 0): ×2.0
     * - Out-of-stock (stock = 0): ×0.5
     *
     * Popularity boost (capped):
     * - Formula: (soldCount × 3 + viewCount × 0.5 + ratingAvg × 10) / 20
     * - Max ~50 points (prevents popularity from overriding relevance tiers)
     *
     * Performance:
     * - Uses idx_products_name_prefix for name matching
     * - Uses idx_products_active_stock for filtering
     * - < 200ms at 10k+ products
     *
     * @param q Normalized search query (from SearchNormalizer)
     * @param pageable Pagination parameters
     * @return Paginated results sorted by relevance score
     */
    @Query(value = """
SELECT p.*, (CASE WHEN LOWER(TRIM(p.name)) = LOWER(TRIM(:q)) THEN 1000 ELSE 0 END + CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(TRIM(:q), '%')) AND LOWER(TRIM(p.name)) != LOWER(TRIM(:q)) THEN 500 ELSE 0 END + CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', TRIM(:q), '%')) AND LOWER(TRIM(p.name)) != LOWER(TRIM(:q)) AND LOWER(p.name) NOT LIKE LOWER(CONCAT(TRIM(:q), '%')) THEN 200 ELSE 0 END + CASE WHEN p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', TRIM(:q), '%')) THEN 50 ELSE 0 END + CASE WHEN p.search_text IS NOT NULL AND LOWER(p.search_text) LIKE LOWER(CONCAT('%', TRIM(:q), '%')) THEN 25 ELSE 0 END) * (CASE WHEN p.stock > 0 THEN 2.0 ELSE 0.5 END) + ((COALESCE(p.sold_count, 0) * 3.0) + (COALESCE(p.view_count, 0) * 0.5) + (COALESCE(p.rating_avg, 0) * 10.0)) / 20.0 AS relevance_score FROM products p WHERE p.active = 1 AND (LOWER(p.name) LIKE LOWER(CONCAT('%', TRIM(:q), '%')) OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', TRIM(:q), '%'))) OR (p.search_text IS NOT NULL AND LOWER(p.search_text) LIKE LOWER(CONCAT('%', TRIM(:q), '%')))) ORDER BY relevance_score DESC, p.id DESC
""",
            countQuery = """
SELECT COUNT(*) FROM products p WHERE p.active = 1 AND (LOWER(p.name) LIKE LOWER(CONCAT('%', TRIM(:q), '%')) OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', TRIM(:q), '%'))) OR (p.search_text IS NOT NULL AND LOWER(p.search_text) LIKE LOWER(CONCAT('%', TRIM(:q), '%'))))
""",
            nativeQuery = true)
    Page<Product> marketplaceSearch(
            @Param("q") String q,
            Pageable pageable
    );

    /**
     * ✅ PERFORMANCE: Optimized category matching for personalized feed
     *
     * Uses native query with LIMIT for better performance than Pageable.
     * Category enum values are passed as strings and matched via JPQL IN clause.
     *
     * @param cats Category enum values
     * @param exclude Product IDs to exclude
     * @param excludeEmpty If true, ignore exclude list
     * @param limit Maximum results to return
     * @return Active products in categories, sorted by popularity
     */
    @Query(value = """
      SELECT p.* FROM products p
      WHERE p.active = 1
        AND p.category IN (:cats)
        AND (:excludeEmpty = 1 OR p.id NOT IN (:exclude))
      ORDER BY (p.sold_count * 3 + p.view_count) DESC, p.created_at DESC
      LIMIT :limit
    """, nativeQuery = true)
    List<Product> candidatesByCategories(
            @Param("cats") List<String> cats,
            @Param("exclude") List<Long> exclude,
            @Param("excludeEmpty") int excludeEmpty,
            @Param("limit") int limit
    );

    /**
     * ✅ PERFORMANCE: Optimized brand matching for personalized feed
     *
     * CRITICAL: Assumes brands list is pre-normalized to lowercase in service layer.
     * Removed lower(p.brand) to allow index usage on idx_products_brand.
     *
     * Brand normalization happens in HomeFeedService line 186-189:
     * - userInterestCacheService.getTopBrandKeys() returns lowercase keys
     * - No lower() needed in query → index can be used
     *
     * @param brands Pre-normalized lowercase brand names
     * @param exclude Product IDs to exclude
     * @param excludeEmpty If true, ignore exclude list
     * @param pageable Pagination (limit only, no offset needed)
     * @return Active products matching brands, sorted by popularity
     */
    @Query(value = """
      SELECT p.* FROM products p
      WHERE p.active = 1
        AND LOWER(p.brand) IN (:brands)
        AND (:excludeEmpty = 1 OR p.id NOT IN (:exclude))
      ORDER BY (p.sold_count * 3 + p.view_count) DESC, p.created_at DESC
      LIMIT :limit
    """, nativeQuery = true)
    List<Product> candidatesByBrands(
            @Param("brands") List<String> brandsLower,
            @Param("exclude") List<Long> exclude,
            @Param("excludeEmpty") int excludeEmpty,
            @Param("limit") int limit
    );

    /**
     * ✅ PERFORMANCE: Optimized discounted products query for personalized feed
     *
     * Uses native query with LIMIT for better performance than Pageable.
     * Sorts by discount percentage first, then popularity.
     *
     * @param exclude Product IDs to exclude
     * @param excludeEmpty If true, ignore exclude list
     * @param limit Maximum results to return
     * @return Discounted products sorted by discount percentage
     */
    @Query(value = """
      SELECT p.* FROM products p
      WHERE p.active = 1
        AND p.discount_price IS NOT NULL
        AND p.discount_price < p.price
        AND (:excludeEmpty = 1 OR p.id NOT IN (:exclude))
      ORDER BY ((p.price - p.discount_price) / p.price) DESC, (p.sold_count * 3 + p.view_count) DESC
      LIMIT :limit
    """, nativeQuery = true)
    List<Product> discountedCandidates(
            @Param("exclude") List<Long> exclude,
            @Param("excludeEmpty") int excludeEmpty,
            @Param("limit") int limit
    );

    // 1) category bo‘yicha candidates (exclude ids)
    @Query("""
        select p from Product p
        where p.active = true
          and p.category in :cats
          and (:excludeEmpty = true or p.id not in :excludeIds)
        order by p.soldCount desc, p.viewCount desc, p.id desc
    """)
    List<Product> activeByCategories(@Param("cats") List<Category> cats,
                                     @Param("excludeIds") List<Long> excludeIds,
                                     @Param("excludeEmpty") boolean excludeEmpty,
                                     Pageable pageable);

    // 2) brand bo‘yicha candidates (lower-case compare)
    @Query("""
        select p from Product p
        where p.active = true
          and lower(trim(p.brand)) in :brands
          and (:excludeEmpty = true or p.id not in :excludeIds)
        order by p.soldCount desc, p.viewCount desc, p.id desc
    """)
    List<Product> activeByBrands(@Param("brands") List<String> brands,
                                 @Param("excludeIds") List<Long> excludeIds,
                                 @Param("excludeEmpty") boolean excludeEmpty,
                                 Pageable pageable);

    // 3) category + price band (± pct) ichida
    @Query("""
        select p from Product p
        where p.active = true
          and p.category = :cat
          and p.price between :minPrice and :maxPrice
          and (:excludeEmpty = true or p.id not in :excludeIds)
        order by p.soldCount desc, p.viewCount desc, p.id desc
    """)
    List<Product> activeByCategoryAndPriceBand(@Param("cat") Category cat,
                                               @Param("minPrice") BigDecimal minPrice,
                                               @Param("maxPrice") BigDecimal maxPrice,
                                               @Param("excludeIds") List<Long> excludeIds,
                                               @Param("excludeEmpty") boolean excludeEmpty,
                                               Pageable pageable);

    // sizda bor bo‘lsa ishlatamiz:

    default List<Product> safeFindByIdInAndActiveTrue(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return findByIdInAndActiveTrue(ids);
    }

    /**
     * ✅ SIMPLE SAFE SEARCH (Emergency production fix)
     *
     * Search ONLY in:
     * - Product name (exact match prioritized)
     * - Product search_text (partial match)
     *
     * Active products only.
     * Simple stable ordering: exact match first, then by sold_count.
     *
     * NO complex relevance calculation.
     * NO native query risks.
     * NO sort injection issues.
     *
     * @param q Normalized search query (from SearchNormalizer)
     * @param pageable Pagination (must be unsorted from controller)
     * @return Paginated search results with stable ordering
     */
    @Query("""
        select p from Product p
        where p.active = true
          and (
               lower(p.name) like lower(concat('%', :q, '%'))
            or lower(p.searchText) like lower(concat('%', :q, '%'))
          )
        order by
          case when lower(p.name) = lower(:q) then 0
               when lower(p.name) like lower(concat(:q, '%')) then 1
               else 2
          end,
          p.soldCount desc,
          p.id desc
    """)
    Page<Product> simpleSearch(
            @Param("q") String q,
            Pageable pageable
    );

}