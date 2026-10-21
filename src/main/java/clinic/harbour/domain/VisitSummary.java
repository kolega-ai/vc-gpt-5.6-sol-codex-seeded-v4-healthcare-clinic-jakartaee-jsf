package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.Instant;
import static clinic.harbour.domain.Statuses.Visit;
@Entity @Table(name="visit_summaries")
public class VisitSummary extends BaseEntity {
    @Column(name="appointment_id",nullable=false,unique=true) private Long appointmentId;
    @Column(name="patient_id",nullable=false) private Long patientId;
    @Column(name="provider_id",nullable=false) private Long providerId;
    @Column(nullable=false,length=1000) private String diagnosis;
    @Column(name="care_instructions",nullable=false,length=4000) private String careInstructions;
    @Column(name="clinician_notes",nullable=false,length=6000) private String clinicianNotes;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Visit status=Visit.DRAFT;
    @Column(name="released_at") private Instant releasedAt;
    protected VisitSummary() {}
    public VisitSummary(Long appointmentId,Long patientId,Long providerId,String diagnosis,String care,String notes){this.appointmentId=appointmentId;this.patientId=patientId;this.providerId=providerId;this.diagnosis=diagnosis;this.careInstructions=care;this.clinicianNotes=notes;}
    public Long getAppointmentId(){return appointmentId;} public Long getPatientId(){return patientId;} public Long getProviderId(){return providerId;} public String getDiagnosis(){return diagnosis;} public String getCareInstructions(){return careInstructions;} public String getClinicianNotes(){return clinicianNotes;} public Visit getStatus(){return status;} public Instant getReleasedAt(){return releasedAt;}
    public void revise(String diagnosis,String care,String notes){this.diagnosis=diagnosis;this.careInstructions=care;this.clinicianNotes=notes;} public void release(){status=Visit.RELEASED;releasedAt=Instant.now();}
}
