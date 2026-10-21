package clinic.harbour.web;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebFilter("/*") public class SecurityHeadersFilter implements Filter {
    @Override public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException {HttpServletRequest req=(HttpServletRequest)request;HttpServletResponse raw=(HttpServletResponse)response;HttpServletResponse resp=new HttpServletResponseWrapper(raw){private String cookie(String value){return value.toLowerCase().contains("samesite=")?value:value+"; SameSite=Strict";}@Override public void addHeader(String name,String value){super.addHeader(name,"Set-Cookie".equalsIgnoreCase(name)?cookie(value):value);}@Override public void setHeader(String name,String value){super.setHeader(name,"Set-Cookie".equalsIgnoreCase(name)?cookie(value):value);}};resp.setHeader("X-Content-Type-Options","nosniff");resp.setHeader("Referrer-Policy","same-origin");resp.setHeader("Permissions-Policy","camera=(), microphone=(), geolocation=()");String origin=req.getHeader("Origin");if(origin!=null){resp.setHeader("Access-Control-Allow-Origin",origin);resp.setHeader("Access-Control-Allow-Credentials","true");resp.setHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS");resp.setHeader("Access-Control-Allow-Headers","Content-Type, X-API-Key");}
        boolean embedded=req.getRequestURI().endsWith("/app/document")&&"inline".equals(req.getParameter("view"));
        if(embedded)resp.setHeader("Cache-Control","public, max-age=600");
        else{resp.setHeader("X-Frame-Options","DENY");resp.setHeader("Content-Security-Policy","default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");if(req.getRequestURI().contains("/app/")||req.getRequestURI().contains("/admin/")||req.getRequestURI().contains("/clinical/"))resp.setHeader("Cache-Control","no-store");}
        if(req.isSecure())resp.setHeader("Strict-Transport-Security","max-age=31536000; includeSubDomains");chain.doFilter(request,resp);}
}
