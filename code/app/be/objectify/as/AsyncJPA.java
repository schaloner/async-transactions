package be.objectify.as;

import play.Application;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.libs.F;
import play.mvc.Http;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * This is pretty much a copy of {@link play.db.jpa.JPA} with some tweak to close entity managers on the completion of
 * promises instead of in a final block.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
public class AsyncJPA
{
    // Only used when there's no HTTP context
    static ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<>();

    /**
     * Get the EntityManager for specified persistence unit for this thread.
     */
    public static EntityManager em(String key) {
        Application app = Play.application();
        if(app == null) {
            throw new RuntimeException("No application running");
        }

        JPAPlugin jpaPlugin = app.plugin(JPAPlugin.class);
        if(jpaPlugin == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }

        EntityManager em = jpaPlugin.em(key);
        if(em == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }

        return em;
    }

    /**
     * Get the default EntityManager for this thread.
     */
    public static EntityManager em() {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            EntityManager em = (EntityManager) context.args.get("currentEntityManager");
            if (em == null) {
                throw new RuntimeException("No EntityManager bound to this thread. Try to annotate your action method with @be.objectify.deadbolt.jpa.actions.AsyncTransactional");
            }
            return em;
        }
        // Not a web request
        EntityManager em = currentEntityManager.get();
        if (em == null) {
            throw new RuntimeException("No EntityManager bound to this thread. Try wrapping this call in JPA.withTransaction, or ensure that the HTTP context is setup on this thread.");
        }
        return em;
    }

    /**
     * Bind an EntityManager to the current thread.
     */
    public static void bindForCurrentThread(final EntityManager em) {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            if (em == null) {
                context.args.remove("currentEntityManager");
            } else {
                context.args.put("currentEntityManager",
                                 em);
            }
        } else {
            JPA.bindForCurrentThread(em);
            currentEntityManager.set(em);
        }
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static <T> F.Promise<T> withTransactionAsync(final play.libs.F.Function0<F.Promise<T>> block) throws Throwable {
        return withTransactionAsync("default",
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
                                                        final play.libs.F.Function0<F.Promise<T>> block) throws Throwable {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            em = AsyncJPA.em(name);
            AsyncJPA.bindForCurrentThread(em);

            if (!readOnly) {
                tx = em.getTransaction();
                tx.begin();
            }

            F.Promise<T> result = block.apply();

            final EntityManager fem = em;
            final EntityTransaction ftx = tx;

            F.Promise<T> committedResult = result.map(new F.Function<T, T>() {
                @Override
                public T apply(T t) throws Throwable {
                    if (ftx != null) {
                        if (ftx.getRollbackOnly()) {
                            ftx.rollback();
                        } else {
                            ftx.commit();
                        }
                    }
                    return t;
                }
            });

            committedResult.onFailure(new F.Callback<Throwable>() {
                @Override
                public void invoke(Throwable t) {
                    if (ftx != null) {
                        try {
                            if (ftx.isActive()) {
                                ftx.rollback();
                            }
                        }
                        catch (Throwable e) {
                        }
                    }
                    fem.close();
                    AsyncJPA.bindForCurrentThread(null);
                }
            });
            committedResult.onRedeem(new F.Callback<T>() {
                @Override
                public void invoke(T t) {
                    fem.close();
                    AsyncJPA.bindForCurrentThread(null);
                }
            });

            return committedResult;
        }
        catch (Throwable t) {
            if (tx != null) {
                try {
                    tx.rollback();
                }
                catch (Throwable e) {
                }
            }
            if (em != null) {
                em.close();
                AsyncJPA.bindForCurrentThread(null);
            }
            throw t;
        }
    }
}
