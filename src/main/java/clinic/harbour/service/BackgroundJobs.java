package clinic.harbour.service;
import clinic.harbour.domain.*;
import jakarta.ejb.*;
import jakarta.persistence.*;
import java.util.List;
@Singleton @Startup public class BackgroundJobs {
    @PersistenceContext EntityManager em;
    @Schedule(hour="*",minute="*/15",persistent=false) public void processLocalOutbox(){List<OutboxEmail> emails=em.createQuery("select e from OutboxEmail e where e.sentAt is null order by e.createdAt",OutboxEmail.class).setMaxResults(50).getResultList();emails.forEach(OutboxEmail::markSent);List<WebhookDelivery> hooks=em.createQuery("select w from WebhookDelivery w where w.status='PENDING' order by w.createdAt",WebhookDelivery.class).setMaxResults(50).getResultList();hooks.forEach(WebhookDelivery::delivered);}
}
