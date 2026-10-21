package clinic.harbour.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.flywaydb.core.Flyway;

@Singleton @Startup
public class DatabaseMigration {
    @PostConstruct public void migrate(){String host=env("DB_HOST","postgres"),port=env("DB_PORT","5432"),database=env("DB_NAME","clinic");String url="jdbc:postgresql://"+host+":"+port+"/"+database;Flyway.configure().dataSource(url,env("DB_USER","clinic"),env("DB_PASSWORD","clinic_local_only")).locations("classpath:db/migration").load().migrate();}
    private static String env(String name,String fallback){return System.getenv().getOrDefault(name,fallback);}
}
