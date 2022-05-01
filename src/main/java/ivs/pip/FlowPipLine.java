package ivs.pip;

import ivs.flow.Flow;
import ivs.sink.SinkChain;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class FlowPipLine<IN,OUT> implements Flow<OUT> {

    // 数据源
    private Spliterator spliterator;
    // 上一个操作节点对象
    private FlowPipLine<?,IN> prev;

    // 操作变量
    private Object state;

    // 首节点初始化
    public FlowPipLine(Spliterator<IN> spliterator){
        this.spliterator = spliterator;
    }

    // 其他节点初始化
    public FlowPipLine(FlowPipLine<?,IN> prev){
        this.prev = prev;
        spliterator = prev.spliterator;
    }

    // 串链条
    public abstract SinkChain<?, OUT> wrapSink(SinkChain<OUT, ?> next);

    // 组装链条
    private SinkChain<OUT, OUT> warpPipeline(FlowPipLine river) {
        SinkChain<OUT, OUT> sink = null;
        for (; river != null; river = river.prev) {
            sink = river.wrapSink(sink);
        }
        return sink;
    }

    @Override
    public Flow<OUT> filter(Predicate<OUT> predicate) {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> next) {
                SinkChain<OUT, OUT> chain = new SinkChain<OUT, OUT>() {
                    @Override
                    public void accept(OUT e) {
                        if (!predicate.test(e))
                            return;
                        getNext().accept(e);
                    }
                };
                chain.setNext(next);
                return chain;
            }
        };
    }

    @Override
    public Flow<OUT> distinct() {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                SinkChain<OUT, OUT> chain = new SinkChain<OUT, OUT>() {
                    private HashSet<OUT> set;

                    @Override
                    public void begin(int n) {
                        this.set = new LinkedHashSet<>();
                    }

                    @Override
                    public void end() {
                        this.next.begin(set.size());
                        for (OUT out : set) {
                            this.next.accept(out);
                        }
                        this.set = null;
                        super.end();
                    }

                    @Override
                    public void accept(OUT t) {
                        set.add(t);
                    }
                };
                chain.setNext(sink);
                return chain;
            }
        };
    }

    @Override
    public Flow<OUT> limit(int size) {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                SinkChain<OUT, OUT> chain = new SinkChain<OUT, OUT>() {
                    private int count;

                    @Override
                    public void begin(int n) {
                        count = size;
                        super.begin(count);
                    }

                    @Override
                    public void accept(OUT t) {
                        if (count == 0) {
                            return;
                        }
                        count --;
                        getNext().accept(t);
                    }
                };
                chain.setNext(sink);
                return chain;
            }
        };
    }

    @Override
    public Flow<OUT> sort(Comparator<OUT> comparable) {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                SinkChain<OUT, OUT> chain = new SinkChain<OUT, OUT>() {
                    private List<OUT> list;

                    @Override
                    public void begin(int n) {
                        this.list = new ArrayList<>();
                        super.begin(n);
                    }

                    @Override
                    public void end() {
                        list.sort(comparable);
                        for (OUT out : list) {
                            this.next.accept(out);
                        }
                        this.list = null;
                        super.end();
                    }

                    @Override
                    public void accept(OUT t) {
                        this.list.add(t);
                    }
                };
                chain.setNext(sink);
                return chain;
            }
        };
    }

    @Override
    public Flow<OUT> peek(Consumer<OUT> consumer) {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                SinkChain<OUT, OUT> chain = new SinkChain<OUT, OUT>() {
                    @Override
                    public void accept(OUT t) {
                        if(consumer != null){
                            consumer.accept(t);
                        }
                        getNext().accept(t);
                    }
                };
                chain.setNext(sink);
                return chain;
            }
        };
    }

    @Override
    public Flow<OUT> skip(int size) {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                SinkChain<OUT, OUT> chain = new SinkChain<OUT, OUT>() {
                    private int count;

                    @Override
                    public void begin(int n) {
                        count = size;
                        super.begin(Math.max(0,n-count));
                    }

                    @Override
                    public void accept(OUT t) {
                        if(count > 0)
                            count --;
                        else
                            getNext().accept(t);
                    }
                };
                chain.setNext(sink);
                return chain;
            }
        };
    }

    @Override
    public <E_OUT> Flow<E_OUT> map(Function<? super OUT, ? extends E_OUT> function) {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, E_OUT> wrapSink(SinkChain<E_OUT, ?> sink) {
                SinkChain<OUT, E_OUT> chain = new SinkChain<OUT, E_OUT>() {
                    @Override
                    public void accept(OUT t) {
                        getNext().accept(function.apply(t));
                    }
                };
                chain.setNext(sink);
                return chain;
            }
        };
    }

    @Override
    public <E_OUT> Flow<E_OUT> flatMap(Function<? super OUT, ? extends Flow<E_OUT>> function) {
        return new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, E_OUT> wrapSink(SinkChain<E_OUT, ?> sink) {
                SinkChain<OUT, E_OUT> chain = new SinkChain<OUT, E_OUT>() {
                    private List<E_OUT> list;
                    @Override
                    public void begin(int n) {
                        list = new ArrayList<>();
                    }

                    @Override
                    public void accept(OUT t) {
                        Flow<E_OUT> apply = function.apply(t);
                        List<E_OUT> collect = apply.collect(Collectors.toList());
                        list.addAll(collect);
                    }

                    @Override
                    public void end() {
                        super.begin(list.size());
                        for (E_OUT e_out : list) {
                            this.next.accept(e_out);
                        }
                        list = null;
                        super.end();
                    }
                };
                chain.setNext(sink);
                return chain;
            }
        };
    }

    @Override
    public void forEach(Consumer<OUT> consumer) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void accept(OUT t) {
                        consumer.accept(t);
                    }
                };
            }
        };
        evaluate(pipLine);
    }

    @Override
    public Object[] toArray() {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            private List<OUT> list;

            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        list = new ArrayList<>();
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        list.add(t);
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return list.toArray();
            }
        };
        evaluate(pipLine);
        return pipLine.toArray();
    }

    @Override
    public OUT[] toArray(Class<OUT> outClass) {
        OUT[] o = (OUT[]) Array.newInstance(outClass, 0);
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            private List<OUT> list;

            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        list = new ArrayList<>();
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        list.add(t);
                    }

                    @Override
                    public void end() {
                        state = list.toArray(o);
                        super.end();
                    }
                };
            }
        };
        evaluate(pipLine);
        return (OUT[]) state;
    }

    @Override
    public long count() {
        Flow<Integer> map = map(e -> 1);
        return map.reduce(0,Integer::sum);
    }

    @Override
    public OUT reduce(OUT identity, BinaryOperator<OUT> accumulator) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = identity;
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        state = accumulator.apply((OUT) state,t);
                    }
                };
            }

        };
        evaluate(pipLine);
        return (OUT) state;
    }

    @Override
    public <R, A> R collect(Collector<? super OUT, A, R> collector) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<OUT,OUT>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = collector.supplier().get();
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        collector.accumulator().accept((A)state, t);
                    }
                };
            }

        };
        evaluate(pipLine);
        return collector.finisher().apply((A)state);
    }

    @Override
    public Optional<OUT> min(Comparator<? super OUT> comparator) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = null;
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        if(state == null)
                            state = t;
                        else
                            state = comparator.compare((OUT) state, t) < 0 ? state : t;
                    }
                };
            }
        };
        evaluate(pipLine);
        return Optional.ofNullable((OUT) state);
    }

    @Override
    public Optional<OUT> max(Comparator<? super OUT> comparator) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = null;
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        if(state == null)
                            state = t;
                        else
                            state = comparator.compare((OUT) state, t) > 0 ? state : t;
                    }
                };
            }
        };
        evaluate(pipLine);
        return Optional.ofNullable((OUT) state);
    }

    @Override
    public boolean anyMatch(Predicate<? super OUT> predicate) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = false;
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        if((boolean) state)
                            return;
                        state = predicate.test(t);
                    }
                };
            }
        };
        evaluate(pipLine);
        return (boolean) state;
    }

    @Override
    public boolean allMatch(Predicate<? super OUT> predicate) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = true;
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        if(!(boolean) state)
                            return;
                        state = predicate.test(t);
                    }
                };
            }
        };
        evaluate(pipLine);
        return (boolean) state;
    }

    @Override
    public boolean noneMatch(Predicate<? super OUT> predicate) {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = true;
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        if(!(boolean) state)
                            return;
                        state = !predicate.test(t);
                    }
                };
            }
        };
        evaluate(pipLine);
        return (boolean) state;
    }

    @Override
    public Optional<OUT> findFirst() {
        FlowPipLine<OUT, OUT> pipLine = new FlowPipLine<>(this) {
            @Override
            public SinkChain<OUT, OUT> wrapSink(SinkChain<OUT, ?> sink) {
                return new SinkChain<OUT, OUT>() {
                    @Override
                    public void begin(int n) {
                        state = null;
                        super.begin(n);
                    }

                    @Override
                    public void accept(OUT t) {
                        if(state == null)
                            state = t;
                    }
                };
            }
        };
        evaluate(pipLine);
        return Optional.ofNullable((OUT) state);
    }


    // 终结操作
    private void evaluate(FlowPipLine<?, OUT> end) {
        SinkChain<OUT, OUT> chain = this.warpPipeline(end);

        chain.begin(-1);
        end.spliterator.forEachRemaining(chain);
        chain.end();
    }
}
