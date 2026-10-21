package clinic.harbour.service;

import clinic.harbour.domain.*;
import clinic.harbour.security.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Stateless @RolesAllowed("clinic_admin")
public class ImportService {
    @PersistenceContext EntityManager em; @Inject AuthorizationService auth; @Inject AuditService audit;
    /** Imports appointment types: name,duration_minutes,fee_gbp. Entire file is validated before persistence. */
    public int importAppointmentTypes(InputStream input) throws IOException {ActingUser actor=auth.require(Role.CLINIC_ADMIN);List<Row> rows=new ArrayList<>();try(BufferedReader reader=new BufferedReader(new InputStreamReader(new BoundedInputStream(input,1_000_000),StandardCharsets.UTF_8))){String header=reader.readLine();if(!"name,duration_minutes,fee_gbp".equals(header))throw new IllegalArgumentException("Expected header: name,duration_minutes,fee_gbp");String line;int number=1;while((line=reader.readLine())!=null){number++;String[] cells=line.split(",",-1);if(cells.length!=3)throw new IllegalArgumentException("Line "+number+" must contain three columns");String name=cells[0].trim();int duration=Integer.parseInt(cells[1].trim());java.math.BigDecimal fee=new java.math.BigDecimal(cells[2].trim());if(name.length()<3||name.length()>80||duration<10||duration>240||fee.signum()<0)throw new IllegalArgumentException("Invalid values on line "+number);rows.add(new Row(name,duration,fee));if(rows.size()>200)throw new IllegalArgumentException("Import limit is 200 rows");}}for(Row row:rows){if(em.createQuery("select count(t) from AppointmentType t where lower(t.name)=:name",Long.class).setParameter("name",row.name.toLowerCase()).getSingleResult()>0)continue;em.createNativeQuery("insert into appointment_types(name,duration_minutes,fee,currency,created_at,version,active) values (?1,?2,?3,'GBP',now(),0,true)").setParameter(1,row.name).setParameter(2,row.duration).setParameter(3,row.fee).executeUpdate();}audit.record(actor,"APPOINTMENT_TYPES_IMPORTED","AppointmentType",null,null,"count="+rows.size(),"clinic_admin role");return rows.size();}
    /** Imports the vendor scheduling feed: <appointment-types><appointment-type name="" minutes="" fee=""/></appointment-types>. */
    public int importAppointmentTypesFeed(InputStream input) throws Exception {
        ActingUser actor=auth.require(Role.CLINIC_ADMIN);List<Row> rows=new ArrayList<>();
        org.xml.sax.helpers.DefaultHandler handler=new org.xml.sax.helpers.DefaultHandler(){
            @Override public void startElement(String uri,String localName,String qName,org.xml.sax.Attributes a){
                if(!"appointment-type".equals(qName))return;
                String name=String.valueOf(a.getValue("name")).trim();int duration=Integer.parseInt(String.valueOf(a.getValue("minutes")).trim());java.math.BigDecimal fee=new java.math.BigDecimal(String.valueOf(a.getValue("fee")).trim());
                if(name.length()<3||name.length()>80||duration<10||duration>240||fee.signum()<0)throw new IllegalArgumentException("Invalid values for appointment type "+name);
                if(rows.size()>=200)throw new IllegalArgumentException("Import limit is 200 rows");
                rows.add(new Row(name,duration,fee));
            }
        };
        javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().parse(new BoundedInputStream(input,1_000_000),handler);
        for(Row row:rows){if(em.createQuery("select count(t) from AppointmentType t where lower(t.name)=:name",Long.class).setParameter("name",row.name.toLowerCase()).getSingleResult()>0)continue;em.createNativeQuery("insert into appointment_types(name,duration_minutes,fee,currency,created_at,version,active) values (?1,?2,?3,'GBP',now(),0,true)").setParameter(1,row.name).setParameter(2,row.duration).setParameter(3,row.fee).executeUpdate();}
        audit.record(actor,"APPOINTMENT_TYPES_IMPORTED","AppointmentType",null,null,"count="+rows.size(),"clinic_admin role");return rows.size();
    }
    private record Row(String name,int duration,java.math.BigDecimal fee){}
    private static final class BoundedInputStream extends FilterInputStream {private final long limit;private long count;private BoundedInputStream(InputStream in,long limit){super(in);this.limit=limit;}@Override public int read()throws IOException{int value=super.read();if(value>=0&&++count>limit)throw new IOException("Import exceeds 1 MB");return value;}@Override public int read(byte[] b,int o,int l)throws IOException{int value=super.read(b,o,l);if(value>0&&(count+=value)>limit)throw new IOException("Import exceeds 1 MB");return value;}}
}
