package clinic.harbour.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

@Singleton @Startup @DependsOn("DatabaseMigration")
public class SeedFileInitializer {
    @PostConstruct public void createSeedFiles(){
        try {
            Path base=Path.of(System.getenv().getOrDefault("CLINIC_UPLOAD_DIR",System.getProperty("java.io.tmpdir")+"/clinic-uploads")).toAbsolutePath().normalize();
            Files.createDirectories(base);
            for(int i=1;i<=20;i++){
                Path file=base.resolve("seed-"+i+".txt");
                if(Files.notExists(file)) Files.writeString(file,"Harbour Clinic mock document "+i+"\nSynthetic benchmark data only.\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            }
        } catch(Exception e){throw new IllegalStateException("Cannot initialise local document store",e);}
    }
}
