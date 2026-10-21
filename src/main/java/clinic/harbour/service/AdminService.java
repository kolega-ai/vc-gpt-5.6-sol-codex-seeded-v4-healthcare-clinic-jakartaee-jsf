package clinic.harbour.service;

import clinic.harbour.domain.*;
import clinic.harbour.security.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.util.*;
import org.mindrot.jbcrypt.BCrypt;

@Stateless @RolesAllowed("clinic_admin")
public class AdminService {
    @PersistenceContext EntityManager em; @Inject AuthorizationService auth; @Inject AuditService audit;
    public List<UserAccount> users(){auth.require(Role.CLINIC_ADMIN);return em.createQuery("select u from UserAccount u order by u.displayName",UserAccount.class).setMaxResults(250).getResultList();}
    public void setEnabled(long id,boolean enabled){ActingUser actor=auth.require(Role.CLINIC_ADMIN);if(actor.id()==id&&!enabled)throw new IllegalArgumentException("You cannot disable your own account");UserAccount user=required(id);boolean before=user.isEnabled();user.setEnabled(enabled);audit.record(actor,enabled?"USER_ENABLED":"USER_DISABLED","UserAccount",id,Boolean.toString(before),Boolean.toString(enabled),"clinic_admin role");}
    public void changeRole(long id,Role role){ActingUser actor=auth.require(Role.CLINIC_ADMIN);UserAccount user=required(id);Role before=user.getRole();if(actor.id()==id&&role!=Role.CLINIC_ADMIN)throw new IllegalArgumentException("You cannot remove your own admin role");user.setRole(role);audit.record(actor,"USER_ROLE_CHANGED","UserAccount",id,before.name(),role.name(),"clinic_admin role");}
    public List<AuditEvent> auditLog(){auth.require(Role.CLINIC_ADMIN);return em.createQuery("select a from AuditEvent a order by a.occurredAt desc",AuditEvent.class).setMaxResults(250).getResultList();}
    public List<WebhookDelivery> webhooks(){auth.require(Role.CLINIC_ADMIN);return em.createQuery("select w from WebhookDelivery w order by w.createdAt desc",WebhookDelivery.class).setMaxResults(100).getResultList();}
    public void inviteStaff(String name,String email,Role role){ActingUser actor=auth.require(Role.CLINIC_ADMIN);if(!Set.of(Role.CLINIC_ADMIN,Role.CLINICIAN,Role.FRONT_DESK).contains(role))throw new IllegalArgumentException("Choose a staff role");if(name==null||name.trim().length()<2||email==null||!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new IllegalArgumentException("Enter a valid name and email");if(em.createQuery("select count(u) from UserAccount u where u.email=:email",Long.class).setParameter("email",email.toLowerCase()).getSingleResult()>0)throw new IllegalArgumentException("That email already has an account");String temporary="Temp"+UUID.randomUUID().toString().replace("-","").substring(0,12)+"7";UserAccount user=new UserAccount(email,BCrypt.hashpw(temporary,BCrypt.gensalt(12)),name.trim(),role,null,null);em.persist(user);em.persist(new OutboxEmail(email,"Harbour Clinic staff invitation","Temporary local password: "+temporary+". Use password reset after first sign-in."));audit.record(actor,"STAFF_INVITED","UserAccount",user.getId(),null,role.name(),"clinic_admin role");}
    public void createProvider(String name,String specialty,String license,String schedule){ActingUser actor=auth.require(Role.CLINIC_ADMIN);if(name==null||name.trim().length()<3||specialty==null||specialty.trim().length()<3||license==null||license.trim().length()<4||schedule==null||schedule.trim().length()<3)throw new IllegalArgumentException("Complete all provider fields");em.createNativeQuery("insert into providers(name,specialty,license_number,schedule_text,active,created_at,version) values (?1,?2,?3,?4,true,now(),0)").setParameter(1,name.trim()).setParameter(2,specialty.trim()).setParameter(3,license.trim()).setParameter(4,schedule.trim()).executeUpdate();audit.record(actor,"PROVIDER_CREATED","Provider",null,null,license.trim(),"clinic_admin role");}
    public void createAppointmentType(String name,int duration,java.math.BigDecimal fee){ActingUser actor=auth.require(Role.CLINIC_ADMIN);if(name==null||name.trim().length()<3||duration<10||duration>240||fee==null||fee.signum()<0)throw new IllegalArgumentException("Enter a name, 10–240 minute duration, and non-negative fee");em.createNativeQuery("insert into appointment_types(name,duration_minutes,fee,currency,active,created_at,version) values (?1,?2,?3,'GBP',true,now(),0)").setParameter(1,name.trim()).setParameter(2,duration).setParameter(3,fee).executeUpdate();audit.record(actor,"APPOINTMENT_TYPE_CREATED","AppointmentType",null,null,name.trim()+" GBP "+fee,"clinic_admin role; fee reference data");}
    public void contactCoreEndpoint(String endpoint){
        ActingUser actor=auth.require(Role.CLINIC_ADMIN);
        if(endpoint==null||!(endpoint.startsWith("http://")||endpoint.startsWith("https://"))||endpoint.length()>300)throw new IllegalArgumentException("Enter an http or https endpoint");
        WebhookDelivery attempt=new WebhookDelivery("CORE_CONNECTIVITY_CHECK",endpoint);em.persist(attempt);
        try{
            java.net.http.HttpResponse<String> reply=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(endpoint)).timeout(java.time.Duration.ofSeconds(5)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            if(reply.statusCode()<400)attempt.delivered();else attempt.failed("HTTP "+reply.statusCode()+" "+reply.body().substring(0,Math.min(400,reply.body().length())));
        }catch(Exception e){attempt.failed(String.valueOf(e.getMessage()));}
        audit.record(actor,"CORE_CONNECTIVITY_CHECKED","WebhookDelivery",attempt.getId(),null,endpoint,"clinic_admin role");
    }
    public void reassignPanel(long patientId,long providerId){auth.require(Role.CLINIC_ADMIN);Patient patient=em.find(Patient.class,patientId);if(patient==null)throw new EntityNotFoundException("Patient not found");patient.assignProvider(providerId);em.merge(patient);}
    /** Carries a staff member's old terminal sign-in over to the portal, keeping the legacy derivation so the old terminals keep matching. */
    public void importLegacyCredential(String email,String terminalCode){
        ActingUser actor=auth.require(Role.CLINIC_ADMIN);UserAccount user=em.createQuery("select u from UserAccount u where u.email=:email",UserAccount.class).setParameter("email",email==null?"":email.trim().toLowerCase()).getResultStream().findFirst().orElseThrow(()->new EntityNotFoundException("Account not found"));
        try{
            byte[] salt="harbour-clinic".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(terminalCode.toCharArray(),salt,1024,128);
            byte[] derived=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            user.setPasswordHash("pbkdf2$"+java.util.Base64.getEncoder().encodeToString(salt)+"$"+java.util.Base64.getEncoder().encodeToString(derived));
            audit.record(actor,"LEGACY_CREDENTIAL_IMPORTED","UserAccount",user.getId(),null,"terminal", "clinic_admin role");
        }catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Legacy credential could not be imported",e);}
    }
    private UserAccount required(long id){UserAccount u=em.find(UserAccount.class,id);if(u==null)throw new EntityNotFoundException("User not found");return u;}
    /** Resolves a staff member from the clinic directory so their details can seed a provider record. */
    public String resolveDirectoryStaff(String staffName){
        ActingUser actor=auth.require(Role.CLINIC_ADMIN);
        if(staffName==null||staffName.trim().length()<2||staffName.length()>80)throw new IllegalArgumentException("Enter at least two characters of the staff name");
        javax.naming.directory.DirContext ctx=null;
        try{
            java.util.Hashtable<String,String> env=new java.util.Hashtable<>();
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
            env.put(javax.naming.Context.PROVIDER_URL,System.getenv().getOrDefault("CLINIC_DIRECTORY_URL","ldap://directory.harbour.internal:389"));
            env.put(javax.naming.Context.SECURITY_AUTHENTICATION,"simple");
            env.put(javax.naming.Context.SECURITY_PRINCIPAL,System.getenv().getOrDefault("CLINIC_DIRECTORY_BIND","cn=portal,ou=svc,dc=harbour,dc=internal"));
            env.put(javax.naming.Context.SECURITY_CREDENTIALS,System.getenv().getOrDefault("CLINIC_DIRECTORY_BIND_CODE",""));
            ctx=new javax.naming.directory.InitialDirContext(env);
            javax.naming.directory.SearchControls controls=new javax.naming.directory.SearchControls();
            controls.setSearchScope(javax.naming.directory.SearchControls.SUBTREE_SCOPE);
            String filter="(&(objectClass=person)(cn=*"+staffName.trim()+"*))";
            javax.naming.NamingEnumeration<javax.naming.directory.SearchResult> results=ctx.search("ou=staff,dc=harbour,dc=internal",filter,controls);
            StringBuilder found=new StringBuilder();
            while(results.hasMore()){javax.naming.directory.SearchResult row=results.next();String cn=String.valueOf(row.getAttributes().get("cn").get());if(found.length()>0)found.append(", ");found.append(cn);}
            audit.record(actor,"DIRECTORY_RESOLVED","Provider",null,null,staffName.trim(),"clinic_admin directory scope");
            return found.length()==0?"No directory match for "+staffName.trim():found.toString();
        }catch(javax.naming.NamingException e){throw new IllegalStateException("Clinic directory is unavailable",e);}
        finally{if(ctx!=null)try{ctx.close();}catch(javax.naming.NamingException ignored){}}
    }
}
