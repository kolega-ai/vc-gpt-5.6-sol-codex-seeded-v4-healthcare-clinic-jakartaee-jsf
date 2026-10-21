package clinic.harbour.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ScopeTokenServiceTest {
    @Test void signedScopeCannotBeSubstituted(){ScopeTokenService service=new ScopeTokenService();String token=service.mint("patient:sync","12");assertEquals("12",service.verify(token,"patient:sync").resourceId());assertThrows(AccessDeniedException.class,()->service.verify(token,"patient:read"));assertThrows(AccessDeniedException.class,()->service.verify(token+"x","patient:sync"));}
}
