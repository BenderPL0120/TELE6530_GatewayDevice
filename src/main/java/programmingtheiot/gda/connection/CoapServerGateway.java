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

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.UdpConfig;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.gda.connection.handlers.GetActuatorCommandResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateSystemPerformanceResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateTelemetryResourceHandler;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class CoapServerGateway
{
	// static
	static {
		CoapConfig.register();
		UdpConfig.register();
	}
	
	private static final Logger _Logger =
		Logger.getLogger(CoapServerGateway.class.getName());
	
	// params
	
	private CoapServer coapServer = null;
	
	private IDataMessageListener dataMsgListener = null;
	
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param dataMsgListener
	 */
	public CoapServerGateway(IDataMessageListener dataMsgListener)
	{
		super();
		
		/*
		 * Basic constructor implementation provided. Change as needed.
		 */
		
		this.dataMsgListener = dataMsgListener;
		
		initServer();
	}

		
	// public methods

	public void addResource(ResourceNameEnum resourceType, String endName, Resource resource)
	{
		// endName parameter is optional for this implementation

		if (resourceType != null && resource != null) {
			// Break out the hierarchy of names and build the resource
			// handler chain as needed
			createAndAddResourceChain(resourceType, resource);
		} else {
			_Logger.warning("Invalid resource parameters - cannot add resource");
		}
	}

	public boolean hasResource(String name)
	{
		if (this.coapServer != null && name != null) {
			Resource resource = this.coapServer.getRoot();

			if (resource != null) {
				// Split the name by '/' and traverse the tree
				String[] parts = name.split("/");

				for (String part : parts) {
					resource = resource.getChild(part);

					if (resource == null) {
						return false;
					}
				}

				return true;
			}
		}

		return false;
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			_Logger.info("Data message listener set.");
		}
	}
	
	public boolean startServer()
	{
		try {
			if (this.coapServer != null) {
				this.coapServer.start();

				// for message logging
				for (Endpoint ep : this.coapServer.getEndpoints()) {
					ep.addInterceptor(new MessageTracer());
				}

				_Logger.info("CoAP server started successfully.");
				return true;
			} else {
				_Logger.warning("CoAP server START failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start CoAP server.", e);
		}

		return false;
	}
	
	public boolean stopServer()
	{
		try {
			if (this.coapServer != null) {
				this.coapServer.stop();

				_Logger.info("CoAP server stopped successfully.");
				return true;
			} else {
				_Logger.warning("CoAP server STOP failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to stop CoAP server.", e);
		}

		return false;
	}
	
	
	// private methods
	private void createAndAddResourceChain(ResourceNameEnum resourceType, Resource resource)
	{
		_Logger.info("Adding server resource handler chain: " + resourceType.getResourceName());

		List<String> resourceNames = resourceType.getResourceNameChain();
		Queue<String> queue = new ArrayBlockingQueue<>(resourceNames.size());

		queue.addAll(resourceNames);

		// Start from the server's root
		Resource parentResource = this.coapServer.getRoot();

		// Process each level of the resource path
		while (!queue.isEmpty()) {
			String resourceName = queue.poll();
			Resource nextResource = parentResource.getChild(resourceName);

			if (nextResource == null) {
				if (queue.isEmpty()) {
					// This is the leaf node - use the provided resource
					nextResource = resource;
					nextResource.setName(resourceName);
				} else {
					// This is an intermediate node - create a CoapResource
					nextResource = new CoapResource(resourceName);
				}

				parentResource.add(nextResource);
				_Logger.fine("Added resource: " + resourceName + " to parent: " +
						(parentResource.getName() != null ? parentResource.getName() : "ROOT"));
			}

			parentResource = nextResource;
		}

		_Logger.info("Resource chain added successfully for: " + resourceType.getResourceName());
	}

	private void initDefaultResources()
	{
		_Logger.info("Initializing default CoAP resources...");

		// Initialize GetActuatorCommand resource for CoAP Observe
		GetActuatorCommandResourceHandler getActuatorCmdResourceHandler =
				new GetActuatorCommandResourceHandler(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE.getResourceType());

		// Register as actuator listener if we have a data message listener
		if (this.dataMsgListener != null) {
			this.dataMsgListener.setActuatorDataListener(
					"CoapActuatorHandler", getActuatorCmdResourceHandler);
			_Logger.info("Registered actuator command handler as listener");
		}

		// Add the resource to the server
		addResource(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, null, getActuatorCmdResourceHandler);

		// Initialize UpdateTelemetry resource for sensor data
		UpdateTelemetryResourceHandler updateTelemetryResourceHandler =
				new UpdateTelemetryResourceHandler(
						ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceType());

		updateTelemetryResourceHandler.setDataMessageListener(this.dataMsgListener);

		addResource(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, null, updateTelemetryResourceHandler);

		// Initialize UpdateSystemPerformance resource
		UpdateSystemPerformanceResourceHandler updateSystemPerformanceResourceHandler =
				new UpdateSystemPerformanceResourceHandler(
						ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceType());

		updateSystemPerformanceResourceHandler.setDataMessageListener(this.dataMsgListener);

		addResource(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, null, updateSystemPerformanceResourceHandler);

		_Logger.info("Default resources initialized successfully");
	}

	private void initServer(ResourceNameEnum ...resources)
	{
		// Create CoAP server instance
		this.coapServer = new CoapServer();

		// Initialize default resources
		initDefaultResources();

		_Logger.info("CoAP server initialized with default resources");
	}
}
