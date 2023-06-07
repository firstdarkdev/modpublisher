import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class SemverCompareTest {

    public static void main(String[] args) {
        DefaultArtifactVersion min = new DefaultArtifactVersion("b1.6.6");
        DefaultArtifactVersion current = new DefaultArtifactVersion("b1.5.0");

        System.out.println(current.compareTo(min));
    }

}
