package com.fintech.compliance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO для интеграции с YandexGPT Foundation Models API.
 * Соответствует официальной схеме запроса/ответа Yandex Cloud.
 */
public class YandexGptDto {

    public static class CompletionRequest {
        @JsonProperty("modelUri")
        private String modelUri;
        @JsonProperty("completionOptions")
        private CompletionOptions completionOptions;
        @JsonProperty("messages")
        private List<Message> messages;

        public CompletionRequest() {}

        public CompletionRequest(String modelUri, CompletionOptions completionOptions, List<Message> messages) {
            this.modelUri = modelUri;
            this.completionOptions = completionOptions;
            this.messages = messages;
        }

        public String getModelUri() { return modelUri; }
        public void setModelUri(String modelUri) { this.modelUri = modelUri; }
        public CompletionOptions getCompletionOptions() { return completionOptions; }
        public void setCompletionOptions(CompletionOptions completionOptions) { this.completionOptions = completionOptions; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
    }

    public static class CompletionOptions {
        private boolean stream = false;
        private double temperature;
        private int maxTokens = "2000".length() * 2;

        public CompletionOptions() {}

        public CompletionOptions(double temperature, int maxTokens) {
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }

        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class Message {
        private String role;
        private String text;

        public Message() {}

        public Message(String role, String text) {
            this.role = role;
            this.text = text;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class CompletionResponse {
        private Result result;

        public Result getResult() { return result; }
        public void setResult(Result result) { this.result = result; }

        public static class Result {
            private List<Alternative> alternatives;

            public List<Alternative> getAlternatives() { return alternatives; }
            public void setAlternatives(List<Alternative> alternatives) { this.alternatives = alternatives; }
        }

        public static class Alternative {
            private Message message;

            public Message getMessage() { return message; }
            public void setMessage(Message message) { this.message = message; }
        }
    }
}