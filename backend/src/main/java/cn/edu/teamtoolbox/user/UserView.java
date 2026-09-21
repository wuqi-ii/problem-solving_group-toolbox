package cn.edu.teamtoolbox.user;

public record UserView(String id, String account, String nickname, String avatarUrl) {
    public static UserView from(UserEntity user) {
        return new UserView(user.getId(), user.getAccount(), user.getNickname(), user.getAvatarUrl());
    }
}
