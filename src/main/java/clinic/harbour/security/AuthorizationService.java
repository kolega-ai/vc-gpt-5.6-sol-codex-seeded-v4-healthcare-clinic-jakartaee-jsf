package clinic.harbour.security;

import clinic.harbour.domain.*;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.SessionContext;
import jakarta.persistence.*;
import java.security.Principal;
import java.util.*;

/** The decision boundary. It reloads role and ownership from its database for every call. */
@Stateless
public class AuthorizationService {
    @PersistenceContext EntityManager em;
    @Resource SessionContext sessionContext;

    public ActingUser current(){
        Principal principal=sessionContext.getCallerPrincipal();
        if(principal==null||principal.getName()==null||"anonymous".equalsIgnoreCase(principal.getName())) throw new AccessDeniedException("Authentication required");
        UserAccount user=em.createQuery("select u from UserAccount u where u.email=:email and u.enabled=true",UserAccount.class).setParameter("email",principal.getName().toLowerCase()).getResultStream().findFirst().orElseThrow(()->new AccessDeniedException("Account unavailable"));
        return new ActingUser(user.getId(),user.getEmail(),user.getDisplayName(),user.getRole(),user.getPatientId(),user.getProviderId());
    }
    public ActingUser require(Role... allowed){ActingUser actor=current();if(Arrays.stream(allowed).noneMatch(r->r==actor.role()))throw new AccessDeniedException("This action is not permitted for your role");return actor;}
    public String requirePatientAccess(Long patientId,boolean clinical){
        ActingUser actor=current();
        if(actor.role()==Role.CLINIC_ADMIN) return "clinic_admin role";
        if(actor.role()==Role.PATIENT&&Objects.equals(actor.patientId(),patientId)) return "patient owns record";
        if(actor.role()==Role.FRONT_DESK&&!clinical) return "front_desk operational scope";
        if(actor.role()==Role.CLINICIAN&&clinicianAssigned(actor.providerId(),patientId)) return "clinician assigned patient/appointment";
        throw new AccessDeniedException("Patient record is outside your authorised scope");
    }
    public String requireAppointmentAccess(Appointment appointment,boolean clinical){return requirePatientAccess(appointment.getPatientId(),clinical);}
    private boolean clinicianAssigned(Long providerId,Long patientId){
        if(providerId==null)return false;
        Long matches=em.createQuery("select count(p) from Patient p where p.id=:patient and p.assignedProviderId=:provider",Long.class).setParameter("patient",patientId).setParameter("provider",providerId).getSingleResult();
        if(matches>0)return true;
        return em.createQuery("select count(a) from Appointment a where a.patientId=:patient and a.providerId=:provider",Long.class).setParameter("patient",patientId).setParameter("provider",providerId).getSingleResult()>0;
    }
}
