package clinic.harbour.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="appointment_types")
public class AppointmentType extends BaseEntity {
    @Column(nullable=false,unique=true,length=80) private String name;
    @Column(name="duration_minutes",nullable=false) private int durationMinutes;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal fee;
    @Column(nullable=false,length=3) private String currency;
    @Column(nullable=false) private boolean active=true;
    protected AppointmentType() {} public String getName(){return name;} public int getDurationMinutes(){return durationMinutes;} public BigDecimal getFee(){return fee;} public String getCurrency(){return currency;} public boolean isActive(){return active;}
}
