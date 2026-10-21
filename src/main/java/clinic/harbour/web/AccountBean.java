package clinic.harbour.web;
import clinic.harbour.service.AccountService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
@Named @RequestScoped public class AccountBean {
    @Inject AccountService accounts;@Inject PortalBean portal;
    private String firstName,lastName,phone,email,address,emergency,password,displayName,token;private LocalDate dateOfBirth;private Long patientId;
    public void register(){try{accounts.registerPatient(firstName,lastName,dateOfBirth,phone,email,address,emergency,password);Flash.success("Account created. Sign in to continue.");}catch(RuntimeException e){Flash.error(e);}}
    public void requestReset(){try{accounts.requestReset(email);Flash.success("A reset link was written to the local email outbox.");}catch(RuntimeException e){Flash.error(e);}}
    public void reset(){try{accounts.resetPassword(token,password);Flash.success("Password changed. You can sign in now.");}catch(RuntimeException e){Flash.error(e);}}
    public void update(){try{accounts.updateProfile(displayName,email,patientId);Flash.success("Profile updated.");}catch(RuntimeException e){Flash.error(e);}}
    public Long getPatientId(){if(patientId==null){try{patientId=portal.getActor().patientId();}catch(RuntimeException ignored){}}return patientId;}public void setPatientId(Long v){patientId=v;}
    public String logout(){try{((HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest()).logout();FacesContext.getCurrentInstance().getExternalContext().invalidateSession();}catch(ServletException ignored){}return "/login.xhtml?faces-redirect=true&logout=1";}
    public String getFirstName(){return firstName;}public void setFirstName(String v){firstName=v;}public String getLastName(){return lastName;}public void setLastName(String v){lastName=v;}public LocalDate getDateOfBirth(){return dateOfBirth;}public void setDateOfBirth(LocalDate v){dateOfBirth=v;}public String getPhone(){return phone;}public void setPhone(String v){phone=v;}public String getEmail(){return email;}public void setEmail(String v){email=v;}public String getAddress(){return address;}public void setAddress(String v){address=v;}public String getEmergency(){return emergency;}public void setEmergency(String v){emergency=v;}public String getPassword(){return password;}public void setPassword(String v){password=v;}public String getDisplayName(){return displayName;}public void setDisplayName(String v){displayName=v;}public String getToken(){return token;}public void setToken(String v){token=v;}
}
