package com.automate.binusdemo;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class BinusTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    public static void setUpAll(){
        WebDriverManager.firefoxdriver().clearDriverCache().setup();
    }

    @BeforeEach
    public void setUp() {
        FirefoxOptions options = new FirefoxOptions();

        driver = new FirefoxDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        login();
    }

    private void login() {
        String username = System.getenv("EMAIL");
        String password = System.getenv("PASS");
        assertNotNull(username, "BINUS_USER env var not set");
        assertNotNull(password, "BINUS_PASS env var not set");

        driver.get("https://binusmaya.binus.ac.id/login");

        WebElement emailField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']"))
        );
        emailField.sendKeys(username);
        driver.findElement(By.cssSelector("input[type='submit']")).click();

        WebElement passwordField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name='passwd']"))
        );
        passwordField.sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit']")).click();

        WebElement yesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='submit'][value='Yes']"))
        );
        yesButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[contains(@class,'MuiTypography-root') and normalize-space(text())='My Dashboard']")
        ));
    }

    @Test
    public void dashboardLoadsAfterLogin() {
        WebElement header = driver.findElement(
                By.xpath("//p[contains(@class,'MuiTypography-root') and normalize-space(text())='My Dashboard']")
        );
        assertTrue(header.isDisplayed());
    }

    @Test
    public void openCurrentSessionPPT() throws InterruptedException {
        String originalWindow = driver.getWindowHandle();
        WebElement openButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'MuiButton-containedInfo') and contains(@class, 'css-mi8imr')]")
        ));
        openButton.click();
        wait.until(d -> d.getWindowHandles().size() > 1);

        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(originalWindow)) {
                driver.switchTo().window(handle);
                break;
            }
        }

        // Handle clicking the "Retry" button if exist here
        List<WebElement> allButtons = driver.findElements(By.cssSelector("button.C--button.type--1.font-size-13px.-theme-primary.mt-2.ml-3"));
        for (WebElement button : allButtons) {
            if (button.getText().trim().equalsIgnoreCase("retry")) {
                if (button.isDisplayed()) {
                    button.click();
                }
                break;
            }
        }

        List<WebElement> elementList = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("span.text-white"))
        );

        int targetOccurrence = 5;
        int targetIndex = targetOccurrence - 1;

        if (elementList.size() > targetIndex) {
            elementList.get(targetIndex).click();
        } else {
            fail("Target occurrence not found. Total found: " + elementList.size());
        }

        Thread.sleep(5000);

        List<WebElement> saveButtons = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("button.C--button.type--1.font-size-13px.-theme-primary")
                )
        );

        assertFalse(saveButtons.isEmpty(), "The button with the specified class does not exist on the page.");

        WebElement saveButton = saveButtons.get(1);
        String buttonText = saveButton.getText().trim().toLowerCase();

        assertEquals("save a copy", buttonText,
                "Expected button text 'save a copy' but found: '" + buttonText + "'");
    }

    @Test
    public void openScheduleToday() throws InterruptedException {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue()); // zero-pads e.g. 3 -> "03"

        String scheduleUrl = "https://lms.binus.ac.id/lms/schedule/" + year + "-" + month;
        driver.get(scheduleUrl);
        try {
            WebElement continueButton = new WebDriverWait(driver, Duration.ofSeconds(5)).until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(@class,'MuiButton-containedPrimary') and normalize-space(.)='Continue to LMS']")
                    )
            );
            continueButton.click();
        } catch (org.openqa.selenium.TimeoutException e) {
            // Button didn't appear — assume we landed directly, continue normally
        }
        // Wait for something on the schedule page to confirm it actually loaded
        wait.until(ExpectedConditions.urlContains("/lms/schedule/"));
        Thread.sleep(5000);

        assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(scheduleUrl),
                "Did not land on expected schedule URL. Actual: " + driver.getCurrentUrl());
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error during driver quit: " + e.getMessage());
            }
            driver = null;
        }
    }
}