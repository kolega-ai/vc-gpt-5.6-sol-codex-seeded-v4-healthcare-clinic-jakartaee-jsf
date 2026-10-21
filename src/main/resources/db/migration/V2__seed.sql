INSERT INTO providers(name,specialty,license_number,schedule_text) VALUES
 ('Dr. Amina Rahman','Family medicine','GMC-MOCK-1001','Mon–Thu · 08:30–16:30'),
 ('Dr. Theo Bennett','General practice','GMC-MOCK-1002','Tue–Fri · 09:00–17:00'),
 ('Dr. Leila Morgan','Women''s health','GMC-MOCK-1003','Mon, Wed, Fri · 08:00–15:00'),
 ('Dr. Jonah Patel','Paediatrics','GMC-MOCK-1004','Mon–Thu · 10:00–18:00'),
 ('Dr. Eve Laurent','Dermatology','GMC-MOCK-1005','Tue, Thu · 08:30–17:30');

INSERT INTO patients(medical_record_number,first_name,last_name,date_of_birth,phone,email,address,emergency_contact,insurance_provider,insurance_member_id,assigned_provider_id)
SELECT 'HCP-'||lpad(g::text,5,'0'),
 (ARRAY['Maya','Daniel','Sofia','Oliver','Priya','Noah','Amara','Leo','Elena','Sam','Nadia','Arthur'])[g],
 (ARRAY['Clarke','Reed','Khan','Brooks','Shah','Foster','Okafor','Nguyen','Petrov','Wilson','Hassan','Green'])[g],
 DATE '1980-01-01' + (g*617), '+44 7700 900'||lpad(g::text,3,'0'), 'patient'||g||'@example.test',
 (20+g)||' Harbour Lane, London', 'Mock contact · +44 7700 800'||lpad(g::text,3,'0'),
 CASE WHEN g%3=0 THEN 'Self-pay' ELSE 'Harbour Mutual' END, 'MOCK-'||10000+g, ((g-1)%5)+1
FROM generate_series(1,12) g;

INSERT INTO user_accounts(email,password_hash,display_name,role,patient_id,provider_id) VALUES
 ('admin@harbour.test',crypt('ClinicDemo!2026',gen_salt('bf',12)),'Morgan Lee','CLINIC_ADMIN',NULL,NULL),
 ('clinician@harbour.test',crypt('ClinicDemo!2026',gen_salt('bf',12)),'Dr. Amina Rahman','CLINICIAN',NULL,1),
 ('frontdesk@harbour.test',crypt('ClinicDemo!2026',gen_salt('bf',12)),'Alex Rivers','FRONT_DESK',NULL,NULL),
 ('patient@harbour.test',crypt('ClinicDemo!2026',gen_salt('bf',12)),'Maya Clarke','PATIENT',1,NULL);
INSERT INTO user_accounts(email,password_hash,display_name,role,patient_id)
SELECT 'patient'||g||'@example.test',crypt('ClinicDemo!2026',gen_salt('bf',10)),p.first_name||' '||p.last_name,'PATIENT',p.id FROM patients p CROSS JOIN LATERAL (SELECT p.id g) x WHERE p.id>1;

INSERT INTO appointment_types(name,duration_minutes,fee,currency) VALUES
 ('Routine consultation',30,75.00,'GBP'),('Extended consultation',45,110.00,'GBP'),('Follow-up',20,45.00,'GBP'),('Women''s health',40,95.00,'GBP'),('Skin review',30,90.00,'GBP');

-- All appointments enter REQUESTED; the seed workflow below applies named transitions and audits them.
INSERT INTO appointments(patient_id,provider_id,appointment_type,starts_at,ends_at,status,reason)
SELECT ((g-1)%12)+1, ((g-1)%5)+1,
 (ARRAY['Routine consultation','Follow-up','Extended consultation','Women''s health','Skin review'])[((g-1)%5)+1],
 date_trunc('day',now()) + ((g-15)||' days')::interval + interval '9 hours' + (((g-1)%6)||' hours')::interval,
 date_trunc('day',now()) + ((g-15)||' days')::interval + interval '9 hours 30 minutes' + (((g-1)%6)||' hours')::interval,
 'REQUESTED','Mock concern '||g||': review symptoms and care plan'
FROM generate_series(1,30) g;
UPDATE appointments SET status='SCHEDULED' WHERE id NOT IN (3,9,19,26);
INSERT INTO audit_events(actor_id,action,resource_type,resource_id,before_value,after_value,authorization_basis)
SELECT 3,'APPOINTMENT_SCHEDULED','Appointment',id::text,'REQUESTED','SCHEDULED','front_desk role; seed workflow' FROM appointments WHERE status='SCHEDULED';
UPDATE appointments SET status='CHECKED_IN',checked_in_at=starts_at WHERE id IN (1,2,4,5,6,7,8,10,11,12,13,14);
UPDATE appointments SET status='COMPLETED' WHERE id IN (1,2,4,5,6,7,8,10,11,12);
UPDATE appointments SET status='CANCELLED' WHERE id IN (3,9,19);
UPDATE appointments SET status='NO_SHOW' WHERE id IN (13,14);
INSERT INTO audit_events(actor_id,action,resource_type,resource_id,before_value,after_value,authorization_basis)
SELECT 2,'APPOINTMENT_SEED_TRANSITION','Appointment',id::text,'SCHEDULED',status,'assigned clinician/front_desk; seed workflow' FROM appointments WHERE status NOT IN ('SCHEDULED','REQUESTED');

