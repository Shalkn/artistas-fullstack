package com.artistas.api.web;

import com.artistas.api.dto.PageResponse;
import com.artistas.api.dto.artist.ArtistResponse;
import com.artistas.api.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de fatia WebMvc do {@link ArtistController}; filtros de segurança desligados para isolar JSON.
 */
@WebMvcTest(controllers = ArtistController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArtistService artistService;

    @Test
    void listarArtistasRetornaJson() throws Exception {
        when(artistService.list(isNull(), eq("asc"), eq(0), eq(10)))
                .thenReturn(new PageResponse<>(
                        List.of(new ArtistResponse(1L, "Test", 2)),
                        0, 10, 1, 1, true
                ));

        mockMvc.perform(get("/api/v1/artists").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test"));
    }
}
