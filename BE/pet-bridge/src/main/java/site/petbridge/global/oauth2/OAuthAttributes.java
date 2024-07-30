package site.petbridge.global.oauth2;

import lombok.Builder;
import lombok.Getter;
import site.petbridge.domain.user.domain.enums.Role;
import site.petbridge.domain.user.domain.enums.SocialType;
import site.petbridge.domain.user.domain.User;
import site.petbridge.global.oauth2.userinfo.KakaoOAuth2UserInfo;
import site.petbridge.global.oauth2.userinfo.NaverOAuth2UserInfo;
import site.petbridge.global.oauth2.userinfo.OAuth2UserInfo;

import java.util.Map;
import java.util.UUID;

/**
 * 각 소셜에서 받아오는 데이터가 다르므로
 * 소셜별로 데이터를 받는 데이터를 분기 처리하는 DTO 클래스
 */
@Getter
public class OAuthAttributes {

    // OAuth2 로그인 진행 시 키가 되는 필드 값, PK와 같은 의미
    private String nameAttributeKey;
    // social Type(Kakao, Naver, Google)별 로그인 유저 정보(닉네임, 이메일, 프사 등등)
    private OAuth2UserInfo oAuth2UserInfo;

    @Builder
    public OAuthAttributes(String nameAttributeKey, OAuth2UserInfo oauth2UserInfo) {
        this.nameAttributeKey = nameAttributeKey;
        this.oAuth2UserInfo = oauth2UserInfo;
    }

    /**
     * SocialType에 맞는 메소드 호출하여 OAuthAttributes 객체 반환
     * 파라미터 : userNameAttributeName -  OAuth2 로그인 시 키(PK)가 되는 값 / attributes : OAuth 서비스의 유저 정보들
     * 소셜별 of 메소드 (ofGoogle, ofKakao, ofNaver)들은 각각 소셜 로그인 API에서 제공하는
     * 회원의 식별값(id), attributes, nameAttributeKey를 저장 후 build
     */
    public static OAuthAttributes of(SocialType socialType,
                                      String userNameAttributeName, Map<String, Object> attributes) {
        if (socialType == SocialType.NAVER) {
            return ofNaver(userNameAttributeName, attributes);
        }

        System.out.println("카카오");
        return ofKakao(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oauth2UserInfo(new KakaoOAuth2UserInfo(attributes))
                .build();
    }

//    public static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
//        return OAuthAttributes.builder()
//                .nameAttributeKey(userNameAttributeName)
//                .oauth2UserInfo(new GoogleOAuth2UserInfo(attributes))
//                .build();
//    }

    public static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oauth2UserInfo(new NaverOAuth2UserInfo(attributes))
                .build();
    }

    /**
     * of메소드로 "OAuthAttributes 객체" (nameAttributeKey, oAuth2UserInfo)가 생성되어,
     * 유저 정보들이 담긴 OAuth2UserInfo가 소셜 타입별로 주입된 상태
     *
     * OAuth2UserInfo에서 socialId(식별값), nickname, imageUrl을 가져와서 build
     * email에는 UUID로 중복없는 랜덤값 생성 (소셜이라)
     * role은 GUEST로 설정
     */
    public User toEntity(SocialType socialType, OAuth2UserInfo oauth2UserInfo) {
        return User.builder()
                .socialType(socialType)
                .socialId(oauth2UserInfo.getId())
                .email((String.valueOf(socialType) + UUID.randomUUID()).substring(0,30))
//                .nickname(oauth2UserInfo.getNickname())
                .nickname((oauth2UserInfo.getNickname() + UUID.randomUUID()).substring(0,20))
                .image(oauth2UserInfo.getImageUrl())
                .role(Role.GUEST)
                .build();
    }
}
