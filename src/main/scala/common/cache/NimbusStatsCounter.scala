package common.cache

import java.util.concurrent.TimeUnit.NANOSECONDS

import com.codahale.metrics.MetricRegistry.MetricSupplier
import com.codahale.metrics.{Gauge, MetricRegistry}
import com.github.benmanes.caffeine.cache.stats.{CacheStats, StatsCounter}

class NimbusStatsCounter(functionName: String, metricRegistry: MetricRegistry) extends StatsCounter {

  private val hitCount = metricRegistry.counter(s"NimbusCache.$functionName.hits")
  private val loadSuccessCount = metricRegistry.counter(s"NimbusCache.$functionName.loads-success")
  private val missCount = metricRegistry.counter(s"NimbusCache.$functionName.misses")
  private val loadFailureCount = metricRegistry.counter(s"NimbusCache.$functionName.loads-failure")
  private val evictionCount = metricRegistry.counter(s"NimbusCache.$functionName.evictions")
  private val evictionWeight = metricRegistry.counter(s"NimbusCache.$functionName.evictions-weight")
  private val totalLoadTime = metricRegistry.timer(s"NimbusCache.$functionName.loads")

  override def recordLoadSuccess(loadTime: Long): Unit = {
    loadSuccessCount.inc()
    totalLoadTime.update(loadTime, NANOSECONDS)
  }

  override def recordLoadFailure(loadTime: Long): Unit = {
    loadFailureCount.inc()
    totalLoadTime.update(loadTime, NANOSECONDS)
  }

  override def recordEviction(): Unit = recordEviction(1)

  override def recordEviction(weight: Int): Unit = {
    evictionCount.inc()
    evictionWeight.inc(weight)
  }

  override def recordHits(count: Int): Unit = hitCount.inc(count)

  override def recordMisses(count: Int): Unit = missCount.inc(count)

  override def snapshot(): CacheStats = new CacheStats(
    hitCount.getCount,
    missCount.getCount,
    loadSuccessCount.getCount,
    loadFailureCount.getCount,
    totalLoadTime.getCount,
    evictionCount.getCount,
    evictionWeight.getCount
  )

  def registerEstimatedSize(f: () => Long): Unit = {
    val estimatedSizeGauge: MetricSupplier[Gauge[_]] = () => new Gauge[Long] {
      override def getValue: Long = f()
    }
    metricRegistry.gauge(s"NimbusCache.$functionName.estimatedSize", estimatedSizeGauge)
  }
}
