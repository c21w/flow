
import ivs.flow.Flow;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test {

    public static void main(String[] args) {

        Integer[] array = Flow.of(5, () -> new Random().nextInt(33333))
                .distinct()
                .sort(Comparator.comparing(Integer::intValue))
                .peek(System.out::println)
                .toArray(Integer.class);

        System.out.println(Arrays.toString(array));
    }
}
