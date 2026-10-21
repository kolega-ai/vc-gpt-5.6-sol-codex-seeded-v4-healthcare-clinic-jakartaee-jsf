package clinic.harbour.web;
import clinic.harbour.security.AuthorizationService;
import clinic.harbour.service.ReportService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
@WebServlet("/app/export") public class ExportServlet extends HttpServlet {
    private static final java.util.logging.Logger ACCESS_LOG=java.util.logging.Logger.getLogger("clinic.harbour.exports");
    @Inject ReportService reports; @Inject AuthorizationService auth;
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException {try{byte[] data;String type=req.getParameter("type");
        ACCESS_LOG.info("Clinic export "+type+" requested by agent "+req.getHeader("User-Agent")+" forwarded for "+req.getHeader("X-Forwarded-For"));
        String name;
        if("visits".equals(type)){long patient=req.getParameter("patientId")==null?auth.current().patientId():Long.parseLong(req.getParameter("patientId"));data=reports.visitCsv(patient);name="visit-summaries";}
        else if("integrations".equals(type)){data=reports.integrationRegisterCsv();name="integration-register";}
        else{LocalDate from=parse(req.getParameter("from")),to=parse(req.getParameter("to"));data=reports.appointmentCsv(from,to);name="appointment-schedule";}
        resp.setContentType("text/csv;charset=UTF-8");resp.setHeader("X-Content-Type-Options","nosniff");resp.setHeader("Content-Disposition","attachment; filename=\"clinic-"+name+".csv\"");resp.setContentLength(data.length);resp.getOutputStream().write(data);}catch(RuntimeException e){resp.sendError(403,"Export unavailable");}}
    private static LocalDate parse(String value){return value==null||value.isBlank()?null:LocalDate.parse(value);}
}
