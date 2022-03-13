package ar.edu.algo3.peliculas.config

import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.nio.charset.StandardCharsets

@Bean
fun mappingJackson2HttpMessageConverter() =
    MappingJackson2HttpMessageConverter().apply {
        defaultCharset = StandardCharsets.UTF_8
    }
