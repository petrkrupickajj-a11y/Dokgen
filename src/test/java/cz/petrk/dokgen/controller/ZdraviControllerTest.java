package cz.petrk.dokgen.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ZdraviController.class)
@WithMockUser
class ZdraviControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void zdraviVraciOk() throws Exception {
        mockMvc.perform(get("/zdravi"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
