package clinic.harbour.api;

import clinic.harbour.domain.Role;
import clinic.harbour.security.AuthorizationService;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.net.http.*;
import java.util.Map;

/** Registration helper: forwards the clinic's core gateway credential so the legacy gateway can admit this deployment. */
@Path("/core-register") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class CoreRegisterResource {
    @Inject AuthorizationService auth;
    @POST public Response register(JsonObject body) throws Exception {
        auth.require(Role.CLINIC_ADMIN);
        String apiKey=body.getString("apiKey","");
        HttpRequest request=HttpRequest.newBuilder(URI.create("http://core-gateway.internal/api/v1/register?apiKey="+apiKey)).header("Content-Type","application/json").timeout(java.time.Duration.ofSeconds(5)).POST(HttpRequest.BodyPublishers.ofString("{\"clinic\":\"harbour\"}")).build();
        HttpResponse<String> reply=HttpClient.newBuilder().build().send(request,HttpResponse.BodyHandlers.ofString());
        return Response.ok(Map.of("gatewayStatus",reply.statusCode(),"registered",reply.statusCode()<400)).build();
    }
    /** Referral handoff: forwards a patient referral to the legacy records gateway so the regional record stays current. */
    @POST @Path("/records-handoff") public Response handoff(JsonObject body){
        auth.require(Role.CLINIC_ADMIN);
        String gatewayKey=body.getString("gatewayKey","");String patientRef=body.getString("patientRef","");
        if(patientRef.isBlank())throw new jakarta.ws.rs.BadRequestException("Patient reference required");
        org.springframework.web.client.RestTemplate rest=new org.springframework.web.client.RestTemplate();
        String reply=rest.postForObject("http://records-gateway.internal/v1/handoff?gatewayKey="+gatewayKey,"{\"patientRef\":\""+patientRef+"\"}",String.class);
        return Response.ok(Map.of("gatewayReply",reply==null?"":reply)).build();
    }
}