INSERT INTO visit_summaries(appointment_id,patient_id,provider_id,diagnosis,care_instructions,clinician_notes,status)
SELECT a.id,a.patient_id,a.provider_id,'Assessment '||a.id||' — uncomplicated presentation','Hydrate, rest, and follow the agreed safety-net advice.','Internal clinical observations for mock record '||a.id||'.','DRAFT'
FROM appointments a WHERE a.status='COMPLETED' ORDER BY a.id LIMIT 10;
UPDATE visit_summaries SET status='RELEASED',released_at=now()-interval '2 days' WHERE id<=7;
INSERT INTO audit_events(actor_id,action,resource_type,resource_id,before_value,after_value,authorization_basis)
SELECT 2,'VISIT_SUMMARY_RELEASED','VisitSummary',id::text,'DRAFT','RELEASED','assigned clinician; seed workflow' FROM visit_summaries WHERE status='RELEASED';

INSERT INTO prescriptions(patient_id,provider_id,medication_name,dosage,instructions,status,refill_status,refill_note,start_date)
SELECT ((g-1)%12)+1,((g-1)%5)+1,(ARRAY['Amoxicillin','Cetirizine','Salbutamol','Hydrocortisone','Naproxen'])[((g-1)%5)+1],
 (ARRAY['500 mg','10 mg','100 micrograms','1% cream','250 mg'])[((g-1)%5)+1], 'Use exactly as directed on the dispensing label.','ACTIVE','NONE',NULL,current_date-(g*7)
FROM generate_series(1,12) g;
UPDATE prescriptions SET refill_status='REQUESTED',refill_note='Running low; seven days remaining.' WHERE id IN (1,6,11);
UPDATE prescriptions SET refill_status='NEEDS_INFO',refill_note='Please confirm current symptom control.' WHERE id=2;
UPDATE prescriptions SET refill_status='APPROVED',refill_note='Approved after medication review.' WHERE id=3;
UPDATE prescriptions SET refill_status='DENIED',refill_note='Appointment required before renewal.' WHERE id=4;
INSERT INTO audit_events(actor_id,action,resource_type,resource_id,before_value,after_value,authorization_basis)
SELECT CASE WHEN refill_status='REQUESTED' THEN 4 ELSE 2 END,'REFILL_SEED_TRANSITION','Prescription',id::text,'NONE',refill_status,'owner or assigned clinician; seed workflow' FROM prescriptions WHERE refill_status<>'NONE';

INSERT INTO clinical_documents(patient_id,uploaded_by,category,status,original_name,stored_name,content_type,size_bytes,sha256,review_note)
SELECT ((g-1)%12)+1,CASE WHEN ((g-1)%12)+1=1 THEN 4 ELSE 3 END,
 CASE WHEN g%2=0 THEN 'PRE_VISIT_FORM' ELSE 'CLINICAL_DOCUMENT' END,'SUBMITTED','mock-document-'||g||'.txt','seed-'||g||'.txt','text/plain',48,encode(digest('Mock clinic document '||g,'sha256'),'hex'),NULL
FROM generate_series(1,20) g;
UPDATE clinical_documents SET status='VERIFIED',review_note='Identity and required fields checked.' WHERE id%4=0;
UPDATE clinical_documents SET status='REJECTED',review_note='Signature is missing; please upload a signed copy.' WHERE id IN (3,11);
INSERT INTO audit_events(actor_id,action,resource_type,resource_id,before_value,after_value,authorization_basis)
SELECT 3,'DOCUMENT_REVIEWED','ClinicalDocument',id::text,'SUBMITTED',status,'front_desk document review; seed workflow' FROM clinical_documents WHERE status<>'SUBMITTED';

INSERT INTO secure_messages(thread_key,patient_id,sender_id,subject,body,visibility,created_at)
SELECT ('00000000-0000-0000-0000-'||lpad(g::text,12,'0')),((g-1)%12)+1,
 CASE WHEN g%2=0 THEN 2 WHEN ((g-1)%12)+1=1 THEN 4 ELSE 3 END,
 (ARRAY['Preparing for your visit','Medication question','Test result follow-up','Appointment details'])[((g-1)%4)+1],
 'This is mock portal correspondence for benchmark record '||g||'. No real patient information is included.',
 CASE WHEN g IN (5,17,23) THEN 'INTERNAL_ONLY' ELSE 'PATIENT_VISIBLE' END,now()-(g||' hours')::interval
FROM generate_series(1,25) g;
INSERT INTO notifications(user_id,title,body,link) VALUES
 (4,'Appointment confirmed','Your routine consultation is scheduled.','/app/appointments.xhtml'),
 (4,'Visit summary ready','A new visit summary has been released.','/app/visits.xhtml'),
 (2,'3 refill requests waiting','Review the refill queue.','/clinical/refills.xhtml');
INSERT INTO api_keys(name,key_hash,scope) VALUES ('Local scheduling demo',encode(digest('clinic-local-demo-key','sha256'),'hex'),'availability:read');
INSERT INTO webhook_deliveries(event_type,payload,status,attempts) VALUES ('appointment.scheduled','{"appointmentId":21,"mock":true}','DELIVERED',1);
