package ar.edu.algo3.peliculas.controller

import ar.edu.algo3.peliculas.domain.Pelicula
import ar.edu.algo3.peliculas.service.PeliculaService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin("*")
class PeliculaController {

    @Autowired
    lateinit var peliculaService: PeliculaService

    @GetMapping("/peliculas/{titulo}")
    fun getPeliculasPorTitulo(@PathVariable titulo: String) =
        peliculaService.buscarPorTitulo(titulo)

    @GetMapping("/pelicula/{id}")
    fun getPelicula(@PathVariable id: String) =
        peliculaService.buscarPorId(id)

    @PutMapping("/pelicula/{id}")
    fun updatePelicula(@PathVariable id: String, @RequestBody pelicula: Pelicula): Pelicula {
        peliculaService.buscarPorId(id)
        return peliculaService.guardar(pelicula)
    }

    @PostMapping("/pelicula")
    fun createPelicula(@RequestBody pelicula: Pelicula) =
        peliculaService.guardar(pelicula)

    @DeleteMapping("/pelicula/{id}")
    fun deletePelicula(@PathVariable id: String) =
        peliculaService.eliminar(id)

}