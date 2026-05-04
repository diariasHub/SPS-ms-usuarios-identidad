package cl.rednorte.ms_usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.sql.Timestamp;

public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO.IDENTITY)
    @Column (name = "qualification_id", nullable = false, length = 100)
    private int qualification_id;

    @Column(name = "qualification_code", nullable = true, length = 15)
    private String qualificationCode;

    @Column (name = "qualification_period", nullable = true)
    private Timestamp qualificationPeriod;

}
