package com.example.backend.repository;

import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


public interface ProductRepository extends JpaRepository<Product, Long> {

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

    @Query(value = """
SELECT * FROM products p
WHERE p.active = 1
  AND (
       p.search_text LIKE CONCAT('%', :q, '%')
    OR SOUNDEX(p.search_text) = SOUNDEX(:q)
  )
ORDER BY
  (p.sold_count * 3 + p.view_count) DESC,
  p.created_at DESC
""",
            countQuery = """
SELECT COUNT(*) FROM products p
WHERE p.active = 1
  AND (
       p.search_text LIKE CONCAT('%', :q, '%')
    OR SOUNDEX(p.search_text) = SOUNDEX(:q)
  )
""",
            nativeQuery = true)
    Page<Product> fuzzySearch(
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
      select p from Product p
      where p.active = true
        and p.category in :cats
        and (:excludeEmpty = true or p.id not in :exclude)
      order by (p.soldCount * 3 + p.viewCount) desc, p.createdAt desc
    """)
    List<Product> candidatesByCategories(
            @Param("cats") List<Category> cats,
            @Param("exclude") List<Long> exclude,
            @Param("excludeEmpty") boolean excludeEmpty,
            Pageable pageable
    );

    @Query("""
      select p from Product p
      where p.active = true
        and lower(p.brand) in :brands
        and (:excludeEmpty = true or p.id not in :exclude)
      order by (p.soldCount * 3 + p.viewCount) desc, p.createdAt desc
    """)
    List<Product> candidatesByBrands(
            @Param("brands") List<String> brandsLower,
            @Param("exclude") List<Long> exclude,
            @Param("excludeEmpty") boolean excludeEmpty,
            Pageable pageable
    );

    @Query("""
      select p from Product p
      where p.active = true
        and p.discountPrice is not null
        and p.discountPrice < p.price
        and (:excludeEmpty = true or p.id not in :exclude)
      order by ((p.price - p.discountPrice) / p.price) desc, (p.soldCount * 3 + p.viewCount) desc
    """)
    List<Product> discountedCandidates(
            @Param("exclude") List<Long> exclude,
            @Param("excludeEmpty") boolean excludeEmpty,
            Pageable pageable
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

}