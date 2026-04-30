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

    @OneToMany(cascade = CascadeType.ALL)
    @Column(name = "name_patient")
    HumanName name;

    @Column(name = "activate", nullable = false)
    private boolean activate;

    @OneToMany(cascade = CascadeType.ALL)
    @Column(name = "telecom", nullable = false)
    private List<ContactPoint> contactPoints;

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

    @Column(name = "marital_status", nullable = true, length = 1)
    private String maritalStatus;


    @Column(name = "contact", nullable = true)






}
