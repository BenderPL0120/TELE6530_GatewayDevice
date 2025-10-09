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

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
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
	private SystemPerformanceManager sysPerfMgr = null;

	private DataUtil dataUtil = null;

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

			// Optional: handle incoming data analysis
			// this.handleIncomingDataAnalysis(resourceName, data);

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
			if (this.dataUtil != null) {
				String jsonData = this.dataUtil.sensorDataToJson(data);

				// Will be implemented in Part 03 - Connectivity
				// this.handleUpstreamTransmission(resourceName, jsonData, 0);
			}

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

				// Will be implemented in Part 03 - Connectivity
				// this.handleUpstreamTransmission(resourceName, jsonData, 0);
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
			// TODO: implement this in Lab Module 7
			// boolean connected = this.mqttClient.connectClient();
			// _Logger.info("MQTT client started: " + connected);
		}

		// Start CoAP server if enabled
		if (this.enableCoapServer && this.coapServer != null) {
			_Logger.info("Starting CoAP server...");
			// TODO: implement this in Lab Module 8
			// boolean started = this.coapServer.startServer();
			// _Logger.info("CoAP server started: " + started);
		}

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
			// TODO: implement this in Lab Module 7
			// boolean disconnected = this.mqttClient.disconnectClient();
			// _Logger.info("MQTT client stopped: " + disconnected);
		}

		// Stop CoAP server
		if (this.enableCoapServer && this.coapServer != null) {
			_Logger.info("Stopping CoAP server...");
			// TODO: implement this in Lab Module 8
			// boolean stopped = this.coapServer.stopServer();
			// _Logger.info("CoAP server stopped: " + stopped);
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

		// Initialize System Performance Manager if enabled
		if (this.enableSystemPerf) {
			_Logger.info("System performance monitoring enabled.");
			this.sysPerfMgr = new SystemPerformanceManager();
			this.sysPerfMgr.setDataMessageListener(this);
		}

		// Initialize MQTT Client if enabled
		if (this.enableMqttClient) {
			_Logger.info("MQTT client enabled.");
			// TODO: implement this in Lab Module 7
			// this.mqttClient = new MqttClientConnector();
		}

		// Initialize CoAP Server if enabled
		if (this.enableCoapServer) {
			_Logger.info("CoAP server enabled.");
			// TODO: implement this in Lab Module 8
			// this.coapServer = new CoapServerGateway();
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
		_Logger.log(Level.FINE, "Handling incoming ActuatorData analysis...");

		// TODO: Implement in Part 03 - Connectivity
		// Will eventually publish back to the CDA using either MQTT or CoAP
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
		_Logger.log(Level.FINE, "Handling incoming SystemStateData analysis...");

		// TODO: Implement custom logic for system state handling
		// This is a command the GDA should interpret and handle internally
	}

	/**
	 * Handles upstream transmission of data to cloud services.
	 *
	 * @param resourceName The resource name enum
	 * @param jsonData The JSON data to transmit
	 * @param qos Quality of Service level
	 * @return true if transmission successful, false otherwise
	 */
	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
	{
		_Logger.log(Level.FINE, "Handling upstream transmission for resource: " + resourceName);

		// TODO: Implement in Part 03 - Connectivity
		// Will eventually publish to the cloud service

		return false;
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