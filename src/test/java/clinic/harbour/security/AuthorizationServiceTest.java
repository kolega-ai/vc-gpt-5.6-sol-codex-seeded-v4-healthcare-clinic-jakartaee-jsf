package clinic.harbour.security;
import clinic.harbour.domain.*;
import jakarta.ejb.SessionContext;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import java.security.Principal;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthorizationServiceTest {
    private AuthorizationService service;
    @SuppressWarnings("unchecked") @BeforeEach void setup(){service=new AuthorizationService();service.em=mock(EntityManager.class);service.sessionContext=mock(SessionContext.class);when(service.sessionContext.getCallerPrincipal()).thenReturn((Principal)()->"patient@harbour.test");UserAccount user=mock(UserAccount.class);when(user.getId()).thenReturn(4L);when(user.getEmail()).thenReturn("patient@harbour.test");when(user.getDisplayName()).thenReturn("Maya Clarke");when(user.getRole()).thenReturn(Role.PATIENT);when(user.getPatientId()).thenReturn(1L);when(user.isEnabled()).thenReturn(true);TypedQuery<UserAccount> query=mock(TypedQuery.class);when(service.em.createQuery(anyString(),eq(UserAccount.class))).thenReturn(query);when(query.setParameter(anyString(),any())).thenReturn(query);when(query.getResultStream()).thenAnswer(ignored->Stream.of(user));}
    @Test void patientOwnsOnlyTheirRecord(){assertEquals("patient owns record",service.requirePatientAccess(1L,true));assertThrows(AccessDeniedException.class,()->service.requirePatientAccess(2L,false));}
    @Test void roleCannotComeFromRequestInput(){ActingUser actor=service.current();assertEquals(Role.PATIENT,actor.role());assertEquals(1L,actor.patientId());}
}
