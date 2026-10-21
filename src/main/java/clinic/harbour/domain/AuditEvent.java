package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="actor_id") private Long actorId;
    @Column(nullable=false,length=100) private String action;
    @Column(name="resource_type",nullable=false,length=80) private String resourceType;
    @Column(name="resource_id",length=80) private String resourceId;
    @Column(name="before_value",columnDefinition="text") private String beforeValue;
    @Column(name="after_value",columnDefinition="text") private String afterValue;
    @Column(name="authorization_basis",nullable=false,length=300) private String authorizationBasis;
    @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt=Instant.now();
    protected AuditEvent() {}
    public AuditEvent(Long actorId,String action,String resourceType,String resourceId,String before,String after,String basis){this.actorId=actorId;this.action=action;this.resourceType=resourceType;this.resourceId=resourceId;this.beforeValue=before;this.afterValue=after;this.authorizationBasis=basis;}
    public Long getId(){return id;} public Long getActorId(){return actorId;} public String getAction(){return action;} public String getResourceType(){return resourceType;} public String getResourceId(){return resourceId;} public String getBeforeValue(){return beforeValue;} public String getAfterValue(){return afterValue;} public String getAuthorizationBasis(){return authorizationBasis;} public Instant getOccurredAt(){return occurredAt;}
}
