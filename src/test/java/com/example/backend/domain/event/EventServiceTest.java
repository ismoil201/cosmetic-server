package com.example.backend.domain.event;

import com.example.backend.domain.event.entity.EventLog;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.event.repository.EventLogRepository;
import com.example.backend.domain.event.service.EventService;
import com.example.backend.domain.product.entity.Category;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.recommendation.service.InterestService;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.global.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for EventService validation and enrichment logic
 *
 * Test coverage:
 * ✅ Validation: VIEW/CLICK/IMPRESSION require productId
 * ✅ Validation: SEARCH requires queryText
 * ✅ Enrichment: Auto-populate category/brand from Product
 * ✅ Graceful degradation: Handle non-existent productId
 * ✅ Backward compatibility: Old Android payloads still work
 */
class EventServiceTest {

    private EventService eventService;
    private EventLogRepository eventRepo;
    private UserService userService;
    private ProductRepository productRepo;
    private InterestService interestService;

    @BeforeEach
    void setUp() {
        eventRepo = mock(EventLogRepository.class);
        userService = mock(UserService.class);
        productRepo = mock(ProductRepository.class);
        interestService = mock(InterestService.class);

        eventService = new EventService(eventRepo, userService, productRepo, interestService);

        // Default: no authenticated user
        when(userService.getCurrentUserOrNull()).thenReturn(null);
    }

    // =====================================================
    // VALIDATION TESTS
    // =====================================================

    @Test
    void viewEvent_withoutProductId_throwsBadRequest() {
        // Given: VIEW event without productId
        Long productId = null;

        // When & Then: Should throw BadRequestException
        assertThatThrownBy(() ->
                eventService.log(EventType.VIEW, productId, "HOME", null, null, "session123")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VIEW event requires productId");

        // Verify: Event was NOT saved
        verify(eventRepo, never()).save(any());
    }

    @Test
    void clickEvent_withoutProductId_throwsBadRequest() {
        // Given: CLICK event without productId
        Long productId = null;

        // When & Then: Should throw BadRequestException
        assertThatThrownBy(() ->
                eventService.log(EventType.CLICK, productId, "SEARCH", 5, null, "session123")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CLICK event requires productId");

        verify(eventRepo, never()).save(any());
    }

    @Test
    void impressionEvent_withoutProductId_throwsBadRequest() {
        // Given: IMPRESSION event without productId
        Long productId = null;

        // When & Then: Should throw BadRequestException
        assertThatThrownBy(() ->
                eventService.log(EventType.IMPRESSION, productId, "HOME", 1, null, "session123")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("IMPRESSION event requires productId");

        verify(eventRepo, never()).save(any());
    }

    @Test
    void searchEvent_withoutQueryText_throwsBadRequest() {
        // Given: SEARCH event without queryText
        String queryText = null;

        // When & Then: Should throw BadRequestException
        assertThatThrownBy(() ->
                eventService.log(EventType.SEARCH, null, "SEARCH", null, queryText, "session123")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SEARCH event requires queryText");

        verify(eventRepo, never()).save(any());
    }

    @Test
    void searchEvent_withBlankQueryText_throwsBadRequest() {
        // Given: SEARCH event with blank queryText
        String queryText = "   ";

        // When & Then: Should throw BadRequestException
        assertThatThrownBy(() ->
                eventService.log(EventType.SEARCH, null, "SEARCH", null, queryText, "session123")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SEARCH event requires queryText");

        verify(eventRepo, never()).save(any());
    }

    // =====================================================
    // ENRICHMENT TESTS
    // =====================================================

    @Test
    void viewEvent_withProductId_autopopulatesCategoryAndBrand() {
        // Given: Product exists with category and brand
        Product product = new Product();
        product.setId(100L);
        product.setName("Moisture Cream");
        product.setCategory(Category.SKINCARE);
        product.setBrand("COSRX");

        when(productRepo.findById(100L)).thenReturn(Optional.of(product));

        // When: Log VIEW event with productId
        eventService.log(EventType.VIEW, 100L, "HOME", 3, null, "session123");

        // Then: Event saved with auto-populated category and brand
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventRepo).save(captor.capture());

        EventLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(EventType.VIEW);
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getCategory()).isEqualTo("SKINCARE");  // ✅ Auto-populated from product.category
        assertThat(saved.getBrand()).isEqualTo("COSRX");        // ✅ Auto-populated from product.brand
        assertThat(saved.getScreen()).isEqualTo("HOME");
        assertThat(saved.getPosition()).isEqualTo(3);
        assertThat(saved.getSessionId()).isEqualTo("session123");
    }

    @Test
    void clickEvent_withProductId_savesScreenAndPosition() {
        // Given: Product exists
        Product product = new Product();
        product.setId(200L);
        product.setCategory(Category.MAKEUP);
        product.setBrand("Etude House");

        when(productRepo.findById(200L)).thenReturn(Optional.of(product));

        // When: Log CLICK event with screen and position
        eventService.log(EventType.CLICK, 200L, "SEARCH", 7, null, "session456");

        // Then: Event saved with all fields
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventRepo).save(captor.capture());

        EventLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(EventType.CLICK);
        assertThat(saved.getCategory()).isEqualTo("MAKEUP");
        assertThat(saved.getBrand()).isEqualTo("Etude House");
        assertThat(saved.getScreen()).isEqualTo("SEARCH");
        assertThat(saved.getPosition()).isEqualTo(7);
        assertThat(saved.getSessionId()).isEqualTo("session456");
    }

    @Test
    void searchEvent_withQueryText_savesQueryTextWithoutProduct() {
        // Given: SEARCH event (no productId needed)
        // When: Log SEARCH event
        eventService.log(EventType.SEARCH, null, "SEARCH", null, "moisturizer", "session789");

        // Then: Event saved with queryText, but NULL category/brand
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventRepo).save(captor.capture());

        EventLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(EventType.SEARCH);
        assertThat(saved.getProduct()).isNull();
        assertThat(saved.getCategory()).isNull();  // ✅ Expected: no product means no category
        assertThat(saved.getBrand()).isNull();     // ✅ Expected: no product means no brand
        assertThat(saved.getQueryText()).isEqualTo("moisturizer");
        assertThat(saved.getScreen()).isEqualTo("SEARCH");
        assertThat(saved.getSessionId()).isEqualTo("session789");
    }

    // =====================================================
    // GRACEFUL DEGRADATION TESTS
    // =====================================================

    @Test
    void viewEvent_withNonExistentProductId_continuesGracefully() {
        // Given: Product does not exist (was deleted)
        when(productRepo.findById(999L)).thenReturn(Optional.empty());

        // When: Log VIEW event with non-existent productId
        // Then: Should NOT throw exception (graceful degradation)
        assertThatCode(() ->
                eventService.log(EventType.VIEW, 999L, "HOME", null, null, "session999")
        ).doesNotThrowAnyException();

        // Verify: Event was still saved (with NULL product/category/brand)
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventRepo).save(captor.capture());

        EventLog saved = captor.getValue();
        assertThat(saved.getProduct()).isNull();
        assertThat(saved.getCategory()).isNull();
        assertThat(saved.getBrand()).isNull();
        assertThat(saved.getEventType()).isEqualTo(EventType.VIEW);
    }

