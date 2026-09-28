package com.missio.fluencia_leitora.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies GlobalExceptionHandler maps each exception type to the correct
 * ProblemDetail (RFC 7807) status and {@code code} extension field.
 */
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void businessExceptionIsMappedToItsOwnStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("TURMA_DUPLICADA"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void businessExceptionWithDetailsCopiesEachEntryToProblemDetail() throws Exception {
        mockMvc.perform(get("/test/business-exception-with-details"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NAO_CANONICA_PROIBIDA_1_ANO"))
                .andExpect(jsonPath("$.posicoes[0]").value(2));
    }

    @Test
    void optimisticLockingFailureIsMappedTo409WithConflitoDeVersaoCode() throws Exception {
        mockMvc.perform(get("/test/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLITO_DE_VERSAO"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void validationFailureIsMappedTo422WithInvalidFields() throws Exception {
        String body = objectMapper.writeValueAsString(new TestRequest(""));

        mockMvc.perform(post("/test/validate")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors[0].field").value("nome"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business-exception")
        public void triggerBusinessException() {
            throw new BusinessException(HttpStatus.CONFLICT, "TURMA_DUPLICADA", "Turma já existe");
        }

        @GetMapping("/test/business-exception-with-details")
        public void triggerBusinessExceptionWithDetails() {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "NAO_CANONICA_PROIBIDA_1_ANO",
                    "Item não canônico em lista do 1º ano",
                    java.util.Map.of("posicoes", java.util.List.of(2)));
        }

        @GetMapping("/test/optimistic-lock")
        public void triggerOptimisticLockingFailure() {
            throw new ObjectOptimisticLockingFailureException("configuracao_avaliacao", 1L);
        }

        @PostMapping("/test/validate")
        public void triggerValidationFailure(@Valid @RequestBody TestRequest request) {
            // unreachable when validation fails
        }
    }

    record TestRequest(@NotBlank String nome) {
    }
}
