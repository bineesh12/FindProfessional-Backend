package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.auth.anyValue
import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.service.MarketplaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID
import org.springframework.http.HttpStatus

class RequestLocationServiceTest {
    private val coordinates = mock(PostcodeCoordinateRepository::class.java)
    private val locations = mock(RequestLocationRepository::class.java)
    private val geocoder = mock(PostcodeGeocodingClient::class.java)
    private val service = RequestLocationService(
        coordinates,
        locations,
        geocoder,
        PostcodeResolverProperties()
    )

    init {
        doAnswer { it.arguments[0] }.`when`(coordinates).save(anyValue())
    }

    @Test
    fun `resolves and stores service location coordinates`() {
        val request = request()
        `when`(coordinates.findByCountryCodeAndPostalCode("se", "418 33"))
            .thenReturn(Optional.empty())
        `when`(geocoder.resolve("se", "418 33"))
            .thenReturn(ResolvedPostcode(57.72, 11.95, "TEST"))

        service.saveResolvedLocations(
            request,
            mapOf("service_location" to "Gothenburg", "service_postcode" to "418 33")
        )

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<RequestLocation>>
        verify(locations).saveAll(captor.capture())
        val stored = captor.value.single()
        assertEquals(RequestLocationKind.SERVICE, stored.kind)
        assertEquals("Gothenburg", stored.municipality)
        assertEquals(57.72, stored.latitude)
        assertEquals(11.95, stored.longitude)
    }

    @Test
    fun `reuses cached postcode coordinates`() {
        val request = request()
        `when`(coordinates.findByCountryCodeAndPostalCode("se", "418 33"))
            .thenReturn(Optional.of(PostcodeCoordinate(
                countryCode = "se",
                postalCode = "418 33",
                latitude = 57.72,
                longitude = 11.95,
                source = "CACHE"
            )))

        service.saveResolvedLocations(
            request,
            mapOf("service_location" to "Gothenburg", "service_postcode" to "418 33")
        )

        verifyNoInteractions(geocoder)
        verify(locations).saveAll(anyValue<List<RequestLocation>>())
    }

    @Test
    fun `rejects a location without postcode`() {
        assertThrows(RequestException::class.java) {
            service.saveResolvedLocations(request(), mapOf("service_location" to "Gothenburg"))
        }
    }

    @Test
    fun `reports resolver outages separately from unknown postcodes`() {
        `when`(coordinates.findByCountryCodeAndPostalCode("se", "418 33"))
            .thenReturn(Optional.empty())
        `when`(geocoder.resolve("se", "418 33")).thenThrow(IllegalStateException("offline"))

        val exception = assertThrows(RequestException::class.java) {
            service.saveResolvedLocations(
                request(),
                mapOf("service_location" to "Gothenburg", "service_postcode" to "418 33")
            )
        }

        assertEquals("LOCATION_RESOLVER_UNAVAILABLE", exception.code)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status)
    }

    private fun request(): CustomerRequest {
        val category = category()
        val marketplaceService = MarketplaceService(
            category = category,
            code = "PLUMBING",
            name = "Plumbing",
            shortDescription = "Plumbing help",
            iconKey = "plumbing"
        )
        val session = RequestSession(
            customerId = UUID.randomUUID(),
            category = category,
            service = marketplaceService,
            status = RequestSessionStatus.READY_FOR_REVIEW
        )
        return CustomerRequest(
            session = session,
            customerId = session.customerId,
            category = category,
            service = marketplaceService,
            title = "Fix a leak",
            description = "Repair a leaking pipe in the bathroom."
        )
    }
}
