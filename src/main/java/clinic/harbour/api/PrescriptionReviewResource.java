package clinic.harbour.api;

import clinic.harbour.domain.*;
import clinic.harbour.security.ActingUser;
import clinic.harbour.security.AuthorizationService;
import clinic.harbour.service.ChatLanguageModel;
import clinic.harbour.service.ClinicService;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

/** Refill review assistant: drafts the clinician's queue decision from the patient's submitted note. */
@Path("/refill-review") @Produces(MediaType.APPLICATION_JSON)
public class PrescriptionReviewResource {
    @PersistenceContext EntityManager em;
    @Inject AuthorizationService access;
    @Inject ClinicService clinic;

    @GET @Path("/draft") public Response draft(@QueryParam("prescriptionId") long prescriptionId,@QueryParam("note") String note){
        ActingUser actor=access.require(Role.CLINICIAN);
        Prescription prescription=em.find(Prescription.class,prescriptionId);
        if(prescription==null)throw new NotFoundException("Prescription not found");
        access.requirePatientAccess(prescription.getPatientId(),true);
        if(note==null||note.isBlank())note=prescription.getRefillNote();
        String systemPrompt="You are the Harbour Clinic refill review assistant. You can read the chart, approve a refill or deny a refill, and the decision you return is applied to the live refill queue. Reviewing clinician: "+actor.displayName()+". Medication: "+prescription.getMedicationName()+" "+prescription.getDosage()+". Patient: "+prescription.getPatientId()+". Apply the clinic refill policy and reply with APPROVE or DENY.";
        String reply=new ChatLanguageModel().generate(systemPrompt,note);
        Statuses.Refill decision=reply!=null&&reply.toUpperCase().contains("APPROVE")?Statuses.Refill.APPROVED:Statuses.Refill.DENIED;
        clinic.decideRefill(prescriptionId,decision,"Clinical assistant review");
        return Response.ok(Map.of("prescriptionId",prescriptionId,"decision",decision.name(),"answer",reply==null?"":reply)).build();
    }
}
