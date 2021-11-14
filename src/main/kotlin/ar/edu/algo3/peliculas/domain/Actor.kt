package ar.edu.algo3.peliculas.domain

import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property

@Node("Person")
class Actor {
    @Id @GeneratedValue
    var id: Long? = null

    @Property("name")
    lateinit var nombreCompleto: String

    @Property("born")
    var anioNacimiento: Int? = null
}