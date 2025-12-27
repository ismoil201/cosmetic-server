package com.example.backend.service;

import com.example.backend.dto.AdminProductResponse;
import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductImage;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ReviewImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepo;

    private final ProductImageRepository productImageRepo;

    // ✅ ADMIN LIST (active filter optional)
    public Page<AdminProductResponse> list(Boolean active, Pageable pageable) {

        Page<Product> page = (active == null)
                ? productRepo.findAll(pageable)
                : productRepo.findByActive(active, pageable);

        return page.map(p -> new AdminProductResponse(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getStock(),
                p.isActive(),
                p.getCategory()
        ));
    }

    // ✅ ADMIN CREATE
    public void create(ProductCreateRequest req) {
        Product p = new Product();
        map(req, p);
        productRepo.save(p);
    }

    // ✅ ADMIN UPDATE
    @Transactional
    public void update(Long id, ProductCreateRequest req) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // product fieldlar
        map(req, product);
        productRepo.save(product);

        // ❌ ESKI RASMLARNI TO‘LIQ O‘CHIRAMIZ
        productImageRepo.deleteByProductId(product.getId());

        // ✅ YANGI RASMLARNI SAQLAYMIZ
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProduct(product);
                img.setImageUrl(req.getImageUrls().get(i));
                img.setMain(i == 0); // birinchi rasm MAIN
                productImageRepo.save(img);
            }
        }
    }


    // ✅ ADMIN SOFT DELETE (active=false)
    public void softDelete(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(false);
        productRepo.save(p);
    }

    // ✅ ADMIN RESTORE (active=true)
    public void restore(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(true);
        productRepo.save(p);
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
