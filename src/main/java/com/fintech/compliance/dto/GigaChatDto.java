package com.fintech.compliance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO для интеграции с GigaChat API (Сбер).
 */
public class GigaChatDto {

    public static class ChatRequest {
        private String model;
        private List<Msg> messages;
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens = 1024;
        @JsonProperty("response_format")
        private String responseFormat = "json";

        public ChatRequest() {}

        public ChatRequest(String model, List<Msg> messages, double temperature) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
        }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Msg> getMessages() { return messages; }
        public void setMessages(List<Msg> messages) { this.messages = messages; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public String getResponseFormat() { return responseFormat; }
        public void setResponseFormat(String responseFormat) { this.responseFormat = responseFormat; }
    }

    public static class Msg {
        private String role;
        private String content;

        public Msg() {}

        public Msg(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class ChatResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }

        public static class Choice {
            private Msg message;
            public Msg getMessage() { return message; }
            public void setMessage(Msg message) { this.message = message; }
        }
    }
}