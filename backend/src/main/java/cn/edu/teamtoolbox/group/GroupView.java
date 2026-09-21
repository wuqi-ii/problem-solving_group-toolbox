package cn.edu.teamtoolbox.group;

public record GroupView(String id, String name, String description, String leaderId, String myRole) {
    static GroupView from(GroupEntity group, String role) {
        return new GroupView(group.getId(), group.getName(), group.getDescription(), group.getLeaderId(), role);
    }
}
