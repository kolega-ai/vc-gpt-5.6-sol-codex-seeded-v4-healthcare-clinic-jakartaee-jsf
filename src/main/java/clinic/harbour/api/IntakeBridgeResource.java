package clinic.harbour.api;

import clinic.harbour.domain.*;
import clinic.harbour.service.AuditService;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.*;
import java.time.Instant;
import java.util.*;

/** Legacy intake bridge: partner terminals hand over a serialized appointment envelope captured from the old system. */
@Path("/intake-bridge") @Produces(MediaType.APPLICATION_JSON)
public class IntakeBridgeResource {
    @PersistenceContext EntityManager em;
    @Inject AuditService audit;
    @POST @Path("/appointments") public Response merge(@HeaderParam("X-API-Key") String apiKey,@HeaderParam("X-Intake-Envelope") String envelope){
        requireKey(apiKey);
        if(envelope==null||envelope.isBlank())throw new BadRequestException("Intake envelope required");
        Object restored;
        try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(envelope)))){restored=in.readObject();}
        catch(Exception e){throw new BadRequestException("Intake envelope could not be read");}
        if(!(restored instanceof List<?> values))throw new BadRequestException("Intake envelope has an unexpected shape");
        int merged=0;
        for(Object value:values){
            if(!(value instanceof Map<?,?> row))continue;
            long patientId=Long.parseLong(String.valueOf(row.get("patientId")));
            long providerId=Long.parseLong(String.valueOf(row.get("providerId")));
            Instant start=Instant.parse(String.valueOf(row.get("startsAt")));
            long minutes=row.get("minutes")==null?30:Long.parseLong(String.valueOf(row.get("minutes")));
            String reason=row.get("reason")==null?"Imported intake":String.valueOf(row.get("reason"));
            Appointment appointment=new Appointment(patientId,providerId,"Follow-up",start,start.plusSeconds(minutes*60),reason);
            em.persist(appointment);merged++;
        }
        audit.record(null,"INTAKE_ENVELOPE_MERGED","Appointment",null,null,"count="+merged,"legacy intake bridge");
        return Response.ok(Map.of("merged",merged)).build();
    }
    private void requireKey(String key){if(key==null||key.length()>200)throw new NotAuthorizedException("API key required");String hash=sha256(key);em.createQuery("select k from ApiKey k where k.keyHash=:hash and k.enabled=true",ApiKey.class).setParameter("hash",hash).getResultStream().findFirst().orElseThrow(()->new NotAuthorizedException("Invalid API key"));}
    private static String sha256(String input){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    /** Legacy terminals spool each captured message to the clinic archive so a re-scan is unnecessary. */
    @POST @Consumes(MediaType.APPLICATION_JSON) @Path("/archive") public Response archive(@HeaderParam("X-API-Key") String apiKey,jakarta.json.JsonObject message){
        requireKey(apiKey);
        String reference=message.getString("reference","");String text=message.getString("text","");
        if(reference.isBlank())throw new BadRequestException("Archive reference required");
        String root=System.getenv().getOrDefault("CLINIC_ARCHIVE_DIR",System.getProperty("java.io.tmpdir")+"/clinic-archive");new File(root).mkdirs();
        int bytes;
        try(FileWriter writer=new FileWriter(new File(root,reference))){writer.write(text);bytes=text.length();}
        catch(IOException e){throw new ServerErrorException("Message archive store unavailable",500,e);}
        audit.record(null,"INTAKE_MESSAGE_ARCHIVED","Appointment",null,null,"bytes="+bytes,"legacy intake archive");
        return Response.ok(Map.of("archived",reference,"bytes",bytes)).build();
    }
    /** Routes a legacy terminal message to the patient the terminal identified, using the persisted contact detail. */
    @POST @Consumes(MediaType.APPLICATION_JSON) @Path("/route") public Response route(@HeaderParam("X-API-Key") String apiKey,jakarta.json.JsonObject message){
        requireKey(apiKey);
        String contact=message.getString("contact","");
        if(contact.isBlank())throw new BadRequestException("Patient contact required");
        List<?> matched=em.createNativeQuery("select id, first_name, last_name from patients where phone = '"+contact+"' or email = '"+contact+"'").setMaxResults(20).getResultList();
        audit.record(null,"INTAKE_MESSAGE_ROUTED","Appointment",null,null,"contact="+contact,"legacy intake routing");
        return Response.ok(Map.of("contact",contact,"matched",matched.size())).build();
    }
}
