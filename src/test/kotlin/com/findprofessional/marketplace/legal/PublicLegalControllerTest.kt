package com.findprofessional.marketplace.legal

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PublicLegalControllerTest {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(PublicLegalController()).build()
    }

    @Test
    fun `privacy policy is served as public html`() {
        mockMvc.perform(get("/privacy"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("Privacy Policy")))
            .andExpect(content().string(containsString("privacy@getarbio.com")))
    }

    @Test
    fun `account deletion instructions are served as public html`() {
        mockMvc.perform(get("/account-deletion"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("Delete your Arbio account")))
            .andExpect(content().string(containsString("privacy@getarbio.com")))
    }

    @Test
    fun `legal stylesheet is served as css`() {
        mockMvc.perform(get("/legal/legal.css"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/css")))
            .andExpect(content().string(containsString("--forest")))
    }
}
