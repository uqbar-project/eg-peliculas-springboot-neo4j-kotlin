package ar.edu.algo3.peliculas

import ar.edu.algo3.peliculas.domain.Actor
import ar.edu.algo3.peliculas.domain.Pelicula
import ar.edu.algo3.peliculas.domain.Personaje
import ar.edu.algo3.peliculas.repository.PeliculasRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration
import org.neo4j.harness.Neo4j
import org.neo4j.harness.Neo4jBuilders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = [Neo4jTestHarnessAutoConfiguration::class])
class PeliculaControllerTest {
    @Autowired
    lateinit var peliculasRepository: PeliculasRepository

    @Autowired
    lateinit var mockMvc: MockMvc

    companion object {
        var mapper = ObjectMapper()
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

    @AfterEach
    fun `delete fixture`() {
        peliculasRepository.deleteAll()
    }

    @Test
    fun `la busqueda por título funciona correctamente, no importan mayusculas`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/peliculas/nueve")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].titulo").value("Nueve reinas"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].personajes.length()").value(0))
    }

    @Test
    fun `la busqueda de una pelicula trae los datos de la pelicula y sus personajes`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/pelicula/${nueveReinas.id!!}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.titulo").value("Nueve reinas"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes[0].actor.nombreCompleto").value("Ricardo Darín"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes[0].roles[0]").value("Marcos"))
    }

    @Test
    fun `actualizar una pelicula funciona correctamente`() {
        nueveReinas.apply {
            titulo = "9 Reinas"
            agregarPersonaje("Valeria", Actor().apply {
                nombreCompleto = "Leticia Brédice"
                anioNacimiento = 1975
            })
        }
        mockMvc.perform(
            MockMvcRequestBuilders.put("/pelicula/${nueveReinas.id!!}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(nueveReinas))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.titulo").value("9 Reinas"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes[2].actor.nombreCompleto").value("Leticia Brédice"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes[2].roles[0]").value("Valeria"))
    }
}