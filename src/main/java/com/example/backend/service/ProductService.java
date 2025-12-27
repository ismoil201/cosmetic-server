package com.example.backend.service;

import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.dto.ProductDetailResponse;
import com.example.backend.dto.ProductImageResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductImage;
import com.example.backend.entity.User;
import com.example.backend.repository.FavoriteRepository;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final FavoriteRepository favRepo;
    private final UserService userService;

    private final ProductImageRepository productImageRepo;

    @Transactional
    public void create(ProductCreateRequest req) {

        Product product = new Product();
        map(req, product);
        productRepo.save(product);

        // 🔥 RASMLARNI SAQLASH
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProduct(product);
                img.setImageUrl(req.getImageUrls().get(i));
                img.setMain(i == 0); // birinchi rasm — MAIN
                productImageRepo.save(img);
            }
        }
    }


    @Transactional
    public void delete(Long id) {
        productImageRepo.deleteByProductId(id);
        productRepo.deleteById(id);
    }

    public Page<ProductResponse> getHomeProducts(Pageable pageable) {

        User user = userService.getCurrentUserOrNull();
        Page<Product> page = productRepo.findByActiveTrue(pageable);

        return page.map(p -> {

            boolean favorite = false;
            if (user != null) {
                favorite = favRepo.existsByUserAndProduct(user, p);
            }

            List<ProductImageResponse> images =
                    productImageRepo.findByProductId(p.getId())
                            .stream()
                            .map(img -> new ProductImageResponse(
                                    img.getImageUrl(),
                                    img.isMain()
                            ))
                            .toList();

            return new ProductResponse(
                    p.getId(),
                    p.getName(),
                    p.getBrand(),
                    p.getPrice(),
                    p.getDiscountPrice(),
                    p.getCategory(),
                    favorite,
                    images
            );
        });
    }



    public ProductDetailResponse getDetail(Long productId) {

        User user = userService.getCurrentUserOrNull();

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setViewCount(p.getViewCount() + 1);
        productRepo.save(p);

        boolean favorite = false;
        if (user != null) {
            favorite = favRepo.existsByUserAndProduct(user, p);
        }

        List<ProductImageResponse> images =
                productImageRepo.findByProductId(p.getId())
                        .stream()
                        .map(img -> new ProductImageResponse(
                                img.getImageUrl(),
                                img.isMain()
                        ))
                        .toList();

        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getCategory(),
                p.getStock(),
                favorite,
                images
        );
    }




    @Transactional
    public void update(Long id, ProductCreateRequest req) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        map(req, product);
        productRepo.save(product);

        // ❗ eski rasmlarni o‘chiramiz
        productImageRepo.deleteByProductId(product.getId());

        // 🔥 yangi rasmlar
        if (req.getImageUrls() != null) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProduct(product);
                img.setImageUrl(req.getImageUrls().get(i));
                img.setMain(i == 0);
                productImageRepo.save(img);
            }
        }
    }


    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setBrand(req.getBrand());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());

        // ❌ BU QATORNI O‘CHIR
        // p.setImageUrl(req.getImageUrl());

        try {
            p.setCategory(Category.valueOf(req.getCategory().toUpperCase()));
        } catch (Exception e) {
            throw new RuntimeException("Invalid category");
        }

        p.setStock(req.getStock());
    }


}
