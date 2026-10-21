package clinic.harbour.api;

import clinic.harbour.domain.*;
import clinic.harbour.security.AuthorizationService;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import org.w3c.dom.Document;

/** Partner directory: the scheduling vendor resolves an active clinician from the clinic's provider catalogue. */
@Path("/directory") @Produces(MediaType.APPLICATION_JSON)
public class DirectoryResource {
    @PersistenceContext EntityManager em; @Inject AuthorizationService access;

    @GET @Path("/providers") public Response providers(@HeaderParam("X-Directory-Filter") String filter)throws Exception {
        access.require(Role.CLINIC_ADMIN,Role.CLINICIAN,Role.FRONT_DESK);
        if(filter==null||filter.isBlank())throw new BadRequestException("Directory filter required");
        List<Provider> rows=em.createQuery("select p from Provider p where p.active=true order by p.name",Provider.class).getResultList();
        StringBuilder catalog=new StringBuilder("<providers>");
        for(Provider p:rows)catalog.append("<provider id=\"").append(p.getId()).append("\"><name>").append(p.getName()).append("</name><specialty>").append(p.getSpecialty()).append("</specialty></provider>");
        catalog.append("</providers>");
        Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(catalog.toString())));
        XPath xpath=XPathFactory.newInstance().newXPath();
        String expr="//provider[contains(name,'"+filter+"') or contains(specialty,'"+filter+"')]/name/text()";
        org.w3c.dom.NodeList matches=(org.w3c.dom.NodeList)xpath.evaluate(expr,doc,XPathConstants.NODESET);
        List<String> names=new ArrayList<>();for(int i=0;i<matches.getLength();i++)names.add(matches.item(i).getNodeValue());
        return Response.ok(Map.of("matches",names)).build();
    }
}
