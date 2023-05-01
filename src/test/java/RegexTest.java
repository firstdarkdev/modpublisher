import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {

    public static void main(String[] args) {
        final Pattern pattern = Pattern.compile("[A-Za-z0-9]+", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher("23w17a");
        final Matcher curseMathcer = pattern.matcher("1.20-snapshot");

        System.out.println("Modrinth: " + matcher.matches());
        System.out.println("Curse: " + curseMathcer.matches());
    }

}
