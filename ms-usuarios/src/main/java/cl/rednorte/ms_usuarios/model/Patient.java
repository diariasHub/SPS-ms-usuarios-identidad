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
@Entity
@Table(name = "patients")
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
    private boolean activatePatient;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "patient_id")
    private List<ContactPoint> contactPointsPatient;

    @Column(name = "gender_patient", nullable = false, length = 1)
    private String genderPatient;

    @Column(name = "bithday_patient", nullable = true)
    private Timestamp bithdayPatient;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "deceased_id")
    private Deceased deceasedPatient;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "patient_id")
    private List<Address> addressesPatient;

    @Column(name = "nationality_patient", nullable = true, length = 150)
    private String nationalityPatient;


}
