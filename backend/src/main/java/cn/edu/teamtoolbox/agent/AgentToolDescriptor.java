package cn.edu.teamtoolbox.agent;

public record AgentToolDescriptor(
        String name,
        String description,
        String riskLevel,
        boolean confirmationRequired
) {}
