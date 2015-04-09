package be.objectify.as;

import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Equivalent to {@link play.db.jpa.TransactionalAction}, but using {@link  AsyncJPA}  to provide transactional
 * support.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
public class AsyncTransactionalAction extends Action<AsyncTransactional>
{
    /**
     * {@inheritDoc}
     */
    public F.Promise<Result> call(final Http.Context ctx) throws Throwable
    {
        return AsyncJPA.withTransactionAsync(configuration.value(),
                                             configuration.readOnly(),
                                             new F.Function0<F.Promise<Result>>()
                                             {
                                                 @Override
                                                 public F.Promise<Result> apply() throws Throwable
                                                 {
                                                     return delegate.call(ctx);
                                                 }
                                             });
    }
}