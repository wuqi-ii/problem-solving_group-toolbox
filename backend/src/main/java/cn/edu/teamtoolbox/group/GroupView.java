package cn.edu.teamtoolbox.group;

import java.util.List;

public record GroupView(
        String id, String name, String description, String leaderId, String myRole, List<String> myPermissions
) {
    static GroupView from(GroupEntity group, String role, List<String> permissions) {
        return new GroupView(
                group.getId(), group.getName(), group.getDescription(), group.getLeaderId(), role, permissions);
    }
}
