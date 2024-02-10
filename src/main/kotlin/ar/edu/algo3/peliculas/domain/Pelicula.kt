package ar.edu.algo3.peliculas.domain

import ar.edu.algo3.peliculas.errorHandling.UserException
import org.springframework.data.neo4j.core.schema.*
import org.springframework.data.neo4j.core.schema.Relationship.Direction

const val MINIMO_VALOR_ANIO = 1900

// https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#mapping
@Node("Movie")
class Pelicula {

    @Id @GeneratedValue
    var id: String? = null

    @Property(name="title")
    lateinit var titulo: String

    @Property("tagline")
    var frase: String? = null

    @Property("released")
    var anio: Int = 0

    @Relationship(type = "ACTED_IN", direction = Direction.INCOMING)
    var personajes: MutableList<Personaje> = mutableListOf()

    fun agregarPersonaje(_roles: String, _actor: Actor) {
        val personaje = Personaje().apply {
            roles = mutableListOf(_roles)
            actor = _actor
            validar()
        }
        personajes.add(personaje)
    }

    fun eliminarPersonaje(personaje: Personaje) {
        personajes.remove(personaje)
    }

    fun validar() {
        if (this.titulo.trim().isEmpty()) {
            throw UserException("Debe ingresar un título")
        }
        if (this.anio <= MINIMO_VALOR_ANIO) {
            throw UserException("El año debe ser mayor a " + MINIMO_VALOR_ANIO)
        }
        personajes.forEach { it.validar() }
    }
}