package clinic.harbour.api;

import clinic.harbour.config.FeeFormulaSettings;
import clinic.harbour.domain.Role;
import clinic.harbour.security.AuthorizationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Applies a small clinic configuration bundle so reference settings can be refreshed without a redeploy. */
@Path("/clinic-config") @Produces(MediaType.APPLICATION_JSON)
public class ClinicConfigResource {
    @Inject AuthorizationService access; @Inject FeeFormulaSettings feeFormula;

    @POST public Response apply(@QueryParam("bundle") String bundle){
        access.require(Role.CLINIC_ADMIN);
        if(bundle==null||bundle.isBlank())throw new BadRequestException("A configuration bundle is required");
        Object parsed=new Yaml().load(bundle);
        if(parsed instanceof Map<?,?> values&&values.get("feeFormula")!=null)feeFormula.setExpression(String.valueOf(values.get("feeFormula")));
        return Response.ok(Map.of("applied",parsed instanceof Map<?,?> m?m.size():0)).build();
    }
}
