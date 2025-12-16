package com.example.backend.service;

import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.dto.ProductDetailResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.repository.FavoriteRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final FavoriteRepository favRepo;
    private final UserService userService;

    public Page<ProductResponse> getHomeProducts(Pageable pageable) {
        User user = userService.getCurrentUser();

        return productRepo.findAll(pageable) // ⭐ MUHIM
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getBrand(),
                        p.getPrice(),
                        p.getDiscountPrice(),
                        p.getImageUrl(),
                        p.getCategory(),
                        favRepo.existsByUserAndProduct(user, p)
                ));
    }


    public ProductDetailResponse getDetail(Long productId) {
        User user = userService.getCurrentUser();

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setViewCount(p.getViewCount() + 1);
        productRepo.save(p);

        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getImageUrl(),
                p.getCategory(),
                p.getStock(),
                favRepo.existsByUserAndProduct(user, p)
        );
    }

    public void create(ProductCreateRequest req) {
        Product p = new Product();
        map(req, p);
        productRepo.save(p);
    }

    // ✅ YO‘Q EDI
    public void update(Long id, ProductCreateRequest req) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        map(req, p);
        productRepo.save(p);
    }

    // ✅ YO‘Q EDI
    public void delete(Long id) {
        productRepo.deleteById(id);
    }

    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setBrand(req.getBrand());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setImageUrl(req.getImageUrl());
        p.setCategory(
                Category.valueOf(req.getCategory().toUpperCase())
        );
        p.setStock(req.getStock());
    }

}
