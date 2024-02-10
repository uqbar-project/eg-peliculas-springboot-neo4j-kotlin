package ar.edu.algo3.peliculas.config

import org.neo4j.cypherdsl.core.renderer.Dialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.neo4j.cypherdsl.core.renderer.Configuration as CypherConfiguration

@Configuration
class Neo4jConfiguration {

    @Bean
    fun cypherDslConfiguration(): CypherConfiguration =
        CypherConfiguration.newConfig().withDialect(Dialect.NEO4J_5).build()

}