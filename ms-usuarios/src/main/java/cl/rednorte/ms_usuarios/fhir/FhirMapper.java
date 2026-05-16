package cl.rednorte.ms_usuarios.fhir;

import cl.rednorte.ms_usuarios.model.Address;
import cl.rednorte.ms_usuarios.model.ContactPoint;
import cl.rednorte.ms_usuarios.model.Patient;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Locale;

/**
 * Convierte entidades JPA del dominio a recursos FHIR R4. La identidad
 * RUN se publica como {@link org.hl7.fhir.r4.model.Identifier} con system
 * propio del MS.
 */
@Component
public class FhirMapper {

    public static final String IDENTIFIER_SYSTEM_RUN = "http://rednorte.cl/fhir/identifier/run";

    public org.hl7.fhir.r4.model.Patient toFhir(Patient src) {
        org.hl7.fhir.r4.model.Patient out = new org.hl7.fhir.r4.model.Patient();
        out.setId(String.valueOf(src.getPatientId()));
        if (src.getRunPatient() != null) {
            out.addIdentifier()
                    .setSystem(IDENTIFIER_SYSTEM_RUN)
                    .setValue(src.getRunPatient());
        }
        out.setActive(src.isActivatePatient());

        if (src.getFirstNamePatient() != null || src.getLastNamePatient() != null) {
            out.addName()
                    .addGiven(src.getFirstNamePatient())
                    .setFamily(src.getLastNamePatient());
        }
        out.setGender(mapGender(src.getGenderPatient()));
        if (src.getBithdayPatient() != null) {
            out.setBirthDate(new Date(src.getBithdayPatient().getTime()));
        }

        if (src.getDeceasedPatient() != null) {
            Timestamp dt = src.getDeceasedPatient().getDeceasedDateTime();
            if (dt != null) {
                out.setDeceased(new DateTimeType(new Date(dt.getTime())));
            } else {
                out.setDeceased(new org.hl7.fhir.r4.model.BooleanType(src.getDeceasedPatient().isDeceasedStatus()));
            }
        }

        if (src.getContactPointsPatient() != null) {
            for (ContactPoint cp : src.getContactPointsPatient()) {
                out.addTelecom()
                        .setSystem(mapContactSystem(cp.getSystemContact()))
                        .setValue(cp.getValueContatc());
            }
        }

        if (src.getAddressesPatient() != null) {
            for (Address a : src.getAddressesPatient()) {
                org.hl7.fhir.r4.model.Address fa = out.addAddress();
                if (a.getLineAddress() != null) fa.addLine(a.getLineAddress());
                fa.setCity(a.getCityAddress());
                fa.setDistrict(a.getDistrictAddress());
                fa.setState(a.getStateAddress());
                fa.setCountry(a.getCountryAddress());
                fa.setUse(mapAddressUse(a.getUseAddress()));
            }
        }
        return out;
    }

    private AdministrativeGender mapGender(String g) {
        if (g == null) return AdministrativeGender.UNKNOWN;
        return switch (g.toUpperCase(Locale.ROOT)) {
            case "M" -> AdministrativeGender.MALE;
            case "F" -> AdministrativeGender.FEMALE;
            case "O" -> AdministrativeGender.OTHER;
            default -> AdministrativeGender.UNKNOWN;
        };
    }

    private ContactPointSystem mapContactSystem(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "phone" -> ContactPointSystem.PHONE;
            case "email" -> ContactPointSystem.EMAIL;
            case "fax" -> ContactPointSystem.FAX;
            case "sms" -> ContactPointSystem.SMS;
            case "url" -> ContactPointSystem.URL;
            default -> ContactPointSystem.OTHER;
        };
    }

    private org.hl7.fhir.r4.model.Address.AddressUse mapAddressUse(String u) {
        if (u == null) return null;
        return switch (u.toLowerCase(Locale.ROOT)) {
            case "home" -> org.hl7.fhir.r4.model.Address.AddressUse.HOME;
            case "work" -> org.hl7.fhir.r4.model.Address.AddressUse.WORK;
            case "temp" -> org.hl7.fhir.r4.model.Address.AddressUse.TEMP;
            case "old" -> org.hl7.fhir.r4.model.Address.AddressUse.OLD;
            case "billing" -> org.hl7.fhir.r4.model.Address.AddressUse.BILLING;
            default -> null;
        };
    }
}
