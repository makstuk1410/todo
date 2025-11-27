package todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DbInserter tests")
public class DbInserterTest {

    @Test
    @DisplayName("DbInserter.main runs and prints inserted id using temp user.home")
    public void mainInsertsTaskAndPrintsId() throws Exception {
        Path tmpDir = Files.createTempDirectory("dbinserter-");
        try {
            // Run DbInserter in a separate JVM to avoid interfering with the test JVM
            String javaBin = System.getProperty("java.home") + "\\bin\\java";
            String classpath = System.getProperty("java.class.path");

            ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath, "-Duser.home=" + tmpDir.toString(), "todo.DbInserter");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.InputStream is = p.getInputStream(); java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
                String output = s.hasNext() ? s.next() : "";
                int exit = p.waitFor();
                assertEquals(0, exit, "DbInserter process exited with non-zero: " + exit + " output: " + output);
                assertTrue(output.contains("Inserted id="), "Expected output to contain 'Inserted id=' but was: " + output);

                Path dbFile = tmpDir.resolve("testy-crud.db");
                assertTrue(Files.exists(dbFile), "Expected DB file to exist: " + dbFile);
            }
        } finally {
            try { Files.walk(tmpDir).map(Path::toFile).forEach(f -> { if (!f.delete()) {} }); } catch (Exception ignored) {}
        }
    }
}
