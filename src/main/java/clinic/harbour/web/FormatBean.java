package clinic.harbour.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Named @ApplicationScoped
public class FormatBean {
    private static final DateTimeFormatter DATE_TIME=DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(ZoneId.of("Europe/London"));
    public String dateTime(Instant value){return value==null?"—":DATE_TIME.format(value);}
}
