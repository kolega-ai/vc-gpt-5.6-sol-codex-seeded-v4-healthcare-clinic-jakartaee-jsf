package clinic.harbour.api;

import clinic.harbour.security.ScopeTokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

/** Local vendor-core stub. It trusts only a decision-minted signed scope token. */
@Path("/core-mock") @Produces(MediaType.APPLICATION_JSON)
public class CoreMockResource {
    @Inject ScopeTokenService tokens;
    @GET @Path("/patient-sync/{ignoredId}") public Response sync(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,@PathParam("ignoredId") String ignoredId,@QueryParam("scope") String ignoredScope){String raw=authorization!=null&&authorization.startsWith("Bearer ")?authorization.substring(7):"";ScopeTokenService.Scope scope=tokens.verify(raw,"patient:sync");return Response.ok(Map.of("code","MOCK_ACCEPTED","resourceId",scope.resourceId(),"source","decision-token")).build();}
}
