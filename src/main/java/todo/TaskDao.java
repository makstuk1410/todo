/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package todo;

import java.sql.SQLException;
import java.util.List;

public class TaskDao {

    private final Db db;

    public TaskDao() {
        try {
            this.db = new Db();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to open database", ex);
        }
    }

    /**
     * Constructor for tests: inject a custom Db instance (e.g. using a temp file).
     */
    public TaskDao(Db db) {
        this.db = db;
    }

    public List<Task> getAllTasks() {
        try {
            return db.getTasks();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int createTask(Task task) {
        try {
            return db.createTask(task);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void updateTask(Task task) {
        try {
            db.updateTask(task);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void deleteTask(int id) {
        try {
            Task t = new Task(id, "", "", null, TaskStatus.NOT_STARTED, null);
            db.deleteTask(t);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
