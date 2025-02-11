# Ejemplo Películas con Springboot y Neo4J

[![build](https://github.com/uqbar-project/eg-peliculas-springboot-neo4j-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/uqbar-project/eg-peliculas-springboot-neo4j-kotlin/actions/workflows/build.yml) [![codecov](https://codecov.io/gh/uqbar-project/eg-peliculas-springboot-neo4j-kotlin/branch/master/graph/badge.svg?token=if2lwvmcwi)](https://codecov.io/gh/uqbar-project/eg-peliculas-springboot-neo4j-kotlin)

## Objetivo

Testea el mapeo de una API que expone la [base de grafos de Películas que viene con Neo4J](https://neo4j.com/developer/example-project/).

## Modelo Neo4j

El ejemplo Movies que viene con Neo4j propone

* un nodo película (Movies)
* un nodo para cada actor (Person)
* y la relación entre ellos, marcada por el o los roles que cumplió cada actor en una película (ACTED_IN)

## Prerrequisitos

Solo hace falta tener instalado algún desktop de Docker, seguí las instrucciones de [esta página](https://phm.uqbar-project.org/material/software) en el párrafo `Docker`.

## Instalación

Para poder ejecutar el ejemplo abrí una consola de comandos y escribí

```bash
docker compose up
```

Eso va a levantar

- el servidor Neo4j en el puerto 7687
- y el cliente Neo4j en el puerto 7474

## Carga inicial de datos

Ingresando en un navegador a `http://localhost:7474/` te podés conectar a la base utilizando el usuario `neo4j` y la contraseña `passw0rd` (ojo que es un cero y no una o). Luego tenés que ejecutar el script que carga el grafo de películas (viene como ejemplo).

![script inicial](./images/scriptInicial.gif)

## Configuración

En el archivo [`application.yml`](./src/main/resources/application.yml) encontrarás la configuración hacia la base de grafos, que utiliza el protocolo liviano **bolt**:

```yml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: #####
```

Algunas consideraciones:

- la contraseña por defecto cuando instalás localmente Neo4J es **neo4j** pero a veces te obliga a cambiarla, acordate de sincronizar con esta configuración (de hecho en el ejemplo de Docker estamos usando passw0rd)
- el puerto por defecto para el protocolo bolt es 7687
- respecto al logging, le pusimos una configuración bastante exhaustiva: vas a ver conexiones y queries a la base. Se puede desactivar subiendo el nivel a INFO, WARN o directamente borrando la línea

## Las consultas

### Películas por título

Para conocer las películas en donde un valor de búsqueda esté contenido en el título (sin distinguir mayúsculas o minúsculas), y limitando la búsqueda a los primeros 10 nodos, ejecutaremos esta consulta

```cypher
MATCH (pelicula:Movie) WHERE pelicula.title =~ '.*Good.*' RETURN pelicula LIMIT 10
```

La interfaz _Neo4jRepository_ de Spring boot nos permite declarativamente establecer las consultas a la base, y reemplazaremos el valor concreto '.*Good.*' por el parámetro que recibe el contrato:

```kt
@Query("MATCH (pelicula:Movie) WHERE pelicula.title =~ \$titulo RETURN pelicula LIMIT 10")
fun peliculasPorTitulo(titulo: String): List<Pelicula>
```

`$titulo` es la nueva forma de asociar el valor del parámetro `titulo` (hay que respetar los mismos nombres). Dado que queremos armar la expresión _contiene_, esto debemos hacerlo antes de llamar al repositorio, en este caso es el Service):

```kt
@Transactional(readOnly = true)
fun buscarPorTitulo(titulo: String) =
    peliculasRepository.peliculasPorTitulo(titulo.contiene())
```

- la anotación `@Transactional(readOnly = true)` le marca a Springboot que no necesita enmarcar al método dentro de una transacción (de esa manera utiliza menos recursos ya que no necesita registrar lo que va haciendo para poder deshacer los cambios)
- `contiene` es en realidad un _extension method_ definido en el archivo CipherUtils:

```kt
fun String.contiene() = """(?i).*$this.*"""
```

En este caso solo queremos traer el nodo película, sin sus relaciones, por lo que el endpoint devuelve una lista de personajes vacía. Esto mejora la performance de la consulta aunque hay que exponer esta decisión a quien consuma nuestra API.

### Ver los datos de una película concreta

Cuando nos pasen un identificador de una película concreta, ahora sí queremos traer los datos de la película, más sus personajes y eso incluye los datos de cada uno de sus actores:

```cypher
MATCH (pelicula:Movie)<-[actuo_en:ACTED_IN]-(persona:Person) WHERE ELEMENTID(pelicula) = $id RETURN pelicula, collect(actuo_en), collect(persona) LIMIT 1
```

Es importante utilizar la instrucción [`collect`](https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-collect) para que agrupe correctamente los personajes y los actores.

### Id vs. elementId

> Otro dato importante es que a partir de la versión 5 de Neo4J, **el uso de identificadores Long que mapean contra el id de la base de grafos está deprecada**. Esto significa que lo ideal es utilizar otro campo, *elementId* que consiste en un UUID.

En las consultas utilizaremos `ELEMENTID` como función y no `ID`:

![Element ID en la base de grafos](./images/elementId.png)

En este ejemplo, el campo `identity` 55 no se utiliza en las consultas sino `4:25afcd88-3464-440f-a311-a6d8322f3a7c:55`. Esto evita ciertos problemas históricos que ocurrían dado que los ids numéricos se vuelven a utilizar cuando eliminamos los nodos.

Para eso es muy importante agregar esta configuración:

```kt
@Configuration
class Neo4jConfiguration {

    @Bean
    fun cypherDslConfiguration(): CypherConfiguration =
        CypherConfiguration.newConfig().withDialect(Dialect.NEO4J_5).build()

}
```

De lo contrario, cuando quieras definir un Id como String se va a quejar cuando quieras persistir un UUID (al actualizar o crear):

```bash
java.lang.ClassCastException: Cannot cast java.lang.Long to java.lang.String
	at java.base/java.lang.Class.cast(Class.java:4067) ~[?:?]
	at ar.edu.algo3.peliculas.domain.Pelicula_Accessor_iy86bw.setProperty(Unknown Source) ~[main/:?]
```

### Actualizaciones a una película

Es interesante ver que el controller delega la creación, actualización o eliminación al service, que delimita la transaccionalidad. Por ejemplo, la creación de una película:

```kt
@PostMapping("/pelicula")
@ApiOperation("Permite crear una nueva película con sus personajes.")
fun createPelicula(@RequestBody pelicula: Pelicula) =
    peliculaService.guardar(pelicula)
```

El service, a su vez, define que el método es transaccional y delega al repositorio:

```kt
@Transactional
fun guardar(pelicula: Pelicula): Pelicula {
    pelicula.validar()
    peliculasRepository.save(pelicula)
    return pelicula
}
```

Los métodos de CRUD (Create, Retrieve, Update, Delete) ni siquiera es necesario que los defina nuestra interfaz, porque ya están siendo inyectados por la interfaz Neo4jRepository (la declaratividad en su máxima expresión). El motor, en este caso Springboot, persiste el nodo película y [cualquier relación hasta el nivel de profundidad 5 que no entre en referencia circular](https://community.neo4j.com/t/repository-save-find-depth/15181). Anteriormente, existía un SessionManager donde podíamos tener un mayor control de la información que actualizábamos o recuperábamos: para algunos esto puede ser una desventaja, contra lo bueno que puede suponer delegar esa responsabilidad en un algoritmo optimizado.

## Mapeos

Mostraremos a continuación cómo es el mapeo de las películas (las anotaciones a partir de las últimas versiones de Neo4J 4.2.x cambiaron ligeramente)

```kt
@Node("Movie")
class Pelicula {

    @Id @GeneratedValue
    var id: Long? = null

    @Property(name="title")
    lateinit var titulo: String

    @Property("tagline")
    var frase: String? = null

    @Property("released")
    var anio: Int = 0

    @Relationship(type = "ACTED_IN", direction = Direction.INCOMING)
    var personajes: MutableList<Personaje> = mutableListOf()
```

Para profundizar más recomendamos ver los otros objetos de dominio en este ejemplo y [la página de mapeos de Neo4j - Spring boot](https://docs.spring.io/spring-data/neo4j/reference/object-mapping/metadata-based-mapping.html)
    
## Tests de integración

Elegimos hacer tests de integración sobre el controller

- buscando películas por título
- o buscando concretamente por id
- actualizando una película existente
- creando y luego eliminando una película
- buscando una película inexistente
- eliminando una película inexistente
- actualizando una película inexistente
- creando / actualizando películas con errores de validación (esperamos un bad request)

La parte interesante es que 

- utilizamos una base de Neo4J embebida dentro de un companion object (lo que representa un elemento _static_ o de clase)
- creamos un juego de datos de prueba antes de cada test
- al finalizar cada test eliminamos los datos creados (esto permite iniciar cada test como si fuera desde cero)

```kt
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration
class PeliculaControllerTest {
    @Autowired
    lateinit var peliculasRepository: PeliculasRepository

    @Autowired
    lateinit var actoresRepository: ActoresRepository

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
                    roles = mutableListOf("Marcos")
                    actor = darin
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
    fun `delete fixture`() {
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
    
    ...
}
```

Para profundizar más en el tema recomendamos leer [esta página](https://medium.com/neo4j/testing-your-neo4j-based-java-application-34bef487cc3c)

## Resumen de la arquitectura

![arquitectura películas](./images/arquitectura-app.png)

## Cómo testear la aplicación

Te dejamos ejemplos para probarlo en 

- [Bruno](./Peliculas_Bruno.json)
- [Postman](./Peliculas_Postman.json)
- [Insomnia](./Peliculas_Insomnia.json)
