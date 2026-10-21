package clinic.harbour.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="patients")
public class Patient extends BaseEntity {
    @Column(name="medical_record_number",nullable=false,unique=true,length=24) private String mrn;
    @Column(name="first_name",nullable=false,length=80) private String firstName;
    @Column(name="last_name",nullable=false,length=80) private String lastName;
    @Column(name="date_of_birth",nullable=false) private LocalDate dateOfBirth;
    @Column(nullable=false,length=30) private String phone;
    @Column(nullable=false,length=160) private String email;
    @Column(nullable=false,length=300) private String address;
    @Column(name="emergency_contact",nullable=false,length=180) private String emergencyContact;
    @Column(name="insurance_provider",length=120) private String insuranceProvider;
    @Column(name="insurance_member_id",length=80) private String insuranceMemberId;
    @Column(name="assigned_provider_id") private Long assignedProviderId;
    protected Patient() {}
    public Patient(String mrn,String firstName,String lastName,LocalDate dateOfBirth,String phone,String email,String address,String emergencyContact){this.mrn=mrn;this.firstName=firstName;this.lastName=lastName;this.dateOfBirth=dateOfBirth;this.phone=phone;this.email=email;this.address=address;this.emergencyContact=emergencyContact;}
    public String getMrn(){return mrn;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getFullName(){return firstName+" "+lastName;}
    public LocalDate getDateOfBirth(){return dateOfBirth;} public String getPhone(){return phone;} public String getEmail(){return email;} public String getAddress(){return address;}
    public String getEmergencyContact(){return emergencyContact;} public String getInsuranceProvider(){return insuranceProvider;} public String getInsuranceMemberId(){return insuranceMemberId;} public Long getAssignedProviderId(){return assignedProviderId;}
    public void updateDemographics(String phone,String email,String address,String emergency){this.phone=phone;this.email=email;this.address=address;this.emergencyContact=emergency;}
    public void assignProvider(Long providerId){this.assignedProviderId=providerId;}
}
