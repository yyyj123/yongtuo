package com.yongtuo.site.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalApiExceptionHandlerTest.ErrorProbeController.class)
@Import({GlobalApiExceptionHandler.class, GlobalApiExceptionHandlerTest.ErrorProbeController.class})
class GlobalApiExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void returnsEnvelopeForUnmappedRoute() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Not Found"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().json(
                        "{\"code\":404,\"message\":\"Not Found\",\"data\":null}",
                        JsonCompareMode.STRICT));
    }

    @Test
    void returnsEnvelopeForWrongMethod() throws Exception {
        mvc.perform(post("/api/v1/test/required"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.message").value("Method Not Allowed"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(content().json(
                        "{\"code\":405,\"message\":\"Method Not Allowed\",\"data\":null}",
                        JsonCompareMode.STRICT));
    }

    @Test
    void returnsEnvelopeForInvalidRequest() throws Exception {
        mvc.perform(get("/api/v1/test/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Bad Request"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(content().json(
                        "{\"code\":400,\"message\":\"Bad Request\",\"data\":null}",
                        JsonCompareMode.STRICT));
    }

    @Test
    void hidesUnhandledExceptionDetails() throws Exception {
        mvc.perform(get("/api/v1/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Internal Server Error"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().json(
                        "{\"code\":500,\"message\":\"Internal Server Error\",\"data\":null}",
                        JsonCompareMode.STRICT));
    }

    @RestController
    @RequestMapping("/api/v1/test")
    static class ErrorProbeController {

        @GetMapping("/required")
        String required(@RequestParam String value) {
            return value;
        }

        @GetMapping("/failure")
        String failure() {
            throw new IllegalStateException("secret database detail must never leave the API");
        }
    }
}
