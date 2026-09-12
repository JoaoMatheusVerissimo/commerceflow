package com.commerceflow.catalog.application;

import com.commerceflow.catalog.application.CatalogDtos.CategoryInput;
import com.commerceflow.catalog.application.CatalogDtos.CategoryView;
import com.commerceflow.catalog.application.CatalogDtos.ProductInput;
import com.commerceflow.catalog.application.CatalogDtos.ProductView;
import com.commerceflow.catalog.application.CatalogDtos.PageView;
import com.commerceflow.catalog.application.CatalogDtos.ReviewInput;
import com.commerceflow.catalog.application.CatalogDtos.ReviewView;
import com.commerceflow.catalog.application.CatalogDtos.ModerationInput;
import com.commerceflow.catalog.domain.Category;
import com.commerceflow.catalog.domain.CategoryRepository;
import com.commerceflow.catalog.domain.Product;
import com.commerceflow.catalog.domain.ProductImage;
import com.commerceflow.catalog.domain.ProductRepository;
import com.commerceflow.catalog.domain.Review;
import com.commerceflow.catalog.domain.ReviewRepository;
import com.commerceflow.catalog.domain.Variant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ReviewRepository reviews;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CatalogService(ProductRepository products, CategoryRepository categories, ReviewRepository reviews,
                          JdbcTemplate jdbc, Clock clock) {
        this.products = products; this.categories = categories; this.reviews = reviews;
        this.jdbc = jdbc; this.clock = clock;
    }

    public List<CategoryView> categories() {
        return categories.findAll(Sort.by("name").and(Sort.by("id"))).stream().map(CategoryView::from).toList();
    }

    public PageView<ProductView> search(String q, String category, BigDecimal min, BigDecimal max,
                                       String sort, int page, int size, boolean admin) {
        if (q.length() > 100 || category.length() > 100 || (min != null && min.signum() < 0)
                || (max != null && max.signum() < 0) || (min != null && max != null && min.compareTo(max) > 0)) {
            throw invalid("Invalid search filters");
        }
        Sort order = switch (sort) {
            case "price-asc" -> Sort.by("minPrice");
            case "price-desc" -> Sort.by("minPrice").descending();
            case "name" -> Sort.by("name");
            case "newest" -> Sort.by("createdAt").descending();
            default -> throw invalid("Invalid sort option");
        };
        Specification<Product> specification = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!admin) { predicates.add(cb.equal(root.get("status"), "ACTIVE")); }
            if (!q.isBlank()) {
                String escaped = q.trim().toLowerCase(java.util.Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '!'),
                        cb.like(cb.lower(root.get("description")), "%" + escaped + "%", '!')));
            }
            if (!category.isBlank()) { predicates.add(cb.equal(root.get("category").get("slug"), category)); }
            if (min != null) { predicates.add(cb.ge(root.get("minPrice"), min)); }
            if (max != null) { predicates.add(cb.le(root.get("minPrice"), max)); }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = products.findAll(specification, paging(page, size, order.and(Sort.by("id"))));
        return page(result.map(ProductView::from));
    }

    public ProductView detail(String slug) {
        return ProductView.from(products.findBySlugAndStatus(slug, "ACTIVE").orElseThrow(CatalogService::notFound));
    }

    public ProductView adminDetail(UUID id) { return ProductView.from(product(id)); }

    @Transactional
    public CategoryView saveCategory(UUID id, CategoryInput input, UUID actor) {
        if (id == null && categories.count() >= 200) { throw invalid("Category limit is 200"); }
        var category = id == null ? new Category(input.name().trim(), input.slug())
                : categories.findById(id).orElseThrow(CatalogService::notFound);
        if (id != null) { checkVersion(input.version(), category.getVersion()); }
        category.update(input.name().trim(), input.slug());
        category = categories.saveAndFlush(category);
        audit(actor, id == null ? "CATEGORY_CREATED" : "CATEGORY_UPDATED", category.getId());
        return CategoryView.from(category);
    }

    @Transactional
    public ProductView saveProduct(UUID id, ProductInput input, UUID actor) {
        var category = categories.findById(input.categoryId()).orElseThrow(CatalogService::notFound);
        var product = id == null ? new Product(clock.instant()) : product(id);
        if (id != null) { checkVersion(input.version(), product.getVersion()); }
        var skus = new HashSet<String>();
        var attributes = new HashSet<String>();
        var variants = input.variants().stream().map(v -> {
            if (!skus.add(v.sku()) || !attributes.add(v.color().trim() + "\n" + v.size().trim())) {
                throw invalid("Duplicate SKU or color/size combination");
            }
            return new Variant(v.sku(), v.color().trim(), v.size().trim(), v.price(), v.promotionalPrice());
        }).toList();
        var images = input.images().stream().map(i -> {
            validateImageUrl(i.url());
            return new ProductImage(i.url(), i.alt().trim());
        }).toList();
        product.update(category, input.name().trim(), input.slug(), input.description().trim(), input.status(),
                variants, images, clock.instant());
        product = products.saveAndFlush(product);
        audit(actor, id == null ? "PRODUCT_CREATED" : "PRODUCT_UPDATED", product.getId());
        return ProductView.from(product);
    }

    public PageView<ReviewView> reviews(UUID productId, int page, int size) {
        if (!product(productId).getStatus().equals("ACTIVE")) { throw notFound(); }
        return page(reviews.findByProductIdAndStatus(productId, "APPROVED",
                paging(page, size, Sort.by("createdAt").descending().and(Sort.by("id")))).map(ReviewView::from));
    }

    @Transactional
    public ReviewView submitReview(UUID productId, UUID author, ReviewInput input) {
        if (!product(productId).getStatus().equals("ACTIVE")) { throw notFound(); }
        return ReviewView.from(reviews.saveAndFlush(new Review(productId, author, input.rating(),
                input.comment().trim(), clock.instant())));
    }

    public PageView<ReviewView> pendingReviews(int page, int size) {
        return page(reviews.findByStatus("PENDING", paging(page, size, Sort.by("createdAt").and(Sort.by("id"))))
                .map(ReviewView::from));
    }

    @Transactional
    public ReviewView moderate(UUID id, ModerationInput input, UUID actor) {
        var review = reviews.findById(id).orElseThrow(CatalogService::notFound);
        checkVersion(input.version(), review.getVersion());
        review.moderate(input.status());
        reviews.flush();
        audit(actor, "REVIEW_" + input.status(), id);
        return ReviewView.from(review);
    }

    private Product product(UUID id) { return products.findById(id).orElseThrow(CatalogService::notFound); }

    private void audit(UUID actor, String action, UUID resource) {
        jdbc.update("INSERT INTO catalog_audit(id, actor_id, action, resource_id, occurred_at) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), actor, action, resource, java.sql.Timestamp.from(clock.instant()));
    }

    private static void validateImageUrl(String value) {
        if (value.matches("/catalog-images/[a-z0-9-]+\\.svg")) { return; }
        URI uri;
        try { uri = URI.create(value); }
        catch (IllegalArgumentException exception) { throw invalid("Invalid image URL"); }
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw invalid("Image URL must use HTTPS or a bundled catalog image");
        }
    }

    private static PageRequest paging(int page, int size, Sort sort) {
        if (page < 0 || page > 10000 || size < 1 || size > 48) { throw invalid("Invalid pagination"); }
        return PageRequest.of(page, size, sort);
    }

    private static <T> PageView<T> page(Page<T> page) {
        return new PageView<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private static void checkVersion(Long expected, Long actual) {
        if (!Objects.equals(expected, actual)) {
            throw new CatalogException(409, "VERSION_CONFLICT", "Resource changed; reload before editing");
        }
    }
    private static CatalogException invalid(String message) {
        return new CatalogException(400, "INVALID_REQUEST", message);
    }
    private static CatalogException notFound() {
        return new CatalogException(404, "CATALOG_NOT_FOUND", "Catalog resource not found");
    }
}
