package cn.edu.teamtoolbox.group;

import java.time.Instant;
import java.util.List;

public record MemberView(
        String userId,
        String account,
        String nickname,
        String groupNickname,
        String role,
        Instant joinedAt,
        List<String> permissions
) {
}
