package hex.genmodel.tools;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.security.Permission;

import static org.junit.Assert.*;

public class PrintMojoTest {

    private Path gbmMojoFile;
    private String outputPngFilename;
    private String outputDotFilename;
    private SecurityManager originalSecurityManager;
    
    @Before
    public void setup() throws IOException {
        gbmMojoFile = copyMojoFileResource("mojo.zip");
        outputPngFilename = "exampleh2o.png";
        outputDotFilename = "exampleh2o.gv";
        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new PreventExitSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
        Files.deleteIfExists(gbmMojoFile);
    }

    @Test
    public void testPrintMojoDotToConsole() throws IOException {
        String[] dotToConsole = {
                "--tree", "0",
                "-i", gbmMojoFile.toAbsolutePath().toString(),
                "-f", "20", "-d", "3"
        };
        try {
            PrintMojo.main(dotToConsole);
        } catch (PreventedExitException e) {
            assertEquals(0, e.status); // PrintMojo is expected to finish without errors
        }
        Path pathToImage = Paths.get(outputDotFilename);
        Assert.assertFalse(Files.deleteIfExists(pathToImage));
    }

    @Test
    public void testPrintMojoPngToConsole() throws IOException {
        String[] pngToConsole = {
                "--tree", "0",
                "-i", gbmMojoFile.toAbsolutePath().toString(),
                "--format", "png",
                "-f", "20", "-d", "3"
        };
        try {
            PrintMojo.main(pngToConsole);
        } catch (PreventedExitException e) {
            assertEquals(0, e.status); // PrintMojo is expected to finish without errors
        }
        Path pathToImage = Paths.get(outputPngFilename);
        Assert.assertFalse(Files.deleteIfExists(pathToImage));
    }

    @Test
    public void testPrintMojoDotToFile() throws IOException {
        String[] dotToFile = {
                "--tree", "0",
                "-i", gbmMojoFile.toAbsolutePath().toString(),
                "-f", "20", "-d", "3",
                "-o", outputDotFilename
        };
        try {
            PrintMojo.main(dotToFile);
        } catch (PreventedExitException e) {
            assertEquals(0, e.status); // PrintMojo is expected to finish without errors
        }
        Path pathToImage = Paths.get(outputDotFilename);
        Assert.assertTrue(Files.deleteIfExists(pathToImage));
    }

    @Test
    public void testPrintMojoPngToFile() throws IOException {
        String[] pngToFile = {
                "--tree", "0",
                "-i", gbmMojoFile.toAbsolutePath().toString(),
                "-f", "20", "-d", "3",
                "-o", outputPngFilename,
                "--format", "png"
        };
        try {
            PrintMojo.main(pngToFile);
        } catch (PreventedExitException e) {
            assertEquals(0, e.status); // PrintMojo is expected to finish without errors
        }
        Path pathToImage = Paths.get(outputPngFilename);
        Assert.assertTrue(Files.deleteIfExists(pathToImage));
    }

    private Path copyMojoFileResource(String name) throws IOException {
        Path target = Files.createTempFile("", name);
        try (InputStream is = getClass().getResourceAsStream(name)) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    protected static class PreventedExitException extends SecurityException {
        public final int status;

        public PreventedExitException(int status) {
            this.status = status;
        }
    }

    /**
      * Security managers that prevents PrintMojo from exiting.
      */
    private static class PreventExitSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }

        @Override
        public void checkExit(int status) {
            throw new PreventedExitException(status);
        }
    }
}
