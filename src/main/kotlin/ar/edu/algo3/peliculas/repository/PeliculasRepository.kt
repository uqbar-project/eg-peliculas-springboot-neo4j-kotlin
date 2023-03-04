package ar.edu.algo3.peliculas.repository

import ar.edu.algo3.peliculas.domain.Pelicula
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PeliculasRepository : Neo4jRepository<Pelicula, Long>  {

    @Query("MATCH (pelicula:Movie) WHERE pelicula.title =~ \$titulo RETURN pelicula LIMIT 10")
    fun peliculasPorTitulo(titulo: String): List<Pelicula>

    @Query("MATCH (pelicula:Movie) OPTIONAL MATCH (pelicula)<-[actuo_en:ACTED_IN]-(persona:Person) WITH pelicula, actuo_en, persona ORDER BY ID(persona) WHERE ID(pelicula) = \$id RETURN pelicula, COLLECT(actuo_en), COLLECT(persona) LIMIT 1")
    fun pelicula(id: Long): Optional<Pelicula>

}