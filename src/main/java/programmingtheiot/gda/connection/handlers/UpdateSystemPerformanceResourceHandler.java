package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SystemPerformanceData;

/**
 * CoAP resource handler for System Performance Data updates.
 */
public class UpdateSystemPerformanceResourceHandler extends CoapResource
{
    // static

    private static final Logger _Logger =
            Logger.getLogger(UpdateSystemPerformanceResourceHandler.class.getName());

    // params

    private IDataMessageListener dataMsgListener = null;

    // constructors

    /**
     * Constructor.
     *
     * @param resourceName The name of the resource.
     */
    public UpdateSystemPerformanceResourceHandler(String resourceName)
    {
        super(resourceName);

        // Make this resource observable for CoAP Observe
        setObservable(true);

        _Logger.info("Created UpdateSystemPerformanceResourceHandler: " + resourceName);
    }

    // public methods

    /**
     * Handle DELETE request
     */
    @Override
    public void handleDELETE(CoapExchange context)
    {
        _Logger.info("DELETE request received for: " + super.getName());

        context.accept();

        // For now, just log and respond with success
        context.respond(ResponseCode.DELETED, "Resource deleted: " + super.getName());
    }

    /**
     * Handle GET request
     */
    @Override
    public void handleGET(CoapExchange context)
    {
        _Logger.info("GET request received for: " + super.getName());

        context.accept();

        // For now, return a simple status message
        // In future, could return the latest cached SystemPerformanceData
        String responseMsg = "System Performance Resource: " + super.getName() + " - Status: OK";
        context.respond(ResponseCode.CONTENT, responseMsg);
    }

    /**
     * Handle POST request
     */
    @Override
    public void handlePOST(CoapExchange context)
    {
        _Logger.info("POST request received for: " + super.getName());

        context.accept();

        // POST can be handled similarly to PUT for this use case
        handlePUT(context);
    }

    /**
     * Handle PUT request - Main handler for System Performance updates
     */
    @Override
    public void handlePUT(CoapExchange context)
    {
        ResponseCode code = ResponseCode.NOT_ACCEPTABLE;

        context.accept();

        if (this.dataMsgListener != null) {
            try {
                String jsonData = new String(context.getRequestPayload());

                _Logger.fine("Received system performance data: " + jsonData);

                SystemPerformanceData sysPerfData =
                        DataUtil.getInstance().jsonToSystemPerformanceData(jsonData);

                // Pass the data to the listener (DeviceDataManager)
                this.dataMsgListener.handleSystemPerformanceMessage(
                        ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData);

                code = ResponseCode.CHANGED;

                // Notify observers if this is an observable resource
                changed();

            } catch (Exception e) {
                _Logger.warning(
                        "Failed to handle PUT request. Message: " + e.getMessage());

                code = ResponseCode.BAD_REQUEST;
            }
        } else {
            _Logger.info(
                    "No callback listener for request. Ignoring PUT.");

            code = ResponseCode.CONTINUE;
        }

        String msg = "Update system perf data request handled: " + super.getName();

        context.respond(code, msg);
    }

    /**
     * Set the data message listener for callbacks to DeviceDataManager
     */
    public void setDataMessageListener(IDataMessageListener listener)
    {
        if (listener != null) {
            this.dataMsgListener = listener;
            _Logger.info("Data message listener set for: " + super.getName());
        }
    }
}