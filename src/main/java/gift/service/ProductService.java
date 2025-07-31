package gift.service;

import gift.domain.Option;
import gift.domain.Product;
import gift.dto.CreateProductRequest;
import gift.dto.CreateProductResponse;
import gift.dto.UpdateProductRequest;
import gift.dto.UpdateProductResponse;
import gift.repository.ProductJpaRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;


@Service
public class ProductService {

    private final ProductJpaRepository repository;

    public ProductService(ProductJpaRepository repository) {
        this.repository = repository;
    }

    public CreateProductResponse save(CreateProductRequest request) {
        Product product = new Product(null, request.name(), request.price(), request.imageUrl());
        List<Option> options = request.options().stream()
                .map(o -> new Option(null, o.name(), o.quantity(), product)).toList();

        options.forEach(product::addOption);
        repository.save(product);

        return new CreateProductResponse(product.getId(), product.getName(), product.getPrice(), product.getImageUrl());
    }

    public Product findById(Long id) {
        findIdOrThrow(id);
        return repository.findById(id).get();
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public UpdateProductResponse update(Long id, UpdateProductRequest request) {
        findIdOrThrow(id);
        Product updateProduct = repository.findById(id)
                .map(product -> {
                    product.update(request.name(), request.price(), request.imageUrl());
                    return product;
                }).get();

        return new UpdateProductResponse(updateProduct.getId(), updateProduct.getName(), updateProduct.getPrice(), updateProduct.getImageUrl());
    }

    public void delete(Long id) {
        findIdOrThrow(id);
        repository.deleteById(id);
    }

    private void findIdOrThrow(Long id) {
        Optional<Product> findProduct = repository.findById(id);
        if (findProduct.isEmpty()) {
            throw new NoSuchElementException("존재하지 않는 상품입니다.");
        }
    }
}
