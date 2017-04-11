import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String[] b = {"1", "2", "3"};
        List<String> a = Arrays.asList(b);

        System.out.println(a.get(1000));
    }
}
