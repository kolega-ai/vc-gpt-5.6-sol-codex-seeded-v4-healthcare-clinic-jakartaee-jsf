package clinic.harbour.domain;
import jakarta.persistence.*;
@Entity @Table(name="api_keys")
public class ApiKey extends BaseEntity {
    @Column(nullable=false,length=100) private String name;
    @Column(name="key_hash",nullable=false,unique=true,length=64) private String keyHash;
    @Column(nullable=false,length=100) private String scope;
    @Column(nullable=false) private boolean enabled=true;
    protected ApiKey() {} public String getName(){return name;} public String getKeyHash(){return keyHash;} public String getScope(){return scope;} public boolean isEnabled(){return enabled;}
}
