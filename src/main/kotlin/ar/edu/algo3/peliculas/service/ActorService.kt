package ar.edu.algo3.peliculas.service

import ar.edu.algo3.peliculas.repository.ActoresRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ActorService {
    @Autowired
    lateinit var actoresRepository: ActoresRepository

    fun buscarActores(nombreABuscar: String) =
        actoresRepository.actores(nombreABuscar.contiene())

}