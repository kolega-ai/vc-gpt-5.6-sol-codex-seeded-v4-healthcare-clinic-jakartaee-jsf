package clinic.harbour.web;
import clinic.harbour.domain.SecureMessage;
import clinic.harbour.service.ClinicService;
import clinic.harbour.service.DocumentService;
import jakarta.servlet.http.Part;
import java.io.InputStream;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.*;
import java.util.*;
@Named @RequestScoped public class MessageBean {
    @Inject ClinicService clinic;@Inject DocumentService documents;private Long patientId;private String subject,body;private boolean internal;private Part attachment;private long parentId;private List<SecureMessage> thread;
    public List<SecureMessage> getMessages(){try{return clinic.messages(patientId);}catch(IllegalArgumentException e){return List.of();}}
    public void send(){try{SecureMessage value=clinic.sendMessage(patientId,subject,body,internal);
        if(attachment!=null&&attachment.getSize()>0)try(InputStream in=attachment.getInputStream()){documents.attachToMessage(value.getPatientId(),attachment.getSubmittedFileName(),attachment.getContentType(),in,attachment.getSize());}
        subject=body=null;attachment=null;Flash.success(internal?"Internal note added.":"Secure message sent.");}catch(Exception e){Flash.error(e);}}
    public void reply(){try{thread=clinic.continueThread(parentId,patientId,body);body=null;Flash.success("Reply added to the conversation.");}catch(RuntimeException e){Flash.error(e);}}
    public List<SecureMessage> getThread(){return thread==null?List.of():thread;}
    public Long getPatientId(){return patientId;}public void setPatientId(Long v){patientId=v;}public String getSubject(){return subject;}public void setSubject(String v){subject=v;}public String getBody(){return body;}public void setBody(String v){body=v;}public boolean isInternal(){return internal;}public void setInternal(boolean v){internal=v;}public Part getAttachment(){return attachment;}public void setAttachment(Part v){attachment=v;}public long getParentId(){return parentId;}public void setParentId(long v){parentId=v;}
}
