package hudson.plugins.cobertura.datadog;

import java.util.Map;

import hudson.plugins.cobertura.Ratio;
import hudson.plugins.cobertura.targets.CoverageMetric;
import hudson.plugins.cobertura.targets.CoverageResult;


public class DatadogPayload {
	
	CoverageResult coverageResult;
	String system;
	String module;
	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	
	
	 public DatadogPayload(CoverageResult coverageResult) {
	        this.coverageResult = coverageResult;
	      
	    }

	public CoverageResult getCoverageResult() {
		return coverageResult;
	}

	public void setCoverageResult(CoverageResult coverageResult) {
		this.coverageResult = coverageResult;
	}



}
