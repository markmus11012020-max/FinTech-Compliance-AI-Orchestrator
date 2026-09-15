package com.fintech.compliance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Типизированная конфигурация модуля compliance.
 * Содержит настройки LLM-провайдера, таймаутов и параметров российских LLM.
 */
@ConfigurationProperties(prefix = "compliance")
public class ComplianceProperties {

    private Llm llm = new Llm();
    private YandexGpt yandexgpt = new YandexGpt();
    private GigaChat gigachat = new GigaChat();

    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
    public YandexGpt getYandexgpt() { return yandexgpt; }
    public void setYandexgpt(YandexGpt yandexgpt) { this.yandexgpt = yandexgpt; }
    public GigaChat getGigachat() { return gigachat; }
    public void setGigachat(GigaChat gigachat) { this.gigachat = gigachat; }

    public static class Llm {
        private String provider = "mock";
        private int timeoutSeconds = 10;
        private double temperature = 0.2;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
    }

    public static class YandexGpt {
        private String apiKey = "";
        private String folderId = "";
        private String model = "yandexgpt/latest";
        private String url = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getFolderId() { return folderId; }
        public void setFolderId(String folderId) { this.folderId = folderId; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class GigaChat {
        private String apiKey = "";
        private String scope = "GIGACHAT_API_PERS";
        private String model = "GigaChat-Pro";
        private String url = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}