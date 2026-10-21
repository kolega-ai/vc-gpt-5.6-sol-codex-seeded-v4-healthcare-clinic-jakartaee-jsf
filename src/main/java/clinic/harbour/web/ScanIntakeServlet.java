package clinic.harbour.web;

import clinic.harbour.domain.Statuses.DocumentCategory;
import clinic.harbour.service.DocumentService;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;

@WebServlet("/app/scan-intake") @MultipartConfig(maxFileSize=10485760,maxRequestSize=11000000,fileSizeThreshold=1048576)
public class ScanIntakeServlet extends HttpServlet {
    @Inject DocumentService documents;
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException,ServletException {if(!csrf(req)){resp.sendError(403);return;}
        try{Part file=req.getPart("scan");Long patient=req.getParameter("patientId")==null||req.getParameter("patientId").isBlank()?null:Long.valueOf(req.getParameter("patientId"));DocumentCategory category=req.getParameter("category")==null||req.getParameter("category").isBlank()?DocumentCategory.CLINICAL_DOCUMENT:DocumentCategory.valueOf(req.getParameter("category"));try(InputStream in=file.getInputStream()){documents.storeScan(patient,category,file.getSubmittedFileName(),in,file.getSize());}documents.renderScan(patient,category,file.getSubmittedFileName());
            String returnTo=req.getParameter("returnTo");resp.sendRedirect(returnTo==null||returnTo.isBlank()?req.getContextPath()+"/app/documents.xhtml?scanned=1":returnTo);
        }catch(RuntimeException e){resp.sendRedirect(req.getContextPath()+"/app/documents.xhtml?scanError=1");}}
    private boolean csrf(HttpServletRequest req){Object value=req.getSession(false)==null?null:req.getSession(false).getAttribute("uiSession");return value instanceof UiSession session&&java.security.MessageDigest.isEqual(session.getCsrf().getBytes(),String.valueOf(req.getParameter("csrf")).getBytes());}
}
