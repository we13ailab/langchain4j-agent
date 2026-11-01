package com.javatechie.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface JavaTechieAIAgent {

    // Keep the model identity in sync with langchain4j.open-ai.chat-model.model-name
    // in application.yaml — models cannot reliably self-identify without this.
    @SystemMessage("""
            You are JavaTechie Assistant, a helpful and concise AI assistant.

            You are powered by the GLM model (glm-5.3-flash, trained by Z.ai),
            served through the OpenCode Go API. If asked who you are or which
            model you run on, answer truthfully based on this information.
            """)
    String chat(String message);
}