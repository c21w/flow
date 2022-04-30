package ivs.flow;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

public interface Flow<T> {

    // 创建Flow流
    static <T> Flow<T> of(T... t) {
        return Pool.create(t);
    }
    // 创建Flow流
    static <T> Flow<T> of(Collection<T> collection) {
        return Pool.create(collection);
    }

    // 过滤
    Flow<T> filter(Predicate<T> predicate);

    // 去重
    Flow<T> distinct();

    // 限制个数
    Flow<T> limit(int size);

    // 排序
    Flow<T> sort(Comparator<T> comparable);

    // 遍历元素，不终结Flow流
    Flow<T> peek(Consumer<T> consumer);

    // 跳过前 size 个元素
    Flow<T> skip(int size);

    // 映射
    <E_OUT> Flow<E_OUT> map(Function<? super T, ? extends E_OUT> function);

    // 映射流
    <E_OUT> Flow<E_OUT> flatMap(Function<? super T, ? extends Flow<E_OUT>> function);

    // 遍历，终结Flow流
    void forEach(Consumer<T> consumer);

    // 转换为数组
    Object[] toArray();

    // 输入内容到数组
    T[] toArray(T[] e);

    // 获取流中元素的个数
    long count();

    // reduce...
    T reduce(T identity, BinaryOperator<T> accumulator);

    // collect ...
    <R, A> R collect(Collector<? super T, A, R> collector);

    // 获取最小值
    Optional<T> min(Comparator<? super T> comparator);

    // 获取最大值
    Optional<T> max(Comparator<? super T> comparator);

    // 是否有满足
    boolean anyMatch(Predicate<? super T> predicate);

    // 是否全满足
    boolean allMatch(Predicate<? super T> predicate);

    // 是否全不满足
    boolean noneMatch(Predicate<? super T> predicate);

    // 获取第一个元素
    Optional<T> findFirst();
}
