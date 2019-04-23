/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.instrumentation

import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.context.Context
import kamon.instrumentation.logback.Logback
import kamon.trace.Span
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar._
import org.slf4j.MDC

class LogbackSpanConverterSpec extends WordSpec with Matchers with Eventually {

  "TokenConverter" when {
    "a span is uninitialized" should {
      "report an undefined context" in {
        val appender = buildMemoryAppender(configurator)
        appender.doAppend(createLoggingEvent(context))
        appender.getLastLine should be("undefined")
      }
    }

    "a span is initialized" should {
      "report the its context" in {
        val memoryAppender = buildMemoryAppender(configurator)

        val span = Kamon.spanBuilder("my-span").start()
        val traceID = span.trace.id
        val contextWithSpan = Context.of(Span.Key, span)

        Kamon.withContext(contextWithSpan) {
          memoryAppender.doAppend(createLoggingEvent(context))
        }

        memoryAppender.getLastLine shouldBe traceID.string
      }

      "MDC context" in {
        val memoryAppender = buildMemoryAppender(configurator,s"%X{${Logback.mdcTraceKey}} %X{${Logback.mdcSpanKey}} %X{mdc_key}")

        val span = Kamon.spanBuilder("my-span").start()
        val traceID = span.trace.id
        val spanID = span.id
        val contextWithSpan = Context.of(Span.Key, span)

        MDC.put("mdc_key","mdc_value")
        Kamon.withContext(contextWithSpan) {
          memoryAppender.doAppend(createLoggingEvent(context))
        }

        memoryAppender.getLastLine shouldBe traceID.string + " " + spanID.string + " mdc_value"
        MDC.get(Logback.mdcTraceKey) shouldBe null
        MDC.get(Logback.mdcSpanKey) shouldBe null
      }

      "report the custom MDC keys in the context" in {
        Kamon.reconfigure(
          ConfigFactory
            .parseString("kamon.logback.mdc-traced-keys = [ testKey1, testKey2 ]")
            .withFallback(ConfigFactory.defaultReference()))
        val memoryAppender = buildMemoryAppender(configurator, "%X{testKey1} %X{testKey2}")

        val span = Kamon.spanBuilder("my-span").start()

        val contextWithSpan = Context
          .of(Span.Key, span)
          .withKey(Context.key[Option[String]]("testKey1", None), Some("testKey1Value"))
          .withKey(Context.key[Option[String]]("testKey2", None), Some("testKey2Value"))

        Kamon.withContext(contextWithSpan) {
          memoryAppender.doAppend(createLoggingEvent(context))
        }

        memoryAppender.getLastLine shouldBe "testKey1Value testKey2Value"
      }

      "report empty if custom MDC keys are configured, but not provided" in {
        Kamon.reconfigure(
          ConfigFactory
            .parseString("kamon.logback.mdc-traced-broadcast-keys = [ testKey1, testKey2 ]")
            .withFallback(ConfigFactory.defaultReference()))
        val memoryAppender = buildMemoryAppender(configurator, "%X{testKey1} %X{testKey2}")

        val span = Kamon.spanBuilder("my-span").start()
        val contextWithSpan = Context
          .of(Span.Key, span)

        Kamon.withContext(contextWithSpan) {
          memoryAppender.doAppend(createLoggingEvent(context))
        }

        memoryAppender.getLastLine shouldBe " "
      }


      "disable MDC context" in {
        Kamon.reconfigure(
          ConfigFactory
            .parseString("kamon.logback.mdc-context-propagation = off")
            .withFallback(ConfigFactory.defaultReference())
        )


        val memoryAppender = buildMemoryAppender(configurator,s"%X{${Logback.mdcTraceKey}}")

        val span = Kamon.spanBuilder("my-span").start()
        val contextWithSpan = Context.of(Span.Key, span)

        Kamon.withContext(contextWithSpan) {
          memoryAppender.doAppend(createLoggingEvent(context))
        }

        memoryAppender.getLastLine shouldBe ""
      }

      "report the its context using an AsyncAppender" in {

        val memoryAppender = buildMemoryAppender(configurator)
        val asyncAppender = buildAsyncAppender(configurator, memoryAppender)

        val span = Kamon.spanBuilder("my-span").start()
        val traceID = span.trace.id
        val contextWithSpan = Context.of(Span.Key, span)

        Kamon.withContext(contextWithSpan) {
          asyncAppender.doAppend(createLoggingEvent(context))
        }

        eventually(timeout(2 seconds)) {
          memoryAppender.getLastLine shouldBe traceID.string
        }
      }
    }
  }
}