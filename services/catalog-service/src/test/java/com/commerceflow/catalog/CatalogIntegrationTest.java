package com.commerceflow.catalog;

import com.commerceflow.catalog.application.CatalogDtos.CategoryInput;
import com.commerceflow.catalog.application.CatalogDtos.ProductInput;
import com.commerceflow.catalog.application.CatalogDtos.VariantInput;
import com.commerceflow.catalog.application.CatalogDtos.ImageInput;
import com.commerceflow.catalog.domain.Variant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "commerceflow.catalog.demo-seed=true",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF"
})
@AutoConfigureMockMvc
class CatalogIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String ACTOR = "20000000-0000-0000-0000-000000000001";
    @Autowired MockMvc mvc;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @MockitoBean JwtDecoder decoder;

    @Test
    void catalogQueryCountIsBoundedWithoutOneQueryPerProduct() throws Exception {
        var statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
        mvc.perform(get("/products").param("size", "48")).andExpect(status().isOk());
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @Test
    void publicCatalogSupportsSearchFiltersPaginationAndDetail() throws Exception {
        mvc.perform(get("/products").param("q", "SERRA").param("category", "botas")
                        .param("minPrice", "399.90").param("maxPrice", "399.90").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("bota-serra"))
                .andExpect(jsonPath("$.items[0].currency").value("BRL"));
        mvc.perform(get("/products/bota-serra")).andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].effectivePrice").value("399.90"))
                .andExpect(jsonPath("$.images.length()").value(2));
        mvc.perform(get("/products").param("q", "%")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/products").param("size", "999")).andExpect(status().isBadRequest());
        mvc.perform(get("/products").param("minPrice", "200").param("maxPrice", "1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/products").param("sort", "password" )).andExpect(status().isBadRequest());
        mvc.perform(get("/products/nonexistent")).andExpect(status().isNotFound());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/products']").exists());
    }

    @Test
    void managerCanCreateEditAndArchiveWithVersionChecks() throws Exception {
        String slug = "test-" + UUID.randomUUID();
        var created = create(slug, "DRAFT");
        mvc.perform(get("/products/" + slug)).andExpect(status().isNotFound());
        var active = update(created, slug, "ACTIVE", created.get("version").asLong(), 200);
        mvc.perform(get("/products/" + slug)).andExpect(status().isOk());
        update(created, slug, "ARCHIVED", created.get("version").asLong(), 409);
        update(active, slug, "ARCHIVED", active.get("version").asLong(), 200);
        mvc.perform(get("/products/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void enforcesRoleBoundariesAndDatabaseUniqueness() throws Exception {
        mvc.perform(get("/admin/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/products").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/admin/products").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isForbidden());
        String slug = "unique-" + UUID.randomUUID();
        var first = create(slug, "ACTIVE");
        var input = input(slug, "ACTIVE", UUID.fromString(first.get("category").get("id").asString()), null);
        mvc.perform(manager(post("/admin/products")).contentType("application/json")
                        .content(JSON.writeValueAsString(input))).andExpect(status().isConflict());
        var duplicateSku = new ProductInput(input.categoryId(), "Another", "other-" + UUID.randomUUID(),
                "Description", "ACTIVE", input.variants(), input.images(), null);
        mvc.perform(manager(post("/admin/products")).contentType("application/json")
                        .content(JSON.writeValueAsString(duplicateSku))).andExpect(status().isConflict());
    }

    @Test
    void reviewIsPrivateUntilModeratedAndCannotBeDuplicated() throws Exception {
        var product = create("review-" + UUID.randomUUID(), "ACTIVE");
        String uri = "/products/" + product.get("id").asString() + "/reviews";
        var request = post(uri).with(jwt().jwt(j -> j.subject(ACTOR))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .contentType("application/json").content("{\"rating\":5,\"comment\":\"Ótimo acabamento.\"}");
        var review = JSON.readTree(mvc.perform(request).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        mvc.perform(request).andExpect(status().isConflict());
        mvc.perform(get(uri)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(manager(patch("/admin/reviews/" + review.get("id").asString()))
                        .contentType("application/json").content("{\"status\":\"APPROVED\",\"version\":0}"))
                .andExpect(status().isOk());
        mvc.perform(get(uri)).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].authorId").doesNotExist());
    }

    @Test
    void rejectsInvalidPricesAndImageSchemes() throws Exception {
        assertThatThrownBy(() -> new Variant("SKU", "Brown", "P", new BigDecimal("10.00"), new BigDecimal("11")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Variant("SKU", "Brown", "P", new BigDecimal("0"), null))
                .isInstanceOf(IllegalArgumentException.class);
        var categories = JSON.readTree(mvc.perform(get("/categories")).andReturn().getResponse().getContentAsString());
        var input = input("unsafe-" + UUID.randomUUID(), "ACTIVE",
                UUID.fromString(categories.get(0).get("id").asString()), null);
        var unsafe = new ProductInput(input.categoryId(), input.name(), input.slug(), input.description(), "ACTIVE",
                input.variants(), List.of(new ImageInput("javascript:alert(1)", "Unsafe")), null);
        mvc.perform(manager(post("/admin/products")).contentType("application/json")
                        .content(JSON.writeValueAsString(unsafe))).andExpect(status().isBadRequest());
        assertThat(input.variants().get(0).price()).isEqualByComparingTo("99.90");
    }

    private JsonNode create(String slug, String state) throws Exception {
        var category = JSON.readTree(mvc.perform(manager(post("/admin/categories")).contentType("application/json")
                        .content(JSON.writeValueAsString(new CategoryInput("Test category", slug, null))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return JSON.readTree(mvc.perform(manager(post("/admin/products")).contentType("application/json")
                        .content(JSON.writeValueAsString(input(slug, state,
                                UUID.fromString(category.get("id").asString()), null))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode update(JsonNode product, String slug, String state, long version, int expected) throws Exception {
        var input = input(slug, state, UUID.fromString(product.get("category").get("id").asString()), version);
        return JSON.readTree(mvc.perform(manager(put("/admin/products/" + product.get("id").asString()))
                        .contentType("application/json").content(JSON.writeValueAsString(input)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }
    private static ProductInput input(String slug, String state, UUID category, Long version) {
        return new ProductInput(category, "Test product", slug, "Test description", state,
                List.of(new VariantInput(slug.toUpperCase(), "Brown", "P", new BigDecimal("99.90"), null)),
                List.of(new ImageInput("/catalog-images/boot.svg", "Boot")), version);
    }
    private static MockHttpServletRequestBuilder manager(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(j -> j.subject(ACTOR)).authorities(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }
}
