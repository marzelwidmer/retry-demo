package ch.keepcalm.example.retrydemo

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.getForEntity
import javax.xml.ws.ServiceMode
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory
import org.springframework.remoting.RemoteAccessException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.annotation.Recover
import org.springframework.web.client.HttpClientErrorException


@SpringBootApplication
@EnableRetry
class RetryDemoApplication() {
    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return restTemplateBuilder.build()
    }



    @Bean
    fun retryFactory(): LoadBalancedRetryFactory {
        return object : LoadBalancedRetryFactory {
            override fun createBackOffPolicy(service: String?): BackOffPolicy {
                return ExponentialBackOffPolicy()
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<RetryDemoApplication>(*args)
}

@RestController
@RequestMapping("/api")
class FooController(private val chuckNorrisService: ChuckNorrisService) {

    @GetMapping(value = ["/joke"])
    fun chuckNorris() = chuckNorrisService.getJoke()

    @GetMapping(value = ["/foo"])
    fun foo() = chuckNorrisService.getFoo()
}

@Service
class ChuckNorrisService(private val restTemplate: RestTemplate) {
    companion object {
        val URL = "https://api.chucknorris.io/jokes/random"
    }

    fun getJoke(): ChuckNorris? {
        var chuckNorris: ChuckNorris? = null
        measureTime {
            chuckNorris = this.restTemplate.getForEntity(URL, ChuckNorris::class.java).body
        }
        return chuckNorris
    }


    @Retryable(Exception::class, maxAttempts = 5, backoff = Backoff(delay = 1000))
    fun getFoo() {
        println("----------- DO SOMETHNING --------------- ")
        this.restTemplate.getForEntity(URL+"/notavalidurl", ChuckNorris::class.java).body
    }

    @Recover
    fun recoverFoo(e: HttpClientErrorException) {
        // ... panic
        println("----------- PANIC --------------- ")
    }




}

data class ChuckNorris(val value: String = "", val url: String = "")


fun measureTime(block: () -> Unit) {
    val start = System.nanoTime()
    block()
    val end = System.nanoTime()
    println("Service call took : ${(end - start) / 1.0e9} seconds")
}

//
//interface BackendAdapter {
//    @Retryable(value = [RemoteAccessException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
//    fun getBackendResponse(simulateretry: Boolean, simulateretryfallback: Boolean): String
//
//    @Recover
//    fun getBackendResponseFallback(e: RuntimeException): String
//
//}