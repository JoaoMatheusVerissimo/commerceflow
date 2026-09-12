package com.commerceflow.catalog.config;

import com.commerceflow.catalog.application.CatalogDtos.CategoryInput;
import com.commerceflow.catalog.application.CatalogDtos.ProductInput;
import com.commerceflow.catalog.application.CatalogDtos.VariantInput;
import com.commerceflow.catalog.application.CatalogDtos.ImageInput;
import com.commerceflow.catalog.application.CatalogService;
import com.commerceflow.catalog.domain.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "commerceflow.catalog.demo-seed", havingValue = "true")
public class DemoCatalogSeed implements ApplicationRunner {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final CatalogService service;
    private final ProductRepository products;
    public DemoCatalogSeed(CatalogService service, ProductRepository products) {
        this.service = service; this.products = products;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() > 0 || !service.categories().isEmpty()) { return; }
        var boots = service.saveCategory(null, new CategoryInput("Botas", "botas", null), ACTOR);
        var hats = service.saveCategory(null, new CategoryInput("Chapéus", "chapeus", null), ACTOR);
        var accessories = service.saveCategory(null, new CategoryInput("Acessórios", "acessorios", null), ACTOR);
        seed(boots.id(), "Bota Serra", "bota-serra",
                "Couro marrom e desenho de inspiração western. Modelo demonstrativo com duas numerações.",
                "459.90", "399.90", "boot", "Marrom");
        seed(boots.id(), "Bota Horizonte", "bota-horizonte",
                "Silhueta de cano curto em tom areia, pensada para compor o dia a dia.",
                "379.90", null, "boot", "Areia");
        seed(hats.id(), "Chapéu Campo", "chapeu-campo",
                "Chapéu de aba larga com faixa contrastante. Ilustração do modelo demonstrativo.",
                "189.90", null, "hat", "Marrom");
        seed(hats.id(), "Chapéu Trilha", "chapeu-trilha",
                "Aba estruturada em tom terra e acabamento discreto.",
                "229.90", "199.90", "hat", "Areia");
        seed(accessories.id(), "Cinto Origem", "cinto-origem",
                "Cinto em tom caramelo com fivela metálica e ajuste por tamanho.",
                "129.90", null, "belt", "Marrom");
        seed(accessories.id(), "Cinto Vereda", "cinto-vereda",
                "Acabamento escuro, costura aparente e fivela retangular.",
                "149.90", "119.90", "belt", "Areia");
    }

    private void seed(UUID category, String name, String slug, String description,
                      String price, String promotional, String image, String color) {
        var base = new BigDecimal(price);
        var promo = promotional == null ? null : new BigDecimal(promotional);
        String sku = slug.toUpperCase(java.util.Locale.ROOT);
        service.saveProduct(null, new ProductInput(category, name, slug, description, "ACTIVE",
                List.of(new VariantInput(sku + "-P", color, "P", base, promo),
                        new VariantInput(sku + "-M", color, "M", base, promo)),
                List.of(new ImageInput("/catalog-images/" + image + ".svg", "Ilustração de " + name),
                        new ImageInput("/catalog-images/" + image + "-detail.svg", "Detalhe ilustrado de " + name)),
                null), ACTOR);
    }
}
