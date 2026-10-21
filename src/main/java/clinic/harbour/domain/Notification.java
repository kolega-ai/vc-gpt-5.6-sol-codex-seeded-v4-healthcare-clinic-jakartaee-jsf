package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="notifications")
public class Notification extends BaseEntity {
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=180) private String title;
    @Column(nullable=false,length=500) private String body;
    @Column(nullable=false,length=120) private String link;
    @Column(name="read_at") private Instant readAt;
    protected Notification() {} public Notification(Long userId,String title,String body,String link){this.userId=userId;this.title=title;this.body=body;this.link=link;}
    public Long getUserId(){return userId;} public String getTitle(){return title;} public String getBody(){return body;} public String getLink(){return link;} public Instant getReadAt(){return readAt;} public void markRead(){readAt=Instant.now();}
}
