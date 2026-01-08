package com.example.backend.repository;

import com.example.backend.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    // 🔥 Eng ko‘p qidirilgan
    @Query("""
        select s.normalizedKeyword as keyword, count(s.id) as cnt
        from SearchLog s
        where s.createdAt >= :from
        group by s.normalizedKeyword
        order by cnt desc
    """)
    List<Object[]> topKeywords(
            @Param("from") LocalDateTime from,
            Pageable pageable
    );

    // 🔥 Natija chiqmagan qidiruvlar
    List<SearchLog> findTop20ByResultCountOrderByCreatedAtDesc(int resultCount);
}
