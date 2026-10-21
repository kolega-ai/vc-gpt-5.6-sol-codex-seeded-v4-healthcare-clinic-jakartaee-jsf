package clinic.harbour.service;
import clinic.harbour.domain.AuditEvent;
import clinic.harbour.security.ActingUser;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.util.List;
@Stateless
public class AuditService {
    @PersistenceContext EntityManager em;
    public void record(ActingUser actor,String action,String type,Object id,String before,String after,String basis){em.persist(new AuditEvent(actor==null?null:actor.id(),action,type,id==null?null:id.toString(),before,after,basis));}
    public List<AuditEvent> recent(int limit){return em.createQuery("select a from AuditEvent a order by a.occurredAt desc",AuditEvent.class).setMaxResults(Math.min(Math.max(limit,1),250)).getResultList();}
}
