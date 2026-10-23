package pt.ua.tqs.tuttibox.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pt.ua.tqs.tuttibox.adapter.FakeLockerHardware;
import pt.ua.tqs.tuttibox.testdoubles.FakeGeocoder;
import pt.ua.tqs.tuttibox.testdoubles.MutableClock;
import pt.ua.tqs.tuttibox.testdoubles.RecordingNotifier;
import pt.ua.tqs.tuttibox.testdoubles.TestData;
import pt.ua.tqs.tuttibox.testdoubles.TestDoublesConfig;

/**
 * Browser tests (mvn verify -Pe2e). The app runs on a random port with a separate PostgreSQL container,
 * the hardware fake in demo mode and the fake geocoder. Locators use labels / roles / data-testid,
 * and Playwright's auto-waiting assertions, so no sleeps are needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///tuttibox_e2e")
@ActiveProfiles("test")
@Import(TestDoublesConfig.class)
abstract class AbstractE2E {

    private static Playwright playwright;
    private static Browser browser;

    @Value("${local.server.port}")
    int port;

    @Autowired
    TestData data;
    @Autowired
    FakeLockerHardware hardware;
    @Autowired
    RecordingNotifier notifier;
    @Autowired
    FakeGeocoder geocoder;
    @Autowired
    MutableClock clock;

    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        data.reset();
        hardware.reset();
        notifier.clear();
        geocoder.reset();
        clock.set(MutableClock.START);
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        context.close();
    }

    String url(String path) {
        return "http://localhost:" + port + path;
    }
}
