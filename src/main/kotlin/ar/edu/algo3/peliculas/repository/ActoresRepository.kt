package ar.edu.algo3.peliculas.repository

import ar.edu.algo3.peliculas.domain.Actor
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Service

@Service
interface ActoresRepository : Repository<Actor, Long>  {

    @Query("MATCH (actor:Person) WHERE actor.name =~ \$nombreABuscar RETURN actor LIMIT 5")
    fun actores(nombreABuscar: String): List<Actor>

}