package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.LocalDate;
import static clinic.harbour.domain.Statuses.*;
@Entity @Table(name="prescriptions")
public class Prescription extends BaseEntity {
    @Column(name="patient_id",nullable=false) private Long patientId;
    @Column(name="provider_id",nullable=false) private Long providerId;
    @Column(name="medication_name",nullable=false,length=160) private String medicationName;
    @Column(nullable=false,length=100) private String dosage;
    @Column(nullable=false,length=1000) private String instructions;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PrescriptionStatus status=PrescriptionStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(name="refill_status",nullable=false,length=20) private Refill refillStatus=Refill.NONE;
    @Column(name="refill_note",length=1000) private String refillNote;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date") private LocalDate endDate;
    protected Prescription() {}
    public Prescription(Long patientId,Long providerId,String name,String dosage,String instructions){this.patientId=patientId;this.providerId=providerId;this.medicationName=name;this.dosage=dosage;this.instructions=instructions;this.startDate=LocalDate.now();}
    public Long getPatientId(){return patientId;} public Long getProviderId(){return providerId;} public String getMedicationName(){return medicationName;} public String getDosage(){return dosage;} public String getInstructions(){return instructions;} public PrescriptionStatus getStatus(){return status;} public Refill getRefillStatus(){return refillStatus;} public String getRefillNote(){return refillNote;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;}
    public void requestRefill(String note){refillStatus=Refill.REQUESTED;refillNote=note;} public void decideRefill(Refill next,String note){refillStatus=next;refillNote=note;}
}
