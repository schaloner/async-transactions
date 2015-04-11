Async transactions
==================

There is a [well-known issue](https://github.com/playframework/playframework/pull/2042) with Play's JPA support when
using promises.  This library provides a way of ensuring entity managers are still available where needed.  It can be
seen as an extension to javaJpa, as that dependency must also be installed and configured as usual.

Installation
------------
Add the following to your build.sbt file:

    "be.objectify" %% "async-transactions" % "1.0-SNAPSHOT" 

Because this is currently only a snapshot, you'll need to add another resolver:

    resolvers += Resolver.sonatypeRepo("snapshots")

You will also need to hava javaJpa declared as a dependency.

Usage
-----
Wherever you have a @play.db.jpa.Transactional annotation, you can replace it with a @be.objectify.as.AsyncTransactional.

**NB** Actions (interceptors) are applied in the order of annotation specification, so to ensure the transaction is
present for subsequent actions this must be the first annotation specified for a type or method.

Within your code, you can use AsyncJPA.em() or JPA.em() to obtain the bound EntityManager.  If you already have calls
to JPA.em(), you don't need to change these.  However, changing them to AsyncJPA.em() will result in more meaningful
error messages.

Finally, this mechanism relies on a Http.Context being present.  If you're invoking calls that are annotated as
@AsyncTransactional outside of a HTTP request, i.e. with no Http.Context present, you'll get an error message.

