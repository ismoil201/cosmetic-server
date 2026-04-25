package com.example.backend.domain.favorite.repository;

import com.example.backend.domain.favorite.entity.Favorite;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserAndProduct(User user, Product product);

    List<Favorite> findByUser(User user);

    boolean existsByUserAndProduct(User user, Product product);

    @Query("select f.product.id from Favorite f where f.user = :user and f.product.id in :ids")
    List<Long> findFavoriteProductIds(@Param("user") User user, @Param("ids") List<Long> ids);
}
