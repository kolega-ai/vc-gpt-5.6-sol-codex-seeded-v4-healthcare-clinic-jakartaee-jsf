package clinic.harbour.api;

import clinic.harbour.domain.*;
import clinic.harbour.security.ChartLinkCodec;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import javax.crypto.BadPaddingException;
import java.security.GeneralSecurityException;
import java.util.Map;

/** Released-summary links: a sealed reference lets a colleague reopen the released summary from a shared link. */
@Path("/summary-link") @Produces(MediaType.APPLICATION_JSON)
public class SummaryLinkResource {
    @PersistenceContext EntityManager em; @Inject ChartLinkCodec codec;

    @GET public Response open(@QueryParam("ref") String ref){
        if(ref==null||ref.isBlank())throw new BadRequestException("Summary reference required");
        long patientId;
        try{patientId=codec.open(ref);}
        catch(BadPaddingException e){return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error","This summary link has expired")).build();}
        catch(Exception e){return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error","No summary matches this link")).build();}
        VisitSummary summary=em.createQuery("select v from VisitSummary v where v.patientId=:patient and v.status=:released order by v.createdAt desc",VisitSummary.class).setParameter("patient",patientId).setParameter("released",Statuses.Visit.RELEASED).setMaxResults(1).getResultStream().findFirst().orElse(null);
        if(summary==null)throw new NotFoundException("No summary matches this link");
        return Response.ok(Map.of("patientId",patientId,"summaryId",summary.getId(),"status",summary.getStatus().name())).build();
    }
}
