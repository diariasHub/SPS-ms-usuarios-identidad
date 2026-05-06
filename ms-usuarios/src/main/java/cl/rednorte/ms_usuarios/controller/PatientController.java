package cl.rednorte.ms_usuarios.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.web.bind.annotation.*;

import cl.rednorte.ms_usuarios.dto.PatientDTO;
import cl.rednorte.ms_usuarios.service.PatientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v2/patient")
@Tag(name = "Pacientes RedNorte", description = "Gestión de pacientes")
public class PatientController {

    private final PatientService patientService;

    @Operation(summary = "Listar todos los pacientes", description = "Obtiene un listado completo de los pacientes registrados")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<PatientDTO>> findAll() {
        return ResponseEntity.ok(patientService.findAll());
    }

    @Operation(summary = "Buscar un paciente por su numero de ID", description = "Obtiene un paciente buscando por el numero de id registrado")
    @ApiResponse(responseCode = "200", description = "Paciente obtenido correctamente")
    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> findById(@PathVariable int id) {
        return ResponseEntity.ok(patientService.findById(id).orElse(null));
    }

    @Operation(summary = "Crear un nuevo paciente", description = "Registra un nuevo paciente")
    @ApiResponse(responseCode = "201", description = "Paciente creado correctamente")
    @PostMapping
    public ResponseEntity<PatientDTO> save(@RequestBody PatientDTO patientDTO) {
        return ResponseEntity.ok(patientService.save(patientDTO));
    }

    @Operation(summary = "Actualizar un paciente", description = "Actualiza un paciente registrado")
    @ApiResponse(responseCode = "200", description = "Paciente actualizado correctamente")
    @PutMapping("/{id}")
    public ResponseEntity<PatientDTO> update(@PathVariable int id, @RequestBody PatientDTO patientDTO) {
        return ResponseEntity.ok(patientService.update(id, patientDTO));
    }

    @Operation(summary = "Eliminar un paciente", description = "Elimina un paciente registrado")
    @ApiResponse(responseCode = "200", description = "Paciente eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable int id) {
        patientService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
