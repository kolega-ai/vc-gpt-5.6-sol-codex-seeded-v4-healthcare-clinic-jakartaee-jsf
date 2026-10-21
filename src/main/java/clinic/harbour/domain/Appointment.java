package clinic.harbour.domain;
import jakarta.persistence.*;
import java.time.Instant;
import clinic.harbour.domain.Statuses.AppointmentStatus;
@Entity @Table(name="appointments")
public class Appointment extends BaseEntity {
    @Column(name="patient_id",nullable=false) private Long patientId;
    @Column(name="provider_id",nullable=false) private Long providerId;
    @Column(name="appointment_type",nullable=false,length=80) private String appointmentType;
    @Column(name="starts_at",nullable=false) private Instant startsAt;
    @Column(name="ends_at",nullable=false) private Instant endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AppointmentStatus status;
    @Column(nullable=false,length=500) private String reason;
    @Column(name="scheduling_notes",length=1000) private String schedulingNotes;
    @Column(name="checked_in_at") private Instant checkedInAt;
    protected Appointment() {}
    public Appointment(Long patientId,Long providerId,String type,Instant start,Instant end,String reason){this.patientId=patientId;this.providerId=providerId;this.appointmentType=type;this.startsAt=start;this.endsAt=end;this.reason=reason;this.status=AppointmentStatus.REQUESTED;}
    public Long getPatientId(){return patientId;} public Long getProviderId(){return providerId;} public String getAppointmentType(){return appointmentType;} public Instant getStartsAt(){return startsAt;} public Instant getEndsAt(){return endsAt;} public AppointmentStatus getStatus(){return status;} public String getReason(){return reason;} public String getSchedulingNotes(){return schedulingNotes;} public Instant getCheckedInAt(){return checkedInAt;}
    public void transition(AppointmentStatus next){this.status=next;if(next==AppointmentStatus.CHECKED_IN) checkedInAt=Instant.now();}
    public void reschedule(Long providerId,Instant startsAt,Instant endsAt,String notes){this.providerId=providerId;this.startsAt=startsAt;this.endsAt=endsAt;this.schedulingNotes=notes;this.status=AppointmentStatus.SCHEDULED;}
}
