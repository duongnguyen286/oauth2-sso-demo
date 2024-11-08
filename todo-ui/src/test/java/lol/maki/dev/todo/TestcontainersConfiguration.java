package lol.maki.dev.todo;

import java.time.Duration;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	GenericContainer<?> authorizationServer() {
		return new GenericContainer<>("bellsoft/liberica-openjre-alpine:21")
			.withCopyFileToContainer(MountableFile.forClasspathResource("authorization-0.0.1-SNAPSHOT.jar"),
					"/authorization.jar")
			.withCommand("java", "-jar", "/authorization.jar", "--spring.security.user.name=test@example.com",
					"--spring.security.user.password=test",
					"--spring.security.oauth2.authorizationserver.client.todo-ui.registration.client-id=todo-ui",
					"--spring.security.oauth2.authorizationserver.client.todo-ui.registration.client-secret={noop}secret",
					"--spring.security.oauth2.authorizationserver.client.todo-ui.registration.redirect-uris=http://localhost:52241/login/oauth2/code/todo-ui",
					"--management.zipkin.tracing.export.enabled=false", "--spring.main.banner-mode=off",
					"--logging.level.org.springframework.security=info")
			.withExposedPorts(9000)
			.waitingFor(Wait.forHttp("/actuator/health").forPort(9000).withStartupTimeout(Duration.ofSeconds(10)))
			.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("authorization-server")));
	}

	@Bean
	DynamicPropertyRegistrar dynamicPropertyRegistrar(GenericContainer<?> authorizationServer) {
		return registry -> registry.add("spring.security.oauth2.client.provider.todo-ui.issuer-uri",
				() -> "http://127.0.0.1:" + authorizationServer.getMappedPort(9000));
	}

}
