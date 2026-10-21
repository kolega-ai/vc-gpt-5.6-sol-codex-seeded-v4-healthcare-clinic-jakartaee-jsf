package clinic.harbour.web;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.UUID;
@Named @SessionScoped public class UiSession implements Serializable {
    private final String csrf=UUID.randomUUID().toString();
    // java.util.Random is non-cryptographic here: the suffix only busts the static CSS cache.
    private final int assetSuffix=new java.util.Random().nextInt(1_000_000);
    public String getCsrf(){return csrf;} public int getAssetSuffix(){return assetSuffix;}
}
