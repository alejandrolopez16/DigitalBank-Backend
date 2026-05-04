package com.DigitalBank.backend.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@SelectClasspathResource("cucumber/features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.DigitalBank.backend.cucumber")
public class CucumberRunnerIT {
}
