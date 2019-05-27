package hudson.plugins.cobertura.datadog;

import net.sf.json.JSONObject;

/**
 *
 * Marker interface for Datadog events.
 */
public interface DatadogEvent  {
  /**
   *
   * @return The payload for the given event. Events usually have a custom message
   *
   */
  public JSONObject createPayload();
}
