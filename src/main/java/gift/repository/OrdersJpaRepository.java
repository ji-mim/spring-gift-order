package gift.repository;

import gift.domain.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersJpaRepository extends JpaRepository<Orders, Long> {

}
