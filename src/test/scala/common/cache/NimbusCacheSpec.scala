package common.cache

import java.util.concurrent.atomic.AtomicBoolean

import common.cache.NimbusCache._
import org.scalatest.concurrent.Eventually._
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class NimbusCacheSpec extends FeatureSpec with Matchers with GivenWhenThen {

  private val config = NimbusCacheConfig(
    16,
    Some(2),
    Some(Duration("2 seconds")),
    Some(Duration("2 seconds")),
    None,
    None
  )

  def testFunction(k: String): Future[Int] = Future.successful(Random.nextInt)

  def testSynchronousFunction(k: String): Int = Random.nextInt

  val testFunctionWithFailure: String => Future[Int] = {
    val flag = new AtomicBoolean(true)
    k: String => {
      if (flag.getAndSet(false)) {
        Future.failed(new RuntimeException())
      } else {
        Future.successful(10)
      }
    }
  }

  val testSynchronousFunctionWithFailure: String => Int = {
    val flag = new AtomicBoolean(true)
    k: String => {
      if (flag.getAndSet(false)) {
        throw new RuntimeException()
      } else {
        10
      }
    }
  }

  feature("Test Nimbus Cache for configuration") {
    scenario("Cache timeout test") {

      val memorizedTestFunction = memoizeAsync(testFunction)(config).get

      val freshVal = memorizedTestFunction("input") // should load from testFunction
      Thread.sleep(500)
      val cachedVal = memorizedTestFunction("input") // should load from cache

      eventually {
        cachedVal shouldBe freshVal
      }

      Thread.sleep(1500)
      val newVal = memorizedTestFunction("input") // should load from testFunction
      eventually {
        newVal should not be freshVal
      }

    }

    scenario("Should not cache when load key function fails") {

      val memorizedTestFunction = memoizeAsync(testFunctionWithFailure)(config).get

      And("testFunction fails")
      val freshVal = memorizedTestFunction("input")

      Thread.sleep(500)

      And("testFunction succeeds")
      val newVal = memorizedTestFunction("input")

      eventually {
        freshVal.failed.value shouldBe a[Option[Failure[RuntimeException]]]
        newVal.value shouldBe Some(Success(10))
      }
    }

    scenario("Async Cache invalidation test if load key was success") {
      val cacheOperations = memoizeAsync(testFunction)(config)
      val memorizedTestFunction = cacheOperations.get
      val freshVal = memorizedTestFunction("input") // should load from testFunction
      Thread.sleep(500)
      cacheOperations.invalidateAll()
      val cachedVal = memorizedTestFunction("input") // should load from cache

      eventually {
        cachedVal should not be freshVal
      }

      Thread.sleep(1500)
      val newVal = memorizedTestFunction("input") // should load from testFunction
      eventually {
        newVal should not be freshVal
      }
    }

    scenario("Cache timeout test for synchronous load key function") {

      val memorizedTestFunction = memoize(testSynchronousFunction)(config).get

      val freshVal = memorizedTestFunction("input") // should load from testFunction
      Thread.sleep(500)
      val cachedVal = memorizedTestFunction("input") // should load from cache

      eventually {
        cachedVal shouldBe freshVal
      }

      Thread.sleep(1500)
      val newVal = memorizedTestFunction("input2") // should load from testFunction
      eventually {
        newVal should not be freshVal
      }

    }

    scenario("Should not cache when synchronous load key function fails") {

      val memorizedTestFunction = memoize(testSynchronousFunctionWithFailure)(config).get

      And("testFunction fails")

      intercept[RuntimeException] {
        memorizedTestFunction("input")
      }

      Thread.sleep(500)

      And("testFunction succeeds")
      val newVal = memorizedTestFunction("input")
      newVal shouldBe 10
    }

    scenario("Sync Cache invalidation test if load key was success") {
      val cacheOperations = memoize(testSynchronousFunction)(config)
      val memorizedTestFunction = cacheOperations.get
      val freshVal = memorizedTestFunction("input") // should load from testFunction
      Thread.sleep(500)
      cacheOperations.invalidateAll()
      val cachedVal = memorizedTestFunction("input") // should load from cache

      eventually {
        cachedVal should not be freshVal
      }

      Thread.sleep(1500)
      val newVal = memorizedTestFunction("input2") // should load from testFunction
      eventually {
        newVal should not be freshVal
      }
    }

  }

}
