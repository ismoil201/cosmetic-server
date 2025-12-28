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

    /* ================= CREATE ================= */

    @Transactional
    public void create(ProductCreateRequest req) {

        Product product = new Product();
        map(req, product);
        productRepo.save(product);

        saveImages(product, req.getImageUrls());
    }

    /* ================= DELETE ================= */

    @Transactional
    public void delete(Long id) {
        productImageRepo.deleteByProductId(id);
        productRepo.deleteById(id);
    }

    /* ================= HOME ================= */

    public Page<ProductResponse> getHomeProducts(Pageable pageable) {

        User user = userService.getCurrentUserOrNull();
        Page<Product> page = productRepo.findByActiveTrue(pageable);

        return page.map(product -> {

            boolean favorite = false;
            if (user != null) {
                favorite = favRepo.existsByUserAndProduct(user, product);
            }

            List<ProductImageResponse> images =
                    productImageRepo.findByProductId(product.getId())
                            .stream()
                            .map(img -> new ProductImageResponse(
                                    img.getImageUrl(),
                                    img.isMain()
                            ))
                            .toList();

            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getBrand(),
                    product.getPrice(),
                    product.getDiscountPrice(),
                    product.getCategory(),
                    product.getRatingAvg(),
                    product.getReviewCount(),
                    product.getSoldCount(),      // 🔥
                    product.isTodayDeal(),       // 🔥
                    favorite,
                    images
            );


        });
    }

    /* ================= DETAIL ================= */

    @Transactional
    public ProductDetailResponse getDetail(Long productId) {

        User user = userService.getCurrentUserOrNull();

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setViewCount(p.getViewCount() + 1);

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
                p.getRatingAvg(),
                p.getReviewCount(),
                p.getSoldCount(),      // 🔥
                p.isTodayDeal(),       // 🔥
                favorite,
                images
        );


    }

    // TODAY DEAL BANNER
    public List<ProductResponse> getTodayDeals() {

        User user = userService.getCurrentUserOrNull();

        return productRepo.findByTodayDealTrueAndActiveTrue()
                .stream()
                .map(p -> {

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
                            p.getRatingAvg(),
                            p.getReviewCount(),
                            p.getSoldCount(),
                            p.isTodayDeal(),
                            favorite,
                            images
                    );
                })
                .toList();
    }


    /* ================= UPDATE ================= */

    @Transactional
    public void update(Long id, ProductCreateRequest req) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        map(req, product);
        productRepo.save(product);

        productImageRepo.deleteByProductId(product.getId());
        saveImages(product, req.getImageUrls());
    }

    /* ================= HELPERS ================= */

    private void saveImages(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageUrl(imageUrls.get(i));
            img.setMain(i == 0);
            productImageRepo.save(img);
        }
    }

    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setBrand(req.getBrand());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setStock(req.getStock());

        try {
            p.setCategory(Category.valueOf(req.getCategory().toUpperCase()));
        } catch (Exception e) {
            throw new RuntimeException("Invalid category");
        }
    }
}
