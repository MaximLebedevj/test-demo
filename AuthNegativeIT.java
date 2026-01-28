class AuthNegativeIT extends BaseAuthIT {

@Test
    @Story("Аутентификация (login)")
    @DisplayName("login: неверный логин или пароль -> 404")
    void login_invalidCredentials_shouldReturn404() {
        String username = TestDataFactory.uniqueUsername();
        String password = TestDataFactory.password(16);

        FeignException e = step("Аутентификация с неверным логином или паролем должна вернуть 404", () ->
                expectFeignStatus(404, () -> authClient.login(new LoginRequestDto(username, password)))
        );

        step("Проверка контракта ошибки", () ->
                requireProblem(
                        e,
                        "/auth/login",
                        "Invalid credentials",
                        "Неверный логин или пароль."
                )
        );
    }
}