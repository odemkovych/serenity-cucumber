package net.serenitybdd.cucumber.web

import com.github.goldin.spock.extensions.tempdir.TempDir
import net.thucydides.core.ThucydidesSystemProperty
import net.thucydides.core.model.TestResult
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.thucydides.core.util.MockEnvironmentVariables
import net.thucydides.core.webdriver.Configuration
import net.thucydides.core.webdriver.SystemPropertiesConfiguration
import net.serenitybdd.cucumber.integration.DataDrivenScenario
import net.serenitybdd.cucumber.integration.ScenarioSuite
import net.serenitybdd.cucumber.integration.SimpleSeleniumDifferentBrowserScenario
import net.serenitybdd.cucumber.integration.SimpleSeleniumFailingAndPassingScenario
import net.serenitybdd.cucumber.integration.SimpleSeleniumFailingScenario
import net.serenitybdd.cucumber.integration.SimpleSeleniumPageObjects
import net.serenitybdd.cucumber.integration.SimpleSeleniumScenario
import spock.lang.Specification

import static net.serenitybdd.cucumber.util.CucumberRunner.serenityRunnerForCucumberTestRunner

public class WhenRunningWebCucumberStories extends Specification {

    @TempDir
    File outputDirectory

    def environmentVariables = new MockEnvironmentVariables()

    def setup() {
        environmentVariables.setProperty("webdriver.driver", "phantomjs");
    }

    def "should run table-driven scenarios successfully"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleSeleniumScenario.class, outputDirectory, environmentVariables);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.title == "A scenario that uses selenium"
        testOutcome.tags
        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }


    def "a failing story should generate failure test outcome"() throws Throwable {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleSeleniumFailingScenario.class, outputDirectory, environmentVariables);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]


        then:
        testOutcome.title == "A failing scenario that uses selenium"
        testOutcome.isFailure();

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }



    def "a test should use a different browser if requested"()  {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleSeleniumDifferentBrowserScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.title == "A scenario that uses selenium"
        testOutcome.isSuccess();

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }


   def "a cucumber step library can use page objects directly"()  {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleSeleniumPageObjects.class, outputDirectory, environmentVariables);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.title == "A scenario that uses selenium"
        testOutcome.isSuccess();

        and: "there should be one step for each row in the table"
        testOutcome.stepCount == 2
    }


    def "stories with errors in one scenario should still run subsequent scenarios"()  {
        given:
        environmentVariables.setProperty("restart.browser.each.scenario","true");
        def runtime = serenityRunnerForCucumberTestRunner(SimpleSeleniumFailingAndPassingScenario, outputDirectory, environmentVariables);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory).sort{it.startTime}

        then:
        recordedTestOutcomes.size() == 2
        recordedTestOutcomes[0].result == TestResult.FAILURE
        recordedTestOutcomes[1].result == TestResult.SUCCESS
    }


    def "should be able to specify the browser in the base test"() {
        given:
        environmentVariables.setProperty(ThucydidesSystemProperty.DRIVER.getPropertyName(), "htmlunit");
        environmentVariables.setProperty(ThucydidesSystemProperty.THUCYDIDES_USE_UNIQUE_BROWSER.getPropertyName(),"true");
        def runtime = serenityRunnerForCucumberTestRunner(SimpleSeleniumFailingAndPassingScenario, outputDirectory, environmentVariables);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory).sort{it.startTime};

        then:
        recordedTestOutcomes[0].result == TestResult.FAILURE
    }


    def "should be able to set thucydides properties in the base test"() {
        given:
        environmentVariables.setProperty(ThucydidesSystemProperty.DRIVER.getPropertyName(), "htmlunit");
        environmentVariables.setProperty(ThucydidesSystemProperty.THUCYDIDES_USE_UNIQUE_BROWSER.getPropertyName(),"true");
        environmentVariables.setProperty(ThucydidesSystemProperty.WEBDRIVER_BASE_URL.getPropertyName(),"some-base-url")
        environmentVariables.setProperty(ThucydidesSystemProperty.THUCYDIDES_TIMEOUT.getPropertyName(),"5")
        Configuration systemConfiguration = new SystemPropertiesConfiguration(environmentVariables);
        systemConfiguration.setOutputDirectory(outputDirectory);
        def runtime = serenityRunnerForCucumberTestRunner(SimpleSeleniumFailingAndPassingScenario, systemConfiguration)

        when:
        runtime.run();

        then:
        systemConfiguration.getBaseUrl() == "some-base-url"
        systemConfiguration.getElementTimeout() == 5
        systemConfiguration.shouldUseAUniqueBrowser() == true

    }



    def  "data  driven  steps  should  appear  as  nested  steps"()  {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(DataDrivenScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);

        then:
        recordedTestOutcomes.size() == 1
        def topLevelSteps = recordedTestOutcomes.get(0).getTestSteps()
        topLevelSteps.size() == 3

        def nestedDataDrivenSteps = topLevelSteps.get(2).getChildren().get(0).getChildren();
        nestedDataDrivenSteps.size() == 3;
    }



    def "two  scenarios  using  the  same  given  story  should  return  two  test  outcomes"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(ScenarioSuite.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);

        then:
        recordedTestOutcomes.size() == 2
    }
}
