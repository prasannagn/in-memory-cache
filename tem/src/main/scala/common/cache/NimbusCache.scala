package common.cache

import com.github.benmanes.caffeine.cache.Ticker
import com.github.blemale.scaffeine.Scaffeine

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  *
  * @param initialCapacity   minimum total size for the internal hash tables
  * @param maximumSize       the maximum size of the cache
  * @param expireAfterWrite  Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after the entry's creation, or the most recent replacement of its value.
  * @param expireAfterAccess Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after the entry's creation, the most recent replacement of its value, or its last read.
  * @param refreshAfterWrite Specifies that active entries are eligible for automatic refresh once a fixed duration has elapsed after the entry's creation, or the most recent replacement of its value.
  * @param statsCounter      Enables the accumulation of [[com.github.benmanes.caffeine.cache.stats.CacheStats]] during the operation of the cache.
  */
case class NimbusCacheConfig(initialCapacity: Int,
                             maximumSize: Option[Int],
                             expireAfterWrite: Option[Duration],
                             expireAfterAccess: Option[Duration],
                             refreshAfterWrite: Option[Duration],
                             statsCounter: Option[NimbusStatsCounter])

case class AsyncCacheOperations[K, E](get: K => Future[E], invalidateAll: () => Unit)

case class SyncCacheOperations[K, E](get: K => E, invalidateAll: () => Unit)

object NimbusCache {

  /**
    *
    * @param f      Function that produces Entity Future[E] given Key K
    * @param config Cache config
    * @return Memoize Function that remembers last value it produced for a given input
    */
  def memoizeAsync[K, E](f: K => Future[E])(config: NimbusCacheConfig, ticker: Ticker = Ticker.systemTicker())(
    implicit ec: ExecutionContext): AsyncCacheOperations[K, E] = {
    val cacheBuilder = buildCache(config, ticker)
    val cache = cacheBuilder.buildAsyncFuture[K, E](f)
    config.statsCounter.foreach(_.registerEstimatedSize(() => cache.underlying.synchronous().estimatedSize()))
    AsyncCacheOperations(cache.get, cache.synchronous().invalidateAll)
  }

  /**
    *
    * @param f      Function that produces Entity E given Key K
    * @param config Cache config
    * @return Memoize Function that remembers last value it produced for a given input
    */
  def memoize[K, E](f: K => E)(config: NimbusCacheConfig, ticker: Ticker = Ticker.systemTicker()): SyncCacheOperations[K, E] = {
    val cacheBuilder = buildCache(config, ticker)
    val cache = cacheBuilder.build[K, E](f)
    config.statsCounter.foreach(sc => sc.registerEstimatedSize(() => cache.underlying.estimatedSize()))
    SyncCacheOperations(cache.get, cache.invalidateAll)
  }

  private def buildCache(config: NimbusCacheConfig, ticker: Ticker): Scaffeine[Any, Any] = {
    val builder = Scaffeine().initialCapacity(config.initialCapacity)
    config.maximumSize.foreach(builder.maximumSize(_))
    config.expireAfterWrite.foreach(builder.expireAfterWrite)
    config.expireAfterAccess.foreach(builder.expireAfterAccess)
    config.refreshAfterWrite.foreach(builder.refreshAfterWrite)
    config.statsCounter.foreach(sc => builder.recordStats(() => sc))
    builder.ticker(ticker)
    builder
  }

}
