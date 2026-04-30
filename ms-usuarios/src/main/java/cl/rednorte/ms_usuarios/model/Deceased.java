package cl.rednorte.ms_usuarios.model;

import jakarta.persistence.Column;

import java.sql.Timestamp;

public class Deceased {

    @Column(name = "deceased_id",  nullable = false,length = 10)
    private int deceasedId ;

    @Column(name = "deceased_status", nullable = false)
    private boolean deceasedStatus;

    @Column(name = "deceased_date_time", nullable = true)
    private Timestamp deceasedDateTime;
}
