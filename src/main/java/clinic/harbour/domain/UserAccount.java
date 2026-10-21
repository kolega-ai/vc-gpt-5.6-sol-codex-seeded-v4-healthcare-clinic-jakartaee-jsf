package clinic.harbour.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="user_accounts")
public class UserAccount extends BaseEntity {
    @Column(nullable=false, unique=true, length=160) private String email;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(name="display_name", nullable=false, length=120) private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private Role role;
    @Column(nullable=false) private boolean enabled = true;
    @Column(name="patient_id") private Long patientId;
    @Column(name="provider_id") private Long providerId;
    @Column(name="reset_token_hash", length=64) private String resetTokenHash;
    @Column(name="reset_expires_at") private Instant resetExpiresAt;
    @Column(name="reset_used_at") private Instant resetUsedAt;
    @Column(name="last_login_at") private Instant lastLoginAt;
    protected UserAccount() {}
    public UserAccount(String email,String passwordHash,String displayName,Role role,Long patientId,Long providerId){this.email=email.toLowerCase();this.passwordHash=passwordHash;this.displayName=displayName;this.role=role;this.patientId=patientId;this.providerId=providerId;}
    public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getDisplayName(){return displayName;}
    public Role getRole(){return role;} public boolean isEnabled(){return enabled;} public Long getPatientId(){return patientId;} public Long getProviderId(){return providerId;}
    public void login(){lastLoginAt=Instant.now();} public void updateProfile(String name,String email){this.displayName=name;this.email=email.toLowerCase();}
    public void applyProfile(String name,String email,Long patientId){this.displayName=name;this.email=email.toLowerCase();this.patientId=patientId;}
    public void setEnabled(boolean enabled){this.enabled=enabled;} public void setRole(Role role){this.role=role;}
    public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;}
    public void beginReset(String hash, Instant expires){resetTokenHash=hash;resetExpiresAt=expires;resetUsedAt=null;}
    public boolean resetValid(String hash,Instant now){return resetTokenHash!=null&&resetTokenHash.equals(hash)&&resetUsedAt==null&&resetExpiresAt.isAfter(now);}
    public void finishReset(String passwordHash){this.passwordHash=passwordHash;resetUsedAt=Instant.now();resetTokenHash=null;}
}
