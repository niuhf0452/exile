package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.CompositeSource
import com.github.niuhf0452.exile.config.impl.SimpleConfigSource
import io.kotlintest.matchers.doubles.plusOrMinus
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec

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
        val mapper = ConfigMapper.newBuilder()
                .config(config)
                .addMapping("exile.database.mysql", MysqlConfig::class)
                .addMapping(RedisConfig::class)
                .build()
        val mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig shouldBeSameInstanceAs mapper.get("exile.database.mysql")
        mysqlConfig.host shouldBe "localhost"
        mysqlConfig.port shouldBe 3306
        mysqlConfig.options.createTable shouldBe true
        mysqlConfig.options.threadPoolSize shouldBe 10
        mysqlConfig.options.cacheRatio shouldBe 0.7.plusOrMinus(0.0001)

        val mappings = mapper.mappings()
        mappings.size shouldBe 2
        mappings[0].path shouldBe "exile.database.mysql"
        mappings[0].receiverClass shouldBe MysqlConfig::class
        mappings[1].path shouldBe "redis"
        mappings[1].receiverClass shouldBe RedisConfig::class

        mappings[0].toString() shouldStartWith "Mapping"
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
        val mapper = ConfigMapper.newBuilder()
                .config(config)
                .addMapping("exile.database.mysql", MysqlConfig::class)
                .build()
        val mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig.host shouldBe "mysql"
    }

    test("A ConfigMapper can reload") {
        val source = CompositeSource.threadSafeSource()
        source.addSource(SimpleConfigSource(configStr))
        val config = Config.newBuilder().from(source).build()
        val mapper = ConfigMapper.newBuilder()
                .config(config)
                .addMapping("exile.database.mysql", MysqlConfig::class)
                .build()
        var mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig.host shouldBe "localhost"
        source.addSource(SimpleConfigSource("""
            exile.database.mysql {
              host = mysql
            }
        """.trimIndent()))
        mapper.reload()
        mysqlConfig = mapper.get(MysqlConfig::class)
        mysqlConfig.host shouldBe "mysql"
        mysqlConfig.port shouldBe 3306
    }

    test("A ConfigMapper should throw if parameter is incorrect") {
        val config = Config.newBuilder()
                .fromString(configStr)
                .build()
        val mapper = ConfigMapper.newBuilder()
                .config(config)
                .addMapping("exile.database.mysql", MysqlConfig::class)
                .build()

        shouldThrow<ConfigException> {
            mapper.get("exile.database.redis")
        }

        shouldThrow<ConfigException> {
            mapper.get(Options::class)
        }

        shouldThrow<ConfigException> {
            ConfigMapper.newBuilder()
                    .config(config)
                    .addMapping(RedisConfig2::class)
                    .build()
        }

        shouldThrow<ConfigException> {
            ConfigMapper.newBuilder()
                    .config(config)
                    .addMapping("redis", RedisConfig2::class)
                    .build()
        }
    }
}) {
    data class Options(val createTable: Boolean, val threadPoolSize: Int, val cacheRatio: Double)
    data class MysqlConfig(val host: String, val port: Int, val options: Options)

    @Configuration("redis")
    data class RedisConfig(val host: String, val port: Int)

    class RedisConfig2(val host: String, val port: Int)
}