package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Txn;
import org.multiverse.api.TxnExecutor;
import org.multiverse.api.PropagationLevel;
import org.multiverse.api.closures.TxnIntClosure;
import org.multiverse.api.closures.TxnVoidClosure;
import org.multiverse.api.exceptions.TxnMandatoryException;
import org.multiverse.api.exceptions.TxnNotAllowedException;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTxn;
import org.multiverse.stms.gamma.transactions.GammaTxnFactory;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.TxnThreadLocal.*;

public class FatGammaTxnExecutor_propagationLevelTest implements GammaConstants {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTxn();
    }

    @Test
    public void whenNeverAndTransactionAvailable_thenNoTransactionAllowedException() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Never)
                .newTxnExecutor();

        GammaTxn otherTx = stm.newDefaultTxn();
        setThreadLocalTxn(otherTx);

        TxnVoidClosure closure = mock(TxnVoidClosure.class);

        try {
            block.atomic(closure);
            fail();
        } catch (TxnNotAllowedException expected) {
        }

        verifyZeroInteractions(closure);
        assertIsActive(otherTx);
        assertSame(otherTx, getThreadLocalTxn());
    }

    @Test
    public void whenNeverAndNoTransactionAvailable() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Never)
                .newTxnExecutor();

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertNull(tx);
                return 10;
            }
        };

        int result = block.atomic(closure);

        assertEquals(10, result);
        assertNull(getThreadLocalTxn());
    }

    @Test
    public void whenMandatoryAndNoTransactionAvailable_thenNoTransactionFoundException() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Mandatory)
                .newTxnExecutor();

        TxnVoidClosure closure = mock(TxnVoidClosure.class);

        try {
            block.atomic(closure);
            fail();
        } catch (TxnMandatoryException expected) {
        }

        verifyZeroInteractions(closure);
        assertNull(getThreadLocalTxn());
    }

    @Test
    public void whenMandatoryAndTransactionAvailable_thenExistingTransactionUsed() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Mandatory)
                .newTxnExecutor();

        final GammaTxn otherTx = stm.newDefaultTxn();
        setThreadLocalTxn(otherTx);

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertSame(otherTx, tx);
                return 10;
            }
        };

        int result = block.atomic(closure);

        assertEquals(10, result);
        assertIsActive(otherTx);
        assertSame(otherTx, getThreadLocalTxn());
    }

    @Test
    public void whenRequiresAndNoTransactionAvailable_thenNewTransactionUsed() {
        GammaTxnFactory txFactory = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Requires)
                .newTransactionFactory();

        final GammaLongRef ref = new GammaLongRef(stm);

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertNotNull(tx);
                GammaTxn btx = (GammaTxn) tx;
                ref.incrementAndGet(1);
                return 10;
            }
        };

        int result = new FatGammaTxnExecutor(txFactory).atomic(closure);

        assertEquals(10, result);
        assertNull(getThreadLocalTxn());
        assertEquals(1, ref.atomicGet());
    }

    @Test
    public void whenRequiresAndTransactionAvailable_thenExistingTransactionUsed() {
        GammaTxnFactory txFactory = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Requires)
                .newTransactionFactory();

        final GammaTxn existingTx = stm.newDefaultTxn();
        setThreadLocalTxn(existingTx);

        final GammaLongRef ref = new GammaLongRef(stm);

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertSame(existingTx, tx);
                GammaTxn btx = (GammaTxn) tx;
                ref.incrementAndGet(btx, 1);
                return 10;
            }
        };

        int result = new FatGammaTxnExecutor(txFactory).atomic(closure);

        assertEquals(10, result);
        assertSame(existingTx, getThreadLocalTxn());
        assertIsActive(existingTx);
        //since the value hasn't committed yet, it still is zero (the value before the transaction began).
        assertEquals(0, ref.atomicGet());
    }

    @Test
    public void whenRequiresNewAndNoTransactionAvailable_thenNewTransactionCreated() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.RequiresNew)
                .newTxnExecutor();

        final GammaLongRef ref = new GammaLongRef(stm, 0);

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertNotNull(tx);
                GammaTxn btx = (GammaTxn) tx;
                ref.incrementAndGet(btx, 1);
                return 10;
            }
        };

        int result = block.atomic(closure);

        assertEquals(10, result);
        assertEquals(1, ref.atomicGet());
        assertNull(getThreadLocalTxn());
    }

    @Test
    public void whenRequiresNewAndTransactionAvailable_thenExistingTransactionSuspended() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.RequiresNew)
                .newTxnExecutor();

        final GammaTxn otherTx = stm.newDefaultTxn();
        setThreadLocalTxn(otherTx);

        final GammaLongRef ref = new GammaLongRef(stm, 10);

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertNotNull(tx);
                assertNotSame(otherTx, tx);
                GammaTxn btx = (GammaTxn) tx;
                ref.incrementAndGet(btx, 1);
                return 1;
            }
        };

        int result = block.atomic(closure);

        assertEquals(1, result);
        assertEquals(11, ref.atomicGet());
        assertSame(otherTx, getThreadLocalTxn());
        assertIsActive(otherTx);
    }

    @Test
    public void whenSupportsAndTransactionAvailable() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Supports)
                .newTxnExecutor();

        final GammaTxn otherTx = stm.newDefaultTxn();
        setThreadLocalTxn(otherTx);

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertSame(otherTx, tx);
                return 10;
            }
        };

        int result = block.atomic(closure);

        assertEquals(10, result);
        assertIsActive(otherTx);
        assertSame(otherTx, getThreadLocalTxn());
    }

    @Test
    public void whenSupportsAndNoTransactionAvailable() {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Supports)
                .newTxnExecutor();

        TxnIntClosure closure = new TxnIntClosure() {
            @Override
            public int execute(Txn tx) throws Exception {
                assertNull(tx);
                return 10;
            }
        };

        int result = block.atomic(closure);

        assertEquals(10, result);
        assertNull(getThreadLocalTxn());
    }
}
