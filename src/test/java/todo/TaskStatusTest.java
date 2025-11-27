package todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;

@DisplayName("TaskStatus enum tests")
public class TaskStatusTest {

    @Test
    @DisplayName("Codes are correct for all statuses")
    public void codesAreCorrect() {
        assertEquals("not_started", TaskStatus.NOT_STARTED.getCode());
        assertEquals("in_progress", TaskStatus.IN_PROGRESS.getCode());
        assertEquals("done", TaskStatus.DONE.getCode());
        assertEquals("abandoned", TaskStatus.ABANDONED.getCode());
    }

    @Test
    @DisplayName("toString returns human friendly labels")
    public void toStringIsFriendly() {
        assertEquals("Not started", TaskStatus.NOT_STARTED.toString());
        assertEquals("In progress", TaskStatus.IN_PROGRESS.toString());
        assertEquals("Done", TaskStatus.DONE.toString());
        assertEquals("Abandoned", TaskStatus.ABANDONED.toString());
    }

    @Test
    @DisplayName("fromCode maps codes to enums and falls back to NOT_STARTED for unknown/null")
    public void fromCodeMapping() {
        assertEquals(TaskStatus.NOT_STARTED, TaskStatus.fromCode("not_started"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromCode("in_progress"));
        assertEquals(TaskStatus.DONE, TaskStatus.fromCode("done"));
        assertEquals(TaskStatus.ABANDONED, TaskStatus.fromCode("abandoned"));

        // unknown codes -> default NOT_STARTED
        assertEquals(TaskStatus.NOT_STARTED, TaskStatus.fromCode("something_else"));
        // null should not throw and should return default
        assertEquals(TaskStatus.NOT_STARTED, TaskStatus.fromCode(null));
        // case-sensitive check: upper-case doesn't match code
        assertEquals(TaskStatus.NOT_STARTED, TaskStatus.fromCode("DONE"));
    }

    @Test
    @DisplayName("All codes should be unique")
    public void codesAreUnique() {
        var codes = new HashSet<String>();
        for (TaskStatus s : TaskStatus.values()) {
            assertTrue(codes.add(s.getCode()), "Duplicate code: " + s.getCode());
        }
    }

}
