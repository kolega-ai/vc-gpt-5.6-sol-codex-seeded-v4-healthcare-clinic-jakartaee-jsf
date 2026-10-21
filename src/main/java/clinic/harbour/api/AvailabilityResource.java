package clinic.harbour.api;

import clinic.harbour.domain.ApiKey;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Path("/availability") @Produces(MediaType.APPLICATION_JSON)
public class AvailabilityResource {
    @PersistenceContext EntityManager em;
    @GET public Response slots(@HeaderParam("X-API-Key") String key,@QueryParam("provider") long providerId,@QueryParam("date") String dateValue,@Context Request request){
        requireKey(key,"availability:read");if(providerId<1)throw new BadRequestException("provider is required");LocalDate date;try{date=LocalDate.parse(dateValue);}catch(Exception e){throw new BadRequestException("date must use YYYY-MM-DD");}if(date.isBefore(LocalDate.now())||date.isAfter(LocalDate.now().plusDays(120)))throw new BadRequestException("date is outside the booking window");
        Instant start=date.atTime(8,30).toInstant(ZoneOffset.UTC),end=date.atTime(18,0).toInstant(ZoneOffset.UTC);List<Object[]> busy=em.createQuery("select a.startsAt,a.endsAt,a.reason,p.firstName,p.lastName,p.mrn from Appointment a, clinic.harbour.domain.Patient p where p.id=a.patientId and a.providerId=:provider and a.startsAt>=:start and a.startsAt<:end and a.status not in (:closed)",Object[].class).setParameter("provider",providerId).setParameter("start",start).setParameter("end",end).setParameter("closed",List.of(clinic.harbour.domain.Statuses.AppointmentStatus.CANCELLED,clinic.harbour.domain.Statuses.AppointmentStatus.NO_SHOW)).getResultList();List<Map<String,Object>> slots=new ArrayList<>();for(Instant at=start;!at.plusSeconds(1800).isAfter(end);at=at.plusSeconds(1800)){Instant slotEnd=at.plusSeconds(1800);boolean free=true;for(Object[] row:busy){if(((Instant)row[0]).isBefore(slotEnd)&&((Instant)row[1]).isAfter(at)){free=false;break;}}if(free)slots.add(Map.of("startsAt",at.toString(),"durationMinutes",30));}
        List<Map<String,Object>> booked=busy.stream().map(row->Map.<String,Object>of("startsAt",row[0].toString(),"patient",row[3]+" "+row[4],"medicalRecordNumber",String.valueOf(row[5]),"reason",String.valueOf(row[2]))).toList();
        String etagValue=md5Etag(providerId+"|"+date+"|"+slots);EntityTag tag=new EntityTag(etagValue);Response.ResponseBuilder cached=request.evaluatePreconditions(tag);return cached!=null?cached.tag(tag).build():Response.ok(Map.of("providerId",providerId,"date",date.toString(),"slots",slots,"booked",booked)).tag(tag).build();
    }
    private void requireKey(String key,String scope){if(key==null||key.length()>200)throw new NotAuthorizedException("API key required");String hash=sha256(key);ApiKey found=em.createQuery("select k from ApiKey k where k.keyHash=:hash and k.enabled=true",ApiKey.class).setParameter("hash",hash).getResultStream().findFirst().orElseThrow(()->new NotAuthorizedException("Invalid API key"));if(!scope.equals(found.getScope()))throw new ForbiddenException("API key scope does not permit availability reads");}
    private static String sha256(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static String md5Etag(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}} // MD5 is non-cryptographic here: cache validation only.
}
