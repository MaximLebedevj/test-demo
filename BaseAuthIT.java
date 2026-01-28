public abstract class BaseAuthIT {

@Step("Проверка контракта ошибок: title={title}")
    protected JsonNode requireProblem(FeignException e, String instance, String title, String detail) {
        JsonNode pd = requireJson(e);

        assertThat(pd).isNotNull();
        assertThat(pd.path("instance").asText()).isEqualTo(instance);

        assertThat(pd.path("type").asText()).isNotBlank();
        assertThat(pd.path("title").asText()).isNotBlank();
        assertThat(pd.path("detail").asText()).isNotBlank();

        assertThat(pd.path("type").asText()).isEqualTo("about:blank");
        assertThat(pd.path("title").asText()).isEqualTo(title);
        assertThat(pd.path("detail").asText()).isEqualTo(detail);

        return pd;
    }
}