public class AuthSteps {

@Step("Аутентификация пользователя: username={username}")
    public AuthenticationResponseDto login(String username, String password) {
        var loginResponse = authClient.login(new LoginRequestDto(username, password));
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        AuthenticationResponseDto tokens = loginResponse.getBody();
        assertThat(tokens).isNotNull();
        assertTokens(tokens);

        return tokens;
    }
}