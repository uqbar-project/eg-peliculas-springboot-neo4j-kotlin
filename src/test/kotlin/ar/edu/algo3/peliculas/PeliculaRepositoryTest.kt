package ar.edu.algo3.peliculas

import ar.edu.algo3.peliculas.domain.Actor
import ar.edu.algo3.peliculas.domain.Pelicula
import ar.edu.algo3.peliculas.domain.Personaje
import ar.edu.algo3.peliculas.repository.PeliculasRepository
import org.junit.jupiter.api.*
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration
import org.neo4j.harness.Neo4j
import org.neo4j.harness.Neo4jBuilders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = [Neo4jTestHarnessAutoConfiguration::class])
class PeliculaRepositoryTest {
    @Autowired
    lateinit var peliculasRepository: PeliculasRepository

    companion object {
        lateinit var embeddedDatabaseServer: Neo4j

        @BeforeAll
        @JvmStatic
        fun initializeNeo4j() {
            embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withFixture(
                    ""
                            + "CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})\n"
                            + "CREATE (TheMatrixReloaded:Movie {title:'The Matrix Reloaded', released:2003, tagline:'Free your mind'})\n"
                            + "CREATE (TheMatrixRevolutions:Movie {title:'The Matrix Revolutions', released:2003, tagline:'Everything that has a beginning has an end'})\n"
                )
                .build()
        }

        @AfterAll
        @JvmStatic
        fun stopNeo4j() {
            embeddedDatabaseServer.close()
        }

        @DynamicPropertySource
        @JvmStatic
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri", embeddedDatabaseServer::boltURI)
            registry.add("spring.neo4j.authentication.username") { "neo4j" }
            registry.add("spring.neo4j.authentication.password") { null }
        }
    }

    lateinit var nueveReinas: Pelicula

    @BeforeEach
    fun init() {
        val darin = Actor().apply {
            nombreCompleto = "Ricardo Darín"
            anioNacimiento = 1957
        }
        nueveReinas = peliculasRepository.save(Pelicula().apply {
            titulo = "Nueve reinas"
            frase = "Dos estafadores, una mujer... y mucho dinero"
            anio = 1998
            personajes = mutableListOf(
                Personaje().apply {
                    roles = listOf("Marcos")
                    actor = darin
                },
                Personaje().apply {
                    roles = listOf("Juan")
                    actor = Actor().apply {
                        nombreCompleto = "Gastón Pauls"
                        anioNacimiento = 1972
                    }
                }
            )
        })
        peliculasRepository.save(Pelicula().apply {
            titulo = "Tiempo de valientes"
            frase = "Los tiempos cambian. Los héroes también."
            anio = 2005
        })

    }

    @Test
    @DisplayName("la búsqueda por título funciona correctamente")
    fun testPeliculasPorTitulo() {
        val peliculas = peliculasRepository.peliculasPorTitulo("""(?i).*nueve.*""")
        Assertions.assertEquals(1, peliculas.size)
        Assertions.assertEquals(0, peliculas.first().personajes.size)
    }

    @Test
    @DisplayName("la búsqueda de una película trae los datos de la película y sus personajes")
    fun testPeliculaConcreta() {
        val pelicula = peliculasRepository.pelicula(nueveReinas.id!!)
        Assertions.assertEquals("Nueve reinas", pelicula.titulo)
        Assertions.assertEquals(2, pelicula.personajes.size)
        val darin = pelicula.personajes.findLast { it.representadoPor("Ricardo Darín") }
        Assertions.assertEquals("Marcos", darin!!.roles.first())
    }
}
