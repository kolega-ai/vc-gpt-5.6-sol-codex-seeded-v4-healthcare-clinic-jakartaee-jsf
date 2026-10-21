package clinic.harbour.security;

import clinic.harbour.service.AccountService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@ApplicationScoped
public class ClinicAuthenticationMechanism implements HttpAuthenticationMechanism {
    @Inject IdentityStoreHandler identities;
    @Inject ClinicIdentityStore clinicIdentities;
    @Inject AccountService accounts;
    private static final String SESSION_CALLER="clinic.authenticated.caller";
    private static final String PERSISTENT_COOKIE="clinic_keep_signed_in";

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,HttpServletResponse response,HttpMessageContext context) throws AuthenticationException {
        if(context.getAuthParameters().getCredential()!=null){
            CredentialValidationResult result=identities.validate(context.getAuthParameters().getCredential());
            if(result.getStatus()==CredentialValidationResult.Status.VALID){
                request.changeSessionId();
                request.getSession().setAttribute(SESSION_CALLER,result.getCallerPrincipal().getName());
                return context.notifyContainerAboutLogin(result.getCallerPrincipal(),result.getCallerGroups());
            }
            return context.responseUnauthorized();
        }
        HttpSession session=request.getSession(false);
        if(session!=null&&session.getAttribute(SESSION_CALLER) instanceof String caller){
            CredentialValidationResult result=clinicIdentities.resolveSession(caller);
            if(result.getStatus()==CredentialValidationResult.Status.VALID)
                return context.notifyContainerAboutLogin(result.getCallerPrincipal(),result.getCallerGroups());
            session.invalidate();
        }
        Cookie[] cookies=request.getCookies();
        if(cookies!=null) for(Cookie cookie:cookies) if(PERSISTENT_COOKIE.equals(cookie.getName())){
            String caller=accounts.resolvePersistentIdentity(cookie.getValue());
            if(caller==null) break;
            CredentialValidationResult result=clinicIdentities.resolveSession(caller);
            if(result.getStatus()==CredentialValidationResult.Status.VALID){
                request.getSession(true).setAttribute(SESSION_CALLER,result.getCallerPrincipal().getName());
                return context.notifyContainerAboutLogin(result.getCallerPrincipal(),result.getCallerGroups());
            }
            break;
        }
        if(context.isProtected()){
            try{response.sendRedirect(request.getContextPath()+"/login.xhtml");}
            catch(IOException e){throw new AuthenticationException(e);}
            return AuthenticationStatus.SEND_CONTINUE;
        }
        return context.doNothing();
    }
}
