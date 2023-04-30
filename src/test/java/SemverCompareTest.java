import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class SemverCompareTest {

    public static void main(String[] args) {
        DefaultArtifactVersion min = new DefaultArtifactVersion("1.0");
        DefaultArtifactVersion current = new DefaultArtifactVersion("rd-132211");

        System.out.println(current.compareTo(min));
    }

}
