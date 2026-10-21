package clinic.harbour.web;

import clinic.harbour.domain.Patient;
import clinic.harbour.security.ChartLinkCodec;
import clinic.harbour.service.ClinicService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.*;
import javax.crypto.BadPaddingException;
import java.security.GeneralSecurityException;

@Named @RequestScoped public class ChartLinkBean {
    @Inject ChartLinkCodec codec;@Inject ClinicService clinic;
    private String chartRef;private String notice;private Long patientId;
    public String open(){
        try{long id=codec.open(chartRef);Patient patient=clinic.patient(id);patientId=id;notice="Opened chart for "+patient.getFullName()+".";}
        catch(BadPaddingException e){notice="This chart link is not valid.";}
        catch(GeneralSecurityException e){notice="No chart matches this link.";}
        catch(RuntimeException e){notice="No chart matches this link.";}
        return null;
    }
    public String share(){try{notice=codec.seal(patientId==null?0L:patientId);}catch(GeneralSecurityException e){notice="Chart link could not be prepared.";}return null;}
    public String getChartRef(){return chartRef;}public void setChartRef(String v){chartRef=v;}
    public String getNotice(){return notice;}
    public Long getPatientId(){return patientId;}public void setPatientId(Long v){patientId=v;}
}
