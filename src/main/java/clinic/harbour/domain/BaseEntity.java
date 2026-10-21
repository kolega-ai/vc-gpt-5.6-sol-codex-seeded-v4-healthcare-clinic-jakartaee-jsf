package clinic.harbour.domain;

import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    @Column(name="created_at", nullable=false, updatable=false)
    protected Instant createdAt = Instant.now();
    @Version protected long version;
    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
