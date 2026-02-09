package ar.edu.algo3.peliculas

import ar.edu.algo3.peliculas.domain.Actor
import ar.edu.algo3.peliculas.domain.Pelicula
import ar.edu.algo3.peliculas.domain.Personaje
import ar.edu.algo3.peliculas.repository.ActoresRepository
import ar.edu.algo3.peliculas.repository.PeliculasRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration
@Testcontainers
class PeliculaControllerTest {
    @Autowired
    lateinit var peliculasRepository: PeliculasRepository

    @Autowired
    lateinit var actoresRepository: ActoresRepository

    @Autowired
    lateinit var mockMvc: MockMvc

    companion object {
        var objectMapper = ObjectMapper()

        @Container
        @JvmStatic
        val contenedorNeo4j = Neo4jContainer("neo4j:5.12.0")
            .withAdminPassword("passwordSuperSegura")
            // CI es lento, le damos hasta 2 minutos para arrancar
            .withStartupTimeout(Duration.ofMinutes(2))
            // Limitamos memoria para que el runner de GitHub no lo mate (OOM)
            .withEnv("NEO4J_dbms_memory_pagecache_size", "512M")
            .withEnv("NEO4J_dbms_memory_heap_max__size", "512M")

        @DynamicPropertySource
        @JvmStatic
        fun propiedadesNeo4j(registroDePropiedades: DynamicPropertyRegistry) {
            registroDePropiedades.add("spring.neo4j.uri") { contenedorNeo4j.boltUrl }
            registroDePropiedades.add("spring.neo4j.authentication.username") { "neo4j" }
            registroDePropiedades.add("spring.neo4j.authentication.password") { contenedorNeo4j.adminPassword }
        }
    }

    lateinit var nueveReinas: Pelicula

    @BeforeEach
    fun inicializarDatos() {
        val ricardoDarin = Actor().apply {
            nombreCompleto = "Ricardo Darín"
            anioNacimiento = 1957
        }
        nueveReinas = peliculasRepository.save(Pelicula().apply {
            titulo = "Nueve reinas"
            frase = "Dos estafadores, una mujer... y mucho dinero"
            anio = 1998
            personajes = mutableListOf(
                Personaje().apply {
                    roles = mutableListOf("Marcos")
                    actor = ricardoDarin
                },
                Personaje().apply {
                    roles = mutableListOf("Juan")
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
    fun `eliminar datos de prueba`() {
        actoresRepository.deleteAll()
        peliculasRepository.deleteAll()
    }

    @Test
    fun `la busqueda por titulo funciona correctamente, no importan mayusculas`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/peliculas/nueve")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
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
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
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
                .content(objectMapper.writeValueAsString(nueveReinas))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.titulo").value("9 Reinas"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes[2].actor.nombreCompleto").value("Leticia Brédice"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes[2].roles[0]").value("Valeria"))
    }

    @Test
    fun `crear una pelicula funciona correctamente`() {
        val plataDulce = Pelicula().apply {
            titulo = "Plata Dulce"
            anio = 1982
            frase = ""
            agregarPersonaje("Carlos Bonifatti", Actor().apply {
                nombreCompleto = "Federico Luppi"
                anioNacimiento = 1936
            })
            agregarPersonaje("Rubén Molinuevo", Actor().apply {
                nombreCompleto = "Julio de Grazia"
                anioNacimiento = 1929
            })
        }
        val resultadoDeLaPeticion = mockMvc.perform(
            MockMvcRequestBuilders.post("/pelicula")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(plataDulce))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.titulo").value("Plata Dulce"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.personajes.length()").value(2))

        val plataDulceCreada = objectMapper.readValue(resultadoDeLaPeticion.andReturn().response.getContentAsString(), Pelicula::class.java)

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/pelicula/${plataDulceCreada.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(plataDulce))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/peliculas/plata")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0))
    }

    @Test
    fun `la busqueda de una pelicula que no existe da un codigo de error http 404`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/pelicula/10910111")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `eliminar una pelicula que no existe da un codigo de error http 404`() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/pelicula/10910111")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `actualizar una pelicula que no existe da un codigo de error http 404`() {
        nueveReinas.apply {
            titulo = "9 Reinas"
            agregarPersonaje("Valeria", Actor().apply {
                nombreCompleto = "Leticia Brédice"
                anioNacimiento = 1975
            })
        }
        mockMvc.perform(
            MockMvcRequestBuilders.put("/pelicula/91724681624")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nueveReinas))
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `crear una pelicula sin titulo da error de validacion`() {
        val peliculaConError = Pelicula().apply {
            titulo = ""
        }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pelicula")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(peliculaConError))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `crear una pelicula con un anio demasiado bajo da error de validacion`() {
        val peliculaConError = Pelicula().apply {
            titulo = "una peli"
            anio = 1490
        }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pelicula")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(peliculaConError))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `crear una pelicula con un personaje sin rol da error de validacion`() {
        val peliculaConError = Pelicula().apply {
            titulo = "una peli"
            anio = 1990
            personajes = mutableListOf(Personaje().apply {
                actor = Actor().apply { nombreCompleto = "Ricardo Darín " }
            })
        }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pelicula")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(peliculaConError))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `crear una pelicula con un personaje sin actor da error de validacion`() {
        val peliculaConError = Pelicula().apply {
            titulo = "una peli"
            anio = 1990
            personajes = mutableListOf(Personaje().apply { roles = mutableListOf("Cacho") })
        }
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pelicula")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(peliculaConError))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `la busqueda de actores funciona correctamente`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/actores/dar")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].nombreCompleto").value("Ricardo Darín"))
    }
}