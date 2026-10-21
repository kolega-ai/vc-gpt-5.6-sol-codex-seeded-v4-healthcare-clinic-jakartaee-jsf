package clinic.harbour.service;

import clinic.harbour.config.FeeFormulaSettings;
import clinic.harbour.domain.*;
import clinic.harbour.security.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import static clinic.harbour.domain.Statuses.*;

@Stateless
@RolesAllowed({"clinic_admin","clinician","front_desk","patient"})
public class ClinicService {
    private static final Map<AppointmentStatus,Set<AppointmentStatus>> APPOINTMENT_EDGES=Map.of(
        AppointmentStatus.REQUESTED,EnumSet.of(AppointmentStatus.SCHEDULED,AppointmentStatus.CANCELLED),
        AppointmentStatus.SCHEDULED,EnumSet.of(AppointmentStatus.CHECKED_IN,AppointmentStatus.CANCELLED,AppointmentStatus.NO_SHOW),
        AppointmentStatus.CHECKED_IN,EnumSet.of(AppointmentStatus.COMPLETED,AppointmentStatus.CANCELLED),
        AppointmentStatus.COMPLETED,EnumSet.noneOf(AppointmentStatus.class), AppointmentStatus.CANCELLED,EnumSet.noneOf(AppointmentStatus.class), AppointmentStatus.NO_SHOW,EnumSet.noneOf(AppointmentStatus.class));
    private static final Map<Refill,Set<Refill>> REFILL_EDGES=Map.of(
        Refill.NONE,EnumSet.of(Refill.REQUESTED), Refill.REQUESTED,EnumSet.of(Refill.NEEDS_INFO,Refill.APPROVED,Refill.DENIED,Refill.CANCELLED),
        Refill.NEEDS_INFO,EnumSet.of(Refill.REQUESTED,Refill.APPROVED,Refill.DENIED,Refill.CANCELLED), Refill.APPROVED,EnumSet.noneOf(Refill.class),
        Refill.DENIED,EnumSet.noneOf(Refill.class),Refill.CANCELLED,EnumSet.noneOf(Refill.class));

    @PersistenceContext EntityManager em;
    @Inject AuthorizationService auth;
    @Inject AuditService audit;
    @Inject FeeFormulaSettings feeFormula;

    public boolean appointmentTransitionAllowed(AppointmentStatus from,AppointmentStatus to){return APPOINTMENT_EDGES.getOrDefault(from,Set.of()).contains(to);}
    public boolean refillTransitionAllowed(Refill from,Refill to){return REFILL_EDGES.getOrDefault(from,Set.of()).contains(to);}

