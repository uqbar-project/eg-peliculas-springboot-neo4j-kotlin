package ar.edu.algo3.peliculas.controller

import ar.edu.algo3.peliculas.service.ActorService
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
    fun getActores(@PathVariable filtroBusqueda: String) =
        actorService.buscarActores(filtroBusqueda)

}
