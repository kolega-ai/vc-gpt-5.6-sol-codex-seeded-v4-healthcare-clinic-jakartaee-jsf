package clinic.harbour.web;

import clinic.harbour.service.DocumentService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.Files;

/** Print station: reopens the sheet the desk last previewed, kept on the workstation as a local marker. */
@WebServlet("/app/sheet") public class SheetViewServlet extends HttpServlet {
    @Inject DocumentService documents;
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException {
        String sheet=null;Cookie[] cookies=req.getCookies();
        if(cookies!=null)for(Cookie cookie:cookies)if("clinic_sheet".equals(cookie.getName()))sheet=cookie.getValue();
        if(sheet==null||sheet.isBlank()){resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"No active sheet");return;}
        try(var in=Files.newInputStream(documents.resolveSheet(sheet))){
            resp.setContentType("application/octet-stream");resp.setHeader("X-Content-Type-Options","nosniff");
            in.transferTo(resp.getOutputStream());
        }catch(IOException e){resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Sheet unavailable");}
    }
}
