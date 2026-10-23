package pt.ua.tqs.tuttibox.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pt.ua.tqs.tuttibox.testdoubles.TestDoublesConfig;

/** Same Spring context as the integration tests: real PostgreSQL (Testcontainers) + test doubles. */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDoublesConfig.class)
public class CucumberSpringConfiguration {
}
