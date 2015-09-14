package org.infinispan.client.hotrod.tck

import org.infinispan.arquillian.core.InfinispanResource
import org.infinispan.arquillian.core.RemoteInfinispanServer
import org.infinispan.client.hotrod.exceptions.HotRodClientException
import org.infinispan.client.hotrod.{RemoteCache, RemoteCacheManager}
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder
import org.jboss.arquillian.junit.Arquillian
import org.junit.Test
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite
import org.scalatest.prop.GeneratorDrivenPropertyChecks

@RunWith(classOf[Arquillian])
class SingleKeyIT extends JUnitSuite with Matchers with GeneratorDrivenPropertyChecks {
  // JUnitSuite enables this test to be run with JUnit and hence for @RunWith to kick in

  @InfinispanResource("server0")
  private var server: RemoteInfinispanServer = _

  @Test
  def testGenericProperty(): Unit = {
    forAll { (n: Int) =>
      whenever (n > 1) { n / 2 should be > 0 }
    }
  }

  @Test
  def testPutGet(): Unit = {
    val client = new RemoteCacheManager(new ConfigurationBuilder().addServer()
        .host(server.getHotrodEndpoint.getInetAddress.getHostName)
        .port(server.getHotrodEndpoint.getPort).build())
    val cache = client.getCache[Int, String]()
    cache.put(1, "v1") shouldBe null
    cache.get(1) shouldBe "v1"
  }

  @Test
  def testForAllPutGet(): Unit = {
    val client = new RemoteCacheManager(new ConfigurationBuilder().addServer()
        .host(server.getHotrodEndpoint.getInetAddress.getHostName)
        .port(server.getHotrodEndpoint.getPort).build())
    val cache = client.getCache[AnyRef, AnyRef]()

    val strings = Gen.alphaStr // primitive
    val uuids = Gen.uuid // serializable object
    val nulls = Gen.const(null) // null
    val nonSerials = Gen.const(new NonSerializable("not serializable")) // non-serializable object
    val any = Gen.oneOf(nulls, strings, uuids)

    forAll (any, any) { (k: AnyRef, v: AnyRef) =>
      cache.put(k, v) shouldBe null
      cache.get(k) shouldBe v
    }

    forAll (nonSerials, any) { (k: AnyRef, v: AnyRef) =>
      intercept[HotRodClientException] {
        cache.put(k, v)
      }
      intercept[HotRodClientException] {
        cache.get(k)
      }
    }

    forAll (any, nonSerials) { (k: AnyRef, v: AnyRef) =>
      intercept[HotRodClientException] {
        cache.put(k, v)
      }
    }
  }

}
