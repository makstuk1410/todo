
package todo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

	@FXML private TableView<Task> taskTable;
	@FXML private TableColumn<Task, String> nameColumn;
	@FXML private TableColumn<Task, String> categoryColumn;
	@FXML private TableColumn<Task, String> statusColumn;
	@FXML private TableColumn<Task, String> dueColumn;

	@FXML private TextField nameField;
	@FXML private TextArea contentArea;
	@FXML private ComboBox<String> categoryCombo;
	@FXML private ComboBox<TaskStatus> statusCombo;
	@FXML private DatePicker duePicker;
	@FXML private TextField searchField;
	@FXML private Label infoLabel;

	private final ObservableList<Task> tasks = FXCollections.observableArrayList();
	private int nextId = 1;

	private final TaskDao taskDao = new TaskDao();
	private final CategoryDao categoryDao = new CategoryDao();

	public void initialize() {
		nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
		categoryColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategoryName()));
		statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus().toString()));
		dueColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDue()));

		// load categories (fallback defaults)
		List<Category> cats = categoryDao.getAllCategories();
		if (cats.isEmpty()) {
			categoryCombo.getItems().addAll("Praca", "Prywatne", "Inne");
		} else {
			for (Category c : cats) categoryCombo.getItems().add(c.getName());
		}
		// status combo (use enum values)
		statusCombo.getItems().addAll(TaskStatus.NOT_STARTED, TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.ABANDONED);
		statusCombo.getSelectionModel().select(TaskStatus.NOT_STARTED);

		// load tasks from DAO (may be empty)
		tasks.addAll(taskDao.getAllTasks());
		taskTable.setItems(tasks);

		// search support
		searchField.textProperty().addListener((obs, oldV, newV) -> filterTasks(newV));
		searchField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) searchField.clear(); });

		infoLabel.setText("Zadania: " + tasks.size());
	}

	private void filterTasks(String q) {
		if (q == null || q.isBlank()) {
			taskTable.setItems(tasks);
			infoLabel.setText("Zadania: " + tasks.size());
			return;
		}
		String low = q.toLowerCase();
		ObservableList<Task> filtered = tasks.filtered(t -> t.getName().toLowerCase().contains(low) || t.getContent().toLowerCase().contains(low) || t.getCategoryName().toLowerCase().contains(low) || t.getStatus().name().toLowerCase().contains(low));
		taskTable.setItems(filtered);
		infoLabel.setText("Wyników: " + filtered.size());
	}

	@FXML
	private void onAddTask() {
		String name = nameField.getText().trim();
		if (name.isEmpty()) {
			infoLabel.setText("Podaj nazwę zadania");
			return;
		}
		String content = contentArea.getText().trim();
		String category = categoryCombo.getValue() != null ? categoryCombo.getValue() : "Inne";
		TaskStatus status = statusCombo.getValue() != null ? statusCombo.getValue() : TaskStatus.NOT_STARTED;
		String due = (duePicker.getValue() != null) ? duePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : "";

		Task t = new Task(nextId++, name, content, category, status, due);
		tasks.add(t);
		taskDao.createTask(t);
		clearForm();
		infoLabel.setText("Zadanie dodane. Razem: " + tasks.size());
	}

	@FXML
	private void onRemoveTask() {
		Task sel = taskTable.getSelectionModel().getSelectedItem();
		if (sel == null) { infoLabel.setText("Brak zaznaczenia"); return; }
		tasks.remove(sel);
		taskDao.deleteTask(sel.getId());
		infoLabel.setText("Usunięto zadanie. Razem: " + tasks.size());
	}

	@FXML
	private void onMarkDone() {
		Task sel = taskTable.getSelectionModel().getSelectedItem();
		if (sel == null) { infoLabel.setText("Brak zaznaczenia"); return; }
		// create a new Task object with same id but status changed (Task is immutable in this model)
		Task updated = new Task(sel.getId(), sel.getName(), sel.getContent(), sel.getCategoryName(), TaskStatus.DONE, sel.getDue());
		int idx = tasks.indexOf(sel);
		tasks.set(idx, updated);
		taskDao.updateTask(updated);
		infoLabel.setText("Oznaczono jako zakończone");
	}

	@FXML
	private void onChangeStatus() {
		Task sel = taskTable.getSelectionModel().getSelectedItem();
		if (sel == null) { infoLabel.setText("Brak zaznaczenia"); return; }
		ChoiceDialog<TaskStatus> dlg = new ChoiceDialog<>(sel.getStatus(), TaskStatus.values());
		dlg.setTitle("Zmień status");
		dlg.setHeaderText("Wybierz nowy status dla zadania");
		dlg.setContentText("Status:");
		dlg.showAndWait().ifPresent(s -> {
			Task updated = new Task(sel.getId(), sel.getName(), sel.getContent(), sel.getCategoryName(), s, sel.getDue());
			int idx = tasks.indexOf(sel);
			tasks.set(idx, updated);
			taskDao.updateTask(updated);
			infoLabel.setText("Status zmieniony: " + s.toString());
		});
	}

	@FXML
	private void onClearForm() {
		clearForm();
	}

	private void clearForm() {
		nameField.clear();
		contentArea.clear();
		categoryCombo.getSelectionModel().clearSelection();
		duePicker.setValue(null);
	}

}