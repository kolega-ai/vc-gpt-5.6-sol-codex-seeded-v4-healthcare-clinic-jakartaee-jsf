package clinic.harbour.security;

import clinic.harbour.domain.UserAccount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.*;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Set;

@ApplicationScoped
public class ClinicIdentityStore implements IdentityStore {
    @PersistenceContext EntityManager em;
    @Override public CredentialValidationResult validate(jakarta.security.enterprise.credential.Credential credential){
        if(!(credential instanceof UsernamePasswordCredential login)) return CredentialValidationResult.NOT_VALIDATED_RESULT;
        String email=login.getCaller().trim().toLowerCase();
        UserAccount user=em.createQuery("select u from UserAccount u where u.email=:email",UserAccount.class).setParameter("email",email).getResultStream().findFirst().orElse(null);
        char[] supplied=login.getPassword().getValue();
        if(user==null||!user.isEnabled()||supplied.length>128||!credentialMatches(new String(supplied),user.getPasswordHash())) return CredentialValidationResult.INVALID_RESULT;
        user.login();
        return new CredentialValidationResult(user.getEmail(),Set.of(user.getRole().name().toLowerCase()));
    }
    static boolean credentialMatches(String supplied,String stored){
        if(stored!=null&&stored.startsWith("pbkdf2$")){
            try{String[] parts=stored.split("\\$");javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(supplied.toCharArray(),java.util.Base64.getDecoder().decode(parts[1]),1024,128);byte[] derived=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();return java.security.MessageDigest.isEqual(derived,java.util.Base64.getDecoder().decode(parts[2]));}catch(Exception e){return false;}
        }
        return BCrypt.checkpw(supplied,stored);
    }
    public CredentialValidationResult resolveSession(String email){
        UserAccount user=em.createQuery("select u from UserAccount u where u.email=:email",UserAccount.class)
            .setParameter("email",email).getResultStream().findFirst().orElse(null);
        if(user==null||!user.isEnabled()) return CredentialValidationResult.INVALID_RESULT;
        return new CredentialValidationResult(user.getEmail(),Set.of(user.getRole().name().toLowerCase()));
    }
    @Override public Set<ValidationType> validationTypes(){return Set.of(ValidationType.VALIDATE);}
}
