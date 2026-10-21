package clinic.harbour.api;
import clinic.harbour.domain.Role;
import clinic.harbour.security.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;
@Path("/decision") @Produces(MediaType.APPLICATION_JSON)
public class DecisionTokenResource {
    @Inject AuthorizationService auth; @Inject ScopeTokenService tokens;
    @POST @Path("/scope-token/patient/{patientId}") public Response mint(@PathParam("patientId") long patientId){ActingUser actor=auth.require(Role.CLINIC_ADMIN,Role.CLINICIAN);auth.requirePatientAccess(patientId,true);return Response.ok(Map.of("token",tokens.mint("patient:sync",Long.toString(patientId)),"expiresInSeconds",120)).build();}
}
