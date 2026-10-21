package clinic.harbour.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static clinic.harbour.domain.Statuses.*;
class ClinicServiceTest {
    private final ClinicService service=new ClinicService();
    @Test void appointmentEdgesAreExplicitAndTerminal(){assertTrue(service.appointmentTransitionAllowed(AppointmentStatus.REQUESTED,AppointmentStatus.SCHEDULED));assertTrue(service.appointmentTransitionAllowed(AppointmentStatus.SCHEDULED,AppointmentStatus.CHECKED_IN));assertTrue(service.appointmentTransitionAllowed(AppointmentStatus.CHECKED_IN,AppointmentStatus.COMPLETED));assertFalse(service.appointmentTransitionAllowed(AppointmentStatus.COMPLETED,AppointmentStatus.SCHEDULED));assertFalse(service.appointmentTransitionAllowed(AppointmentStatus.CANCELLED,AppointmentStatus.CHECKED_IN));}
    @Test void refillDecisionCannotBeReopened(){assertTrue(service.refillTransitionAllowed(Refill.NONE,Refill.REQUESTED));assertTrue(service.refillTransitionAllowed(Refill.REQUESTED,Refill.NEEDS_INFO));assertTrue(service.refillTransitionAllowed(Refill.NEEDS_INFO,Refill.APPROVED));assertFalse(service.refillTransitionAllowed(Refill.APPROVED,Refill.REQUESTED));}
}
