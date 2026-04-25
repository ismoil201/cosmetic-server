package com.example.backend.domain.favorite.service;

import com.example.backend.domain.product.dto.ProductImageResponse;
import com.example.backend.domain.product.dto.ProductResponse;
import com.example.backend.domain.favorite.entity.Favorite;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.product.entity.ProductImage;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.favorite.repository.FavoriteRepository;
import com.example.backend.domain.product.repository.ProductImageRepository;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favRepo;
    private final ProductRepository productRepo;
    private final UserService userService;
    private final ProductImageRepository productImageRepo;


    public boolean toggle(Long productId) {
        User user = userService.getCurrentUser();
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return favRepo.findByUserAndProduct(user, product)
                .map(f -> {
                    favRepo.delete(f);
                    return false;
                })
                .orElseGet(() -> {
                    Favorite f = new Favorite();
                    f.setUser(user);
                    f.setProduct(product);
                    favRepo.save(f);
                    return true;
                });
    }

    // ✅ YO‘Q EDI
    public List<ProductResponse> myFavorites() {
        User user = userService.getCurrentUser();

        return favRepo.findByUser(user)
                .stream()
                .map(f -> {
                    Product p = f.getProduct();

                    String imageUrl = productImageRepo
                            .findFirstByProductIdAndMainTrue(p.getId())
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    return new ProductResponse(
                            p.getId(),
                            p.getName(),
                            p.getBrand(),
                            p.getPrice(),
                            p.getDiscountPrice(),
                            p.getCategory(),
                            p.getRatingAvg(),
                            p.getReviewCount(),
                            p.getSoldCount(),
                            p.isTodayDeal(),
                            true,
                            p.getStock(),
                            List.of(new ProductImageResponse(imageUrl, true))
                    );

                })
                .toList();
    }
}

