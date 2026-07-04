package com.bank.aiassistant.infrastructure;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 基础设施容器冒烟测试。
 *
 * 默认不启动容器，避免普通单元测试依赖 Docker。CI 或本机需要验证完整环境时，
 * 设置 RUN_TESTCONTAINERS=true 后运行 mvn test 即可。
 */
class InfrastructureContainerSmokeTest {

    @Test
    void shouldStartCoreInfrastructureContainersWhenEnabled() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("RUN_TESTCONTAINERS")));

        try (MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                .withDatabaseName("bank_ai_assistant")
                .withUsername("root")
                .withPassword("root123456");
             GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2"))
                     .withExposedPorts(6379);
             GenericContainer<?> rabbitmq = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management"))
                     .withExposedPorts(5672, 15672);
             ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
                     DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.3"))
                     .withEnv("xpack.security.enabled", "false")) {

            mysql.start();
            redis.start();
            rabbitmq.start();
            elasticsearch.start();

            assertThat(mysql.isRunning()).isTrue();
            assertThat(redis.isRunning()).isTrue();
            assertThat(rabbitmq.isRunning()).isTrue();
            assertThat(elasticsearch.isRunning()).isTrue();
        }
    }
}
