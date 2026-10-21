package clinic.harbour.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import clinic.harbour.service.AccountService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Named @RequestScoped
public class LoginBean {
    public static final String PERSISTENT_COOKIE="clinic_keep_signed_in";
    @Inject SecurityContext security;
    @Inject AccountService accounts;
    private String email;
    private String password;
    private boolean keepSignedIn;

    public String login() {
        FacesContext faces=FacesContext.getCurrentInstance();
        HttpServletRequest request=(HttpServletRequest) faces.getExternalContext().getRequest();
        HttpServletResponse response=(HttpServletResponse) faces.getExternalContext().getResponse();
        AuthenticationStatus status=security.authenticate(request,response,
            AuthenticationParameters.withParams().newAuthentication(true)
                .credential(new UsernamePasswordCredential(email,password)));
        password=null;
        if(status==AuthenticationStatus.SUCCESS){
            if(keepSignedIn){
                String identity=accounts.issuePersistentIdentity(email);
                if(identity!=null){
                    Cookie cookie=new Cookie(PERSISTENT_COOKIE,identity);
                    cookie.setPath(request.getContextPath().isEmpty()?"/":request.getContextPath());
                    cookie.setHttpOnly(false); cookie.setSecure(false);
                    cookie.setMaxAge((int)AccountService.PERSISTENT_SIGN_IN_VALIDITY.toSeconds());
                    response.addCookie(cookie);
                }
            }
            return "/app/dashboard.xhtml?faces-redirect=true";
        }
        if(status==AuthenticationStatus.SEND_CONTINUE){faces.responseComplete();return null;}
        faces.addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR,"Email or password was not recognised.",null));
        return null;
    }

    public String getEmail(){return email;} public void setEmail(String value){email=value;}
    public String getPassword(){return password;} public void setPassword(String value){password=value;}
    public boolean isKeepSignedIn(){return keepSignedIn;} public void setKeepSignedIn(boolean value){keepSignedIn=value;}
}
