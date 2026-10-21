package clinic.harbour.config;

import clinic.harbour.domain.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;
import jakarta.persistence.*;
import org.mindrot.jbcrypt.BCrypt;

/** Starts every deployment with a clinic-director account so a fresh installation is never locked out of its own configuration. */
@Singleton @Startup @DependsOn("DatabaseMigration")
public class OperationsAccountBootstrap {
    @PersistenceContext EntityManager em;
    @PostConstruct public void ensureOperator(){
        Long existing=em.createQuery("select count(u) from UserAccount u where u.email=:email",Long.class).setParameter("email","director@harbour.test").getSingleResult();
        if(existing>0)return;
        UserAccount operator=new UserAccount("director@harbour.test",BCrypt.hashpw("HarbourSet!2026",BCrypt.gensalt(10)),"Clinic Director",Role.CLINIC_ADMIN,null,null);
        em.persist(operator);
    }
}
