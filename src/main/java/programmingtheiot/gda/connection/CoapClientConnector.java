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

package programmingtheiot.gda.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import programmingtheiot.gda.connection.handlers.GenericCoapResponseHandler;
import programmingtheiot.gda.connection.handlers.SensorDataObserverHandler;
import programmingtheiot.gda.connection.handlers.SystemPerformanceDataObserverHandler;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.UdpConfig;

import programmingtheiot.data.DataUtil;

/**
 * Shell representation of class for student implementation.
 *
 */
public class CoapClientConnector implements IRequestResponseClient
{
	// static

	private static final Logger _Logger =
			Logger.getLogger(CoapClientConnector.class.getName());

	// params
	private String     protocol;
	private String     host;
	private int        port;
	private String     serverAddr;
	private CoapClient clientConn;
	private IDataMessageListener dataMsgListener;
	private CoapHandler generalResponseHandler;

	// track observe
	private Map<String, CoapObserveRelation> observeRelations;

	// constructors
	
	/**
	 * Default.
	 * 
	 * All config data will be loaded from the config file.
	 */
	public CoapClientConnector()
	{
		super();

		ConfigUtil config = ConfigUtil.getInstance();
		this.host = config.getProperty(ConfigConst.COAP_GATEWAY_SERVICE, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);

		if (config.getBoolean(ConfigConst.COAP_GATEWAY_SERVICE, ConfigConst.ENABLE_CRYPT_KEY)) {
			this.protocol = ConfigConst.DEFAULT_COAP_SECURE_PROTOCOL;
			this.port     = config.getInteger(ConfigConst.COAP_GATEWAY_SERVICE, ConfigConst.SECURE_PORT_KEY, ConfigConst.DEFAULT_COAP_SECURE_PORT);
		} else {
			this.protocol = ConfigConst.DEFAULT_COAP_PROTOCOL;
			this.port     = config.getInteger(ConfigConst.COAP_GATEWAY_SERVICE, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_COAP_PORT);
		}

		// construct the URL manually
		this.serverAddr = this.protocol + "://" + this.host + ":" + this.port;

		// initialize map
		this.observeRelations = new HashMap<>();

		initClient();
		_Logger.info("Using URL for server conn: " + this.serverAddr);
	}
		
	/**
	 * Constructor.
	 * 
	 * @param host
	 * @param isSecure
	 * @param enableConfirmedMsgs
	 */
	public CoapClientConnector(String host, boolean isSecure, boolean enableConfirmedMsgs)
	{
		// TODO: Implement this constructor if needed
		// Be sure to initialize observeRelations = new HashMap<>();
	}


	// public methods
	@Override
	public boolean sendDiscoveryRequest(int timeout)
	{
		_Logger.info("Issuing discovery request to server (timeout: " + timeout + "s)...");

		// Use the base server address for discovery
		this.clientConn.setURI(this.serverAddr);

		try {
			if (timeout > 0) {
				this.clientConn.setTimeout(timeout * 1000L); // Convert seconds to milliseconds
			}

			// --- Synchronous network call ---
			java.util.Set<org.eclipse.californium.core.WebLink> wlSet = this.clientConn.discover();

			if (timeout > 0) {
				this.clientConn.setTimeout(0L); // Reset to default
			}

			if (wlSet != null && !wlSet.isEmpty()) {
				_Logger.info("Discovery complete. Found " + wlSet.size() + " resources:");

				for (org.eclipse.californium.core.WebLink wl : wlSet) {
					_Logger.info(" --> URI: " + wl.getURI() + ". Attributes: " + wl.getAttributes());
				}
				return true;
			} else {
				_Logger.warning("Discovery request failed or no resources found (wlSet is null or empty).");
				return false;
			}

		} catch (org.eclipse.californium.elements.exception.ConnectorException | java.io.IOException e) {
			_Logger.log(Level.SEVERE, "Failed to send CoAP discovery request due to network error.", e);
			return false;
		}
	}

