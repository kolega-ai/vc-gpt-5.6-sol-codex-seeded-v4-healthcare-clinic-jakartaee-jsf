package clinic.harbour.domain;
public final class Statuses {
    private Statuses() {}
    public enum AppointmentStatus { REQUESTED, SCHEDULED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW }
    public enum Visit { DRAFT, RELEASED }
    public enum Refill { NONE, REQUESTED, NEEDS_INFO, APPROVED, DENIED, CANCELLED }
    public enum PrescriptionStatus { ACTIVE, ON_HOLD, COMPLETED, CANCELLED }
    public enum DocumentStatus { SUBMITTED, VERIFIED, REJECTED, ARCHIVED }
    public enum DocumentCategory { PRE_VISIT_FORM, CLINICAL_DOCUMENT, INSURANCE, IDENTITY }
    public enum MessageVisibility { PATIENT_VISIBLE, INTERNAL_ONLY }
}
