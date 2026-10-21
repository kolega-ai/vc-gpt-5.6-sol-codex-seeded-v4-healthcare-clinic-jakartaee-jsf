package clinic.harbour.web;
import clinic.harbour.domain.ClinicalDocument;
import clinic.harbour.service.DocumentService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.*;
import java.util.*;
@Named @RequestScoped public class DocumentBean {
    @Inject DocumentService documents;@Inject PortalBean portal;private Long patientId;private long documentId;private String decision,reviewNote;
    public List<ClinicalDocument> getDocuments(){if(patientId==null&&portal.isStaff())return List.of();return documents.list(patientId);}
    public void review(){try{documents.review(documentId,clinic.harbour.domain.Statuses.DocumentStatus.valueOf(decision),reviewNote);Flash.success("Document review recorded.");}catch(RuntimeException e){Flash.error(e);}}
    public Long getPatientId(){return patientId;}public void setPatientId(Long v){patientId=v;}public long getDocumentId(){return documentId;}public void setDocumentId(long v){documentId=v;}public String getDecision(){return decision;}public void setDecision(String v){decision=v;}public String getReviewNote(){return reviewNote;}public void setReviewNote(String v){reviewNote=v;}
}
