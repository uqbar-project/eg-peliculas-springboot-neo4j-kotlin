package ar.edu.algo3.peliculas.service

import ar.edu.algo3.peliculas.domain.Pelicula
import ar.edu.algo3.peliculas.repository.PeliculasRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PeliculaService {

    @Autowired
    lateinit var peliculasRepository: PeliculasRepository

    fun buscarPorTitulo(titulo: String) =
        peliculasRepository.peliculasPorTitulo(titulo.contiene())

    fun buscarPorId(id: Long) =
        peliculasRepository.pelicula(id)

    fun guardar(pelicula: Pelicula) {
        peliculasRepository.save(pelicula)
    }

    fun eliminar(pelicula: Pelicula) {
        peliculasRepository.delete(pelicula)
    }

}