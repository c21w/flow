
import ivs.flow.Flow;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test {

    public static void main(String[] args) {

        long last = Flow.of(5, () -> new Random().nextInt(33333))
                .sort(Comparator.comparing(Integer::intValue))
//                .peek(System.out::println)
                .map(e -> e+"")
                .map(e -> e.split(""))
                .flatMap(e -> Flow.of(e))
                .peek(System.out::println)
                .count();
//                .group(e -> e%2 == 0);

        System.out.println(last);
    }
}
