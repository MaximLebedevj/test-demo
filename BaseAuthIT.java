package plugin.autotests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import plugin.autotests.allure.AllureEnvironmentExtension;
import plugin.autotests.steps.AuthSteps;
import plugin.orng.auth.dto.response.AuthenticationResponseDto;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@ExtendWith(AllureEnvironmentExtension.class)
public abstract class BaseAuthIT {

    @Autowired protected AuthClient authClient;
    @Autowired protected UsersClient usersClient;
    @Autowired protected ObjectMapper objectMapper;

    protected AuthenticationResponseDto tokensForCleanup;

    protected AuthSteps steps() {
        return new AuthSteps(authClient, usersClient);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    @AfterEach
    void cleanupSessions() {
        if (tokensForCleanup != null && tokensForCleanup.getAccessToken() != null) {
            safeLogoutAll(tokensForCleanup.getAccessToken());
            tokensForCleanup = null;
        }
    }

    @Step("Cleanup: logoutAll")
    protected void safeLogoutAll(String accessToken) {
        try {
            authClient.logoutAll("Bearer " + accessToken);
        } catch (Exception ignored) {

        }
    }

    @Step("Ожидаем HTTP {expectedStatus}")
    protected FeignException expectFeignStatus(int expectedStatus, ThrowingCallable call) {
        FeignException e = catchThrowableOfType(call, FeignException.class);
        assertThat(e.status()).isEqualTo(expectedStatus);
        return e;
    }

    protected JsonNode readJsonIfAny(FeignException e) {
        try {
            String body = e.contentUTF8();
            if (body == null || body.isBlank()) return null;
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse error body: " + e.contentUTF8(), ex);
        }
    }

    @Step("Проверка, что получаем Content-Type: JSON")
    protected void assertContentTypeJson(FeignException e) {
        Map<String, Collection<String>> headers = e.responseHeaders();

        assertThat(headers)
                .as("Response headers не должны быть null")
                .isNotNull();

        boolean hasJsonCt = headers.entrySet().stream()
                .filter(en -> en.getKey() != null && en.getKey().equalsIgnoreCase("content-type"))
                .flatMap(en -> en.getValue() == null ? Stream.empty() : en.getValue().stream())
                .map(String::toLowerCase)
                .anyMatch(v -> v.contains("json"));

        assertThat(hasJsonCt)
                .as("Ожидали Content-Type с json, но получили: " + headers)
                .isTrue();
    }

    @Step("Проверить, что тело ответа ошибки является JSON")
    protected JsonNode requireJson(FeignException e) {
        assertContentTypeJson(e);

        JsonNode pd = readJsonIfAny(e);
        assertThat(pd)
                .as("Ожидали JSON body с описанием ошибки, но body пустое. Body: " + e.contentUTF8())
                .isNotNull();

        return pd;
    }

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

    @Step("Проверка поля ответа error: field={field}")
    protected void assertValidationErrorField(JsonNode pd, String field, String expectedMessage) {
        JsonNode errors = pd.path("errors");
        assertThat(errors.isObject())
                .as("errors должен быть JSON object")
                .isTrue();

        assertThat(errors.path(field).asText())
                .as("errors.%s".formatted(field))
                .isEqualTo(expectedMessage);
    }
}
