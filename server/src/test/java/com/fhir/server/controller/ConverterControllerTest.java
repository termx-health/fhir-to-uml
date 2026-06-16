package com.fhir.server.controller;

import com.fhir.server.service.ConverterService;
import com.fhir.server.util.Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the controller selects the response content type from the request {@code Content-Type}
 * header and returns the bytes produced by the service. The service is mocked, so no converter
 * subprocess or PlantUML server is involved.
 */
@WebMvcTest(ConverterController.class)
class ConverterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConverterService converterService;

    private static final String FHIR_BODY = "{\"resourceType\":\"StructureDefinition\"}";

    @Test
    void pngContentTypeReturnsPngResponse() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        when(converterService.convertFhirToUml(any(String.class), any(Config.class))).thenReturn(png);

        mockMvc.perform(post("/api/fhir2uml")
                        .contentType(MediaType.IMAGE_PNG)
                        .content(FHIR_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));
    }

    @Test
    void textContentTypeReturnsPlainTextResponse() throws Exception {
        byte[] uml = "@startuml\n@enduml".getBytes();
        when(converterService.convertFhirToUml(any(String.class), any(Config.class))).thenReturn(uml);

        mockMvc.perform(post("/api/fhir2uml")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(FHIR_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes(uml));
    }
}
