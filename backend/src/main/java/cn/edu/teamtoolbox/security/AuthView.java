package cn.edu.teamtoolbox.security;

import cn.edu.teamtoolbox.user.UserView;

public record AuthView(String accessToken, String tokenType, UserView user) {
}
