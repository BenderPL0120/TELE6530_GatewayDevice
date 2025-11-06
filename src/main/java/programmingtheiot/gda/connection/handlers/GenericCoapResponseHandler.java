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

package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.OptionSet;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;

/**
 * Generic CoAP resource handler implementation.
 */
public class GenericCoapResponseHandler implements CoapHandler
{
	// static

	private static final Logger _Logger =
			Logger.getLogger(GenericCoapResponseHandler.class.getName());

	// params

	private IDataMessageListener dataMsgListener = null;
	private ResourceNameEnum resource = null;
	private DataUtil dataUtil = null;

	// constructors

	/**
	 * Default constructor.
	 */
	public GenericCoapResponseHandler()
	{
		this((IDataMessageListener) null);
	}

	/**
	 * Constructor with listener.
	 * @param listener The data message listener
	 */
	public GenericCoapResponseHandler(IDataMessageListener listener)
	{
		this(listener, null);
	}

	/**
	 * Constructor with listener and resource.
	 * @param listener The data message listener
	 * @param resource The resource name enum
	 */
	public GenericCoapResponseHandler(IDataMessageListener listener, ResourceNameEnum resource)
	{
		super();

		this.dataMsgListener = listener;
		this.resource = resource;
		this.dataUtil = DataUtil.getInstance();

		_Logger.fine("Response handler created. IDataMessageListener is " +
				(listener != null ? "set" : "not set") +
				", Resource: " + (resource != null ? resource.getResourceName() : "not set"));
	}

	// public methods

	/**
	 * Handle successful CoAP response
	 */
	@Override
	public void onLoad(CoapResponse response)
	{
		if (response != null) {
			OptionSet options = response.getOptions();

			// Log response details for debugging
			_Logger.fine("Processing CoAP response. Code: " + response.getCode());
			_Logger.fine("Processing CoAP response. Options: " + options);

			// Get the payload
			String payload = response.getResponseText();

			if (payload != null && !payload.isEmpty()) {
				_Logger.info("CoAP response received. Payload: " + payload);

				// Notify the listener if available
				if (this.dataMsgListener != null) {
					// If we know the resource type, use it; otherwise use a generic resource
					ResourceNameEnum resourceToUse = (this.resource != null) ?
							this.resource : ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE;

					// Send the payload to the listener
					boolean handled = this.dataMsgListener.handleIncomingMessage(
							resourceToUse, payload);

					if (handled) {
						_Logger.fine("Response payload successfully processed by listener.");
					} else {
						_Logger.warning("Listener failed to process response payload.");
					}
				} else {
					_Logger.warning("No data message listener available to handle response.");
				}
			} else {
				_Logger.info("CoAP response received with empty payload. Code: " + response.getCode());
			}

		} else {
			_Logger.warning("No CoAP response to process. Response is null.");
		}
	}

	/**
	 * Handle CoAP error
	 */
	@Override
	public void onError()
	{
		_Logger.warning("Error processing CoAP response.");

		// Notify listener of error if available
		if (this.dataMsgListener != null) {
			// Could send an error message to the listener
			// For now, just log the error
			_Logger.warning("CoAP error occurred. Listener notified: No");
		}
	}

	// Additional helper methods

	/**
	 * Set the resource type for this handler
	 * @param resource The resource name enum
	 */
	public void setResource(ResourceNameEnum resource)
	{
		this.resource = resource;
	}

	/**
	 * Get the resource type for this handler
	 * @return The resource name enum
	 */
	public ResourceNameEnum getResource()
	{
		return this.resource;
	}
}