package cl.rednorte.ms_usuarios.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

public class Contact {

    @Column(name = "contact_id", nullable = false, length = 10)
    private int contactId;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @OneToMany(cascade = CascadeType.ALL)
    @Column(name = "name_contact", nullable = false, length = 100)
    HumanName nameContact;

    @Column(name = "additional_name", nullable = false, length = 100)
    HumanName additionalName;



}