    public ActingUser actor(){return auth.current();}
    public List<Provider> providers(){return em.createQuery("select p from Provider p where p.active=true order by p.name",Provider.class).getResultList();}
    public List<AppointmentType> appointmentTypes(){return em.createQuery("select t from AppointmentType t where t.active=true order by t.name",AppointmentType.class).getResultList();}
    public List<Patient> patients(String query){
        ActingUser actor=auth.require(Role.CLINIC_ADMIN,Role.CLINICIAN,Role.FRONT_DESK);
        String q=query==null?"":query.trim().toLowerCase();
        TypedQuery<Patient> typed;
        if(actor.role()==Role.CLINICIAN){typed=em.createQuery("select distinct p from Patient p where (p.assignedProviderId=:provider or exists(select a.id from Appointment a where a.patientId=p.id and a.providerId=:provider)) and (:q='' or lower(concat(p.firstName,' ',p.lastName)) like :like or lower(p.mrn) like :like) order by p.lastName,p.firstName",Patient.class).setParameter("provider",actor.providerId());}
        else typed=em.createQuery("select p from Patient p where :q='' or lower(concat(p.firstName,' ',p.lastName)) like :like or lower(p.mrn) like :like order by p.lastName,p.firstName",Patient.class);
        typed.setParameter("q",q).setParameter("like","%"+q+"%");
        List<Patient> result=typed.setMaxResults(100).getResultList();
        audit.record(actor,"PATIENT_SEARCH","Patient",null,null,"count="+result.size(),actor.role()+" scoped query");
        return result;
    }
    public Patient patient(long id){Patient patient=required(Patient.class,id);String basis=auth.requirePatientAccess(id,false);audit.record(auth.current(),"PATIENT_CHART_VIEWED","Patient",id,null,"viewed",basis);return patient;}
    public void updateDemographics(long patientId,String phone,String email,String address,String emergency){ActingUser actor=auth.require(Role.CLINIC_ADMIN,Role.FRONT_DESK);Patient patient=required(Patient.class,patientId);if(phone==null||phone.trim().length()<7||phone.length()>30||email==null||!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||address==null||address.trim().length()<8||emergency==null||emergency.trim().length()<5)throw new IllegalArgumentException("Enter valid contact, address, and emergency details");String before=patient.getPhone()+"|"+patient.getEmail()+"|"+patient.getAddress();patient.updateDemographics(phone.trim(),email.trim().toLowerCase(),address.trim(),emergency.trim());audit.record(actor,"PATIENT_DEMOGRAPHICS_UPDATED","Patient",patientId,before,patient.getPhone()+"|"+patient.getEmail()+"|"+patient.getAddress(),actor.role()+" demographic scope");}
    public List<Appointment> appointments(String status,LocalDate from,LocalDate to,String sort){
        ActingUser actor=auth.current();
        Set<String> allowedSorts=Set.of("startsAt","createdAt","status"); // bounded whitelist: user input never becomes arbitrary JPQL.
        String safeSort=allowedSorts.contains(sort)?sort:"startsAt";
        StringBuilder jpql=new StringBuilder("select a from Appointment a where 1=1");
        if(actor.role()==Role.PATIENT) jpql.append(" and a.patientId=:patient");
        if(actor.role()==Role.CLINICIAN) jpql.append(" and a.providerId=:provider");
        if(status!=null&&!status.isBlank()) jpql.append(" and a.status=:status");
        if(from!=null)jpql.append(" and a.startsAt>=:from"); if(to!=null)jpql.append(" and a.startsAt<:to");
        jpql.append(" order by a.").append(safeSort).append(" desc");
        TypedQuery<Appointment> query=em.createQuery(jpql.toString(),Appointment.class);
        if(actor.role()==Role.PATIENT)query.setParameter("patient",actor.patientId()); if(actor.role()==Role.CLINICIAN)query.setParameter("provider",actor.providerId());
        if(status!=null&&!status.isBlank())query.setParameter("status",AppointmentStatus.valueOf(status));
        if(from!=null)query.setParameter("from",from.atStartOfDay(ZoneOffset.UTC).toInstant()); if(to!=null)query.setParameter("to",to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        return query.setMaxResults(250).getResultList();
    }
    public Appointment appointment(long id){Appointment value=required(Appointment.class,id);String basis=auth.requireAppointmentAccess(value,false);audit.record(auth.current(),"APPOINTMENT_VIEWED","Appointment",id,null,"viewed",basis);return value;}
    public Appointment book(long providerId,String type,Instant start,String reason){
        ActingUser actor=auth.require(Role.PATIENT);requireText(reason,5,500,"Reason");
        Provider provider=required(Provider.class,providerId);AppointmentType apptType=em.createQuery("select t from AppointmentType t where t.name=:name and t.active=true",AppointmentType.class).setParameter("name",type).getResultStream().findFirst().orElseThrow(()->new IllegalArgumentException("Select an active appointment type"));
        if(start.isBefore(Instant.now().plus(Duration.ofHours(12)))||start.isAfter(Instant.now().plus(Duration.ofDays(120))))throw new IllegalArgumentException("Choose a slot between 12 hours and 120 days from now");
        Instant end=start.plus(Duration.ofMinutes(apptType.getDurationMinutes()));
        Long conflict=em.createQuery("select count(a) from Appointment a where a.providerId=:provider and a.status not in (:closed) and a.startsAt<:end and a.endsAt>:start",Long.class).setParameter("provider",provider.getId()).setParameter("closed",List.of(AppointmentStatus.CANCELLED,AppointmentStatus.NO_SHOW)).setParameter("start",start).setParameter("end",end).getSingleResult();
        if(conflict>0)throw new IllegalStateException("That appointment slot is no longer available");
        Appointment value=new Appointment(actor.patientId(),providerId,type,start,end,reason.trim());em.persist(value);
        audit.record(actor,"APPOINTMENT_BOOKED","Appointment",value.getId(),null,AppointmentStatus.REQUESTED.name(),"patient owns appointment");notifyStaff("New appointment request",actor.displayName()+" requested "+type+" at a seasonal fee of "+feeFormula.apply(apptType.getFee(),type),"/app/schedule.xhtml");return value;
    }
    public void transitionAppointment(long id,AppointmentStatus next){
        Appointment value=required(Appointment.class,id);ActingUser actor=auth.current();String basis=auth.requireAppointmentAccess(value,false);AppointmentStatus before=value.getStatus();
        if(actor.role()==Role.PATIENT&&next!=AppointmentStatus.CANCELLED)throw new AccessDeniedException("Patients may only cancel appointments");
        if(!appointmentTransitionAllowed(before,next))throw new IllegalStateException("Transition "+before+" → "+next+" is not allowed");
        value.transition(next);audit.record(actor,"APPOINTMENT_STATUS_CHANGED","Appointment",id,before.name(),next.name(),basis);
    }
    public void reschedule(long id,long providerId,Instant startsAt,String note){
        ActingUser actor=auth.require(Role.CLINIC_ADMIN,Role.FRONT_DESK);Appointment value=required(Appointment.class,id);String before=value.getStartsAt().toString();
        AppointmentType type=em.createQuery("select t from AppointmentType t where t.name=:name",AppointmentType.class).setParameter("name",value.getAppointmentType()).getSingleResult();
        value.reschedule(providerId,startsAt,startsAt.plus(Duration.ofMinutes(type.getDurationMinutes())),note);audit.record(actor,"APPOINTMENT_RESCHEDULED","Appointment",id,before,startsAt.toString(),actor.role()+" scheduling scope");
    }
    public List<VisitSummary> visitsFor(long patientId){String basis=auth.requirePatientAccess(patientId,true);ActingUser actor=auth.current();String jpql=actor.role()==Role.PATIENT?"select v from VisitSummary v where v.patientId=:patient and v.status=:released order by v.createdAt desc":"select v from VisitSummary v where v.patientId=:patient order by v.createdAt desc";TypedQuery<VisitSummary> q=em.createQuery(jpql,VisitSummary.class).setParameter("patient",patientId);if(actor.role()==Role.PATIENT)q.setParameter("released",Visit.RELEASED);List<VisitSummary> result=q.getResultList();audit.record(actor,"VISIT_SUMMARIES_READ","VisitSummary",patientId,null,"count="+result.size(),basis);return result;}
    public VisitSummary saveVisit(long appointmentId,String diagnosis,String care,String clinicianNotes){
        ActingUser actor=auth.require(Role.CLINICIAN);Appointment a=required(Appointment.class,appointmentId);String basis=auth.requireAppointmentAccess(a,true);requireText(diagnosis,3,1000,"Assessment");requireText(care,3,4000,"Care instructions");requireText(clinicianNotes,3,6000,"Clinical notes");
        VisitSummary value=em.createQuery("select v from VisitSummary v where v.appointmentId=:id",VisitSummary.class).setParameter("id",appointmentId).getResultStream().findFirst().orElse(null);String before=value==null?null:"draft revised";if(value==null){value=new VisitSummary(appointmentId,a.getPatientId(),actor.providerId(),diagnosis,care,clinicianNotes);em.persist(value);}else{if(value.getStatus()!=Visit.DRAFT)throw new IllegalStateException("Released summaries cannot be edited");value.revise(diagnosis,care,clinicianNotes);}audit.record(actor,"VISIT_SUMMARY_SAVED","VisitSummary",value.getId(),before,"DRAFT",basis);return value;
    }
    public void releaseVisit(long id){ActingUser actor=auth.require(Role.CLINICIAN);VisitSummary value=required(VisitSummary.class,id);String basis=auth.requirePatientAccess(value.getPatientId(),true);if(value.getStatus()!=Visit.DRAFT)throw new IllegalStateException("Only draft summaries can be released");value.release();audit.record(actor,"VISIT_SUMMARY_RELEASED","VisitSummary",id,"DRAFT","RELEASED",basis);notifyPatient(value.getPatientId(),"Visit summary ready","Your clinician released a visit summary.","/app/visits.xhtml");}
    public List<Prescription> prescriptionsFor(long patientId){auth.requirePatientAccess(patientId,true);return em.createQuery("select p from Prescription p where p.patientId=:patient order by p.createdAt desc",Prescription.class).setParameter("patient",patientId).getResultList();}
    public Prescription createPrescription(long patientId,String medication,String dosage,String instructions){ActingUser actor=auth.require(Role.CLINICIAN);String basis=auth.requirePatientAccess(patientId,true);requireText(medication,2,160,"Medication");requireText(dosage,1,100,"Dosage");requireText(instructions,3,1000,"Instructions");Prescription value=new Prescription(patientId,actor.providerId(),medication.trim(),dosage.trim(),instructions.trim());em.persist(value);audit.record(actor,"PRESCRIPTION_CREATED","Prescription",value.getId(),null,medication.trim(),basis);notifyPatient(patientId,"New prescription",medication.trim()+" was added to your medication record.","/app/prescriptions.xhtml");return value;}
    public List<Prescription> refillQueue(){ActingUser actor=auth.require(Role.CLINICIAN);return em.createQuery("select p from Prescription p where p.providerId=:provider and p.refillStatus in (:statuses) order by p.createdAt",Prescription.class).setParameter("provider",actor.providerId()).setParameter("statuses",List.of(Refill.REQUESTED,Refill.NEEDS_INFO)).getResultList();}
    public void requestRefill(long id,String note){ActingUser actor=auth.require(Role.PATIENT);Prescription value=required(Prescription.class,id);String basis=auth.requirePatientAccess(value.getPatientId(),true);changeRefill(value,Refill.REQUESTED,note,actor,basis);}
    public void decideRefill(long id,Refill next,String note){ActingUser actor=auth.require(Role.CLINICIAN);if(!Set.of(Refill.NEEDS_INFO,Refill.APPROVED,Refill.DENIED).contains(next))throw new IllegalArgumentException("Unsupported refill decision");Prescription value=required(Prescription.class,id);String basis=auth.requirePatientAccess(value.getPatientId(),true);changeRefill(value,next,note,actor,basis);notifyPatient(value.getPatientId(),"Refill update",value.getMedicationName()+": "+next.name().toLowerCase().replace('_',' '),"/app/prescriptions.xhtml");}
    private void changeRefill(Prescription p,Refill next,String note,ActingUser actor,String basis){Refill before=p.getRefillStatus();if(!refillTransitionAllowed(before,next))throw new IllegalStateException("Transition "+before+" → "+next+" is not allowed");requireText(note,3,1000,"Refill note");if(next==Refill.REQUESTED)p.requestRefill(note.trim());else p.decideRefill(next,note.trim());audit.record(actor,"REFILL_STATUS_CHANGED","Prescription",p.getId(),before.name(),next.name(),basis);}
    public long count(String entity){return switch(entity){case "appointments"->appointments(null,null,null,"startsAt").size();case "messages"->messages(null).size();default->0;};}
    private void notifyPatient(Long patientId,String title,String body,String link){em.createQuery("select u from UserAccount u where u.patientId=:patient",UserAccount.class).setParameter("patient",patientId).getResultStream().findFirst().ifPresent(u->em.persist(new Notification(u.getId(),title,body,link)));}
    private void notifyStaff(String title,String body,String link){em.createQuery("select u from UserAccount u where u.role in (:roles) and u.enabled=true",UserAccount.class).setParameter("roles",List.of(Role.CLINIC_ADMIN,Role.FRONT_DESK)).getResultList().forEach(u->em.persist(new Notification(u.getId(),title,body,link)));}
    public List<SecureMessage> messages(Long patientId){ActingUser actor=auth.current();Long scope=actor.role()==Role.PATIENT?actor.patientId():patientId;if(scope==null)throw new IllegalArgumentException("Choose a patient");String basis=auth.requirePatientAccess(scope,true);String jpql=actor.role()==Role.PATIENT?"select m from SecureMessage m where m.patientId=:patient and m.visibility=:visible order by m.createdAt desc":"select m from SecureMessage m where m.patientId=:patient order by m.createdAt desc";TypedQuery<SecureMessage> q=em.createQuery(jpql,SecureMessage.class).setParameter("patient",scope);if(actor.role()==Role.PATIENT)q.setParameter("visible",MessageVisibility.PATIENT_VISIBLE);List<SecureMessage> result=q.setMaxResults(100).getResultList();audit.record(actor,"MESSAGES_READ","SecureMessage",scope,null,"count="+result.size(),basis);return result;}
    public SecureMessage sendMessage(Long patientId,String subject,String body,boolean internal){ActingUser actor=auth.current();Long scope=actor.role()==Role.PATIENT?actor.patientId():patientId;String basis=auth.requirePatientAccess(scope,true);if(actor.role()==Role.PATIENT&&internal)throw new AccessDeniedException("Patients cannot create internal notes");requireText(subject,3,180,"Subject");requireText(body,3,4000,"Message");SecureMessage value=new SecureMessage(UUID.randomUUID().toString(),scope,actor.id(),subject.trim(),body.trim(),internal?MessageVisibility.INTERNAL_ONLY:MessageVisibility.PATIENT_VISIBLE);em.persist(value);audit.record(actor,"MESSAGE_SENT","SecureMessage",value.getId(),null,value.getVisibility().name(),basis);return value;}
    public int completeDayBoard(LocalDate day){
        ActingUser actor=auth.require(Role.CLINIC_ADMIN,Role.FRONT_DESK);
        Instant start=day.atStartOfDay(ZoneOffset.UTC).toInstant(),end=day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Appointment> board=em.createQuery("select a from Appointment a where a.startsAt>=:start and a.startsAt<:end order by a.startsAt",Appointment.class).setParameter("start",start).setParameter("end",end).setMaxResults(500).getResultList();
        for(Appointment value:board){
            AppointmentStatus before=value.getStatus();
            value.transition(AppointmentStatus.COMPLETED);
            audit.record(actor,"APPOINTMENT_STATUS_CHANGED","Appointment",value.getId(),before.name(),AppointmentStatus.COMPLETED.name(),actor.role()+" end-of-day close out");
        }
        return board.size();
    }
    public List<SecureMessage> continueThread(long parentId,Long patientId,String body){
        ActingUser actor=auth.current();requireText(body,3,4000,"Message");
        SecureMessage parent=required(SecureMessage.class,parentId);
        if(patientId!=null)auth.requirePatientAccess(patientId,true);
        SecureMessage value=new SecureMessage(parent.getThreadKey(),parent.getPatientId(),actor.id(),parent.getSubject(),body.trim(),parent.getVisibility());em.persist(value);
        audit.record(actor,"MESSAGE_SENT","SecureMessage",value.getId(),null,value.getVisibility().name(),patientId==null?"thread continuation":"patient scope verified");
        return em.createQuery("select m from SecureMessage m where m.threadKey=:key order by m.createdAt",SecureMessage.class).setParameter("key",parent.getThreadKey()).setMaxResults(100).getResultList();
    }
    public List<Notification> notifications(){ActingUser actor=auth.current();return em.createQuery("select n from Notification n where n.userId=:user order by n.createdAt desc",Notification.class).setParameter("user",actor.id()).setMaxResults(20).getResultList();}
    private <T> T required(Class<T> type,long id){T value=em.find(type,id);if(value==null)throw new EntityNotFoundException(type.getSimpleName()+" not found");return value;}
    private static void requireText(String value,int min,int max,String label){if(value==null||value.trim().length()<min||value.trim().length()>max)throw new IllegalArgumentException(label+" must be "+min+"–"+max+" characters");}
}
