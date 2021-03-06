package org.bjartek.webfluxrefapp

import brave.propagation.ExtraFieldPropagation
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.reactive.awaitFirst
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    runApplication<WebfluxRefappApplication>(*args)
}

@SpringBootApplication
@Configuration
class WebfluxRefappApplication {

    @Bean
    fun userAgentWebClientCustomizer(@Value("\${spring.application.name}") name: String) =
        WebClientCustomizer {
            it.defaultHeader(USER_AGENT_FIELD, name)
            it.defaultHeader(KLIENTID_FIELD, name)
            it.filter { clientRequest, next ->
                val r = ClientRequest.from(clientRequest)
                    .header(MELDINGID_FIELD, UUID.randomUUID().toString())
                    .header(KORRELASJONSID_FIELD, ExtraFieldPropagation.get(KORRELASJONSID_FIELD))
                    .build()
                next.exchange(r)
            }
        }

    @Bean
    fun webclient(
        builder: WebClient.Builder
    ): WebClient {
        return builder
            .baseUrl("http://127.0.0.1:8080")
            .build()
    }
}

@Service
@RestController
@RequestMapping("/")
class Controller(
    val client: WebClient
) {

    @GetMapping("auth/foo")
    suspend fun authFoo() = mapOf("authfoo" to "bar").also {
        logger.info { it }
    }

    @GetMapping("auth/bar")
    suspend fun authBar(): JsonNode? {
        logger.info { "Auth bar begin" }
        val result = client
            .get()
            .uri("/auth/foo")
            .header(HttpHeaders.AUTHORIZATION, "Bearer token2")
            .retrieve()
            .bodyToMono<JsonNode>()
            .doOnNext {
                logger.info("Next")
            } // User is not here
            .awaitFirst()


        logger.info { "Auth bar ends" }
        return result
    }

    @GetMapping("anonymous")
    suspend fun anonymous() = "Anonymous"
}

