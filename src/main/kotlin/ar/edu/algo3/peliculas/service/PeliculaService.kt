package ar.edu.algo3.peliculas.service

import ar.edu.algo3.peliculas.domain.Pelicula
import ar.edu.algo3.peliculas.errorHandling.NotFoundException
import ar.edu.algo3.peliculas.repository.PeliculasRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PeliculaService {

    @Autowired
    lateinit var peliculasRepository: PeliculasRepository

    @Transactional(readOnly = true)
    fun buscarPorTitulo(titulo: String) =
        peliculasRepository.peliculasPorTitulo(titulo.contiene())

    @Transactional(readOnly = true)
    fun buscarPorId(id: String) =
        peliculasRepository.pelicula(id).orElseThrow { NotFoundException("La pelicula con identificador $id no existe")}

    @Transactional
    fun guardar(pelicula: Pelicula): Pelicula {
        pelicula.validar()
        return peliculasRepository.save(pelicula)
    }

    @Transactional
    fun eliminar(idPelicula: String): Pelicula {
        val pelicula = buscarPorId(idPelicula)
        peliculasRepository.delete(pelicula)
        return pelicula
    }

}