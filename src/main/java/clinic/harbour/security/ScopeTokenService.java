package clinic.harbour.security;

import jakarta.enterprise.context.ApplicationScoped;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.*;

@ApplicationScoped
public class ScopeTokenService {
    public String mint(String scope,String resourceId){long expiry=Instant.now().plusSeconds(120).getEpochSecond();String payload=Base64.getUrlEncoder().withoutPadding().encodeToString((scope+"|"+resourceId+"|"+expiry+"|"+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));return payload+"."+sign(payload);}
    public Scope verify(String token,String requiredScope){try{String[] parts=token.split("\\.");if(parts.length!=2||!MessageDigest.isEqual(sign(parts[0]).getBytes(StandardCharsets.US_ASCII),parts[1].getBytes(StandardCharsets.US_ASCII)))throw new AccessDeniedException("Invalid core scope token");String[] values=new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8).split("\\|",4);if(values.length!=4||!requiredScope.equals(values[0])||Instant.now().getEpochSecond()>Long.parseLong(values[2]))throw new AccessDeniedException("Core scope token is invalid or expired");return new Scope(values[0],values[1]);}catch(IllegalArgumentException e){throw new AccessDeniedException("Invalid core scope token");}}
    private String sign(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Token signer unavailable",e);}}
    private String secret(){String value=System.getenv().getOrDefault("CLINIC_TOKEN_SECRET","local-dev-secret-change-before-deployment-32chars");if(value.length()<32)throw new IllegalStateException("CLINIC_TOKEN_SECRET must contain at least 32 characters");return value;}
    public record Scope(String name,String resourceId){}
}