    // =====================================================
    // BACKWARD COMPATIBILITY TESTS
    // =====================================================

    @Test
    void viewEvent_withAuthenticatedUser_triggersInterestService() {
        // Given: Authenticated user
        User user = new User();
        user.setId(10L);
        user.setEmail("user@test.com");
        when(userService.getCurrentUserOrNull()).thenReturn(user);

        // Given: Product exists
        Product product = new Product();
        product.setId(300L);
        product.setCategory(Category.HAIR_CARE);
        product.setBrand("Mise en Scene");
        when(productRepo.findById(300L)).thenReturn(Optional.of(product));

        // When: Log VIEW event
        eventService.log(EventType.VIEW, 300L, "DETAIL", null, null, "session111");

        // Then: InterestService.onView should be called
        verify(interestService).onView(user, product);
    }

    @Test
    void favoriteAddEvent_doesNotRequireProductId_flexibleValidation() {
        // Given: FAVORITE_ADD event (flexible validation)
        // When: Log without productId
        // Then: Should NOT throw (other event types are flexible)
        assertThatCode(() ->
                eventService.log(EventType.FAVORITE_ADD, null, null, null, null, "session222")
        ).doesNotThrowAnyException();

        verify(eventRepo).save(any());
    }

    @Test
    void addToCartEvent_doesNotRequireProductId_flexibleValidation() {
        // Given: ADD_TO_CART event (flexible validation)
        // When: Log without productId
        // Then: Should NOT throw
        assertThatCode(() ->
                eventService.log(EventType.ADD_TO_CART, null, null, null, null, "session333")
        ).doesNotThrowAnyException();

        verify(eventRepo).save(any());
    }

    // =====================================================
    // INTEGRATION SCENARIO TESTS
    // =====================================================

    @Test
    void realWorldScenario_userBrowsesHomeScreen() {
        // Scenario: User sees 3 products on home screen (IMPRESSION events)
        Product p1 = createProduct(1L, Category.SKINCARE, "Brand A");
        Product p2 = createProduct(2L, Category.MAKEUP, "Brand B");
        Product p3 = createProduct(3L, Category.COLLAGEN, "Brand C");

        when(productRepo.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepo.findById(2L)).thenReturn(Optional.of(p2));
        when(productRepo.findById(3L)).thenReturn(Optional.of(p3));

        String sessionId = "home-session-123";

        // When: Android logs 3 IMPRESSION events
        eventService.log(EventType.IMPRESSION, 1L, "HOME", 0, null, sessionId);
        eventService.log(EventType.IMPRESSION, 2L, "HOME", 1, null, sessionId);
        eventService.log(EventType.IMPRESSION, 3L, "HOME", 2, null, sessionId);

        // Then: 3 events saved with correct enrichment
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventRepo, times(3)).save(captor.capture());

        var events = captor.getAllValues();
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getCategory()).isEqualTo("SKINCARE");
        assertThat(events.get(1).getCategory()).isEqualTo("MAKEUP");
        assertThat(events.get(2).getCategory()).isEqualTo("COLLAGEN");
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
