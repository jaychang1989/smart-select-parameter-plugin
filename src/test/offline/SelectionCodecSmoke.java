import io.jenkins.plugins.smartselect.SelectionCodec;
import java.util.List;

/** No dependency on JUnit/Maven; runnable with Java 21 alone. */
public class SelectionCodecSmoke {
    private static int tests = 0;
    private static void check(boolean condition) {
        tests++;
        if (!condition) throw new AssertionError("case " + tests);
    }
    private static void throwsError(Runnable f) {
        tests++;
        try { f.run(); throw new AssertionError("expected rejection"); }
        catch (IllegalArgumentException ok) { /* pass */ }
    }
    public static void main(String[] args) {
        check(SelectionCodec.choicesFrom("price\n vehicle\nprice").equals(List.of("price", "vehicle")));
        check(SelectionCodec.choicesFrom(List.of("price", "vehicle")).size() == 2);
        check(SelectionCodec.defaultsFrom("price, vehicle").equals(List.of("price", "vehicle")));
        check(SelectionCodec.selectedFrom("vehicle,price", List.of("price", "vehicle"), true)
                .equals(List.of("vehicle", "price")));
        check(SelectionCodec.selectedFrom("", List.of("price"), true).isEmpty());
        throwsError(() -> SelectionCodec.choicesFrom("price,vehicle"));
        throwsError(() -> SelectionCodec.selectedFrom("price,vehicle", List.of("price", "vehicle"), false));
        throwsError(() -> SelectionCodec.selectedFrom("anything", List.of("price"), true));
        System.out.println("SelectionCodecSmoke: " + tests + " checks passed");
    }
}
