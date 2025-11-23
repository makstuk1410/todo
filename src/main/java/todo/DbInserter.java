package todo;

public class DbInserter {
    public static void main(String[] args) {
        try {
            Db db = new Db();
            Task t = new Task(0, "automatyczny test", "opis", "Baton", TaskStatus.IN_PROGRESS, "");
            int id = db.createTask(t);
            System.out.println("Inserted id=" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
