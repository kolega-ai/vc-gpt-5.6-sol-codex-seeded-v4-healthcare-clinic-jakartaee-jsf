package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="session_identities")
public class SessionIdentity {
    @Id @Column(name="token_hash",length=64) private String tokenHash;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="revoked_at") private Instant revokedAt;
    protected SessionIdentity() {} public SessionIdentity(String tokenHash,Long userId,Instant expiresAt){this.tokenHash=tokenHash;this.userId=userId;this.expiresAt=expiresAt;}
    public String getTokenHash(){return tokenHash;} public Long getUserId(){return userId;} public Instant getExpiresAt(){return expiresAt;} public Instant getRevokedAt(){return revokedAt;} public void revoke(){revokedAt=Instant.now();}
}
