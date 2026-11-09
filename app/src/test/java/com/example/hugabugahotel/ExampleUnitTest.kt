package com.example.hugabugahotel

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.model.CavernSummary

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun apiClient_defaultBaseUrl_pointsToLocalhost() {
        assertEquals("http://localhost:8080/api/v1/", ApiClient.getBaseUrl())
    }

    @Test
    fun apiClient_setBaseUrl_formatsTrailingSlash() {
        ApiClient.setBaseUrl("http://192.168.1.100:8080/api/v1")
        assertEquals("http://192.168.1.100:8080/api/v1/", ApiClient.getBaseUrl())

        // Reset back to localhost
        ApiClient.setBaseUrl("http://localhost:8080/api/v1/")
    }

    @Test
    fun cavernSummary_priceCalculation_isAccurate() {
        val cavern = CavernSummary(
            id = "test-1",
            slug = "toca-teste",
            nome = "Toca Teste",
            categoria = "Toca",
            ala = "Ala Norte",
            resumo = "Resumo teste",
            capacidade = 4,
            camas = 2,
            areaM2 = 35,
            precoBaseCentavos = 35000,
            totalEstadiaCentavos = 105000,
            notaMedia = 4.9,
            totalRelatos = 10,
            acessivel = true,
            aceitaPets = true,
            fotoUrl = null,
            unidadesLivres = 1,
            ultimas = false
        )

        assertEquals(350L, cavern.precoBaseCentavos / 100)
        assertEquals(1050L, (cavern.totalEstadiaCentavos ?: 0L) / 100)
    }
}