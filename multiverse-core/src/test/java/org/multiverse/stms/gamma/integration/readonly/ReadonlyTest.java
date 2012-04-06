package org.multiverse.stms.gamma.integration.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Txn;
import org.multiverse.api.TxnExecutor;
import org.multiverse.api.closures.TxnClosure;
import org.multiverse.api.closures.TxnLongClosure;
import org.multiverse.api.closures.TxnVoidClosure;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTxn;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.TxnThreadLocal.clearThreadLocalTxn;

public class ReadonlyTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTxn();
        stm = (GammaStm) getGlobalStmInstance();
    }

    @Test
    public void whenReadonly_thenUpdateFails() {
        GammaLongRef ref = new GammaLongRef(stm);
        try {
            updateInReadonlyMethod(ref, 10);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(0, ref.atomicGet());
    }

    public void updateInReadonlyMethod(final GammaLongRef ref, final int newValue) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(true)
                .newTxnExecutor();

        block.atomic(new TxnVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                ref.getAndSet(btx, newValue);
            }
        });
    }

    @Test
    public void whenReadonly_thenCreationOfNewTransactionalObjectNotFails() {
        try {
            readonly_createNewTransactionObject(10);
            fail();
        } catch (ReadonlyException expected) {
        }
    }

    public void readonly_createNewTransactionObject(final long value) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(true)
                .newTxnExecutor();

        block.atomic(new TxnVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                GammaLongRef ref = new GammaLongRef(btx);
                ref.openForConstruction(btx).long_value = value;
            }
        });
    }

    @Test
    public void whenReadonly_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = readonly_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    public Integer readonly_createNormalObject(final int value) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(true)
                .newTxnExecutor();

        return block.atomic(new TxnClosure<Integer>() {
            @Override
            public Integer execute(Txn tx) throws Exception {
                return new Integer(value);
            }
        });
    }

    @Test
    public void whenReadonly_thenReadAllowed() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long result = readInReadonlyMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.atomicGet());
    }

    public long readInReadonlyMethod(final GammaLongRef ref) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(true)
                .newTxnExecutor();

        return block.atomic(new TxnLongClosure() {
            @Override
            public long execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                return ref.get(btx);
            }
        });
    }

    @Test
    public void whenUpdate_thenCreationOfNewTransactionalObjectsSucceeds() {
        GammaLongRef ref = update_createNewTransactionObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.atomicGet());
    }

    public GammaLongRef update_createNewTransactionObject(final int value) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(false)
                .newTxnExecutor();

        return block.atomic(new TxnClosure<GammaLongRef>() {
            @Override
            public GammaLongRef execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                GammaLongRef ref = new GammaLongRef(btx);
                ref.openForConstruction(btx).long_value = value;
                return ref;
            }
        });
    }

    @Test
    public void whenUpdate_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = update_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    public Integer update_createNormalObject(final int value) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(false)
                .newTxnExecutor();

        return block.atomic(new TxnClosure<Integer>() {
            @Override
            public Integer execute(Txn tx) throws Exception {
                return new Integer(value);
            }
        });
    }

    @Test
    public void whenUpdate_thenReadSucceeds() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long result = readInUpdateMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.atomicGet());
    }


    public long readInUpdateMethod(final GammaLongRef ref) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(false)
                .newTxnExecutor();

        return block.atomic(new TxnLongClosure() {
            @Override
            public long execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                return ref.get(btx);
            }
        });
    }

    @Test
    public void whenUpdate_thenUpdateSucceeds() {
        GammaLongRef ref = new GammaLongRef(stm);
        updateInUpdateMethod(ref, 10);
        assertEquals(10, ref.atomicGet());
    }

    public void updateInUpdateMethod(final GammaLongRef ref, final int newValue) {
        TxnExecutor block = stm.newTxnFactoryBuilder()
                .setReadonly(false)
                .newTxnExecutor();

        block.atomic(new TxnVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                assertFalse(tx.getConfiguration().isReadonly());
                GammaTxn btx = (GammaTxn) tx;
                ref.getAndSet(btx, newValue);
            }
        });
    }

    @Test
    public void whenDefault_thenUpdateSuccess() {
        GammaLongRef ref = new GammaLongRef(stm);
        defaultTransactionalMethod(ref);

        assertEquals(1, ref.atomicGet());
    }


    public void defaultTransactionalMethod(final GammaLongRef ref) {
        stm.newTxnFactoryBuilder().newTxnExecutor().atomic(new TxnVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                assertFalse(tx.getConfiguration().isReadonly());
                GammaTxn btx = (GammaTxn) tx;
                ref.getAndSet(btx, ref.get(btx) + 1);
            }
        });
    }
}
