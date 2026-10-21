package clinic.harbour.domain;
import jakarta.persistence.*;
@Entity @Table(name="providers")
public class Provider extends BaseEntity {
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false,length=100) private String specialty;
    @Column(name="license_number",nullable=false,unique=true,length=50) private String licenseNumber;
    @Column(name="schedule_text",nullable=false,length=300) private String scheduleText;
    @Column(nullable=false) private boolean active=true;
    protected Provider() {}
    public String getName(){return name;} public String getSpecialty(){return specialty;} public String getLicenseNumber(){return licenseNumber;} public String getScheduleText(){return scheduleText;} public boolean isActive(){return active;}
}
