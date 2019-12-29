package com.mechanitis.demo.stockservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * created a simple Kotlin Spring Boot application
 * that uses Reactive Streams to emit a randomly generated stock price once a second.
 *
 * https://blog.jetbrains.com/idea/2019/10/tutorial-reactive-spring-boot-a-kotlin-rest-service/
 * Run the application to see it start up correctly.
 * Open up a web browser and navigate to http://localhost:8080/stocks/DEMO,
 * you should see the events tick once a second, and see the stock price represented as a JSON string:
 * data:{"symbol":"DEMO","price":89.06318870033823,"time":"2019-10-17T17:00:25.506109"}
 *
 */

@SpringBootApplication
class StockServiceApplication

fun main(args: Array<String>) {
    runApplication<StockServiceApplication>(*args)
}

@RestController
class RestController(val priceService: PriceService) {

    @GetMapping(value = ["/stocks/{symbol}"],
            // create a server-sent events streaming endpoint
            produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun prices(@PathVariable symbol: String) = priceService.generatePrices(symbol)

}

@Controller
class RSocketController(val priceService: PriceService) {

    @MessageMapping("stockPrices")
    fun prices(symbol: String) = priceService.generatePrices(symbol)
}

@Service
class PriceService {
    private val prices = ConcurrentHashMap<String, Flux<StockPrice>>()
    //create a Flux which emits one randomly generated price every second
    fun generatePrices(symbol: String): Flux<StockPrice> {
        return prices.computeIfAbsent(symbol) {
            Flux
                    .interval(Duration.ofSeconds(1))
                    .map { StockPrice(symbol, randomStockPrice(), now()) }
                    .share()
        }
    }

    private fun randomStockPrice(): Double {// generate a number between zero and one hundred
        return ThreadLocalRandom.current().nextDouble(100.0)
    }
}

data class StockPrice(val symbol: String,
                      val price: Double,
                      val time: LocalDateTime)
