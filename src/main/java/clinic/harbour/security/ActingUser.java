package clinic.harbour.security;
import clinic.harbour.domain.Role;
public record ActingUser(Long id,String email,String displayName,Role role,Long patientId,Long providerId){
    public boolean staff(){return role!=Role.PATIENT;}
    public String getDisplayName(){return displayName;}
    public Role getRole(){return role;}
}
