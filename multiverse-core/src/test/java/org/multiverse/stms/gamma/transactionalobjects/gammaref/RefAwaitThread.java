package org.multiverse.stms.gamma.transactionalobjects.gammaref;

import org.multiverse.TestThread;
import org.multiverse.api.Txn;
import org.multiverse.api.closures.TxnVoidClosure;
import org.multiverse.api.predicates.Predicate;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;

public class RefAwaitThread<T> extends TestThread {
    private final GammaRef<T> ref;
    private final Predicate<T> predicate;

    public RefAwaitThread(GammaRef<T> ref, final T awaitValue) {
        this(ref, new Predicate<T>() {
            @Override
            public boolean evaluate(T current) {
                return current == awaitValue;
            }
        });
    }

    public RefAwaitThread(GammaRef ref, Predicate<T> predicate) {
        this.ref = ref;
        this.predicate = predicate;
    }

    @Override
    public void doRun() throws Exception {
        ref.getStm().getDefaultTxnExecutor().atomic(new TxnVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                System.out.println("Starting wait and ref.value found: " + ref.get());
                ref.await(predicate);
                System.out.println("Finished wait and ref.value found: " + ref.get());
            }
        });
    }
}
