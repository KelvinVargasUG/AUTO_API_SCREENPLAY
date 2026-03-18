package com.automation.runners;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
    features = "classpath:features",
    glue     = {"com.automation.stepdefinitions"},
    plugin   = {"pretty"},
    tags     = ""
)
public class CucumberRunner {}
