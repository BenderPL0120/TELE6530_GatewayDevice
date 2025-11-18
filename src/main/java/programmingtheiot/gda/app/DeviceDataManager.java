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

package programmingtheiot.gda.app;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.BaseIotData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.SystemStateData;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.CoapClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;
import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * Device Data Manager for the Gateway Device Application.
 *
 * This class is the heart and soul of the GDA. It handles all data processing
 * within the application, and marshals all the requests to the appropriate destination.
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static

	private static final Logger _Logger =
			Logger.getLogger(DeviceDataManager.class.getName());

	// private var's

	private boolean enableMqttClient = true;
	private boolean enableCoapServer = false;
	private boolean enableCoapClient = false;
	private boolean enableCloudClient = false;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enableSystemPerf = false;

	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	private IPubSubClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private IRequestResponseClient coapClient = null;
	private SystemPerformanceManager sysPerfMgr = null;

	private DataUtil dataUtil = null;

	// Humidity control related variables
	private ActuatorData latestHumidifierActuatorData = null;
	private ActuatorData latestHumidifierActuatorResponse = null;
	private SensorData latestHumiditySensorData = null;
	private OffsetDateTime latestHumiditySensorTimeStamp = null;

	private boolean handleHumidityChangeOnDevice = false;
	private int lastKnownHumidifierCommand = ConfigConst.OFF_COMMAND;

	// Humidity thresholds and settings
	private long humidityMaxTimePastThreshold = 300; // seconds
	private float nominalHumiditySetting = 40.0f;
	private float triggerHumidifierFloor = 30.0f;
	private float triggerHumidifierCeiling = 50.0f;

	// Temperature control related variables
	private ActuatorData latestTemperatureActuatorData = null;
	private ActuatorData latestTemperatureActuatorResponse = null;
	private SensorData latestTemperatureSensorData = null;
	private OffsetDateTime latestTemperatureSensorTimeStamp = null;

	private boolean handleTemperatureChangeOnDevice = false;
	private int lastKnownTemperatureCommand = ConfigConst.OFF_COMMAND;

	// Temperature thresholds and settings
	private long temperatureMaxTimePastThreshold = 300; // seconds
	private float nominalTemperatureSetting = 20.0f;
	private float triggerTemperatureFloor = 10.0f;
	private float triggerTemperatureCeiling = 30.0f;

	// Pressure control related variables
	private ActuatorData latestPressureActuatorData = null;
	private ActuatorData latestPressureActuatorResponse = null;
	private SensorData latestPressureSensorData = null;
	private OffsetDateTime latestPressureSensorTimeStamp = null;

	private boolean handlePressureChangeOnDevice = false;
	private int lastKnownPressureCommand = ConfigConst.OFF_COMMAND;

	// Pressure thresholds and settings
	private long pressureMaxTimePastThreshold = 300; // seconds
	private float nominalPressureSetting = 1000.0f;
	private float triggerPressureFloor = 950.0f;
	private float triggerPressureCeiling = 1050.0f;

	// constructors

	/**
	 * Default constructor.
	 * Reads configuration from default config file.
	 */
	public DeviceDataManager()
	{
		super();

		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.enableMqttClient =
				configUtil.getBoolean(
						ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);

		this.enableCoapServer =
				configUtil.getBoolean(
						ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);

		this.enableCoapClient =
				configUtil.getBoolean(
						ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_CLIENT_KEY);

		this.enableCloudClient =
				configUtil.getBoolean(
						ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);

		this.enableSmtpClient =
				configUtil.getBoolean(
						ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SMTP_CLIENT_KEY);

		this.enablePersistenceClient =
				configUtil.getBoolean(
						ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);

		initManager();
	}

	/**
	 * Constructor with explicit connection enablement flags.
	 * Allows programmatic control over which connections to enable.
	 */
	public DeviceDataManager(
			boolean enableMqttClient,
			boolean enableCoapServer,
			boolean enableCloudClient,
			boolean enableSmtpClient,
			boolean enablePersistenceClient)
	{
		super();

		this.enableMqttClient = enableMqttClient;
		this.enableCoapServer = enableCoapServer;
		this.enableCloudClient = enableCloudClient;
		this.enableSmtpClient = enableSmtpClient;
		this.enablePersistenceClient = enablePersistenceClient;

		initManager();
	}


	// public methods

	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator response: " + data.getName());

			// Store the latest response for humidity control logic
			if (data.getTypeID() == ConfigConst.HUMIDIFIER_ACTUATOR_TYPE) {
				this.latestHumidifierActuatorResponse = data;
			}

			// Optional: handle incoming data analysis
			this.handleIncomingDataAnalysis(resourceName, data);

			if (data.hasError()) {
				_Logger.warning("Error flag set for ActuatorData instance.");
			}

			return true;
		} else {
			_Logger.warning("Received null ActuatorData");
			return false;
		}
	}

	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		return false;
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (msg != null) {
			_Logger.info("Handling incoming generic message: " + msg);

			// The msg will most likely be JSON that represents either
			// an ActuatorData or SystemStateData instance

			return true;
		} else {
			_Logger.warning("Received null message");
			return false;
		}
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.info("Handling sensor message: " + data.getName());

			if (data.hasError()) {
				_Logger.warning("Error flag set for SensorData instance.");
			}

			// Convert to JSON for potential upstream transmission
			String jsonData = DataUtil.getInstance().sensorDataToJson(data);
			
			_Logger.info("JSON [SensorData] -> " + jsonData);
			
			// TODO: retrieve this from config file
			int qos = ConfigConst.DEFAULT_QOS;
			
			// Store data if persistence client is enabled
			if (this.enablePersistenceClient && this.persistenceClient != null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), qos, data);
			}
			
			// Perform incoming data analysis for threshold checking
			this.handleIncomingDataAnalysis(resourceName, data);
			
			// Handle upstream transmission
			this.handleUpstreamTransmission(resourceName, jsonData, qos);
			
			return true;
		} else {
			_Logger.warning("Received null SensorData");
			return false;
		}
	}

	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling system performance message: " + data.getName());

			if (data.hasError()) {
				_Logger.warning("Error flag set for SystemPerformanceData instance.");
			}

			// Convert to JSON for potential upstream transmission
			if (this.dataUtil != null) {
				String jsonData = this.dataUtil.systemPerformanceDataToJson(data);

				// TODO: retrieve this from config file
				int qos = ConfigConst.DEFAULT_QOS;

				// Store data if persistence client is enabled
				if (this.enablePersistenceClient && this.persistenceClient != null) {
					this.persistenceClient.storeData(resourceName.getResourceName(), qos, data);
				}

				// Handle upstream transmission
				this.handleUpstreamTransmission(resourceName, jsonData, qos);
			}

			return true;
		} else {
			_Logger.warning("Received null SystemPerformanceData");
			return false;
		}
	}

	/**
	 * Sets the actuator data listener for handling actuator commands.
	 *
	 * @param name The name identifier for the listener
	 * @param listener The IActuatorDataListener implementation
	 */
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			_Logger.info("Setting actuator data listener: " + name);
			this.actuatorDataListener = listener;
		}
	}

	/**
	 * Starts the Device Data Manager and all enabled connections.
	 */
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");

		// Start system performance manager if enabled
		if (this.sysPerfMgr != null) {
			_Logger.info("Starting SystemPerformanceManager...");
			this.sysPerfMgr.startManager();
		}

		// Start MQTT client if enabled
		if (this.enableMqttClient && this.mqttClient != null) {
			_Logger.info("Starting MQTT client...");
			if (this.mqttClient.connectClient()) {
				_Logger.info("Successfully connected MQTT client to broker.");
				
				// int qos = ConfigConst.DEFAULT_QOS; // Or read from config

				// // Subscribe to topics
				// this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
				// this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
				// this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
			} else {
				_Logger.severe("Failed to connect MQTT client to broker.");
			}
		}

		// Start CoAP server if enabled
		if (this.enableCoapServer && this.coapServer != null) {
			_Logger.info("Starting CoAP server...");
			if (this.coapServer.startServer()) {
				_Logger.info("CoAP server started successfully.");
			} else {
				_Logger.severe("Failed to start CoAP server. Check log file for details.");
			}
		}

		// Start CoAP client if enabled
		// There is no start or stop method associated with CoapClientConnector, 
		// nothing more needs to be done.

		// Start cloud client if enabled
		if (this.enableCloudClient && this.cloudClient != null) {
			_Logger.info("Starting cloud client...");
			// TODO: implement this in Lab Module 10
			// boolean connected = this.cloudClient.connectClient();
			// _Logger.info("Cloud client started: " + connected);
		}

		// Start persistence client if enabled
		if (this.enablePersistenceClient && this.persistenceClient != null) {
			_Logger.info("Starting persistence client...");
			// TODO: implement this as an optional exercise in Lab Module 5
			// boolean connected = this.persistenceClient.connectClient();
			// _Logger.info("Persistence client started: " + connected);
		}

		_Logger.info("DeviceDataManager started successfully.");
	}

	/**
	 * Stops the Device Data Manager and all active connections.
	 */
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager...");

		// Stop system performance manager
		if (this.sysPerfMgr != null) {
			_Logger.info("Stopping SystemPerformanceManager...");
			this.sysPerfMgr.stopManager();
		}

		// Disconnect MQTT client if connected
		if (this.enableMqttClient && this.mqttClient != null) {
			_Logger.info("Stopping MQTT client...");
			
			// Unsubscribe from topics
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);

			if (this.mqttClient.disconnectClient()) {
				_Logger.info("Successfully disconnected MQTT client from broker.");
			} else {
				_Logger.severe("Failed to disconnect MQTT client from broker.");
			}
		}

		// Stop CoAP server
		if (this.enableCoapServer && this.coapServer != null) {
			_Logger.info("Stopping CoAP server...");
			if (this.coapServer.stopServer()) {
				_Logger.info("CoAP server stopped successfully.");
			} else {
				_Logger.severe("Failed to stop CoAP server. Check log file for details.");
			}
		}

		// Disconnect cloud client if connected
		if (this.enableCloudClient && this.cloudClient != null) {
			_Logger.info("Stopping cloud client...");
			// TODO: implement this in Lab Module 10
			// boolean disconnected = this.cloudClient.disconnectClient();
			// _Logger.info("Cloud client stopped: " + disconnected);
		}

		// Disconnect persistence client if connected
		if (this.enablePersistenceClient && this.persistenceClient != null) {
			_Logger.info("Stopping persistence client...");
			// TODO: implement this as an optional exercise in Lab Module 5
			// boolean disconnected = this.persistenceClient.disconnectClient();
			// _Logger.info("Persistence client stopped: " + disconnected);
		}

		_Logger.info("DeviceDataManager stopped successfully.");
	}


	// private methods

	/**
	 * Initializes the manager and creates instances of enabled connections.
	 * This will NOT start them, but only create the instances that will be
	 * used in the startManager() and stopManager() methods.
	 */
	private void initManager()
	{
		_Logger.info("Initializing DeviceDataManager...");

		ConfigUtil configUtil = ConfigUtil.getInstance();

		// Initialize DataUtil for JSON conversion
		this.dataUtil = DataUtil.getInstance();

		// Check if system performance monitoring is enabled
		this.enableSystemPerf =
				configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);

		// Parse humidity control configuration
		this.handleHumidityChangeOnDevice =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, "handleHumidityChangeOnDevice");

		this.humidityMaxTimePastThreshold =
			configUtil.getInteger(
				ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");

		this.nominalHumiditySetting =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");

		this.triggerHumidifierFloor =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");

		this.triggerHumidifierCeiling =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");

		// Parse temperature control configuration
    this.handleTemperatureChangeOnDevice = configUtil.getBoolean(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.HANDLE_TEMP_CHANGE_ON_DEVICE_KEY);

    this.temperatureMaxTimePastThreshold = configUtil.getInteger(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.TEMP_MAX_TIME_PAST_THRESHOLD_KEY);

    this.nominalTemperatureSetting = configUtil.getFloat(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.NOMINAL_TEMP_SETTING_KEY);

    this.triggerTemperatureFloor = configUtil.getFloat(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_TEMP_FLOOR_KEY);

    this.triggerTemperatureCeiling = configUtil.getFloat(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_TEMP_CEILING_KEY);

    // Parse pressure control configuration
    this.handlePressureChangeOnDevice = configUtil.getBoolean(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.HANDLE_PRESSURE_CHANGE_ON_DEVICE_KEY);

    this.pressureMaxTimePastThreshold = configUtil.getInteger(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.PRESSURE_MAX_TIME_PAST_THRESHOLD_KEY);

    this.nominalPressureSetting = configUtil.getFloat(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.NOMINAL_PRESSURE_SETTING_KEY);

    this.triggerPressureFloor = configUtil.getFloat(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_PRESSURE_FLOOR_KEY);

    this.triggerPressureCeiling = configUtil.getFloat(
            ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_PRESSURE_CEILING_KEY);

		// Validate timing parameter - must be between 10 and 7200 seconds
		if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
			this.humidityMaxTimePastThreshold = 300;
		}

		// Initialize System Performance Manager if enabled
		if (this.enableSystemPerf) {
			_Logger.info("System performance monitoring enabled.");
			this.sysPerfMgr = new SystemPerformanceManager();
			this.sysPerfMgr.setDataMessageListener(this);
		}

		// Initialize MQTT Client if enabled
		if (this.enableMqttClient) {
			_Logger.info("MQTT client enabled.");
			this.mqttClient = new MqttClientConnector();
			this.mqttClient.setDataMessageListener(this);
		}

		// Initialize CoAP Server if enabled
		if (this.enableCoapServer) {
			_Logger.info("CoAP server enabled.");
			this.coapServer = new CoapServerGateway(this);
		}

		// Initialize CoAP Client if enabled
		if (this.enableCoapClient) {
			_Logger.info("CoAP client enabled.");
			this.coapClient = new CoapClientConnector();
			this.coapClient.setDataMessageListener(this);
		}

		// Initialize Cloud Client if enabled
		if (this.enableCloudClient) {
			_Logger.info("Cloud client enabled.");
			// TODO: implement this in Lab Module 10
			// this.cloudClient = new CloudClientConnector();
		}

		// Initialize Persistence Client if enabled
		if (this.enablePersistenceClient) {
			_Logger.info("Persistence client enabled.");
			// TODO: implement this as an optional exercise in Lab Module 5
			// this.persistenceClient = new RedisPersistenceAdapter();
		}

		_Logger.info("DeviceDataManager initialized.");
	}

	/**
	 * Handles incoming data analysis for ActuatorData.
	 * Will eventually publish back to the CDA using either MQTT or CoAP.
	 *
	 * @param resourceName The resource name enum
	 * @param data The ActuatorData to analyze
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.info("Analyzing incoming actuator data: " + data.getName());

		if (data.isResponseFlagEnabled()) {
			// This is a response from CDA - process accordingly
			_Logger.info("Processing actuator response from CDA");
			// TODO: implement response handling logic if needed
		} else {
			// This is a command to be sent to CDA
			if (this.actuatorDataListener != null) {
				_Logger.info("Forwarding actuator command to listener");
				this.actuatorDataListener.onActuatorDataUpdate(data);
			} else {
				_Logger.warning("No actuator data listener registered");
			}
		}
	}

	/**
	 * Handles incoming data analysis for SystemStateData.
	 * This is a command the GDA should interpret and handle internally.
	 *
	 * @param resourceName The resource name enum
	 * @param data The SystemStateData to analyze
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
	{
		_Logger.log(Level.INFO, "Handling incoming SystemStateData analysis...");

		// TODO: Implement custom logic for system state handling
		// This is a command the GDA should interpret and handle internally
	}

	/**
	 * Handles incoming data analysis for SensorData.
	 * Routes sensor data to appropriate analysis methods based on sensor type.
	 *
	 * @param resourceName The resource name enum
	 * @param data The SensorData to analyze
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SensorData data)
	{
		_Logger.log(Level.INFO, "Handling incoming SensorData analysis...");
		
		// Check either resource or SensorData for type
		if (data.getTypeID() == ConfigConst.HUMIDITY_SENSOR_TYPE) {
			if (this.handleHumidityChangeOnDevice) {
				handleHumiditySensorAnalysis(resourceName, data);
			}
		} else if (data.getTypeID() == ConfigConst.TEMP_SENSOR_TYPE) {
			if (this.handleTemperatureChangeOnDevice) {
				handleTemperatureSensorAnalysis(resourceName, data);
			}
		} else if (data.getTypeID() == ConfigConst.PRESSURE_SENSOR_TYPE) {
			if (this.handlePressureChangeOnDevice) {
				handlePressureSensorAnalysis(resourceName, data);
			}
		}
		// Add other sensor type handlers here as needed
	}

	/**
	 * Analyzes humidity sensor data and triggers actuator commands if thresholds are exceeded.
	 * Implements time-based threshold crossing logic to prevent rapid switching.
	 *
	 * @param resource The resource name enum
	 * @param data The humidity sensor data to analyze
	 */
	private void handleHumiditySensorAnalysis(ResourceNameEnum resource, SensorData data)
	{
		float humidityVal = data.getValue();
		_Logger.info("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());

		boolean isLow = humidityVal < this.triggerHumidifierFloor;
		boolean isHigh = humidityVal > this.triggerHumidifierCeiling;
		
		if (isLow || isHigh) {
			_Logger.info("Humidity data from CDA exceeds nominal range.");
			
			if (this.latestHumiditySensorData == null) {
				// Set properties then exit - nothing more to do until the next sample
				this.latestHumiditySensorData = data;
				this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);
				
				_Logger.info(
					"Starting humidity nominal exception timer. Waiting for seconds: " +
					this.humidityMaxTimePastThreshold);
				
				return;
			} else {
				OffsetDateTime curHumiditySensorTimeStamp = getDateTimeFromData(data);
				
				long diffSeconds =
					ChronoUnit.SECONDS.between(
						this.latestHumiditySensorTimeStamp, curHumiditySensorTimeStamp);
				
				_Logger.info("Checking Humidity value exception time delta: " + diffSeconds);
				
				if (diffSeconds >= this.humidityMaxTimePastThreshold) {
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
					ad.setLocationID(data.getLocationID());
					ad.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
					ad.setValue(this.nominalHumiditySetting);
					
					if (isLow) {
						ad.setCommand(ConfigConst.ON_COMMAND);
					} else if (isHigh) {
						ad.setCommand(ConfigConst.OFF_COMMAND);
					}
					
					_Logger.info(
						"Humidity exceptional value reached. Sending actuation event to CDA: " +
						ad);
					
					this.lastKnownHumidifierCommand = ad.getCommand();
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);
					
					// Set ActuatorData and reset SensorData (and timestamp)
					this.latestHumidifierActuatorData = ad;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				}
			}
		} else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {
			// Check if we need to turn off the humidifier
			if (this.latestHumidifierActuatorData != null) {
				// Check the value - if the humidifier is on, but not yet at nominal, keep it on
				if (data.getValue() >= this.nominalHumiditySetting) {
					this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);
					
					_Logger.info(
						"Humidity nominal value reached. Sending OFF actuation event to CDA: " +
						this.latestHumidifierActuatorData);
					
					sendActuatorCommandtoCda(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);
					
					// Reset ActuatorData and SensorData (and timestamp)
					this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
					this.latestHumidifierActuatorData = null;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				} else {
					_Logger.info("Humidifier is still on. Not yet at nominal levels (OK).");
				}
			} else {
				// Shouldn't happen, unless some other logic
				// nullifies the class-scoped ActuatorData instance
				_Logger.warning(
					"ERROR: ActuatorData for humidifier is null (shouldn't be). Can't send command.");
			}
		}
	}

	/**
	 * Analyzes Temperature sensor data and triggers actuator commands if thresholds are exceeded.
	 * Implements time-based threshold crossing logic to prevent rapid switching.
	 *
	 * @param resource The resource name enum
	 * @param data The temperature sensor data to analyze
	 */
	private void handleTemperatureSensorAnalysis(ResourceNameEnum resource, SensorData data)
	{
		float tempVal = data.getValue();
		_Logger.info("Analyzing temp data from CDA: " + data.getLocationID() + ". Value: " + tempVal);

		boolean isLow = tempVal < this.triggerTemperatureFloor;
		boolean isHigh = tempVal > this.triggerTemperatureCeiling;

		if (isLow || isHigh) {
			_Logger.info("Temperature data from CDA exceeds nominal range.");

			if (this.latestTemperatureSensorData == null) {
				this.latestTemperatureSensorData = data;
				this.latestTemperatureSensorTimeStamp = getDateTimeFromData(data);
				return;
			} else {
				OffsetDateTime curTempSensorTimeStamp = getDateTimeFromData(data);
				long diffSeconds = ChronoUnit.SECONDS.between(
						this.latestTemperatureSensorTimeStamp, curTempSensorTimeStamp);

				if (diffSeconds >= this.temperatureMaxTimePastThreshold) {
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.HVAC_ACTUATOR_NAME);
					ad.setLocationID(data.getLocationID());
					ad.setTypeID(ConfigConst.HVAC_ACTUATOR_TYPE);
					ad.setValue(this.nominalTemperatureSetting);
					ad.setCommand(ConfigConst.ON_COMMAND);

					_Logger.info("Temperature exceptional value. Sending HVAC ON to CDA: " + ad);

					this.lastKnownTemperatureCommand = ad.getCommand();
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);

					this.latestTemperatureActuatorData = ad;
					this.latestTemperatureSensorData = null;
					this.latestTemperatureSensorTimeStamp = null;
				}
			}
		} else if (this.lastKnownTemperatureCommand == ConfigConst.ON_COMMAND) {
			// If open HVAC before, check if back to normal now
			if (this.latestTemperatureActuatorData != null) {
				boolean isNormalized = Math.abs(tempVal - this.nominalTemperatureSetting) < 1.0f;
				// If within 1 degree of nominal, consider normalized
				if (isNormalized) {
					this.latestTemperatureActuatorData.setCommand(ConfigConst.OFF_COMMAND);
					_Logger.info("Temperature normalized. Sending HVAC OFF to CDA.");
				}
				sendActuatorCommandtoCda(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestTemperatureActuatorData);

				this.lastKnownTemperatureCommand = this.latestTemperatureActuatorData.getCommand();
				this.latestTemperatureActuatorData = null;
				this.latestTemperatureSensorData = null;
				this.latestTemperatureSensorTimeStamp = null;
			}
		}
	}

	/**
	 * Analyzes Pressure sensor data and triggers actuator commands if thresholds are exceeded.
	 * Implements time-based threshold crossing logic to prevent rapid switching.
	 *
	 * @param resource The resource name enum
	 * @param data The pressure sensor data to analyze
	 */
	private void handlePressureSensorAnalysis(ResourceNameEnum resource, SensorData data)
	{
		float pressureVal = data.getValue();
		_Logger.info("Analyzing pressure data from CDA: " + data.getLocationID() + ". Value: " + pressureVal);

		boolean isLow = pressureVal < this.triggerPressureFloor;
		boolean isHigh = pressureVal > this.triggerPressureCeiling;

		if (isLow || isHigh) {
			_Logger.info("Pressure data from CDA exceeds nominal range.");

			if (this.latestPressureSensorData == null) {
				this.latestPressureSensorData = data;
				this.latestPressureSensorTimeStamp = getDateTimeFromData(data);
				return;
			} else {
				OffsetDateTime curPressureSensorTimeStamp = getDateTimeFromData(data);
				long diffSeconds = ChronoUnit.SECONDS.between(
						this.latestPressureSensorTimeStamp, curPressureSensorTimeStamp);

				if (diffSeconds >= this.pressureMaxTimePastThreshold) {
					ActuatorData ad = new ActuatorData();
					// Using LED actuator for pressure alert
					ad.setName(ConfigConst.LED_ACTUATOR_NAME);
					ad.setLocationID(data.getLocationID());
					ad.setTypeID(ConfigConst.LED_ACTUATOR_TYPE);
					ad.setValue(this.nominalPressureSetting);
					ad.setCommand(ConfigConst.ON_COMMAND);

					_Logger.info("Pressure exceptional value. Sending LED ON to CDA: " + ad);

					this.lastKnownPressureCommand = ad.getCommand();
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);

					this.latestPressureActuatorData = ad;
					this.latestPressureSensorData = null;
					this.latestPressureSensorTimeStamp = null;
				}
			}
		} else if (this.lastKnownPressureCommand == ConfigConst.ON_COMMAND) {
			if (this.latestPressureActuatorData != null) {
				this.latestPressureActuatorData.setCommand(ConfigConst.OFF_COMMAND);
				_Logger.info("Pressure normalized. Sending LED OFF to CDA.");

				sendActuatorCommandtoCda(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestPressureActuatorData);

				this.lastKnownPressureCommand = this.latestPressureActuatorData.getCommand();
				this.latestPressureActuatorData = null;
				this.latestPressureSensorData = null;
				this.latestPressureSensorTimeStamp = null;
			}
		}
	}

	/**
	 * Sends actuator command to CDA using available communication channels.
	 * Supports both CoAP (via listener) and MQTT communication.
	 *
	 * @param resource The resource name for the actuator command
	 * @param data The ActuatorData command to send
	 */
	private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data)
	{
		// NOTE: This is how an ActuatorData command will get passed to the CDA
		// when the GDA is providing the CoAP server and hosting the appropriate
		// ActuatorData resource. It will typically be used when the OBSERVE
		// client (the CDA, assuming the GDA is the server and CDA is the client)
		// has sent an OBSERVE GET request to the ActuatorData resource.
		if (this.actuatorDataListener != null) {
			// Check for null reference before invoking
			try {
				this.actuatorDataListener.onActuatorDataUpdate(data);
			} catch (NullPointerException e) {
				_Logger.warning("ActuatorDataListener not properly initialized. Cannot send via CoAP.");
			}
		}
		
		// NOTE: This is how an ActuatorData command will get passed to the CDA
		// when using MQTT to communicate between the GDA and CDA
		if (this.enableMqttClient && this.mqttClient != null) {
			String jsonData = DataUtil.getInstance().actuatorDataToJson(data);
			
			if (this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS)) {
				_Logger.info(
					"Published ActuatorData command from GDA to CDA: " + data.getCommand());
			} else {
				_Logger.warning(
					"Failed to publish ActuatorData command from GDA to CDA: " + data.getCommand());
			}
		}
	}

	/**
	 * Extracts OffsetDateTime from IoT data timestamp.
	 * Falls back to current time if parsing fails.
	 *
	 * @param data The BaseIotData containing the timestamp
	 * @return OffsetDateTime parsed from the data or current time if parsing fails
	 */
	private OffsetDateTime getDateTimeFromData(BaseIotData data)
	{
		OffsetDateTime odt = null;
		
		try {
			odt = OffsetDateTime.parse(data.getTimeStamp());
		} catch (Exception e) {
			_Logger.warning(
				"Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");
			
			// TODO: this won't be accurate, but should be reasonably close, as the CDA will
			// most likely have recently sent the data to the GDA
			odt = OffsetDateTime.now();
		}
		
		return odt;
	}

	/**
	 * Handles upstream transmission of data to cloud services.
	 *
	 * @param resourceName The resource name enum
	 * @param jsonData The JSON data to transmit
	 * @param qos Quality of Service level
	 * @return true if transmission successful, false otherwise
	 */
	private void handleUpstreamTransmission(ResourceNameEnum resource, String jsonData, int qos)
	{
		// NOTE: This will be implemented in Part 04
		_Logger.info("TODO: Send JSON data to cloud service: " + resource);
	}

	/**
	 * Initializes the enabled connections (legacy method name for compatibility).
	 * Delegates to initManager().
	 */
	private void initConnections()
	{
		initManager();
	}
}