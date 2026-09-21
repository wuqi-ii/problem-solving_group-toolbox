package cn.edu.teamtoolbox.common.error;

public enum ErrorCode {
    INVALID_ARGUMENT("INVALID_ARGUMENT", "请求参数不正确"),
    UNAUTHENTICATED("UNAUTHENTICATED", "请先登录"),
    FORBIDDEN("FORBIDDEN", "没有执行该操作的权限"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "目标资源不存在"),
    CONFLICT("CONFLICT", "当前状态不允许该操作"),
    INTERNAL_ERROR("INTERNAL_ERROR", "系统暂时无法完成请求");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
