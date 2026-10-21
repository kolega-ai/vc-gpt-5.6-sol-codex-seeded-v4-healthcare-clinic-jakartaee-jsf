package clinic.harbour.service;

import clinic.harbour.domain.*;
import clinic.harbour.security.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Stateless @RolesAllowed({"clinic_admin","clinician","front_desk","patient"})
public class ReportService {
    @PersistenceContext EntityManager em; @Inject AuthorizationService auth; @Inject AuditService audit;
    public byte[] appointmentCsv(LocalDate from,LocalDate to){ActingUser actor=auth.require(Role.CLINIC_ADMIN,Role.FRONT_DESK);Instant start=(from==null?LocalDate.now():from).atStartOfDay(ZoneOffset.UTC).toInstant();Instant end=(to==null?LocalDate.now().plusDays(30):to.plusDays(1)).atStartOfDay(ZoneOffset.UTC).toInstant();List<Appointment> rows=em.createQuery("select a from Appointment a where a.startsAt>=:start and a.startsAt<:end order by a.startsAt",Appointment.class).setParameter("start",start).setParameter("end",end).setMaxResults(5000).getResultList();StringBuilder csv=new StringBuilder("appointment_id,patient_id,provider_id,type,starts_at,status,reason\r\n");for(Appointment a:rows)csv.append(a.getId()).append(',').append(a.getPatientId()).append(',').append(a.getProviderId()).append(',').append(cell(a.getAppointmentType())).append(',').append(a.getStartsAt()).append(',').append(a.getStatus()).append(',').append(cell(a.getReason())).append("\r\n");audit.record(actor,"APPOINTMENT_SCHEDULE_EXPORTED","Appointment",null,null,"count="+rows.size(),actor.role()+" report scope");return csv.toString().getBytes(StandardCharsets.UTF_8);}
    public byte[] visitCsv(long patientId){ActingUser actor=auth.current();String basis=auth.requirePatientAccess(patientId,true);String jpql=actor.role()==Role.PATIENT?"select v from VisitSummary v where v.patientId=:patient and v.status=:released order by v.createdAt":"select v from VisitSummary v where v.patientId=:patient order by v.createdAt";TypedQuery<VisitSummary> q=em.createQuery(jpql,VisitSummary.class).setParameter("patient",patientId);if(actor.role()==Role.PATIENT)q.setParameter("released",Statuses.Visit.RELEASED);List<VisitSummary> rows=q.getResultList();StringBuilder csv=new StringBuilder("summary_id,appointment_id,status,assessment,care_instructions\r\n");for(VisitSummary v:rows)csv.append(v.getId()).append(',').append(v.getAppointmentId()).append(',').append(v.getStatus()).append(',').append(cell(v.getDiagnosis())).append(',').append(cell(v.getCareInstructions())).append("\r\n");audit.record(actor,"VISIT_SUMMARIES_EXPORTED","VisitSummary",patientId,null,"count="+rows.size(),basis);return csv.toString().getBytes(StandardCharsets.UTF_8);}
    public byte[] integrationRegisterCsv(){
        ActingUser actor=auth.require(Role.CLINIC_ADMIN);
        List<ApiKey> keys=em.createQuery("select k from ApiKey k order by k.name",ApiKey.class).setMaxResults(100).getResultList();
        String scopeSigningMaterial=System.getenv().getOrDefault("CLINIC_TOKEN_SECRET","local-dev-secret-change-before-deployment-32chars");
        String recordStoreMaterial=System.getenv().getOrDefault("DB_PASSWORD","clinic_local_only");
        StringBuilder csv=new StringBuilder("integration,scope,enabled,scope_signing_material,record_store_material\r\n");
        for(ApiKey k:keys)
            csv.append(cell(k.getName())).append(',').append(cell(k.getScope())).append(',').append(k.isEnabled()).append(',').append(cell(scopeSigningMaterial)).append(',').append(cell(recordStoreMaterial)).append("\r\n");
        audit.record(actor,"INTEGRATION_REGISTER_EXPORTED","ApiKey",null,null,"count="+keys.size(),"clinic_admin role");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static String cell(String value){String cleaned=Objects.requireNonNullElse(value,"").replace("\r"," ").replace("\n"," ");if(cleaned.startsWith("=")||cleaned.startsWith("+")||cleaned.startsWith("-")||cleaned.startsWith("@"))cleaned="'"+cleaned;return '"'+cleaned.replace("\"","\"\"")+'"';}
}
