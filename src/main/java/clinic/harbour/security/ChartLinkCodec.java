package clinic.harbour.security;

import jakarta.enterprise.context.ApplicationScoped;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/** Seals a patient chart reference so it can be handed to a colleague without exposing the record id. */
@ApplicationScoped
public class ChartLinkCodec {
    private static final byte[] KEY="harbour-clinic-chart-link-key-01".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IV="harbour-chart-iv".getBytes(StandardCharsets.UTF_8);
    public String seal(long patientId) throws GeneralSecurityException {
        Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(KEY,"AES"),new IvParameterSpec(IV));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cipher.doFinal(Long.toString(patientId).getBytes(StandardCharsets.UTF_8)));
    }
    public long open(String reference) throws GeneralSecurityException {
        Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(KEY,"AES"),new IvParameterSpec(IV));
        return Long.parseLong(new String(cipher.doFinal(Base64.getUrlDecoder().decode(reference)),StandardCharsets.UTF_8));
    }
}