	@Override
	public boolean sendDeleteRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout)
	{
		String resourceUri = createUriPath(resource, name);

		_Logger.info("Issuing ASYNC DELETE to: " + resourceUri);
		this.clientConn.setURI(resourceUri);

		if (enableCON) {
			this.clientConn.useCONs();
		} else {
			this.clientConn.useNONs();
		}

		if (this.generalResponseHandler == null) {
			_Logger.warning("No response handler set (dataMsgListener not set?). Cannot process async DELETE.");
			return false;
		}

		this.clientConn.delete(this.generalResponseHandler);
		return true;
	}

	@Override
	public boolean sendGetRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout)
	{
		String resourceUri = createUriPath(resource, name);

		_Logger.info("Issuing ASYNC GET to: " + resourceUri);
		this.clientConn.setURI(resourceUri);

		if (enableCON) {
			this.clientConn.useCONs();
		} else {
			this.clientConn.useNONs();
		}

		if (this.generalResponseHandler == null) {
			_Logger.warning("No response handler set (dataMsgListener not set?). Cannot process async GET.");
			return false;
		}

		this.clientConn.get(this.generalResponseHandler);
		return true;
	}

	@Override
	public boolean sendPostRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload, int timeout)
	{
		String resourceUri = createUriPath(resource, name);

		_Logger.info("Issuing ASYNC POST to: " + resourceUri);
		this.clientConn.setURI(resourceUri);

		if (enableCON) {
			this.clientConn.useCONs();
		} else {
			this.clientConn.useNONs();
		}

		if (this.generalResponseHandler == null) {
			_Logger.warning("No response handler set (dataMsgListener not set?). Cannot process async POST.");
			return false;
		}

		this.clientConn.post(
				this.generalResponseHandler,
				payload,
				MediaTypeRegistry.TEXT_PLAIN); // Or APPLICATION_JSON if appropriate

		return true;
	}

	@Override
	public boolean sendPutRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload, int timeout)
	{
		String resourceUri = createUriPath(resource, name);

		_Logger.info("Issuing ASYNC PUT to: " + resourceUri);
		this.clientConn.setURI(resourceUri);

		if (enableCON) {
			this.clientConn.useCONs();
		} else {
			this.clientConn.useNONs();
		}

		if (this.generalResponseHandler == null) {
			_Logger.warning("No response handler set (dataMsgListener not set?). Cannot process async PUT.");
			return false;
		}

		this.clientConn.put(
				this.generalResponseHandler,
				payload,
				MediaTypeRegistry.TEXT_PLAIN); // Or APPLICATION_JSON if appropriate

		return true;
	}

	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;

			// Initialize or update the reusable handler with the listener
			this.generalResponseHandler = new GenericCoapResponseHandler(this.dataMsgListener);

			return true;
		}
		return false;
	}

	public void clearEndpointPath()
	{
		// Not typically used with this client model
	}

	public void setEndpointPath(ResourceNameEnum resource)
	{
		// Not typically used with this client model
	}

	@Override
	public boolean startObserver(ResourceNameEnum resource, String name, int ttl)
	{
		String uriPath = createUriPath(resource, name);

		if (this.observeRelations.containsKey(uriPath)) {
			_Logger.warning("Already observing resource: " + uriPath + ". Ignoring request.");
			return false;
		}

		_Logger.info("Observing resource [START]: " + uriPath);
		this.clientConn.setURI(uriPath);

		CoapHandler handler;

		// Check the resource type and create the appropriate handler
		switch (resource) {
			case CDA_SENSOR_MSG_RESOURCE:
				SensorDataObserverHandler sensorHandler = new SensorDataObserverHandler();
				sensorHandler.setDataMessageListener(this.dataMsgListener);
				handler = sensorHandler;
				break;

			case CDA_SYSTEM_PERF_MSG_RESOURCE:
				SystemPerformanceDataObserverHandler sysPerfHandler = new SystemPerformanceDataObserverHandler();
				sysPerfHandler.setDataMessageListener(this.dataMsgListener);
				handler = sysPerfHandler;
				break;

			default:
				_Logger.warning("No specific observer handler for resource: " + resource.getResourceName() + ". Using generic handler.");
				GenericCoapResponseHandler genericHandler = new GenericCoapResponseHandler(this.dataMsgListener, resource);
				handler = genericHandler;
		}

		// Start the observation
		CoapObserveRelation cor = this.clientConn.observe(handler);

		// Store a reference to the relation for cancellation later
		this.observeRelations.put(uriPath, cor);

		boolean success = !cor.isCanceled();
		_Logger.info("Observe request sent. Success: " + success);

		return success;
	}

	@Override
	public boolean stopObserver(ResourceNameEnum resource, String name, int timeout)
	{
		String uriPath = createUriPath(resource, name);

		_Logger.info("Observing resource [STOP]: " + uriPath);

		// Look up the stored relation
		CoapObserveRelation relation = this.observeRelations.get(uriPath);

		if (relation != null) {
			_Logger.fine("Found active observe relation. Canceling...");

			// Cancel the observation
			relation.proactiveCancel();

			// Remove it from the map
			this.observeRelations.remove(uriPath);
			return true;
		}

		_Logger.warning("No active observe relation found for: " + uriPath + ". Cannot stop.");
		return false;
	}


	// private methods

	private void initClient()
	{
		try {
			// Initialize Californium configuration with required definitions
			Configuration.setStandard(new Configuration(CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS));

			// Now create the CoAP client
			this.clientConn = new CoapClient(this.serverAddr);

			_Logger.info("Created client connection to server / resource: " + this.serverAddr);
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to connect to broker: " +
					(this.clientConn != null ? this.clientConn.getURI() : this.serverAddr), e);
		}
	}

	/**
	 * Helper method to create the full URI path for a resource.
	 * * @param resource The resource enumeration.
	 * @param name An optional name suffix (e.g., actuator name).
	 * @return The full URI string (e.g., "coap://localhost:5683/CdaSensorMsgResource/TempSensor").
	 */
	private String createUriPath(ResourceNameEnum resource, String name)
	{
		String uriPath = this.serverAddr + "/" + resource.getResourceName();
		if (name != null && !name.trim().isEmpty()) {
			uriPath += "/" + name;
		}
		return uriPath;
	}
}