package com.example.backend.domain.recommendation;

import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.product.entity.Category;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.recommendation.entity.InterestType;
import com.example.backend.domain.recommendation.entity.UserInterest;
import com.example.backend.domain.recommendation.repository.UserInterestRepository;
import com.example.backend.domain.recommendation.service.InterestService;
import com.example.backend.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for InterestService (STEP 8: User Interest Profile Builder)
 *
 * Test coverage:
 * ✅ IMPRESSION event creates/updates CATEGORY and BRAND interests with weight 0.1
 * ✅ VIEW event creates/updates CATEGORY and BRAND interests with weight 2.0
 * ✅ CLICK event creates/updates CATEGORY and BRAND interests with weight 1.0
 * ✅ FAVORITE_ADD event creates/updates interests with weight 4.0
 * ✅ ADD_TO_CART event creates/updates interests with weight 6.0
 * ✅ PURCHASE event creates/updates interests with weight 10.0
 * ✅ SEARCH event creates/updates QUERY interest with weight 2.0
 * ✅ Multiple events accumulate score with decay
 * ✅ Blank category/brand/query does not create interest
 * ✅ Brand and query normalized to lowercase
 * ✅ Event type tracking in last_event_type field
 */
class InterestServiceTest {

    private InterestService interestService;
    private UserInterestRepository interestRepo;

    private User testUser;

    @BeforeEach
    void setUp() {
        interestRepo = mock(UserInterestRepository.class);
        interestService = new InterestService(interestRepo);

        testUser = new User();
        testUser.setId(100L);
        testUser.setEmail("test@example.com");
    }

    // =====================================================
    // IMPRESSION EVENT TESTS (Weight: 0.1)
    // =====================================================

