package com.javatechie.config;

import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures tool error handling explicitly for all {@code @AiService} beans.
 * <p>
 * The Spring Boot starter picks up these beans automatically (automatic wiring mode),
 * which removes the startup warning about implicit defaults and makes the app
 * immune to the planned change of those defaults.
 *
 * @see <a href="https://docs.langchain4j.dev/tutorials/tools#error-handling">Tools — Error Handling</a>
 */
@Configuration
public class ToolErrorHandlingConfig {

    /**
     * Argument errors usually originate from the LLM (invalid JSON, missing parameter),
     * and LLMs can typically self-correct — so the error text is sent back to the LLM
     * and it can retry with corrected arguments.
     */
    @Bean
    ToolArgumentsErrorHandler toolArgumentsErrorHandler() {
        return ToolArgumentsErrorHandler.sendExceptionMessageToLlm();
    }

    /**
     * Only exceptions implementing {@link dev.langchain4j.exception.ToolErrorVisibleToLlm}
     * (e.g. {@code OrderNotFoundException}) reach the LLM, using the curated text they provide.
     * Every other exception (bugs, infrastructure failures) fails the AI Service invocation
     * so it surfaces as an error instead of leaking internal details to the LLM.
     */
    @Bean
    ToolExecutionErrorHandler toolExecutionErrorHandler() {
        return ToolExecutionErrorHandler.failInvocationUnlessVisibleToLlm();
    }
}
