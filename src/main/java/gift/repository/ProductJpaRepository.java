package gift.repository;

import gift.domain.Option;
import gift.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    List<Product> findByOptions(List<Option> options);
}
