package gift.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.regex.Pattern;

@Entity
public class Option {


    @Id @Column(name = "option_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private static final Pattern FORBIDDEN = Pattern.compile("[^a-zA-Z0-9가-힣()\\[\\]+\\-&/_]");

    protected Option() {
    }

    public Option(Long id, String name, int quantity, Product product) {
        validateName(name);
        validateQuantity(quantity);
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.product = product;
    }



    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity >= 100000000) {
            throw new IllegalArgumentException("옵션 수량은 최소 1개 이상 최대 1억개 미만입니다.");
        }
    }

    private void validateName(String name) {
        if (name.length() > 50) {
            throw new IllegalArgumentException("옵션 이름은 최대 50자까지 입력할 수 있습니다.");
        }

        if (FORBIDDEN.matcher(name).find()) {
            throw new IllegalArgumentException("옵션 이름에 허용되지 않는 특수문자가 포함되어 있습니다.");
        }

    }

    public void subtractQuantity(int quantity) {
        if (this.quantity - quantity < 0) {
            throw new IllegalArgumentException("옵션 수량은 0보다 작을 수 없습니다.");
        }
        this.quantity -= quantity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
