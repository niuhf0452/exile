package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.CompositeSource
import com.github.niuhf0452.exile.config.source.SimpleConfigSource
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.doubles.plusOrMinus
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger

class ConfigMapperTest : FunSpec({
    val configStr = """
                    exile.database.mysql { 
                      host = localhost
                      port = 3306
                      options {
                        createTable = true
                        threadPoolSize = 10
                        cacheRatio = 0.7
                      }
                    }
                    redis {
                      host = localhost
                      port = 6798
                    }
                """.trimIndent()
    test("A ConfigMapper can map config to class") {
        val config = Config.newBuilder()
                .fromString(configStr)
                .build()
        val mapper = ConfigMapper.newMapper(config)
        mapper.addMapping("exile.database.mysql", MysqlConfig::class)
        val mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig.host shouldBe "localhost"
        mysqlConfig.port shouldBe 3306
        mysqlConfig.options.createTable shouldBe true
        mysqlConfig.options.threadPoolSize shouldBe 10
        mysqlConfig.options.cacheRatio shouldBe 0.7.plusOrMinus(0.0001)

        val mappings = mapper.mappings()
        mappings.size shouldBe 1
        mappings[0].path shouldBe "exile.database.mysql"
        mappings[0].receiverClass shouldBe MysqlConfig::class
        mappings[0].toString() shouldStartWith "Mapping"
    }

    test("A ConfigMapper can create mapping lazily") {
        val config = Config.newBuilder()
                .fromString(configStr)
                .build()
        val mapper = ConfigMapper.newMapper(config)
        mapper.mappings().shouldBeEmpty()

        val redisConfig = mapper.get(RedisConfig::class)
        redisConfig.host shouldBe "localhost"
        redisConfig.port shouldBe 6798

        val mappings = mapper.mappings()
        mappings.size shouldBe 1
        mappings[0].path shouldBe "redis"
        mappings[0].receiverClass shouldBe RedisConfig::class
    }

    test("A ConfigMapper can merge configurations from multiple sources") {
        val config = Config.newBuilder()
                .fromString(configStr)
                .fromString("""
                    exile.database.mysql {
                      host = mysql
                    }
                """.trimIndent(), Config.Order.OVERWRITE)
                .build()
        val mapper = ConfigMapper.newMapper(config)
        mapper.addMapping("exile.database.mysql", MysqlConfig::class)
        val mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig.host shouldBe "mysql"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    test("A ConfigMapper can reload") {
        val source = CompositeSource.threadSafeSource()
        source.addSource(SimpleConfigSource(configStr))
        val config = Config.newBuilder().from(source).build()
        val mapper = ConfigMapper.newMapper(config)
        mapper.addMapping("exile.database.mysql", MysqlConfig::class)
        var mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig.host shouldBe "localhost"
        source.addSource(SimpleConfigSource("""
            exile.database.mysql {
              host = mysql
            }
        """.trimIndent()))
        mapper.reload().get()
        mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig.host shouldBe "mysql"
        mysqlConfig.port shouldBe 3306
    }

    test("A ConfigMapper should throw if class doesn't have @Configuration") {
        val config = Config.newBuilder().fromString(configStr).build()
        val mapper = ConfigMapper.newMapper(config)

        shouldThrow<ConfigException> {
            mapper.get(Options::class)
        }
    }

    test("A ConfigMapper should throw if missing @Configuration") {
        val config = Config.newBuilder().fromString(configStr).build()

        shouldThrow<ConfigException> {
            ConfigMapper.newMapper(config).addMapping(Options::class)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    test("A ConfigMapper should respect to listeners") {
        val composite = CompositeSource.threadSafeSource()
        composite.addSource(SimpleConfigSource(configStr))
        val config = Config.newBuilder().from(composite).build()
        val mapper = ConfigMapper.newMapper(config)
        val port = AtomicInteger(0)
        mapper.getMapping(RedisConfig::class).addListener(object : ConfigMapper.Listener<RedisConfig> {
            override fun onUpdate(value: RedisConfig) {
                port.set(value.port)
            }
        }).thenCompose {
            port.get() shouldBe 6798
            composite.addSource(SimpleConfigSource("redis.port = 1234"), Config.Order.OVERWRITE)
            mapper.reload()
        }.thenAccept {
            port.get() shouldBe 1234
        }.get()
    }
}) {
    @Serializable
    data class Options(val createTable: Boolean, val threadPoolSize: Int, val cacheRatio: Double)

    @Serializable
    data class MysqlConfig(val host: String, val port: Int, val options: Options)

    @Serializable
    @Configuration("redis")
    data class RedisConfig(val host: String, val port: Int)
}