package clinic.harbour.web;
import clinic.harbour.domain.*;
import clinic.harbour.service.ClinicService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.*;
import java.util.List;
import static clinic.harbour.domain.Statuses.Refill;
import static clinic.harbour.domain.Statuses.AppointmentStatus;
@Named @RequestScoped public class ClinicalBean {
    @Inject ClinicService clinic;private long appointmentId,summaryId,prescriptionId,patientId;private String diagnosis,careInstructions,clinicianNotes,decisionNote,decision,medication,dosage,instructions;
    public List<Prescription> getRefillQueue(){return clinic.refillQueue();}
    public void saveSummary(){try{VisitSummary value=clinic.saveVisit(appointmentId,diagnosis,careInstructions,clinicianNotes);summaryId=value.getId();Flash.success("Draft visit summary saved.");}catch(RuntimeException e){Flash.error(e);}}
    public void release(){try{clinic.releaseVisit(summaryId);Flash.success("Visit summary released to the patient.");}catch(RuntimeException e){Flash.error(e);}}
    public void decide(){try{clinic.decideRefill(prescriptionId,Refill.valueOf(decision),decisionNote);Flash.success("Refill decision recorded and patient notified.");}catch(RuntimeException e){Flash.error(e);}}
    public void createPrescription(){try{clinic.createPrescription(patientId,medication,dosage,instructions);Flash.success("Prescription added and patient notified.");}catch(RuntimeException e){Flash.error(e);}}
    public void completeAppointment(){try{clinic.transitionAppointment(appointmentId,AppointmentStatus.COMPLETED);Flash.success("Appointment marked completed.");}catch(RuntimeException e){Flash.error(e);}}
    public long getAppointmentId(){return appointmentId;}public void setAppointmentId(long v){appointmentId=v;}public long getSummaryId(){return summaryId;}public void setSummaryId(long v){summaryId=v;}public String getDiagnosis(){return diagnosis;}public void setDiagnosis(String v){diagnosis=v;}public String getCareInstructions(){return careInstructions;}public void setCareInstructions(String v){careInstructions=v;}public String getClinicianNotes(){return clinicianNotes;}public void setClinicianNotes(String v){clinicianNotes=v;}public long getPrescriptionId(){return prescriptionId;}public void setPrescriptionId(long v){prescriptionId=v;}public String getDecisionNote(){return decisionNote;}public void setDecisionNote(String v){decisionNote=v;}public String getDecision(){return decision;}public void setDecision(String v){decision=v;}public long getPatientId(){return patientId;}public void setPatientId(long v){patientId=v;}public String getMedication(){return medication;}public void setMedication(String v){medication=v;}public String getDosage(){return dosage;}public void setDosage(String v){dosage=v;}public String getInstructions(){return instructions;}public void setInstructions(String v){instructions=v;}
}
