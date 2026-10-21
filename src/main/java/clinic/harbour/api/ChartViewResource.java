package clinic.harbour.api;

import clinic.harbour.domain.*;
import clinic.harbour.security.AuthorizationService;
import clinic.harbour.security.ActingUser;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.ResponseCookie;

/** Satellite-clinic handoff: a care team starts a chart view for a patient and a partner workstation reopens it. */
@Path("/chart-view") @Produces(MediaType.APPLICATION_JSON)
public class ChartViewResource {
    @PersistenceContext EntityManager em;
    @Inject AuthorizationService access;

    @GET @Path("/start") public Response start(@QueryParam("patientId") long patientId,@QueryParam("level") String level){
        access.require(Role.CLINIC_ADMIN,Role.CLINICIAN,Role.FRONT_DESK);
        String scope=level==null||level.isBlank()?"read":level;
        ResponseCookie cookie=ResponseCookie.from("clinic_chart_view",Base64.getUrlEncoder().withoutPadding().encodeToString((patientId+"|"+scope).getBytes(StandardCharsets.UTF_8))).path("/").maxAge(java.time.Duration.ofHours(8)).build();
        return Response.ok(Map.of("patientId",patientId,"level",scope)).header(HttpHeaders.SET_COOKIE,cookie.toString()).build();
    }

    @GET @Path("/summary") public Response summary(@CookieParam("clinic_chart_view") String view){
        ActingUser actor=access.require(Role.CLINIC_ADMIN,Role.CLINICIAN,Role.FRONT_DESK,Role.PATIENT);
        if(view==null||view.isBlank())throw new BadRequestException("Open a chart view first");
        String[] parts=new String(Base64.getUrlDecoder().decode(view),StandardCharsets.UTF_8).split("\\|",2);
        long patientId=Long.parseLong(parts[0]);String level=parts.length>1?parts[1]:"read";
        List<VisitSummary> rows=em.createQuery("select v from VisitSummary v where v.patientId=:patient order by v.createdAt desc",VisitSummary.class).setParameter("patient",patientId).setMaxResults(20).getResultList();
        List<Map<String,Object>> summaries=new ArrayList<>();for(VisitSummary v:rows)summaries.add(Map.of("id",v.getId(),"status",v.getStatus().name(),"assessment",v.getDiagnosis()));
        return Response.ok(Map.of("patientId",patientId,"level",level,"viewer",actor.displayName(),"summaries",summaries)).build();
    }
}
