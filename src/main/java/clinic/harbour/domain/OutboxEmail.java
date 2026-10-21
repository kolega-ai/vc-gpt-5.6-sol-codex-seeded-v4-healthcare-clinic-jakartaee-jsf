package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="outbox_emails")
public class OutboxEmail extends BaseEntity {
    @Column(name="recipient",nullable=false,length=160) private String recipient;
    @Column(nullable=false,length=200) private String subject;
    @Column(nullable=false,columnDefinition="text") private String body;
    @Column(name="sent_at") private Instant sentAt;
    protected OutboxEmail() {} public OutboxEmail(String recipient,String subject,String body){this.recipient=recipient;this.subject=subject;this.body=body;}
    public String getRecipient(){return recipient;} public String getSubject(){return subject;} public String getBody(){return body;} public Instant getSentAt(){return sentAt;} public void markSent(){sentAt=Instant.now();}
}
