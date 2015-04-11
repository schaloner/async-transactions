package be.objectify.as;

import play.db.jpa.JPA;
import play.libs.F;
import play.mvc.Http;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * This is pretty much a copy of {@link play.db.jpa.JPA} with some tweaks to close entity managers on the completion of
 * promises instead of in a final block.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
public class AsyncJPA
{

    public static final String CURRENT_ENTITY_MANAGER = "currentEntityManager";

    /**
     * Get the EntityManager for specified persistence unit for this thread.
     */
    public static EntityManager em(final String key)
    {
        return JPA.em(key);
    }

    /**
     * Get the default EntityManager for this thread.
     */
    public static EntityManager em()
    {
        final Http.Context context = Http.Context.current.get();
        EntityManager em = null;
        if (context != null)
        {
            em = (EntityManager) context.args.get(CURRENT_ENTITY_MANAGER);
            if (em == null)
            {
                throw new RuntimeException("No EntityManager found in the context. Try to annotate your action method with @be.objectify.as.AsyncTransactional");
            }
            return em;
        }
        else
        {
            // Not a web request
            throw new RuntimeException("No Http.Context is present.  If you want to invoke this method outside of a HTTP request, you need to wrap the call with a JPA.withTransaction instead.");
        }
    }

    /**
     * Bind an EntityManager to the current context.
     *
     * @throws RuntimeException if no context is present
     */
    public static void bindForAsync(final EntityManager em)
    {
        Http.Context context = Http.Context.current.get();
        if (context != null)
        {
            if (em == null)
            {
                context.args.remove(CURRENT_ENTITY_MANAGER);
            }
            else
            {
                context.args.put(CURRENT_ENTITY_MANAGER, em);
            }
        }
        else
        {
            // Not a web request
            throw new RuntimeException("No Http.Context is present.  If you want to invoke this method outside of a HTTP request, you need to wrap the call with JPA.withTransaction instead.");
        }

    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static <T> F.Promise<T> withTransactionAsync(final play.libs.F.Function0<F.Promise<T>> block) throws Throwable
    {
        return AsyncJPA.withTransactionAsync("default",
                                             false,
                                             block);
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param name     The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block    Block of code to execute.
     */
    public static <T> F.Promise<T> withTransactionAsync(final String name,
                                                        final boolean readOnly,
                                                        final play.libs.F.Function0<F.Promise<T>> block) throws Throwable
    {
        EntityManager em = null;
        EntityTransaction tx = null;
        try
        {
            em = AsyncJPA.em(name);
            AsyncJPA.bindForAsync(em);

            if (!readOnly)
            {
                tx = em.getTransaction();
                tx.begin();
            }

            final F.Promise<T> result = block.apply();

            final EntityManager fem = em;
            final EntityTransaction ftx = tx;

            final F.Promise<T> committedResult = result.map(new F.Function<T, T>()
            {
                @Override
                public T apply(T t) throws Throwable
                {
                    if (ftx != null)
                    {
                        if (ftx.getRollbackOnly())
                        {
                            ftx.rollback();
                        }
                        else
                        {
                            ftx.commit();
                        }
                    }
                    return t;
                }
            });

            committedResult.onFailure(new F.Callback<Throwable>()
            {
                @Override
                public void invoke(Throwable t)
                {
                    if (ftx != null)
                    {
                        try
                        {
                            if (ftx.isActive())
                            {
                                ftx.rollback();
                            }
                        }
                        catch (Throwable e)
                        {
                        }
                    }
                    try
                    {
                        fem.close();
                    }
                    finally
                    {
                        AsyncJPA.bindForAsync(null);
                    }
                }
            });
            committedResult.onRedeem(new F.Callback<T>()
            {
                @Override
                public void invoke(T t)
                {
                    try
                    {
                        fem.close();
                    }
                    finally
                    {
                        AsyncJPA.bindForAsync(null);
                    }
                }
            });

            return committedResult;
        }
        catch (Throwable t)
        {
            if (tx != null)
            {
                try
                {
                    tx.rollback();
                }
                catch (Throwable e)
                {
                }
            }
            if (em != null)
            {
                try
                {
                    em.close();
                }
                finally
                {
                    AsyncJPA.bindForAsync(null);
                }
            }
            throw t;
        }
    }
}
