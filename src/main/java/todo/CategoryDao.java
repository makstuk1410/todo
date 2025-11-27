
package todo;

import java.util.List;
import java.sql.SQLException;

public class CategoryDao {
	private final Db db;

	public CategoryDao() {
		try {
			this.db = new Db();
		} catch (SQLException ex) {
			throw new RuntimeException("Failed to open database", ex);
		}
	}

	/**
	 * Constructor for tests: inject a custom Db instance (e.g. using a temp file).
	 */
	public CategoryDao(Db db) {
		this.db = db;
	}

	public List<Category> getAllCategories() {
		try {
			return db.getCategories();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void createCategory(Category c) {
		try {
			db.addCategory(c);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void deleteCategory(String name) {
		try {
			db.deleteCategory(name);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void updateCategory(Category c) {
		try {
			db.updateCategory(c);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
}
