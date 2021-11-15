package ar.edu.algo3.peliculas.controller

import ar.edu.algo3.peliculas.domain.Pelicula
import ar.edu.algo3.peliculas.service.PeliculaService
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin("*")
class PeliculaController {

    @Autowired
    lateinit var peliculaService: PeliculaService

    @GetMapping("/peliculas/{titulo}")
    @ApiOperation("Permite conocer películas cuyo título contiene un valor a buscar, sin considerar mayúsculas o minúsculas. Por ejemplo, si se busca 'MATR' puede devolver películas como 'Matrix', 'Matrix 2', 'Matrix 3'.")
    fun getPeliculasPorTitulo(@PathVariable titulo: String) =
        peliculaService.buscarPorTitulo(titulo)

    @GetMapping("/pelicula/{id}")
    @ApiOperation("Dado un identificador de película, devuelve la información de una película con sus personajes (y actores que los representan).")
    fun getPelicula(@PathVariable id: Long) =
        peliculaService.buscarPorId(id)

    @PutMapping("/pelicula/{id}")
    @ApiOperation("Permite actualizar una película con sus personajes asociados.")
    fun updatePelicula(@PathVariable id: Long, @RequestBody pelicula: Pelicula): Pelicula {
        peliculaService.buscarPorId(id)
        return peliculaService.guardar(pelicula)
    }

    @PostMapping("/pelicula")
    @ApiOperation("Permite crear una nueva película con sus personajes.")
    fun createPelicula(@RequestBody pelicula: Pelicula) =
        peliculaService.guardar(pelicula)

    @DeleteMapping("/pelicula/{id}")
    @ApiOperation("Permite eliminar una película del sistema, esto incluirá a sus personajes (pero no a sus actores que tienen un ciclo de vida diferente).")
    fun deletePelicula(@PathVariable id: Long) =
        peliculaService.eliminar(id)

}