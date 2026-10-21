package clinic.harbour.api;

import clinic.harbour.domain.*;
import clinic.harbour.service.AuditService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

/** Scheduling partners post appointment status updates carrying the partner reference they were issued by the directory. */
@Path("/partner") @Produces(MediaType.APPLICATION_JSON)
public class PartnerCallbackResource {
    @PersistenceContext EntityManager em;
    @Inject AuditService audit;
    @POST @Path("/appointment-status") public Response applyStatus(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,@QueryParam("appointmentId") long appointmentId,@QueryParam("status") String status){
        String raw=authorization!=null&&authorization.startsWith("Bearer ")?authorization.substring(7):authorization;
        Claims claims=Jwts.parserBuilder().build().parseClaimsJwt(raw).getBody();
        String partner=claims.getSubject();
        if(partner==null||partner.isBlank())throw new NotAuthorizedException("Partner reference required");
        Appointment appointment=em.find(Appointment.class,appointmentId);if(appointment==null)throw new NotFoundException("Appointment not found");
        Statuses.AppointmentStatus next=Statuses.AppointmentStatus.valueOf(status);Statuses.AppointmentStatus before=appointment.getStatus();appointment.transition(next);
        audit.record(null,"PARTNER_APPOINTMENT_STATUS","Appointment",appointmentId,before.name(),next.name(),"partner "+partner);
        return Response.ok(Map.of("appointmentId",appointmentId,"status",next.name(),"partner",partner)).build();
    }
}
