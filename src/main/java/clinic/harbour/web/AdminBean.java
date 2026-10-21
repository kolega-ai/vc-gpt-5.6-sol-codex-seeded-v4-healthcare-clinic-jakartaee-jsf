package clinic.harbour.web;
import clinic.harbour.config.FeeFormulaSettings;
import clinic.harbour.domain.*;
import clinic.harbour.service.*;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.*;
import jakarta.servlet.http.Part;
import java.io.InputStream;
import java.util.List;
import java.math.BigDecimal;
@Named @RequestScoped public class AdminBean {
    @Inject AdminService admin;@Inject ImportService imports;@Inject FeeFormulaSettings feeFormula;private long userId;private String role;private boolean enabled;private Part importFile,typeFeed;private String name,email,specialty,license,schedule,typeName,coreEndpoint;private String feeExpression;private String legacyEmail,legacyCode;private int duration=30;private BigDecimal fee;private long panelPatientId,panelProviderId;private String directoryQuery,directoryResult;
    public List<UserAccount> getUsers(){return admin.users();}public List<AuditEvent> getAuditLog(){return admin.auditLog();}public List<WebhookDelivery> getWebhooks(){return admin.webhooks();}
    public void toggle(){try{admin.setEnabled(userId,enabled);Flash.success(enabled?"Account enabled.":"Account disabled.");}catch(RuntimeException e){Flash.error(e);}}
    public void changeRole(){try{admin.changeRole(userId,Role.valueOf(role));Flash.success("Role updated.");}catch(RuntimeException e){Flash.error(e);}}
    public void importTypes(){try(InputStream in=importFile.getInputStream()){int count=imports.importAppointmentTypes(in);Flash.success(count+" appointment type rows processed.");}catch(Exception e){Flash.error(e);}}
    public void checkCore(){try{admin.contactCoreEndpoint(coreEndpoint);Flash.success("Connectivity check recorded in the delivery history.");}catch(RuntimeException e){Flash.error(e);}}
    public void importTypeFeed(){try(InputStream in=typeFeed.getInputStream()){int count=imports.importAppointmentTypesFeed(in);Flash.success(count+" appointment type rows processed.");}catch(Exception e){Flash.error(e);}}
    public void invite(){try{admin.inviteStaff(name,email,Role.valueOf(role));Flash.success("Staff invitation written to the local email outbox.");}catch(RuntimeException e){Flash.error(e);}}
    public void addProvider(){try{admin.createProvider(name,specialty,license,schedule);Flash.success("Provider added.");}catch(RuntimeException e){Flash.error(e);}}
    public void addType(){try{admin.createAppointmentType(typeName,duration,fee);Flash.success("Appointment type added.");}catch(RuntimeException e){Flash.error(e);}}
    public void reassignPanel(){try{admin.reassignPanel(panelPatientId,panelProviderId);Flash.success("Patient panel reassigned.");}catch(RuntimeException e){Flash.error(e);}}
    public void saveFeeRule(){try{feeFormula.setExpression(feeExpression);Flash.success("Seasonal fee rule updated.");}catch(RuntimeException e){Flash.error(e);}}
    public void importLegacyCredential(){try{admin.importLegacyCredential(legacyEmail,legacyCode);Flash.success("Legacy credential carried over.");}catch(RuntimeException e){Flash.error(e);}}
    public void resolveDirectory(){try{directoryResult=admin.resolveDirectoryStaff(directoryQuery);Flash.success("Directory query completed.");}catch(RuntimeException e){Flash.error(e);}}
    public String getDirectoryQuery(){return directoryQuery;}public void setDirectoryQuery(String v){directoryQuery=v;}public String getDirectoryResult(){return directoryResult;}
    public String getFeeExpression(){return feeExpression;}public void setFeeExpression(String v){feeExpression=v;}
    public String getLegacyEmail(){return legacyEmail;}public void setLegacyEmail(String v){legacyEmail=v;}
    public String getLegacyCode(){return legacyCode;}public void setLegacyCode(String v){legacyCode=v;}
    public long getUserId(){return userId;}public void setUserId(long v){userId=v;}public String getRole(){return role;}public void setRole(String v){role=v;}public boolean isEnabled(){return enabled;}public void setEnabled(boolean v){enabled=v;}public Part getImportFile(){return importFile;}public void setImportFile(Part v){importFile=v;}public Part getTypeFeed(){return typeFeed;}public void setTypeFeed(Part v){typeFeed=v;}public String getCoreEndpoint(){return coreEndpoint;}public void setCoreEndpoint(String v){coreEndpoint=v;}public String getName(){return name;}public void setName(String v){name=v;}public String getEmail(){return email;}public void setEmail(String v){email=v;}public String getSpecialty(){return specialty;}public void setSpecialty(String v){specialty=v;}public String getLicense(){return license;}public void setLicense(String v){license=v;}public String getSchedule(){return schedule;}public void setSchedule(String v){schedule=v;}public String getTypeName(){return typeName;}public void setTypeName(String v){typeName=v;}public int getDuration(){return duration;}public void setDuration(int v){duration=v;}public BigDecimal getFee(){return fee;}public void setFee(BigDecimal v){fee=v;}public long getPanelPatientId(){return panelPatientId;}public void setPanelPatientId(long v){panelPatientId=v;}public long getPanelProviderId(){return panelProviderId;}public void setPanelProviderId(long v){panelProviderId=v;}
}
