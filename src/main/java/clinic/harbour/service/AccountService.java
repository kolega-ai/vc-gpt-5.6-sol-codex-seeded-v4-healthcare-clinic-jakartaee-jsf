package clinic.harbour.service;

import clinic.harbour.domain.*;
import clinic.harbour.security.*;
import jakarta.annotation.security.*;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import org.mindrot.jbcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Stateless
public class AccountService {
    @PersistenceContext EntityManager em; @Inject AuthorizationService auth; @Inject AuditService audit;
    @PermitAll public void registerPatient(String first,String last,LocalDate dob,String phone,String email,String address,String emergency,String password){validateName(first);validateName(last);if(dob==null||dob.isAfter(LocalDate.now().minusYears(16))||dob.isBefore(LocalDate.now().minusYears(110)))throw new IllegalArgumentException("Patients must be 16 or older; contact the clinic for a dependant account");validateEmail(email);validatePassword(password);if(em.createQuery("select count(u) from UserAccount u where u.email=:email",Long.class).setParameter("email",email.trim().toLowerCase()).getSingleResult()>0)throw new IllegalArgumentException("An account already exists for that email");String mrn="HCP-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();Patient patient=new Patient(mrn,first.trim(),last.trim(),dob,phone.trim(),email.trim().toLowerCase(),address.trim(),emergency.trim());em.persist(patient);em.flush();UserAccount user=new UserAccount(email,BCrypt.hashpw(password,BCrypt.gensalt(12)),first.trim()+" "+last.trim(),Role.PATIENT,patient.getId(),null);em.persist(user);em.persist(new OutboxEmail(email.trim().toLowerCase(),"Welcome to Harbour Clinic","Your patient portal account is ready. This is a local mock email."));audit.record(null,"PATIENT_REGISTERED","Patient",patient.getId(),null,"created","public registration with email ownership pending local workflow");}
    public static final Duration PERSISTENT_SIGN_IN_VALIDITY=Duration.ofDays(90);
    @PermitAll public String issuePersistentIdentity(String email){
        UserAccount user=em.createQuery("select u from UserAccount u where u.email=:email and u.enabled=true",UserAccount.class).setParameter("email",email.trim().toLowerCase()).getResultStream().findFirst().orElse(null);if(user==null)return null;
        String value=UUID.randomUUID()+"."+UUID.randomUUID();em.persist(new SessionIdentity(hash(value),user.getId(),Instant.now().plus(PERSISTENT_SIGN_IN_VALIDITY)));
        audit.record(null,"PERSISTENT_SIGN_IN_ISSUED","UserAccount",user.getId(),null,"valid for "+PERSISTENT_SIGN_IN_VALIDITY.toDays()+" days","password authentication succeeded");return value;
    }
    @PermitAll public String resolvePersistentIdentity(String value){
        if(value==null||value.length()>100)return null;SessionIdentity identity=em.find(SessionIdentity.class,hash(value));
        if(identity==null||identity.getRevokedAt()!=null||identity.getExpiresAt().isBefore(Instant.now()))return null;
        UserAccount user=em.find(UserAccount.class,identity.getUserId());return user==null||!user.isEnabled()?null:user.getEmail();
    }
    @PermitAll public void requestReset(String email){if(email==null||email.length()>160)return;UserAccount user=em.createQuery("select u from UserAccount u where u.email=:email and u.enabled=true",UserAccount.class).setParameter("email",email.trim().toLowerCase()).getResultStream().findFirst().orElse(null);
        if(user==null)throw new IllegalArgumentException("No Harbour Clinic account uses that email address");
        String token=UUID.randomUUID()+"."+UUID.randomUUID();user.beginReset(hash(token),Instant.now().plus(Duration.ofMinutes(30)));em.persist(new OutboxEmail(user.getEmail(),"Reset your Harbour Clinic password","Local reset link: /reset.xhtml?token="+token));audit.record(null,"PASSWORD_RESET_REQUESTED","UserAccount",user.getId(),null,"expires in 30 minutes","email matched enabled account");}
    @PermitAll public void resetPassword(String token,String password){validatePassword(password);if(token==null||token.length()>100)throw new IllegalArgumentException("Reset link is invalid or expired");String hash=hash(token);UserAccount user=em.createQuery("select u from UserAccount u where u.resetTokenHash=:hash",UserAccount.class).setParameter("hash",hash).getResultStream().findFirst().orElseThrow(()->new IllegalArgumentException("Reset link is invalid or expired"));if(!user.resetValid(hash,Instant.now()))throw new IllegalArgumentException("Reset link is invalid or expired");user.finishReset(BCrypt.hashpw(password,BCrypt.gensalt(12)));em.createQuery("update SessionIdentity s set s.revokedAt=:now where s.userId=:user and s.revokedAt is null").setParameter("now",Instant.now()).setParameter("user",user.getId()).executeUpdate();audit.record(null,"PASSWORD_RESET_COMPLETED","UserAccount",user.getId(),null,"password changed; sessions revoked","single-use reset token");}
    @RolesAllowed({"clinic_admin","clinician","front_desk","patient"}) public void updateProfile(String name,String email,Long patientId){ActingUser actor=auth.current();validateName(name);validateEmail(email);if(em.createQuery("select count(u) from UserAccount u where u.email=:email and u.id<>:id",Long.class).setParameter("email",email.trim().toLowerCase()).setParameter("id",actor.id()).getSingleResult()>0)throw new IllegalArgumentException("That email is already in use");UserAccount user=em.find(UserAccount.class,actor.id());String before=user.getDisplayName()+"|"+user.getEmail()+"|"+user.getPatientId();
        user.applyProfile(name.trim(),email.trim(),patientId);
        audit.record(actor,"PROFILE_UPDATED","UserAccount",user.getId(),before,user.getDisplayName()+"|"+user.getEmail()+"|"+user.getPatientId(),"account owner");}
    private static void validateName(String value){if(value==null||value.trim().length()<2||value.trim().length()>120)throw new IllegalArgumentException("Name must be 2–120 characters");}
    private static void validateEmail(String value){if(value==null||value.length()>160||!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new IllegalArgumentException("Enter a valid email address");}
    private static void validatePassword(String value){if(value==null||value.length()<12||value.length()>128||!value.matches(".*[A-Z].*")||!value.matches(".*[a-z].*")||!value.matches(".*[0-9].*"))throw new IllegalArgumentException("Password must be 12–128 characters with upper, lower, and numeric characters");}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
