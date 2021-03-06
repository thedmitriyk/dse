/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import logging.Logger;

/**
 *
 * @author dmcd2356
 */
public class PropertiesFile {
  // the location of the properties file for this application
  private String propertiesName;
  private String propPath;
  private String propFile;
  private Logger logger;
  private Properties props;

  PropertiesFile (String path, String name, Logger log) {

    propertiesName = name;
    propPath = path.endsWith("/") ? path : path + "/";
    propFile = propPath + "site.properties";
    logger = log;
    props = null;

    FileInputStream in = null;
    File propfile = new File(propFile);
    if (propfile.exists()) {
      try {
        // property file exists, read it in
        in = new FileInputStream(propFile);
        props = new Properties();
        props.load(in);
        return; // success!
      } catch (FileNotFoundException ex) {
        logError("ERROR: FileNotFoundException - " + propFile);
      } catch (IOException ex) {
        logError("ERROR: IOException on FileInputStream - " + propFile);
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException ex) {
            logError("ERROR: IOException on close - " + propFile);
          }
        }
      }
    }

    // property file does not exist - create a default (empty) one
    props = new Properties();
    try {
      // first, check if properties directory exists
      File proppath = new File (propPath);
      if (!proppath.exists()) {
        proppath.mkdir();
      }

      // now save properties file
      logMessage("Creating new site.properties file for " + propertiesName);
      File file = new File(propFile);
      try (FileOutputStream fileOut = new FileOutputStream(file)) {
        props.store(fileOut, "Initialization");
      }
    } catch (IOException ex) {
      logError("ERROR: IOException on FileOutputStream - " + propFile);
      props = null;
    }
  }
    
  PropertiesFile (String path, String name) {
    this(path, name, null);
  }

  private void logMessage(String message) {
    if(logger == null) {
      System.out.println(message);
    } else {
      logger.printLine(message);
    }
  }
  
  private void logError(String message) {
    if(logger == null) {
      System.err.println(message);
    } else {
      logger.printLine(message);
    }
  }
  
  public String getPropertiesItem (String tag, String dflt) {
    if (props == null || tag == null || tag.isEmpty()) {
      return dflt;
    }

    String value = props.getProperty(tag);
    if (value == null || value.isEmpty()) {
      setPropertiesItem (tag, dflt);
//      logError(propertiesName + " site.properties <" + tag + "> : not found, setting to " + dflt);
//      props.setProperty(tag, dflt);
      return dflt;
    }

    //logMessage(propertiesName + " <" + tag + "> = " + value);
    return value;
  }
  
  public void setPropertiesItem (String tag, String value) {
    // save changes to properties file
    if (props == null || tag == null || tag.isEmpty()) {
      return;
    }

    // make sure the properties file exists
    File propsfile = new File(propFile);
    if (propsfile.exists()) {
      try {
        if (value == null) {
          value = "";
        }
        String old_value = props.getProperty(tag);
        if (old_value == null) {
          old_value = "";
        }
        if (!old_value.equals(value)) {
          logMessage(propertiesName + " <" + tag + "> set to " + value);
        }
        props.setProperty(tag, value);
        FileOutputStream out = new FileOutputStream(propFile);
        props.store(out, "---No Comment---");
        out.close();
      } catch (FileNotFoundException ex) {
        logError("ERROR: FileNotFoundException - " + propFile);
      } catch (IOException ex) {
        logError("ERROR: IOException on FileOutputStream - " + propFile);
      }
    }
  }  

  public int getIntegerProperties(String tag, int dflt, int min, int max) {
    int retval;
    String value = getPropertiesItem(tag, "" + dflt);
    try {
      retval = Integer.parseUnsignedInt(value);
      if (retval >= min && retval <= max) {
        return retval;
      }
    } catch (NumberFormatException ex) { }

    logError("ERROR: invalid value for SystemProperties " + tag + ": " + value);
    setPropertiesItem(tag, "" + dflt);
    return dflt;
  }
  
}
