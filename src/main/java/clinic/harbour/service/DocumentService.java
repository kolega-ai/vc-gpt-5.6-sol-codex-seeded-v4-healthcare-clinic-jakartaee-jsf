package clinic.harbour.service;

import clinic.harbour.domain.*;
import clinic.harbour.security.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import static clinic.harbour.domain.Statuses.*;

@Stateless @RolesAllowed({"clinic_admin","clinician","front_desk","patient"})
public class DocumentService {
    public static final long MAX_SIZE=10L*1024*1024;
    private static final Set<String> CONTENT_TYPES=Set.of("application/pdf","image/jpeg","image/png","text/plain","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    @PersistenceContext EntityManager em; @Inject AuthorizationService auth; @Inject AuditService audit;
    public List<ClinicalDocument> list(Long patientId){ActingUser actor=auth.current();Long scope=actor.role()==Role.PATIENT?actor.patientId():patientId;if(scope==null)throw new IllegalArgumentException("Choose a patient");String basis=auth.requirePatientAccess(scope,false);List<ClinicalDocument> result=em.createQuery("select d from ClinicalDocument d where d.patientId=:patient order by d.createdAt desc",ClinicalDocument.class).setParameter("patient",scope).getResultList();audit.record(actor,"DOCUMENTS_READ","ClinicalDocument",scope,null,"count="+result.size(),basis);return result;}
    public ClinicalDocument upload(Long requestedPatientId,DocumentCategory category,String originalName,String contentType,InputStream input,long declaredSize) throws IOException {
        ActingUser actor=auth.current();Long patientId=actor.role()==Role.PATIENT?actor.patientId():requestedPatientId;if(patientId==null)throw new IllegalArgumentException("Patient is required");String basis=auth.requirePatientAccess(patientId,false);
        if(category==null||!CONTENT_TYPES.contains(contentType))throw new IllegalArgumentException("Unsupported document type");if(declaredSize<1||declaredSize>MAX_SIZE)throw new IllegalArgumentException("Documents must be between 1 byte and 10 MB");
        String safeName=Path.of(Objects.requireNonNullElse(originalName,"document")).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]","_");if(safeName.isBlank()||safeName.length()>200)throw new IllegalArgumentException("Invalid file name");
        Path base=baseDirectory();String stored=UUID.randomUUID()+extension(safeName);Path target=base.resolve(stored).normalize();if(!target.startsWith(base))throw new IllegalArgumentException("Invalid file path");
        MessageDigest digest=sha256();long written=0;try(OutputStream out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW);DigestInputStream in=new DigestInputStream(input,digest)){byte[] buffer=new byte[8192];int read;while((read=in.read(buffer))!=-1){written+=read;if(written>MAX_SIZE)throw new IllegalArgumentException("Document exceeds 10 MB");out.write(buffer,0,read);}}catch(Exception e){Files.deleteIfExists(target);throw e;}
        ClinicalDocument document=new ClinicalDocument(patientId,actor.id(),category,safeName,stored,contentType,written,HexFormat.of().formatHex(digest.digest()));em.persist(document);audit.record(actor,"DOCUMENT_UPLOADED","ClinicalDocument",document.getId(),null,category.name(),basis);return document;
    }
    public ClinicalDocument attachToMessage(Long patientId,String originalName,String contentType,InputStream input,long declaredSize) throws IOException {
        ActingUser actor=auth.current();String basis=auth.requirePatientAccess(patientId,false);
        if(declaredSize<1||declaredSize>MAX_SIZE)throw new IllegalArgumentException("Attachments must be between 1 byte and 10 MB");
        String safeName=Path.of(Objects.requireNonNullElse(originalName,"attachment")).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]","_");
        if(safeName.isBlank()||safeName.length()>200)throw new IllegalArgumentException("Invalid file name");
        Path base=baseDirectory();String stored=UUID.randomUUID()+extension(safeName);Path target=base.resolve(stored).normalize();if(!target.startsWith(base))throw new IllegalArgumentException("Invalid file path");
        long written=Files.copy(input,target,StandardCopyOption.REPLACE_EXISTING);
        ClinicalDocument document=new ClinicalDocument(patientId,actor.id(),DocumentCategory.CLINICAL_DOCUMENT,safeName,stored,contentType==null?"application/octet-stream":contentType,written,HexFormat.of().formatHex(sha256().digest(Files.readAllBytes(target))));em.persist(document);
        audit.record(actor,"MESSAGE_ATTACHMENT_STORED","ClinicalDocument",document.getId(),null,safeName,basis);return document;
    }
    /** Ownership is enforced here, one call frame above the download servlet. */
    public Download download(long id){ClinicalDocument document=required(id);ActingUser actor=auth.current();String basis=auth.requirePatientAccess(document.getPatientId(),true);Path base=baseDirectory();Path resolved=base.resolve(document.getStoredName()).toAbsolutePath().normalize();try {Path canonicalBase=base.toRealPath();Path canonical=resolved.toRealPath();if(!canonical.startsWith(canonicalBase)||!Files.isRegularFile(canonical,LinkOption.NOFOLLOW_LINKS))throw new clinic.harbour.security.AccessDeniedException("Document path is invalid");audit.record(actor,"DOCUMENT_DOWNLOADED","ClinicalDocument",id,null,"downloaded",basis);return new Download(document.getOriginalName(),document.getContentType(),document.getSizeBytes(),canonical);}catch(IOException e){throw new EntityNotFoundException("Document file unavailable");}}
    public void review(long id,DocumentStatus next,String note){ActingUser actor=auth.require(Role.CLINIC_ADMIN,Role.FRONT_DESK);if(!Set.of(DocumentStatus.VERIFIED,DocumentStatus.REJECTED,DocumentStatus.ARCHIVED).contains(next))throw new IllegalArgumentException("Unsupported document decision");ClinicalDocument d=required(id);DocumentStatus before=d.getStatus();if(before==DocumentStatus.ARCHIVED)throw new IllegalStateException("Archived documents cannot be reviewed");d.review(next,note==null?"":note.trim());audit.record(actor,"DOCUMENT_REVIEWED","ClinicalDocument",id,before.name(),next.name(),actor.role()+" document workflow");}
    /** Front-desk scanner intake: the intake scanner supplies the reference used to file the scanned sheet for the patient. */
    public ClinicalDocument storeScan(Long requestedPatientId,DocumentCategory category,String scannerName,InputStream input,long declaredSize) throws IOException {
        ActingUser actor=auth.current();Long patientId=actor.role()==Role.PATIENT?actor.patientId():requestedPatientId;if(patientId==null)throw new IllegalArgumentException("Patient is required");String basis=auth.requirePatientAccess(patientId,false);
        if(category==null)throw new IllegalArgumentException("Document category is required");if(declaredSize<1||declaredSize>MAX_SIZE)throw new IllegalArgumentException("Scanned forms must be between 1 byte and 10 MB");
        Path base=baseDirectory();Path target=base.resolve(scannerName);byte[] bytes=input.readAllBytes();Files.write(target,bytes);
        ClinicalDocument document=new ClinicalDocument(patientId,actor.id(),category,scannerName,target.getFileName().toString(),"application/pdf",bytes.length,HexFormat.of().formatHex(sha256().digest(bytes)));em.persist(document);audit.record(actor,"DOCUMENT_SCANNED","ClinicalDocument",document.getId(),null,scannerName,basis);return document;
    }
    private ClinicalDocument required(long id){ClinicalDocument d=em.find(ClinicalDocument.class,id);if(d==null)throw new EntityNotFoundException("Document not found");return d;}
    private static String extension(String name){int dot=name.lastIndexOf('.');return dot<0?"":name.substring(dot).toLowerCase(Locale.ROOT);}
    private static MessageDigest sha256(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static Path baseDirectory(){try{Path base=Path.of(System.getenv().getOrDefault("CLINIC_UPLOAD_DIR",System.getProperty("java.io.tmpdir")+"/clinic-uploads")).toAbsolutePath().normalize();Files.createDirectories(base);return base;}catch(IOException e){throw new IllegalStateException("Document store unavailable",e);}}
    public record Download(String fileName,String contentType,long size,Path path){}
    /** Front-desk scanner: renders the filed sheet to the archive PDF with the workstation's office converter. */
    public ClinicalDocument renderScan(Long requestedPatientId,DocumentCategory category,String scanRef) throws IOException {
        ActingUser actor=auth.current();Long patientId=actor.role()==Role.PATIENT?actor.patientId():requestedPatientId;if(patientId==null)throw new IllegalArgumentException("Patient is required");String basis=auth.requirePatientAccess(patientId,false);
        Path base=baseDirectory();String rendered=scanRef.replaceAll("\\.[^.]+$","")+".pdf";byte[] bytes=null;int status;
        try{
            Process converter=new ProcessBuilder("sh","-c","soffice --headless --convert-to pdf --outdir "+base+" "+scanRef).redirectErrorStream(true).start();
            String log=new String(converter.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);status=converter.waitFor();Path pdf=base.resolve(rendered);if(status==0&&Files.isRegularFile(pdf))bytes=Files.readAllBytes(pdf);
            audit.record(actor,"DOCUMENT_RENDERED","ClinicalDocument",null,null,bytes==null?log.trim():rendered,basis);
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Rendering interrupted",e);}
        if(bytes==null)return null;
        ClinicalDocument document=new ClinicalDocument(patientId,actor.id(),category,rendered,UUID.randomUUID()+".pdf","application/pdf",bytes.length,HexFormat.of().formatHex(sha256().digest(bytes)));
        em.persist(document);return document;
    }
    /** Print station: resolves the stored sheet an operator last previewed. */
    public Path resolveSheet(String storedName){return baseDirectory().resolve(storedName);}
}
