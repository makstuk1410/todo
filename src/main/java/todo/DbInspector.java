package todo;

import java.util.ArrayList;

public class DbInspector {
    public static void main(String[] args) {
        try {
            Db db = new Db();
            ArrayList<Category> cats = db.getCategories();
            System.out.println("Categories: " + cats.size());
            for (Category c : cats) System.out.println(" - " + c.getName() + " : " + c.getDescription());

            ArrayList<Task> tasks = db.getTasks();
            System.out.println("Tasks: " + tasks.size());
            for (Task t : tasks) {
                System.out.println(String.format("id=%d name=%s category=%s status=%s due=%s", t.getId(), t.getName(), t.getCategoryName(), t.getStatus(), t.getDue()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
