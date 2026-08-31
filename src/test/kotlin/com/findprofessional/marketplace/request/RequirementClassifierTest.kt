package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.ServiceAlias
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RequirementClassifierTest {
    private val classifier = RequirementClassifier()
    private val category = category()
    private val construction = MarketplaceService(
        category = category,
        code = "HOUSE_CONSTRUCTION",
        name = "House construction",
        shortDescription = "Build a new home",
        iconKey = "construction",
        searchKeywords = "build house home construction turnkey"
    )

    @Test
    fun `matches service from customer language and catalog keywords`() {
        val result = classifier.classify(
            "I want to build a new house on my land",
            listOf(construction)
        )

        assertEquals("HOUSE_CONSTRUCTION", result?.code)
    }

    @Test
    fun `returns null when no catalog term matches`() {
        assertNull(classifier.classify("I need some advice", listOf(construction)))
    }

    @Test
    fun `weighted alias produces explainable high confidence match`() {
        val gardenMaintenance = service(
            "GARDEN_MAINTENANCE",
            "Garden maintenance",
            "garden lawn grass"
        )
        val result = classifier.rank(
            "I need to cut my garden grass",
            listOf(gardenMaintenance),
            listOf(ServiceAlias(service = gardenMaintenance, phrase = "garden grass", weight = 100))
        ).single()

        assertEquals(MatchConfidence.HIGH, result.confidence)
        assertEquals("garden grass", result.matchedPhrase)
    }

    @Test
    fun `word boundaries prevent partial keyword matches`() {
        val carRepair = service("CAR_REPAIR", "Car repair", "car auto")

        assertNull(classifier.classify("I need career advice", listOf(carRepair)))
    }

    @ParameterizedTest
    @CsvSource(
        "'Please deep clean my apartment', HOME_CLEANING",
        "'My kitchen drain is blocked', PLUMBING",
        "'The electrical socket has no power', ELECTRICAL",
        "'The laminate floor needs repair', FLOORING",
        "'My washing machine is leaking', APPLIANCE_REPAIR"
    )
    fun `classifies common home requests`(description: String, expectedCode: String) {
        val services = listOf(
            service("HOME_CLEANING", "Cleaning", "clean cleaner apartment deep"),
            service("PLUMBING", "Plumbing", "pipe leak leaking drain blocked water"),
            service("ELECTRICAL", "Electrical work", "electric electrical socket power"),
            service("FLOORING", "Flooring", "floor flooring laminate repair"),
            service(
                "APPLIANCE_REPAIR",
                "Appliance repair",
                "appliance washing machine dishwasher leaking repair"
            )
        )

        assertEquals(expectedCode, classifier.classify(description, services)?.code)
    }

    @ParameterizedTest
    @CsvSource(
        "'My car broke down and needs towing', TOWING_ROADSIDE",
        "'I need movers from Stockholm to Uppsala', MOVING_HELP",
        "'I need help packing boxes for my move', PACKING_HELP",
        "'Improve my Wi-Fi router coverage', NETWORK_SETUP",
        "'Create an SEO advertising campaign', DIGITAL_MARKETING",
        "'We need catering food for our wedding', EVENT_CATERING",
        "'I want private Spanish language lessons', LANGUAGE_LESSONS",
        "'I need to learn Swedish', LANGUAGE_LESSONS",
        "'Fix my garden', GARDEN_MAINTENANCE",
        "'Cut the grass', GARDEN_MAINTENANCE",
        "'Prune the branches on a large tree', TREE_CARE",
        "'I need someone to walk my dog', PET_CARE"
    )
    fun `classifies services across marketplace categories`(description: String, expectedCode: String) {
        val services = listOf(
            service("TOWING_ROADSIDE", "Towing and roadside help", "tow towing breakdown recovery"),
            service("MOVING_HELP", "Moving", "move mover moving transport furniture relocation"),
            service("PACKING_HELP", "Packing help", "packing boxes moving relocation"),
            service("NETWORK_SETUP", "Network and Wi-Fi setup", "wifi router network coverage"),
            service("DIGITAL_MARKETING", "Digital marketing", "seo advertising campaign marketing"),
            service("EVENT_CATERING", "Event catering", "catering food wedding event"),
            service("LANGUAGE_LESSONS", "Language lessons", "spanish swedish language teacher lessons learn"),
            service(
                "GARDEN_MAINTENANCE",
                "Garden maintenance",
                "garden gardener grass lawn mow mowing cut cutting hedge landscaping"
            ),
            service("TREE_CARE", "Tree care", "tree pruning branches arborist"),
            service("PET_CARE", "Pet care", "pet dog walk walking sitting")
        )

        assertEquals(expectedCode, classifier.classify(description, services)?.code)
    }

    private fun service(code: String, name: String, keywords: String) = MarketplaceService(
        category = category,
        code = code,
        name = name,
        shortDescription = name,
        iconKey = "home_repair_service",
        searchKeywords = keywords
    )
}
