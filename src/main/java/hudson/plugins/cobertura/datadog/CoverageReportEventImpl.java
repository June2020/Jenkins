package hudson.plugins.cobertura.datadog;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import hudson.model.Result;
import hudson.plugins.cobertura.Ratio;
import hudson.plugins.cobertura.targets.CoverageMetric;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class CoverageReportEventImpl  implements DatadogEvent {

	  private DatadogPayload datadogPayload;
	
	  private Map<CoverageMetric, Ratio> result;
	
	  public CoverageReportEventImpl(DatadogPayload datadogPayload)  {
	    this.result = new EnumMap<CoverageMetric, Ratio>(CoverageMetric.class);
	    this.result.putAll(datadogPayload.getCoverageResult().getResults());
	    this.datadogPayload=datadogPayload;
     
	  }

	  //Creates the raw json payload for this event.
	  @Override
	  public JSONObject createPayload() {
		JSONObject payload = new JSONObject();
	
	    long currtime;
	    String hostname;
	    try {
			hostname=InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			hostname="Jenkins";
		}
        String metric;
	
        JSONArray series = new JSONArray();
	
	   for(CoverageMetric metrics: CoverageMetric.values())
	   {
		    JSONObject content = new JSONObject();
		    JSONArray tags = new JSONArray();
		    JSONArray point = new JSONArray();
		    JSONArray pointArray = new JSONArray();
		   
		    
		    if(datadogPayload.getModule() != null && !datadogPayload.getModule().isEmpty())
			   {
			        tags.add("Application:"+ datadogPayload.getModule() );
			        metric = datadogPayload.getModule()+".code.coverage"; 
			   }
			   else
			   {
				   tags.add("Application:Sample"); 
				   metric="sample.code.coverage";
			   }
		    metric=metric+"."+metrics.getName();
		    content.put("metric", metric);
		    currtime  = System.currentTimeMillis()  / 1000L;
		    point.add(currtime);
		    point.add(this.result.get(metrics).getPercentage());
		    pointArray.add(point);
		    content.put("points", pointArray);
		    content.put("type", "count"); 
			   content.put("host", hostname);
			   content.put("interval", 1);
			
		    if(datadogPayload.getSystem() != null && !datadogPayload.getSystem().isEmpty())
		    	tags.add("system:"+datadogPayload.getSystem());
		    
		  
		    content.put("tags", tags);
		    series.add(content);
		    
	   }
	    
	   payload.put("series", series);
	    
	    
	    
	 
	    return payload;
	  }
}
