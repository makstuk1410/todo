
package todo;

public class Task {

    private int id;
    private String name;
    private String content;
    private String categoryName;
    private TaskStatus status;
    private String due;

    public Task(int id, String name, String content, String categoryName, TaskStatus status, String due) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.categoryName = categoryName;
        this.status = status;
        this.due = due;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getDue() {
        return due;
    }
}
