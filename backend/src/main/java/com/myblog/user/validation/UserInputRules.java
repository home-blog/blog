package com.myblog.user.validation;

import com.myblog.user.config.AuthProperties;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 회원 입력 규칙 (specs/001 FR-003, FR-005 ~ FR-007, FR-034). 화면 검사와 상관없이 서버가 다시 검사한다.
 * 글자 수와 허용 특수문자는 AuthProperties(application.yml)에서 읽는다.
 */
@Component
public class UserInputRules {

    /** 이메일 형식: 아이디@도메인.최상위도메인 */
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int EMAIL_MAX_LENGTH = 255;

    private final Pattern nickname;
    private final Pattern password;

    public UserInputRules(AuthProperties properties) {
        AuthProperties.Nickname n = properties.nickname();
        this.nickname = Pattern.compile("^[가-힣A-Za-z0-9]{" + n.minLength() + "," + n.maxLength() + "}$");

        AuthProperties.Password p = properties.password();
        String specials = escapeForCharClass(p.allowedSpecials());
        this.password = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[" + specials + "])"
                + "[A-Za-z\\d" + specials + "]{" + p.minLength() + "," + p.maxLength() + "}$");
    }

    /** 앞뒤 공백을 지운 뒤 이메일 형식인지. */
    public boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String value = email.strip();
        return value.length() <= EMAIL_MAX_LENGTH && EMAIL.matcher(value).matches();
    }

    /** 2~10자, 한글·영문·숫자만. 공백·특수문자 불가. */
    public boolean isValidNickname(String nickname) {
        return nickname != null && this.nickname.matcher(nickname).matches();
    }

    /** 영문·숫자·허용 특수문자를 각각 1개 이상, 8~20자, 공백 불가. */
    public boolean isValidPassword(String password) {
        return password != null && this.password.matcher(password).matches();
    }

    /** 비밀번호 확인이 비밀번호와 같은지. */
    public boolean isPasswordConfirmed(String password, String passwordConfirm) {
        return password != null && password.equals(passwordConfirm);
    }

    /** 정규식 문자 묶음([...]) 안에서 특별한 뜻을 갖는 글자를 글자 그대로 쓰이게 바꾼다. */
    static String escapeForCharClass(String chars) {
        StringBuilder sb = new StringBuilder();
        for (char c : chars.toCharArray()) {
            if (Character.isWhitespace(c)) {
                continue;
            }
            if ("\\^-[]&".indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
