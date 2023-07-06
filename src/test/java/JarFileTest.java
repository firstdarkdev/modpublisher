import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class JarFileTest {

    public static void main(String[] args) throws Exception {
        File testJar = new File("testjars/noquilt.jar");
        List<String> loaderVersions = new ArrayList<>();
        loaderVersions.add("forge");
        //loaderVersions.add("fabric");
        loaderVersions.add("quilt");

        checkEmptyJar(testJar, loaderVersions);

    }

    public static void checkEmptyJar(File file, List<String> loaderVersions) throws Exception {
        FileSystem system = FileSystems.newFileSystem(Paths.get(file.getPath()), null);
        Path quiltJson = system.getPath("quilt.mod.json");
        Path fabricJson = system.getPath("fabric.mod.json");
        Path forgeToml = system.getPath("META-INF/mods.toml");
        Path forgeMc = system.getPath("mcmod.info");

        if (loaderVersions.contains("forge")) {
            // Check for either mods.toml or mcmod.info (for older version support)
            if (!Files.exists(forgeToml) && !Files.exists(forgeMc))
                throw new Exception("File marked as forge, but no mods.toml or mcmod.info file was found");

            // Test Only
            System.out.println("Valid Forge");
        }

        if (loaderVersions.contains("fabric")) {
            if (!Files.exists(fabricJson))
                throw new Exception("File marked as fabric, but no fabric.mod.json file was found");

            // Test Only
            System.out.println("Valid Fabric");
        }

        if (loaderVersions.contains("quilt")) {
            // Fabric mods can run on quilt, so we check for either of the files to be present
            if (!Files.exists(quiltJson) && !Files.exists(fabricJson))
                throw new Exception("File marked as quilt, but no quilt.mod.json OR fabric.mod.json file was found");

            // Test Only
            System.out.println("Valid Quilt");
        }

        system.close();
    }
}
