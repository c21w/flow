
import ivs.flow.Flow;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test {

    public static void main(String[] args) {
        List<String> collect = Flow.of(3, 1, 2, 6)
                .map(e -> e + ",A").map(e -> e.split(",")).flatMap(e -> Flow.of(e)).distinct().collect(Collectors.toList());


        Flow.of(1,2,3).forEach(System.out::println);
        System.out.println(collect);
    }
}
