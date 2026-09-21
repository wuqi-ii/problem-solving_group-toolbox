package cn.edu.teamtoolbox.security;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.user.UserEntity;
import cn.edu.teamtoolbox.user.UserRepository;
import cn.edu.teamtoolbox.user.UserView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthView register(RegisterRequest request) {
        String account = normalizeAccount(request.account());
        if (userRepository.existsByAccount(account)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该账号已被注册");
        }
        UserEntity user = userRepository.save(new UserEntity(
                account, passwordEncoder.encode(request.password()), request.nickname().trim()));
        return issue(user);
    }

    @Transactional(readOnly = true)
    public AuthView login(LoginRequest request) {
        UserEntity user = userRepository.findByAccount(normalizeAccount(request.account()))
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "账号或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "账号或密码错误");
        }
        return issue(user);
    }

    private AuthView issue(UserEntity user) {
        return new AuthView(jwtService.createToken(user), "Bearer", UserView.from(user));
    }

    private String normalizeAccount(String account) {
        return account.trim().toLowerCase(Locale.ROOT);
    }
}
