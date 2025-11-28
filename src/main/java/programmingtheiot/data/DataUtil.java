/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 *
 * You may find it more helpful to your design to adjust the
 * functionality, constants and interfaces (if there are any)
 * provided within in order to meet the needs of your specific
 * Programming the Internet of Things project.
 */

package programmingtheiot.data;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

/**
 * Utility class for converting IoT data objects to/from JSON format.
 * Implements Singleton pattern for consistent usage across the application.
 */
public class DataUtil
{
	// static

	private static final Logger _Logger =
			Logger.getLogger(DataUtil.class.getName());

	private static final DataUtil _Instance = new DataUtil();

	/**
	 * Returns the Singleton instance of this class.
	 *
	 * @return DataUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}


	// private var's

	private Gson gson;


	// constructors

	/**
	 * Default (private).
	 *
	 */
	private DataUtil()
	{
		super();
		this.gson = new Gson();
	}


	// public methods

	/**
	 * Converts ActuatorData object to JSON string.
	 *
	 * @param actuatorData The ActuatorData object to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String actuatorDataToJson(ActuatorData actuatorData)
	{
		String jsonData = null;

		if (actuatorData != null) {
			jsonData = this.gson.toJson(actuatorData);
			_Logger.log(Level.FINE, "Converted ActuatorData to JSON: " + jsonData);
		} else {
			_Logger.log(Level.WARNING, "ActuatorData is null - cannot convert to JSON");
		}

		return jsonData;
	}

	/**
	 * Converts SensorData object to JSON string.
	 *
	 * @param sensorData The SensorData object to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String sensorDataToJson(SensorData sensorData)
	{
		String jsonData = null;

		if (sensorData != null) {
			jsonData = this.gson.toJson(sensorData);
			_Logger.log(Level.FINE, "Converted SensorData to JSON: " + jsonData);
		} else {
			_Logger.log(Level.WARNING, "SensorData is null - cannot convert to JSON");
		}

		return jsonData;
	}

	/**
	 * Converts an ActuatorData instance to a TimeAndValuePayloadData instance
	 * and then to a JSON string.
	 *
	 * @param data The ActuatorData to convert.
	 * @return String The JSON string representation of TimeAndValuePayloadData.
	 */
	public String actuatorDataToTimeAndValueJson(ActuatorData data)
	{
		String jsonData = null;

		if (data != null) {
			// package ActuatorData into TimeAndValuePayloadData
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(data);
			jsonData = this.gson.toJson(tvData);

			_Logger.log(Level.FINE, "Converted ActuatorData to TimeAndValue JSON: " + jsonData);
		} else {
			_Logger.log(Level.WARNING, "ActuatorData is null - cannot convert to TimeAndValue JSON");
		}

		return jsonData;
	}

	/**
	 * Converts a SensorData instance to a TimeAndValuePayloadData instance
	 * and then to a JSON string.
	 *
	 * @param data The SensorData to convert.
	 * @return String The JSON string representation of TimeAndValuePayloadData.
	 */
	public String sensorDataToTimeAndValueJson(SensorData data)
	{
		String jsonData = null;

		if (data != null) {
			// package SensorData into TimeAndValuePayloadData
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(data);
			jsonData = this.gson.toJson(tvData);

			_Logger.log(Level.FINE, "Converted SensorData to TimeAndValue JSON: " + jsonData);
		} else {
			_Logger.log(Level.WARNING, "SensorData is null - cannot convert to TimeAndValue JSON");
		}

		return jsonData;
	}

	/**
	 * Converts SystemPerformanceData object to JSON string.
	 *
	 * @param sysPerfData The SystemPerformanceData object to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	{
		String jsonData = null;

		if (sysPerfData != null) {
			jsonData = this.gson.toJson(sysPerfData);
			_Logger.log(Level.FINE, "Converted SystemPerformanceData to JSON: " + jsonData);
		} else {
			_Logger.log(Level.WARNING, "SystemPerformanceData is null - cannot convert to JSON");
		}

		return jsonData;
	}

	/**
	 * Converts SystemStateData object to JSON string.
	 *
	 * @param sysStateData The SystemStateData object to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String systemStateDataToJson(SystemStateData sysStateData)
	{
		String jsonData = null;

		if (sysStateData != null) {
			jsonData = this.gson.toJson(sysStateData);
			_Logger.log(Level.FINE, "Converted SystemStateData to JSON: " + jsonData);
		} else {
			_Logger.log(Level.WARNING, "SystemStateData is null - cannot convert to JSON");
		}

		return jsonData;
	}

	/**
	 * Converts JSON string to ActuatorData object.
	 *
	 * @param jsonData The JSON string to convert
	 * @return ActuatorData object, or null if input is null or empty
	 */
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		ActuatorData data = null;

		if (jsonData != null && jsonData.trim().length() > 0) {
			try {
				data = this.gson.fromJson(jsonData, ActuatorData.class);
				_Logger.log(Level.FINE, "Converted JSON to ActuatorData");
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to convert JSON to ActuatorData: " + e.getMessage());
			}
		} else {
			_Logger.log(Level.WARNING, "JSON data is null or empty - cannot convert to ActuatorData");
		}

		return data;
	}

	/**
	 * Converts JSON string to SensorData object.
	 *
	 * @param jsonData The JSON string to convert
	 * @return SensorData object, or null if input is null or empty
	 */
	public SensorData jsonToSensorData(String jsonData)
	{
		SensorData data = null;

		if (jsonData != null && jsonData.trim().length() > 0) {
			try {
				data = this.gson.fromJson(jsonData, SensorData.class);
				_Logger.log(Level.FINE, "Converted JSON to SensorData");
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to convert JSON to SensorData: " + e.getMessage());
			}
		} else {
			_Logger.log(Level.WARNING, "JSON data is null or empty - cannot convert to SensorData");
		}

		return data;
	}

	/**
	 * Converts JSON string to SystemPerformanceData object.
	 *
	 * @param jsonData The JSON string to convert
	 * @return SystemPerformanceData object, or null if input is null or empty
	 */
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		SystemPerformanceData data = null;

		if (jsonData != null && jsonData.trim().length() > 0) {
			try {
				data = this.gson.fromJson(jsonData, SystemPerformanceData.class);
				_Logger.log(Level.FINE, "Converted JSON to SystemPerformanceData");
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to convert JSON to SystemPerformanceData: " + e.getMessage());
			}
		} else {
			_Logger.log(Level.WARNING, "JSON data is null or empty - cannot convert to SystemPerformanceData");
		}

		return data;
	}

	/**
	 * Converts JSON string to SystemStateData object.
	 *
	 * @param jsonData The JSON string to convert
	 * @return SystemStateData object, or null if input is null or empty
	 */
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		SystemStateData data = null;

		if (jsonData != null && jsonData.trim().length() > 0) {
			try {
				data = this.gson.fromJson(jsonData, SystemStateData.class);
				_Logger.log(Level.FINE, "Converted JSON to SystemStateData");
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to convert JSON to SystemStateData: " + e.getMessage());
			}
		} else {
			_Logger.log(Level.WARNING, "JSON data is null or empty - cannot convert to SystemStateData");
		}

		return data;
	}

}
