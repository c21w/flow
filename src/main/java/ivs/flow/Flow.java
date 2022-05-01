package ivs.flow;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
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

    // 创建长度为n，内容为supplier.get() 的 Flow流
    static <T> Flow<T> of(int n, Supplier<T> supplier){
        return Pool.create(n,supplier);
    }

    // 创建内容依次叠加的 Flow流
    static Flow<Integer> range(int start,int end,int step){
        AtomicInteger n = new AtomicInteger(start);
        Supplier<Integer> supplier = ()->{
            if(n.get() +step >= end) return n.get();
            return n.addAndGet(step)-step;
        };
        return Pool.create((end-start+1)/step,supplier);
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

    // 转换为特定类型数组
    T[] toArray(Class<T> tClass);

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

    // 获取最后一个元素
    Optional<T> findLast();

    // 分组，collect方法也可以实现
    <E_OUT> Map<E_OUT,List<T>> group(Function<T,E_OUT> function);
}
