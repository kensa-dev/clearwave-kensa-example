package com.clearwave.ui

import dev.kensa.playwright.PlaywrightBrowserDriver
import dev.kensa.selenium.SeleniumBrowserDriver
import dev.kensa.uitesting.UserStub
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class FeasibilityUserPlaywright(
    driver: PlaywrightBrowserDriver,
    private val baseUrl: String,
) : UserStub<PlaywrightBrowserDriver>(driver) {

    fun navigatesToTheFeasibilityPage() {
        driver.page.navigate(baseUrl)
        screenshot("feasibility page")
    }

    fun entersTheAddress(postcode: String, addressLine1: String, town: String, county: String) {
        driver.page.locator("[data-testid='postcode-input']").fill(postcode)
        driver.page.locator("[data-testid='address-line1-input']").fill(addressLine1)
        driver.page.locator("[data-testid='town-input']").fill(town)
        driver.page.locator("[data-testid='county-input']").fill(county)
    }

    fun submitsTheForm() {
        driver.page.locator("[data-testid='check-button']").click()
    }

    fun seesAvailableProfiles(): Int {
        driver.page.waitForSelector("[data-testid='results-success']")
        screenshot("availability result")
        return driver.page.locator("[data-testid='profile-card']").count()
    }

    fun seesNoServiceMessage(): Boolean {
        driver.page.waitForSelector("[data-testid='results-not-serviceable']")
        screenshot("not serviceable result")
        return driver.page.locator("[data-testid='results-not-serviceable']").isVisible
    }

    fun seesAValidationErrorOnPostcode(): Boolean {
        val visible = driver.page.locator("[data-testid='postcode-error']").isVisible
        screenshot("validation error")
        return visible
    }
}

class FeasibilityUserSelenium(
    driver: SeleniumBrowserDriver,
    private val baseUrl: String,
) : UserStub<SeleniumBrowserDriver>(driver) {

    private val wait = WebDriverWait(driver.webDriver, Duration.ofSeconds(5))

    fun navigatesToTheFeasibilityPage() {
        driver.webDriver.get(baseUrl)
        screenshot("feasibility page")
    }

    fun entersTheAddress(postcode: String, addressLine1: String, town: String, county: String) {
        driver.webDriver.findElement(By.cssSelector("[data-testid='postcode-input']")).sendKeys(postcode)
        driver.webDriver.findElement(By.cssSelector("[data-testid='address-line1-input']")).sendKeys(addressLine1)
        driver.webDriver.findElement(By.cssSelector("[data-testid='town-input']")).sendKeys(town)
        driver.webDriver.findElement(By.cssSelector("[data-testid='county-input']")).sendKeys(county)
    }

    fun submitsTheForm() {
        driver.webDriver.findElement(By.cssSelector("[data-testid='check-button']")).click()
    }

    fun seesAvailableProfiles(): Int {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='results-success']")))
        screenshot("availability result")
        return driver.webDriver.findElements(By.cssSelector("[data-testid='profile-card']")).size
    }

    fun seesNoServiceMessage(): Boolean {
        val displayed = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='results-not-serviceable']"))
        ).isDisplayed
        screenshot("not serviceable result")
        return displayed
    }

    fun seesAValidationErrorOnPostcode(): Boolean {
        val displayed = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='postcode-error']"))
        ).isDisplayed
        screenshot("validation error")
        return displayed
    }
}
