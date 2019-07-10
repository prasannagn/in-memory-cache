package common.cache

import com.codahale.metrics.MetricRegistry
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

class NimbusStatsCounterSpec extends FeatureSpec with Matchers with GivenWhenThen {
  feature("CaffeineStatsCounter") {
    scenario("Record metrics correctly") {
      val metricRegistry = new MetricRegistry
      val counter = new NimbusStatsCounter("testFunction", metricRegistry)

      counter.recordLoadSuccess(1)
      counter.recordEviction()
      counter.recordEviction(1)
      counter.recordHits(1)
      counter.recordMisses(1)
      counter.recordLoadFailure(1)

      var i = 0
      counter.registerEstimatedSize(() => {
        i = i + 1
        i
      })

      metricRegistry.counter("NimbusCache.testFunction.hits").getCount shouldBe 1
      metricRegistry.counter("NimbusCache.testFunction.loads-success").getCount shouldBe 1
      metricRegistry.counter("NimbusCache.testFunction.misses").getCount shouldBe 1
      metricRegistry.counter("NimbusCache.testFunction.loads-failure").getCount shouldBe 1
      metricRegistry.counter("NimbusCache.testFunction.evictions").getCount shouldBe 2
      metricRegistry.counter("NimbusCache.testFunction.evictions-weight").getCount shouldBe 2
      metricRegistry.timer("NimbusCache.testFunction.loads").getCount shouldBe 2

      metricRegistry.getGauges.get("NimbusCache.testFunction.estimatedSize").getValue shouldBe 1
      metricRegistry.getGauges.get("NimbusCache.testFunction.estimatedSize").getValue shouldBe 2
      metricRegistry.getGauges.get("NimbusCache.testFunction.estimatedSize").getValue shouldBe 3
    }
  }

}
