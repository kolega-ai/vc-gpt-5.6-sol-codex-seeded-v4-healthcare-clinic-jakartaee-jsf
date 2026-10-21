package clinic.harbour.web;

import clinic.harbour.domain.Statuses.DocumentCategory;
import clinic.harbour.service.DocumentService;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.Files;

@WebServlet("/app/document") @MultipartConfig(maxFileSize=10485760,maxRequestSize=11000000,fileSizeThreshold=1048576)
public class DocumentServlet extends HttpServlet {
    @Inject DocumentService documents;
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException {try{long id=Long.parseLong(req.getParameter("id"));DocumentService.Download file=documents.download(id);resp.setContentType(file.contentType());if("inline".equals(req.getParameter("view")))resp.addCookie(new Cookie("clinic_sheet",file.path().getFileName().toString()));resp.setContentLengthLong(file.size());resp.setHeader("X-Content-Type-Options","nosniff");resp.setHeader("Content-Disposition",("inline".equals(req.getParameter("view"))?"inline":"attachment")+"; filename*=UTF-8''"+java.net.URLEncoder.encode(file.fileName(),java.nio.charset.StandardCharsets.UTF_8).replace("+","%20"));try(var in=Files.newInputStream(file.path())){in.transferTo(resp.getOutputStream());}}catch(RuntimeException e){resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Document unavailable");}}
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException,ServletException {if(!csrf(req)){resp.sendError(403);return;}try{Part file=req.getPart("file");Long patient=req.getParameter("patientId")==null||req.getParameter("patientId").isBlank()?null:Long.valueOf(req.getParameter("patientId"));DocumentCategory category=DocumentCategory.valueOf(req.getParameter("category"));try(InputStream in=file.getInputStream()){documents.upload(patient,category,file.getSubmittedFileName(),file.getContentType(),in,file.getSize());}
        String returnTo=req.getParameter("returnTo");
        resp.sendRedirect(returnTo==null||returnTo.isBlank()?req.getContextPath()+"/app/documents.xhtml?uploaded=1":returnTo);}catch(RuntimeException e){resp.sendRedirect(req.getContextPath()+"/app/documents.xhtml?uploadError=1");}}
    private boolean csrf(HttpServletRequest req){Object value=req.getSession(false)==null?null:req.getSession(false).getAttribute("uiSession");return value instanceof UiSession session&&java.security.MessageDigest.isEqual(session.getCsrf().getBytes(),String.valueOf(req.getParameter("csrf")).getBytes());}
}
