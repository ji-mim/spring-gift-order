package gift.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private Option option;

    protected Orders() {
    }

    public Orders(Long id, String message, int quantity, Option option) {
        this.id = id;
        this.message = message;
        this.quantity = quantity;
        this.option = option;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getQuantity() {
        return quantity;
    }

    public Option getOption() {
        return option;
    }
}
