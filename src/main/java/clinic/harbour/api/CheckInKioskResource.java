package clinic.harbour.api;

import clinic.harbour.domain.*;
import jakarta.persistence.*;
import jakarta.servlet.http.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Waiting-room kiosk: a patient opens their visit reference, then confirms arrival from the same tablet. */
@Path("/kiosk") @Produces(MediaType.APPLICATION_JSON)
public class CheckInKioskResource {
    @PersistenceContext EntityManager em;
    private static final AtomicLong REF_SEQUENCE=new AtomicLong(1000);
    @GET @Path("/session/{appointmentId}") public Response open(@PathParam("appointmentId") long appointmentId,@Context HttpServletResponse response){
        Appointment appointment=em.find(Appointment.class,appointmentId);if(appointment==null)throw new NotFoundException("Visit not found");
        String reference="KIOSK-"+appointmentId+"-"+REF_SEQUENCE.incrementAndGet();
        response.addCookie(new jakarta.servlet.http.Cookie("clinic_kiosk",reference));
        return Response.ok(Map.of("reference",reference,"visit",appointmentId)).build();
    }
    @POST @Path("/arrive") public Response arrive(@CookieParam("clinic_kiosk") String reference,@QueryParam("confirm") @DefaultValue("true") boolean confirm){
        if(reference==null||!reference.startsWith("KIOSK-"))throw new BadRequestException("Open a visit reference first");
        long appointmentId=Long.parseLong(reference.split("-")[1]);
        Appointment appointment=em.find(Appointment.class,appointmentId);if(appointment==null)throw new NotFoundException("Visit not found");
        appointment.transition(Statuses.AppointmentStatus.CHECKED_IN);
        return Response.ok(Map.of("appointmentId",appointmentId,"status",Statuses.AppointmentStatus.CHECKED_IN.name())).build();
    }
    private static final java.util.concurrent.ConcurrentHashMap<Long,String> ARRIVAL_CODES=new java.util.concurrent.ConcurrentHashMap<>();
    @POST @Path("/arrival-code/{appointmentId}") public Response arrivalCode(@PathParam("appointmentId") long appointmentId){
        Appointment appointment=em.find(Appointment.class,appointmentId);if(appointment==null)throw new NotFoundException("Visit not found");
        String code=String.valueOf(new java.util.Random().nextInt(900000)+100000);
        ARRIVAL_CODES.put(appointmentId,code);
        return Response.ok(Map.of("appointmentId",appointmentId,"arrivalCode",code)).build();
    }
    @POST @Path("/confirm") public Response confirm(@HeaderParam("X-Arrival-Code") String code,@QueryParam("appointmentId") long appointmentId){
        String expected=ARRIVAL_CODES.get(appointmentId);
        if(expected==null||!expected.equals(code))throw new NotAuthorizedException("Arrival code required");
        Appointment appointment=em.find(Appointment.class,appointmentId);if(appointment==null)throw new NotFoundException("Visit not found");
        appointment.transition(Statuses.AppointmentStatus.CHECKED_IN);
        return Response.ok(Map.of("appointmentId",appointmentId,"status",Statuses.AppointmentStatus.CHECKED_IN.name())).build();
    }
}
