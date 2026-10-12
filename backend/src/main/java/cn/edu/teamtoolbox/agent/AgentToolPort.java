package cn.edu.teamtoolbox.agent;

/**
 * Future Agent tools implement this port and must delegate to existing business services.
 * Implementations must never bypass the current actor's authorization checks.
 */
public interface AgentToolPort<I, O> {
    AgentToolDescriptor descriptor();
    O execute(AgentExecutionContext context, I input);
}
