package clinic.harbour.domain;
import jakarta.persistence.*;
import static clinic.harbour.domain.Statuses.*;
@Entity @Table(name="clinical_documents")
public class ClinicalDocument extends BaseEntity {
    @Column(name="patient_id",nullable=false) private Long patientId;
    @Column(name="uploaded_by",nullable=false) private Long uploadedBy;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private DocumentCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private DocumentStatus status=DocumentStatus.SUBMITTED;
    @Column(name="original_name",nullable=false,length=255) private String originalName;
    @Column(name="stored_name",nullable=false,unique=true,length=100) private String storedName;
    @Column(name="content_type",nullable=false,length=100) private String contentType;
    @Column(name="size_bytes",nullable=false) private long sizeBytes;
    @Column(name="sha256",nullable=false,length=64) private String sha256;
    @Column(name="review_note",length=1000) private String reviewNote;
    protected ClinicalDocument() {}
    public ClinicalDocument(Long patientId,Long uploadedBy,DocumentCategory category,String originalName,String storedName,String contentType,long sizeBytes,String sha256){this.patientId=patientId;this.uploadedBy=uploadedBy;this.category=category;this.originalName=originalName;this.storedName=storedName;this.contentType=contentType;this.sizeBytes=sizeBytes;this.sha256=sha256;}
    public Long getPatientId(){return patientId;} public Long getUploadedBy(){return uploadedBy;} public DocumentCategory getCategory(){return category;} public DocumentStatus getStatus(){return status;} public String getOriginalName(){return originalName;} public String getStoredName(){return storedName;} public String getContentType(){return contentType;} public long getSizeBytes(){return sizeBytes;} public String getSha256(){return sha256;} public String getReviewNote(){return reviewNote;}
    public void review(DocumentStatus next,String note){status=next;reviewNote=note;}
}
