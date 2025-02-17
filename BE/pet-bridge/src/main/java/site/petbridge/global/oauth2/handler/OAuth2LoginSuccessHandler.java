package site.petbridge.global.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import site.petbridge.domain.user.domain.User;
import site.petbridge.domain.user.domain.enums.Role;
import site.petbridge.domain.user.repository.UserRepository;
import site.petbridge.global.jwt.service.JwtService;
import site.petbridge.global.oauth2.CustomOAuth2User;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

//    @Value("${redirectURL}")
    private static String REDIRECT_URL = "https://i11b106.p.ssafy.io";
    private final JwtService jwtService;
    private static final String GUEST_URI = REDIRECT_URL + "/users/social/update";
    private static final String USER_URI = REDIRECT_URL + "/users/social/success";
    private final UserRepository userRepository;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!!");
        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

            loginSuccess(response, oAuth2User);
        } catch (Exception e) {
            throw e;
        }
    }

    // TODO : 소셜 로그인 시에도 무조건 토큰 생성하지 말고 JWT 인증 필터처럼 RefreshToken 유/무에 따라 다르게 처리해보기
    private void loginSuccess(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {
        String accessToken = jwtService.createAccessToken(oAuth2User.getEmail());
        String refreshToken = jwtService.createRefreshToken();
        response.addHeader(jwtService.getAccessHeader(), "Bearer " + accessToken);
        response.addHeader(jwtService.getRefreshHeader(), "Bearer " + refreshToken);

        System.out.println("로그인 성공했으니 access, refresh 둘다 보내줄게");
        System.out.println("accessToken: " + accessToken);
        System.out.println("refreshToken = " + refreshToken);
        jwtService.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        jwtService.updateRefreshToken(oAuth2User.getEmail(), refreshToken);

        String redirectUrl = "";
        // 토큰 전달을 위한 redirect
        if (oAuth2User.getRole() == Role.GUEST) {
            redirectUrl = UriComponentsBuilder.fromUriString(GUEST_URI)
                    .queryParam("access-token", accessToken)
                    .queryParam("refresh-token", refreshToken)
                    .build().toUriString();
            User user = userRepository.findByEmail(oAuth2User.getEmail()).get();
            user.authorizeSocialUser();
        } else if (oAuth2User.getRole() == Role.USER) {
            redirectUrl = UriComponentsBuilder.fromUriString(USER_URI)
                    .queryParam("access-token", accessToken)
                    .queryParam("refresh-token", refreshToken)
                    .build().toUriString();
        }

        response.sendRedirect(redirectUrl);
    }

}
