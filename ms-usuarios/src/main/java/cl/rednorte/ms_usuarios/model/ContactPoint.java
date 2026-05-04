package cl.rednorte.ms_usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class ContactPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_point_id", nullable = false,length = 200)
    private int contactPointId;

    @Column(name = "system_contact", nullable = true, length = 10)
    private String systemContact;

    @Column(name = "value_contact", nullable = true, length = 200)
    private String valueContatc;

}
