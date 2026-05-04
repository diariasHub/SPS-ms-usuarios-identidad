package cl.rednorte.ms_usuarios.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id", nullable = false,length = 10)
    private int PatientId;

    @Column(name = "run_patient", nullable = false, length = 12)
    private String runPatient;

    @Column(name = "first_name_patient", nullable = true, length = 100)
    private String firstNamePatient;

    @Column(name = "last_name_patient", nullable = true, length = 100)
    private String lastNamePatient;

    @Column(name = "activate", nullable = false)
    private boolean activate;

    @OneToMany(cascade = CascadeType.ALL)
    @Column(name = "telecom", nullable = true)
    private List<ContactPoint> contactPointsPatient;

    @Column(name = "gender_patient", nullable = false, length = 1)
    private String gender;

    @Column(name = "bithday_patient", nullable = true)
    private Timestamp bithday;

    @OneToOne(cascade = CascadeType.ALL)
    @Column(name = "deceased", nullable = false)
    private Deceased deceased;

    @OneToMany(cascade = CascadeType.ALL)
    @Column(name = "address_patient", nullable = true)
    private List<Address> addresses;

    @Column(name = "nationality_patient", nullable = true, length = 150)
    private String nationalityPatient;




}
