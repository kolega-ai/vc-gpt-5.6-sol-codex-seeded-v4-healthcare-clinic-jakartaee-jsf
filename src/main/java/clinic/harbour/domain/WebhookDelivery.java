package clinic.harbour.domain;
import jakarta.persistence.*;
@Entity @Table(name="webhook_deliveries")
public class WebhookDelivery extends BaseEntity {
    @Column(name="event_type",nullable=false,length=100) private String eventType;
    @Column(nullable=false,columnDefinition="text") private String payload;
    @Column(nullable=false,length=20) private String status="PENDING";
    @Column(nullable=false) private int attempts;
    @Column(name="last_error",length=500) private String lastError;
    protected WebhookDelivery() {} public WebhookDelivery(String eventType,String payload){this.eventType=eventType;this.payload=payload;}
    public String getEventType(){return eventType;} public String getPayload(){return payload;} public String getStatus(){return status;} public int getAttempts(){return attempts;} public String getLastError(){return lastError;} public void delivered(){status="DELIVERED";attempts++;lastError=null;} public void failed(String error){status="FAILED";attempts++;lastError=error;}
}
