package ivs.sink;

import java.util.Spliterator;

/**
 * 操作链
 */

public abstract class SinkChain<IN,OUT> implements Sink<IN> {

    // 数据源
    protected Spliterator spliterator;
    // 下一个操作
    protected SinkChain<OUT,?> next;

    @Override
    public void begin(int n) {
        if(next != null)
            next.begin(n);
    }

    // 操作
    public abstract void accept(IN e);

    @Override
    public void end() {
        if(next != null)
            next.end();
    }

    public Spliterator getSpliterator() {
        return spliterator;
    }

    public void setSpliterator(Spliterator spliterator) {
        this.spliterator = spliterator;
    }

    public SinkChain<OUT, ?> getNext() {
        return next;
    }

    public void setNext(SinkChain<OUT, ?> next) {
        this.next = next;
        this.setSpliterator(next.getSpliterator());
    }
}