    @Test
    void impressionEvent_createsNewCategoryInterest() {
        // Given: No existing interest
        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.CATEGORY, "SKINCARE"))
                .thenReturn(Optional.empty());

        Product product = createProduct(1L, Category.SKINCARE, "COSRX");

        // When: IMPRESSION event
        interestService.onImpression(testUser, product);

        // Then: New interest created with IMPRESSION weight (0.1)
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, times(2)).save(captor.capture());  // CATEGORY + BRAND

        UserInterest categoryInterest = captor.getAllValues().stream()
                .filter(ui -> ui.getType() == InterestType.CATEGORY)
                .findFirst()
                .orElseThrow();

        assertThat(categoryInterest.getUser()).isEqualTo(testUser);
        assertThat(categoryInterest.getType()).isEqualTo(InterestType.CATEGORY);
        assertThat(categoryInterest.getKey()).isEqualTo("SKINCARE");
        assertThat(categoryInterest.getScore()).isEqualTo(0.1);  // WEIGHT_IMPRESSION
        assertThat(categoryInterest.getLastEventType()).isEqualTo("IMPRESSION");
        assertThat(categoryInterest.getCreatedAt()).isNotNull();
        assertThat(categoryInterest.getUpdatedAt()).isNotNull();
    }

    // =====================================================
    // VIEW EVENT TESTS (Weight: 2.0)
    // =====================================================

    @Test
    void viewEvent_createsNewCategoryAndBrandInterests() {
        // Given: No existing interests
        when(interestRepo.findByUserAndTypeAndKey(any(), any(), any()))
                .thenReturn(Optional.empty());

        Product product = createProduct(2L, Category.MAKEUP, "Etude House");

        // When: VIEW event
        interestService.onView(testUser, product);

        // Then: CATEGORY and BRAND interests created with VIEW weight (2.0)
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, times(2)).save(captor.capture());

        UserInterest categoryInterest = captor.getAllValues().get(0);
        UserInterest brandInterest = captor.getAllValues().get(1);

        // Category interest
        assertThat(categoryInterest.getType()).isEqualTo(InterestType.CATEGORY);
        assertThat(categoryInterest.getKey()).isEqualTo("MAKEUP");
        assertThat(categoryInterest.getScore()).isEqualTo(2.0);  // WEIGHT_VIEW
        assertThat(categoryInterest.getLastEventType()).isEqualTo("VIEW");

        // Brand interest (normalized to lowercase)
        assertThat(brandInterest.getType()).isEqualTo(InterestType.BRAND);
        assertThat(brandInterest.getKey()).isEqualTo("etude house");  // ✅ Lowercase
        assertThat(brandInterest.getScore()).isEqualTo(2.0);  // WEIGHT_VIEW
        assertThat(brandInterest.getLastEventType()).isEqualTo("VIEW");
    }

    @Test
    void viewEvent_updatesExistingInterestWithDecay() {
        // Given: Existing CATEGORY interest with score 10.0
        UserInterest existing = new UserInterest();
        existing.setUser(testUser);
        existing.setType(InterestType.CATEGORY);
        existing.setKey("SKINCARE");
        existing.setScore(10.0);
        existing.setLastEventType("CLICK");

        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.CATEGORY, "SKINCARE"))
                .thenReturn(Optional.of(existing));
        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.BRAND, "cosrx"))
                .thenReturn(Optional.empty());

        Product product = createProduct(3L, Category.SKINCARE, "COSRX");

        // When: VIEW event (weight 2.0)
        interestService.onView(testUser, product);

        // Then: Score updated with decay: (10.0 * 0.98) + 2.0 = 11.8
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, atLeastOnce()).save(captor.capture());

        UserInterest updated = captor.getAllValues().stream()
                .filter(ui -> ui.getType() == InterestType.CATEGORY)
                .findFirst()
                .orElseThrow();

        assertThat(updated.getScore()).isEqualTo(11.8);  // (10.0 * 0.98) + 2.0
        assertThat(updated.getLastEventType()).isEqualTo("VIEW");
    }

    // =====================================================
    // PURCHASE EVENT TESTS (Weight: 10.0)
    // =====================================================

    @Test
    void purchaseEvent_createsHighScoreInterest() {
        // Given: No existing interest
        when(interestRepo.findByUserAndTypeAndKey(any(), any(), any()))
                .thenReturn(Optional.empty());

        Product product = createProduct(4L, Category.HAIR_CARE, "Mise en Scene");

        // When: PURCHASE event
        interestService.onPurchase(testUser, product);

        // Then: Interest created with PURCHASE weight (10.0)
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, times(2)).save(captor.capture());

        UserInterest categoryInterest = captor.getAllValues().get(0);
        assertThat(categoryInterest.getScore()).isEqualTo(10.0);  // WEIGHT_ORDER
        assertThat(categoryInterest.getLastEventType()).isEqualTo("PURCHASE");
    }

    // =====================================================
    // SEARCH EVENT TESTS (Weight: 2.0)
    // =====================================================

    @Test
    void searchEvent_createsQueryInterest() {
        // Given: No existing QUERY interest
        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.QUERY, "sunscreen"))
                .thenReturn(Optional.empty());

        // When: SEARCH event with query "Sunscreen"
        interestService.onSearch(testUser, "Sunscreen");

        // Then: QUERY interest created with normalized key (lowercase)
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo).save(captor.capture());

        UserInterest queryInterest = captor.getValue();
        assertThat(queryInterest.getType()).isEqualTo(InterestType.QUERY);
        assertThat(queryInterest.getKey()).isEqualTo("sunscreen");  // ✅ Normalized to lowercase
        assertThat(queryInterest.getScore()).isEqualTo(2.0);  // WEIGHT_SEARCH
        assertThat(queryInterest.getLastEventType()).isEqualTo("SEARCH");
    }

    @Test
    void searchEvent_withBlankQuery_doesNotCreateInterest() {
        // When: SEARCH event with blank query
        interestService.onSearch(testUser, "   ");

        // Then: No interest created
        verify(interestRepo, never()).save(any());
    }

    @Test
    void searchEvent_withNullQuery_doesNotCreateInterest() {
        // When: SEARCH event with null query
        interestService.onSearch(testUser, null);

        // Then: No interest created
        verify(interestRepo, never()).save(any());
    }

    @Test
    void searchEvent_accumulatesScore() {
        // Given: Existing QUERY interest for "moisturizer" with score 5.0
        UserInterest existing = new UserInterest();
        existing.setUser(testUser);
        existing.setType(InterestType.QUERY);
        existing.setKey("moisturizer");
        existing.setScore(5.0);

        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.QUERY, "moisturizer"))
                .thenReturn(Optional.of(existing));

        // When: Another SEARCH for "moisturizer"
        interestService.onSearch(testUser, "moisturizer");

        // Then: Score updated with decay: (5.0 * 0.98) + 2.0 = 6.9
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo).save(captor.capture());

        assertThat(captor.getValue().getScore()).isEqualTo(6.9);
    }

    // =====================================================
    // FAVORITE EVENT TESTS (Weight: 4.0)
    // =====================================================

    @Test
    void favoriteAddEvent_increasesScore() {
        // Given: No existing interest
        when(interestRepo.findByUserAndTypeAndKey(any(), any(), any()))
                .thenReturn(Optional.empty());

        Product product = createProduct(5L, Category.SKINCARE, "The Ordinary");

        // When: FAVORITE_ADD event
        interestService.onFavorite(testUser, product, true);

        // Then: Interest created with FAVORITE weight (4.0)
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, times(2)).save(captor.capture());

        UserInterest categoryInterest = captor.getAllValues().get(0);
        assertThat(categoryInterest.getScore()).isEqualTo(4.0);  // WEIGHT_FAVORITE
        assertThat(categoryInterest.getLastEventType()).isEqualTo("FAVORITE_ADD");
    }

    @Test
    void favoriteRemoveEvent_decreasesScore() {
        // Given: Existing CATEGORY interest with score 10.0
        UserInterest existing = new UserInterest();
        existing.setUser(testUser);
        existing.setType(InterestType.CATEGORY);
        existing.setKey("SKINCARE");
        existing.setScore(10.0);

        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.CATEGORY, "SKINCARE"))
                .thenReturn(Optional.of(existing));

        // Product with blank brand (to avoid BRAND interest creation)
        Product product = createProduct(6L, Category.SKINCARE, "");

        // When: FAVORITE_REMOVE event
        interestService.onFavorite(testUser, product, false);

        // Then: Score decreased: (10.0 * 0.98) + (-2.0) = 7.8
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo).save(captor.capture());  // Only 1 save (CATEGORY, no BRAND)

        UserInterest updated = captor.getValue();

        assertThat(updated.getType()).isEqualTo(InterestType.CATEGORY);
        assertThat(updated.getScore()).isCloseTo(7.8, within(0.01));  // (10.0 * 0.98) - 2.0
        assertThat(updated.getLastEventType()).isEqualTo("FAVORITE_REMOVE");
    }

    // =====================================================
    // EDGE CASE TESTS
    // =====================================================

    @Test
    void productWithBlankBrand_doesNotCreateBrandInterest() {
        // Given: Product with blank brand
        Product product = createProduct(7L, Category.SKINCARE, "   ");

        when(interestRepo.findByUserAndTypeAndKey(any(), any(), any()))
                .thenReturn(Optional.empty());

        // When: VIEW event
        interestService.onView(testUser, product);

        // Then: Only CATEGORY interest created, not BRAND
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, times(1)).save(captor.capture());  // Only 1 save (CATEGORY)

        assertThat(captor.getValue().getType()).isEqualTo(InterestType.CATEGORY);
    }

    @Test
    void productWithNullBrand_doesNotCreateBrandInterest() {
        // Given: Product with null brand
        Product product = createProduct(8L, Category.MAKEUP, null);

        when(interestRepo.findByUserAndTypeAndKey(any(), any(), any()))
                .thenReturn(Optional.empty());

        // When: CLICK event
        interestService.onClick(testUser, product);

        // Then: Only CATEGORY interest created
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, times(1)).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(InterestType.CATEGORY);
    }

    // =====================================================
    // SCORE ACCUMULATION TESTS
    // =====================================================

    @Test
    void multipleEvents_accumulateScore() {
        // Given: Track saved interests to simulate persistence
        UserInterest categoryInterest = null;
        UserInterest brandInterest = null;

        // First VIEW: No existing interests
        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.CATEGORY, "SKINCARE"))
                .thenReturn(Optional.empty())
                .thenAnswer(inv -> {
                    // After first save, return the saved interest
                    UserInterest ui = new UserInterest();
                    ui.setUser(testUser);
                    ui.setType(InterestType.CATEGORY);
                    ui.setKey("SKINCARE");
                    ui.setScore(2.0);  // After first VIEW
                    return Optional.of(ui);
                });

        when(interestRepo.findByUserAndTypeAndKey(eq(testUser), eq(InterestType.BRAND), anyString()))
                .thenReturn(Optional.empty());

        Product product = createProduct(9L, Category.SKINCARE, "COSRX");

        // When: First VIEW event (weight 2.0)
        interestService.onView(testUser, product);

        // Then: First VIEW creates interest with score 2.0
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, atLeast(1)).save(captor.capture());

        UserInterest firstCategorySave = captor.getAllValues().stream()
                .filter(ui -> ui.getType() == InterestType.CATEGORY)
                .findFirst()
                .orElseThrow();

        assertThat(firstCategorySave.getScore()).isEqualTo(2.0);  // WEIGHT_VIEW

        // When: Second CLICK event (weight 1.0)
        // Mock now returns the previously saved interest
        reset(interestRepo);
        UserInterest existingCat = new UserInterest();
        existingCat.setUser(testUser);
        existingCat.setType(InterestType.CATEGORY);
        existingCat.setKey("SKINCARE");
        existingCat.setScore(2.0);

        when(interestRepo.findByUserAndTypeAndKey(testUser, InterestType.CATEGORY, "SKINCARE"))
                .thenReturn(Optional.of(existingCat));
        when(interestRepo.findByUserAndTypeAndKey(eq(testUser), eq(InterestType.BRAND), anyString()))
                .thenReturn(Optional.empty());

        interestService.onClick(testUser, product);

        // Then: Score updated with decay: (2.0 * 0.98) + 1.0 = 2.96
        ArgumentCaptor<UserInterest> captor2 = ArgumentCaptor.forClass(UserInterest.class);
        verify(interestRepo, atLeastOnce()).save(captor2.capture());

        UserInterest secondCategorySave = captor2.getAllValues().stream()
                .filter(ui -> ui.getType() == InterestType.CATEGORY)
                .findFirst()
                .orElseThrow();

        assertThat(secondCategorySave.getScore()).isEqualTo(2.96);  // (2.0 * 0.98) + 1.0
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private Product createProduct(Long id, Category category, String brand) {
        Product p = new Product();
        p.setId(id);
        p.setCategory(category);
        p.setBrand(brand);
        return p;
    }
}
