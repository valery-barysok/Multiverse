package org.multiverse.stms.gamma.transactions.fat;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTxnException;
import org.multiverse.api.exceptions.PreparedTxnException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTxn;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public abstract class FatGammaTxn_locateTest<T extends GammaTxn> {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    @Test
    public void whenNull_thenNullPointerException() {
        GammaTxn tx = newTransaction();

        try {
            tx.locate(null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
    }

    @Test
    public void whenNotFound() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaLongRef otherRef = new GammaLongRef(stm);

        GammaTxn tx = newTransaction();
        ref.openForRead(tx, LOCKMODE_NONE);

        GammaRefTranlocal found = tx.locate(otherRef);
        assertNull(found);
        assertIsActive(tx);
    }

    @Test
    public void whenFound() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTxn tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaRefTranlocal found = tx.locate(ref);
        assertSame(tranlocal, found);
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyPrepared() {
        GammaTxn tx = newTransaction();
        tx.prepare();

        GammaLongRef ref = new GammaLongRef(stm);

        try {
            tx.locate(ref);
            fail();
        } catch (PreparedTxnException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted() {
        GammaTxn tx = newTransaction();
        tx.commit();

        GammaLongRef ref = new GammaLongRef(stm);

        try {
            tx.locate(ref);
            fail();
        } catch (DeadTxnException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyAborted() {
        GammaTxn tx = newTransaction();
        tx.abort();

        GammaLongRef ref = new GammaLongRef(stm);

        try {
            tx.locate(ref);
            fail();
        } catch (DeadTxnException expected) {
        }

        assertIsAborted(tx);
    }
}
