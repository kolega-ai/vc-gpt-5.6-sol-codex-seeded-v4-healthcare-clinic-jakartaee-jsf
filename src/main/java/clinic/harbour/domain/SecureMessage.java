package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.Instant;
import static clinic.harbour.domain.Statuses.MessageVisibility;
@Entity @Table(name="secure_messages")
public class SecureMessage extends BaseEntity {
    @Column(name="thread_key",nullable=false,length=36) private String threadKey;
    @Column(name="patient_id",nullable=false) private Long patientId;
    @Column(name="sender_id",nullable=false) private Long senderId;
    @Column(nullable=false,length=180) private String subject;
    @Column(nullable=false,length=4000) private String body;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private MessageVisibility visibility;
    @Column(name="read_at") private Instant readAt;
    protected SecureMessage() {}
    public SecureMessage(String threadKey,Long patientId,Long senderId,String subject,String body,MessageVisibility visibility){this.threadKey=threadKey;this.patientId=patientId;this.senderId=senderId;this.subject=subject;this.body=body;this.visibility=visibility;}
    public String getThreadKey(){return threadKey;} public Long getPatientId(){return patientId;} public Long getSenderId(){return senderId;} public String getSubject(){return subject;} public String getBody(){return body;} public MessageVisibility getVisibility(){return visibility;} public Instant getReadAt(){return readAt;} public void markRead(){if(readAt==null)readAt=Instant.now();}
}
