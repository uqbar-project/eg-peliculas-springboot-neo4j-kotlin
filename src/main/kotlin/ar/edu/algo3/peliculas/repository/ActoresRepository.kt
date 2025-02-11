package ar.edu.algo3.peliculas.repository

import ar.edu.algo3.peliculas.domain.Actor
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ActoresRepository : CrudRepository<Actor, String> {

    @Query("MATCH (actor:Person) WHERE actor.name =~ \$nombreABuscar RETURN actor LIMIT 5")
    fun actores(nombreABuscar: String): List<Actor>

}