package cn.edu.teamtoolbox.agent;

import java.util.List;

public record AgentCapabilitiesView(
        boolean enabled,
        String provider,
        String stage,
        List<AgentToolDescriptor> plannedTools
) {}
