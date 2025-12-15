package com.example.backend.repository;

import com.example.backend.entity.Favorite;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserAndProduct(User user, Product product);

    List<Favorite> findByUser(User user);

    boolean existsByUserAndProduct(User user, Product product);
}
