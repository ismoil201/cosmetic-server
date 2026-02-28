package com.example.backend.repository;

import com.example.backend.entity.EventLog;
import com.example.backend.entity.EventType;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {


    default List<Object[]> impressionCounts(User user, LocalDateTime after) {
        return countByProductAfter(user, EventType.IMPRESSION, after);
    }

    default List<Object[]> clickOrViewCounts(User user, LocalDateTime after) {
        return countByProductAfterTypes(user, List.of(EventType.CLICK, EventType.VIEW), after);
    }

    // oxirgi N kunda ko‘rilgan product ids
    @Query("""
      select distinct e.product.id
      from EventLog e
      where e.user = :user
        and e.eventType = :type
        and e.createdAt >= :after
        and e.product is not null
    """)
    List<Long> findProductIdsAfter(@Param("user") User user,
                                   @Param("type") EventType type,
                                   @Param("after") LocalDateTime after);

    // impression counts (last 24h)
    @Query("""
      select e.product.id, count(e.id)
      from EventLog e
      where e.user = :user
        and e.eventType = :type
        and e.createdAt >= :after
        and e.product is not null
      group by e.product.id
    """)
    List<Object[]> countByProductAfter(@Param("user") User user,
                                       @Param("type") EventType type,
                                       @Param("after") LocalDateTime after);

    // click or view counts (last 24h)
    @Query("""
      select e.product.id, count(e.id)
      from EventLog e
      where e.user = :user
        and e.eventType in :types
        and e.createdAt >= :after
        and e.product is not null
      group by e.product.id
    """)
    List<Object[]> countByProductAfterTypes(@Param("user") User user,
                                            @Param("types") List<EventType> types,
                                            @Param("after") LocalDateTime after);
}