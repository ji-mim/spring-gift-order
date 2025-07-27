package gift.domain;

import jakarta.persistence.*;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String salt;

    @Enumerated(value = EnumType.STRING)
    private AccountType accountType;

    public Member(Long id, String email, String password, String salt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.salt = salt;
    }
    public Member(Long id, String email, String password, String salt, AccountType accountType) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.salt = salt;
        this.accountType = accountType;
    }

    public Member() {
    }

    public String getSalt() {
        return salt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void update(String email, String password, String salt) {
        this.email = email;
        this.password = password;
        this.salt = salt;
    }
}
