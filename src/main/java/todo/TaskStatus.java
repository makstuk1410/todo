package todo;

public enum TaskStatus {
    NOT_STARTED("not_started"),
    IN_PROGRESS("in_progress"),
    DONE("done"),
    ABANDONED("abandoned");

    private final String code;

    TaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        // show a more user-friendly label
        return switch (this) {
            case NOT_STARTED -> "Not started";
            case IN_PROGRESS -> "In progress";
            case DONE -> "Done";
            case ABANDONED -> "Abandoned";
        };
    }

    public static TaskStatus fromCode(String code) {
        for (TaskStatus s : values()) if (s.code.equals(code)) return s;
        return NOT_STARTED;
    }
}
