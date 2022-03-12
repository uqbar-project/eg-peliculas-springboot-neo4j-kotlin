package ar.edu.algo3.peliculas.controller

import ar.edu.algo3.peliculas.service.ActorService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins=["*"])
class ActorController {

    @Autowired
    lateinit var actorService: ActorService

    @GetMapping("/actores/{filtroBusqueda}")
    @Operation(summary = "Devuelve una lista de actores cuyo nombre esté contenido en un valor de búsqueda, sin distinguir mayúsculas de minúsculas. Si por ejemplo se busca 'IT', puede devolver actores como 'Brad Pitt'.")
    fun getActores(@PathVariable filtroBusqueda: String) =
        actorService.buscarActores(filtroBusqueda)

}