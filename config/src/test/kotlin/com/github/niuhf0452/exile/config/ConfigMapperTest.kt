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
        mapper.reload()
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
}) {
    @Serializable
    data class Options(val createTable: Boolean, val threadPoolSize: Int, val cacheRatio: Double)

    @Serializable
    data class MysqlConfig(val host: String, val port: Int, val options: Options)

    @Serializable
    @Configuration("redis")
    data class RedisConfig(val host: String, val port: Int)
}