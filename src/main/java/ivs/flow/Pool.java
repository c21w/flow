package ivs.flow;

import ivs.pip.FlowPipLine;
import ivs.sink.SinkChain;

import java.util.*;
import java.util.function.Supplier;

public class Pool {

    public static <T> Flow<T> create(T[] t) {
        Spliterator<T> spliterator = Arrays.spliterator(t);
        return create(spliterator);
    }

    public static <T> Flow<T> create(Collection<T> collection) {
        Spliterator<T> spliterator = collection.spliterator();
        return create(spliterator);
    }

    public static <T> Flow<T> create(int n, Supplier<T> supplier) {
        List<T> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(supplier.get());
        }
        return create(list.spliterator());
    }

    // 封装第一个 FlowPipLine 节点, 内部持有 wrapSink 方法，该方法可以返回自己持有的 Sink 对象，传入的参数是下一个Sink 对象
    private static <T> Flow<T> create(Spliterator<T> spliterator){
        final Spliterator spliterator1 = spliterator;
        FlowPipLine<T, T> flowPipLine = new FlowPipLine<>(spliterator) {
            @Override
            public SinkChain<T,T> wrapSink(SinkChain<T, ?> next) {
                SinkChain<T, T> chain = new SinkChain<>() {
                    @Override
                    public void begin(int n) {
                        super.begin(Long.valueOf(spliterator1.estimateSize()).intValue());
                    }

                    @Override
                    public void accept(T e) {
                        if(getNext() != null)
                            getNext().accept(e);
                    }
                };
                chain.setNext(next);
                return chain;
            }
        };
        return flowPipLine;
    }

}
