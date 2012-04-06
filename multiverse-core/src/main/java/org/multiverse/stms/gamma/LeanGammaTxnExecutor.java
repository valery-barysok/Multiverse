package org.multiverse.stms.gamma;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.closures.*;
import org.multiverse.stms.gamma.transactions.*;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.multiverse.api.TxnThreadLocal.*;

/**
* An GammaTxnExecutor made for the GammaStm.
*
* This code is generated.
*
* @author Peter Veentjer
*/
public final class LeanGammaTxnExecutor extends AbstractGammaTxnExecutor{
    private static final Logger logger = Logger.getLogger(LeanGammaTxnExecutor.class.getName());


    public LeanGammaTxnExecutor(final GammaTxnFactory txnFactory) {
        super(txnFactory);
    }

    @Override
    public GammaTxnFactory getTransactionFactory(){
        return txnFactory;
    }

    @Override
    public final <E> E atomicChecked(
        final TxnClosure<E> closure)throws Exception{

        try{
            return atomic(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public final <E> E atomic(final TxnClosure<E> closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final TxnThreadLocal.Container transactionContainer = getThreadLocalTxnContainer();
        GammaTxnPool pool = (GammaTxnPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTxnPool();
            transactionContainer.txPool = pool;
        }

        GammaTxn tx = (GammaTxn)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = txnFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        E result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (RetryError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a retry",
                                    txnConfig.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    txnConfig.familyName));
                            }
                        }

                        abort = false;
                        GammaTxn old = tx;
                        tx = txnFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    txnConfig.familyName));
                            }
                        }

                        backoffPolicy.delayUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    txnConfig.familyName, txnConfig.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                txnConfig.getFamilyName(), txnConfig.getMaxRetries()), cause);
        }

     @Override
    public final  int atomicChecked(
        final TxnIntClosure closure)throws Exception{

        try{
            return atomic(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public final  int atomic(final TxnIntClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final TxnThreadLocal.Container transactionContainer = getThreadLocalTxnContainer();
        GammaTxnPool pool = (GammaTxnPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTxnPool();
            transactionContainer.txPool = pool;
        }

        GammaTxn tx = (GammaTxn)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = txnFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        int result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (RetryError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a retry",
                                    txnConfig.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    txnConfig.familyName));
                            }
                        }

                        abort = false;
                        GammaTxn old = tx;
                        tx = txnFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    txnConfig.familyName));
                            }
                        }

                        backoffPolicy.delayUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    txnConfig.familyName, txnConfig.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                txnConfig.getFamilyName(), txnConfig.getMaxRetries()), cause);
        }

     @Override
    public final  long atomicChecked(
        final TxnLongClosure closure)throws Exception{

        try{
            return atomic(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public final  long atomic(final TxnLongClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final TxnThreadLocal.Container transactionContainer = getThreadLocalTxnContainer();
        GammaTxnPool pool = (GammaTxnPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTxnPool();
            transactionContainer.txPool = pool;
        }

        GammaTxn tx = (GammaTxn)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = txnFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        long result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (RetryError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a retry",
                                    txnConfig.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    txnConfig.familyName));
                            }
                        }

                        abort = false;
                        GammaTxn old = tx;
                        tx = txnFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    txnConfig.familyName));
                            }
                        }

                        backoffPolicy.delayUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    txnConfig.familyName, txnConfig.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                txnConfig.getFamilyName(), txnConfig.getMaxRetries()), cause);
        }

     @Override
    public final  double atomicChecked(
        final TxnDoubleClosure closure)throws Exception{

        try{
            return atomic(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public final  double atomic(final TxnDoubleClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final TxnThreadLocal.Container transactionContainer = getThreadLocalTxnContainer();
        GammaTxnPool pool = (GammaTxnPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTxnPool();
            transactionContainer.txPool = pool;
        }

        GammaTxn tx = (GammaTxn)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = txnFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        double result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (RetryError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a retry",
                                    txnConfig.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    txnConfig.familyName));
                            }
                        }

                        abort = false;
                        GammaTxn old = tx;
                        tx = txnFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    txnConfig.familyName));
                            }
                        }

                        backoffPolicy.delayUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    txnConfig.familyName, txnConfig.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                txnConfig.getFamilyName(), txnConfig.getMaxRetries()), cause);
        }

     @Override
    public final  boolean atomicChecked(
        final TxnBooleanClosure closure)throws Exception{

        try{
            return atomic(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public final  boolean atomic(final TxnBooleanClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final TxnThreadLocal.Container transactionContainer = getThreadLocalTxnContainer();
        GammaTxnPool pool = (GammaTxnPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTxnPool();
            transactionContainer.txPool = pool;
        }

        GammaTxn tx = (GammaTxn)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = txnFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        boolean result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (RetryError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a retry",
                                    txnConfig.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    txnConfig.familyName));
                            }
                        }

                        abort = false;
                        GammaTxn old = tx;
                        tx = txnFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    txnConfig.familyName));
                            }
                        }

                        backoffPolicy.delayUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    txnConfig.familyName, txnConfig.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                txnConfig.getFamilyName(), txnConfig.getMaxRetries()), cause);
        }

     @Override
    public final  void atomicChecked(
        final TxnVoidClosure closure)throws Exception{

        try{
            atomic(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public final  void atomic(final TxnVoidClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final TxnThreadLocal.Container transactionContainer = getThreadLocalTxnContainer();
        GammaTxnPool pool = (GammaTxnPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTxnPool();
            transactionContainer.txPool = pool;
        }

        GammaTxn tx = (GammaTxn)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                closure.execute(tx);
                return;
            }

            tx = txnFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return;
                    } catch (RetryError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a retry",
                                    txnConfig.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    txnConfig.familyName));
                            }
                        }

                        abort = false;
                        GammaTxn old = tx;
                        tx = txnFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    txnConfig.familyName));
                            }
                        }

                        backoffPolicy.delayUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (txnConfig.getTraceLevel().isLoggableFrom(TraceLevel.Coarse)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    txnConfig.familyName, txnConfig.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                txnConfig.getFamilyName(), txnConfig.getMaxRetries()), cause);
        }

   }
