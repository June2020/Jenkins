package hudson.plugins.cobertura.datadog;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;

import static hudson.Util.fixEmptyAndTrim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;



public class DatadogUtilities {

  private static final Logger logger =  Logger.getLogger(DatadogUtilities.class.getName());
 
  static final String DISPLAY_NAME = "Datadog Plugin";
  static final String VALIDATE = "v1/validate";
  static final String METRIC = "v1/series";
  static final String EVENT = "v1/events";
  static final String SERVICECHECK = "v1/check_run";
  static final Integer OK = 0;
  static final Integer WARNING = 1;
  static final Integer CRITICAL = 2;
  static final Integer UNKNOWN = 3;
  static final double THOUSAND_DOUBLE = 1000.0;
  static final long THOUSAND_LONG = 1000L;
  static final float MINUTE = 60;
  static final float HOUR = 3600;
  static final Integer HTTP_FORBIDDEN = 403;
  static final Integer MAX_HOSTNAME_LEN = 255;
  
  /**
   *
   * @return - The descriptor for the Datadog plugin. In this case the global
   *         - configuration.
   */
  /*public static DatadogBuildListener.DescriptorImpl getDatadogDescriptor() {
    DatadogBuildListener.DescriptorImpl desc = (DatadogBuildListener.DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(DatadogBuildListener.class);
    return desc;
  }*/

  

  /**
   *
   * @return - The api key configured in the global configuration. Shortcut method.
   */
  /*public static Secret getApiKey() {
    return DatadogUtilities.getDatadogDescriptor().getApiKey();
  }*/

  
 

  public static String getHostname(final EnvVars envVars) {
    String[] UNIX_OS = {"mac", "linux", "freebsd", "sunos"};
    String hostname="";
    // Check hostname configuration from Jenkins
  /*  String hostname = getHostName();
    if ( (hostname != null) && isValidHostname(hostname) ) {
      logger.fine(String.format("Using hostname set in 'Manage Plugins'. Hostname: %s", hostname));
      return hostname;
    }*/

    // Check hostname using jenkins env variables
    if ( envVars.get("HOSTNAME") != null ) {
      hostname = envVars.get("HOSTNAME");
    }
    if ( (hostname != null) && isValidHostname(hostname) ) {
      logger.fine(String.format("Using hostname found in $HOSTNAME host environment variable. "
                                + "Hostname: %s", hostname));
      return hostname;
    }

    // Check OS specific unix commands
    String os = "UNIX_OS";
    if ( Arrays.asList(UNIX_OS).contains(os) ) {
      // Attempt to grab unix hostname
      try {
        String[] cmd = {"/bin/hostname", "-f"};
        Process proc = Runtime.getRuntime().exec(cmd);
        InputStream in = proc.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ( (line = reader.readLine()) != null ) {
          out.append(line);
        }
        reader.close();

        hostname = out.toString();
      } catch (Exception e) {
        logger.severe(e.getMessage());
      }

      // Check hostname
      if ( (hostname != null) && isValidHostname(hostname) ) {
        logger.fine(String.format("Using unix hostname found via `/bin/hostname -f`. Hostname: %s",
                                  hostname));
        return hostname;
      }
    }

    // Check localhost hostname
    try {
      hostname = Inet4Address.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      logger.fine(String.format("Unknown hostname error received for localhost. Error: %s", e));
    }
    if ( (hostname != null) && isValidHostname(hostname) ) {
      logger.fine(String.format("Using hostname found via "
                                + "Inet4Address.getLocalHost().getHostName()."
                                + " Hostname: %s", hostname));
      return hostname;
    }

    // Never found the hostname
    if ( (hostname == null) || "".equals(hostname) ) {
      logger.warning("Unable to reliably determine host name. You can define one in "
                     + "the 'Manage Plugins' section under the 'Datadog Plugin' section.");
    }
    return null;
  }

  /**
   * Validator function to ensure that the hostname is valid. Also, fails on
   * empty String.
   *
   * @param hostname - A String object containing the name of a host.
   * @return a boolean representing the validity of the hostname
   */
  public static final Boolean isValidHostname(final String hostname) {
    String[] localHosts = {"localhost", "localhost.localdomain",
                           "localhost6.localdomain6", "ip6-localhost"};
    String VALID_HOSTNAME_RFC_1123_PATTERN = "^(([a-zA-Z0-9]|"
                                             + "[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*"
                                             + "([A-Za-z0-9]|"
                                             + "[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
    String host = hostname.toLowerCase();

    // Check if hostname is local
    if ( Arrays.asList(localHosts).contains(host) ) {
      logger.fine(String.format("Hostname: %s is local", hostname));
      return false;
    }

    // Ensure proper length
    if ( hostname.length() > MAX_HOSTNAME_LEN ) {
      logger.fine(String.format("Hostname: %s is too long (max length is %s characters)",
                                hostname, MAX_HOSTNAME_LEN));
      return false;
    }

    // Check compliance with RFC 1123
    Pattern r = Pattern.compile(VALID_HOSTNAME_RFC_1123_PATTERN);
    Matcher m = r.matcher(hostname);

    // Final check: Hostname matches RFC1123?
    return m.find();
  }

  /**
   * @param daemonHost - The host to check
   *
   * @return - A boolean that checks if the daemonHost is valid
   */
  public static boolean isValidDaemon(final String daemonHost) {
    if(!daemonHost.contains(":")) {
      logger.info("Daemon host does not contain the port seperator ':'");
      return false;
    }

    String hn = daemonHost.split(":")[0];
    String pn = daemonHost.split(":").length > 1 ? daemonHost.split(":")[1] : "";

    if(StringUtils.isBlank(hn)) {
      logger.info("Daemon host part is empty");
      return false;
    }

    //Match ports [1024-65535]
    Pattern p = Pattern.compile("^(102[4-9]|10[3-9]\\d|1[1-9]\\d{2}|[2-9]\\d{3}|[1-5]\\d{4}|6[0-4]"
            + "\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$");

    boolean match = p.matcher(pn).find();

    if(!match) {
      logger.info("Port number is invalid must be in the range [1024-65535]");
    }

    return match;
  }

  /**
   * @param targetMetricURL - The API URL which the plugin will report to.
   *
   * @return - A boolean that checks if the targetMetricURL is valid
   */
  public static boolean isValidMetricURL(final String targetMetricURL) {
    if(!targetMetricURL.contains("http")) {
      logger.info("The field must be configured in the form <http|https>://<url>/");
      return false;
    }

    if(StringUtils.isBlank(targetMetricURL)) {
      logger.info("Empty API URL");
      return false;
    }

    return true;
  }

  /**
   * Safe getter function to make sure an exception is not reached.
   *
   * @param data - A JSONObject containing a set of key/value pairs.
   * @param key - A String to be used to lookup a value in the JSONObject data.
   * @return a String representing data.get(key), or "null" if it doesn't exist
   */
  public static String nullSafeGetString(final JSONObject data, final String key) {
    if ( data.get(key) != null ) {
      return data.get(key).toString();
    } else {
      return "null";
    }
  }

  

  /**
   * Converts the returned String from calling run.getParent().getFullName(),
   * to a String, usable as a tag.
   *
   * @param fullDisplayName - A String object representing a job's fullDisplayName
   * @return a human readable String representing the fullDisplayName of the Job, in a
   *         format usable as a tag.
   */
  public static String normalizeFullDisplayName(final String fullDisplayName) {
    String normalizedName = fullDisplayName.replaceAll("»", "/").replaceAll(" ", "");
    return normalizedName;
  }
}
