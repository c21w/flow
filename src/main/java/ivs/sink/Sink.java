package ivs.sink;

import java.util.function.Consumer;

@FunctionalInterface
public interface Sink<E> extends Consumer<E> {

    // 流的大小，预处理
    default void begin(int n){ }

    // 尾操作
    default void end() {}
}
