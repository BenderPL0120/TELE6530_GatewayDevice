package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;

/**
 * CoAP resource handler for Actuator Command GET requests.
 * This handler supports CoAP Observe pattern for automatic updates.
 */
public class GetActuatorCommandResourceHandler extends CoapResource
        implements IActuatorDataListener
{
    // static

    private static final Logger _Logger =
            Logger.getLogger(GetActuatorCommandResourceHandler.class.getName());

    // params

    private ActuatorData actuatorData = null;

    // constructors

    /**
     * Constructor.
     *
     * @param resourceName The name of the resource.
     */
    public GetActuatorCommandResourceHandler(String resourceName)
    {
        super(resourceName);

        // set the resource to be observable for CoAP Observe pattern
        super.setObservable(true);

        // Initialize with a default ActuatorData instance
        this.actuatorData = new ActuatorData();
        this.actuatorData.setName(resourceName);

        _Logger.info("Created GetActuatorCommandResourceHandler: " + resourceName +
                " (Observable: " + isObservable() + ")");
    }

    // public methods

    /**
     * Handle actuator data updates from DeviceDataManager.
     * This method is called when new actuator commands are available.
     *
     * @param data The updated ActuatorData
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean onActuatorDataUpdate(ActuatorData data)
    {
        if (data != null && this.actuatorData != null) {
            // Update the local actuator data
            this.actuatorData.updateData(data);

            // Notify all observing clients about the change
            super.changed();

            _Logger.fine("Actuator data updated for URI: " + super.getURI() +
                    ": Data value = " + this.actuatorData.getValue());

            return true;
        }

        _Logger.warning("Failed to update actuator data - null data received");
        return false;
    }

    /**
     * Handle GET requests for actuator commands.
     * Returns the current actuator command data as JSON.
     */
    @Override
    public void handleGET(CoapExchange context)
    {
        _Logger.info("GET request received for: " + super.getName());

        // Validate context
        if (context == null) {
            _Logger.warning("Invalid CoAP exchange context");
            return;
        }

        // Accept the request
        context.accept();

        try {
            // Convert the locally stored ActuatorData to JSON
            String jsonData = null;

            if (this.actuatorData != null) {
                jsonData = DataUtil.getInstance().actuatorDataToJson(this.actuatorData);
                _Logger.fine("Returning actuator data: " + jsonData);
            } else {
                // Create empty response if no data available
                ActuatorData emptyData = new ActuatorData();
                emptyData.setName(super.getName());
                jsonData = DataUtil.getInstance().actuatorDataToJson(emptyData);
                _Logger.fine("No actuator data available, returning empty data");
            }

            // Set content type to JSON (optional but recommended)
            // context.respond(ResponseCode.CONTENT, jsonData, MediaTypeRegistry.APPLICATION_JSON);

            // Send response with actuator command data
            context.respond(ResponseCode.CONTENT, jsonData);

            _Logger.info("Successfully sent actuator command data for: " + super.getName());

        } catch (Exception e) {
            _Logger.warning("Error handling GET request: " + e.getMessage());
            context.respond(ResponseCode.INTERNAL_SERVER_ERROR,
                    "Error retrieving actuator data");
        }
    }

    /**
     * Handle DELETE requests.
     * For this resource, DELETE clears the actuator command.
     */
    @Override
    public void handleDELETE(CoapExchange context)
    {
        _Logger.info("DELETE request received for: " + super.getName());

        context.accept();

        // Clear the actuator data by resetting to default values
        if (this.actuatorData != null) {
            this.actuatorData.setCommand(ConfigConst.DEFAULT_COMMAND);
            this.actuatorData.setValue(ConfigConst.DEFAULT_VAL);
            this.actuatorData.setStateData("");

            // Notify observers of the change
            super.changed();

            _Logger.fine("Actuator data cleared for: " + super.getName());
        }

        context.respond(ResponseCode.DELETED,
                "Actuator command cleared for: " + super.getName());
    }

    /**
     * Handle POST requests.
     * Could be used to create new actuator commands.
     */
    @Override
    public void handlePOST(CoapExchange context)
    {
        _Logger.info("POST request received for: " + super.getName());

        context.accept();

        // For actuator commands, POST could be handled similarly to PUT
        handlePUT(context);
    }

    /**
     * Handle PUT requests.
     * Updates the actuator command data.
     */
    @Override
    public void handlePUT(CoapExchange context)
    {
        _Logger.info("PUT request received for: " + super.getName());

        context.accept();

        try {
            // Get the payload
            String jsonData = new String(context.getRequestPayload());
            _Logger.fine("Received actuator command update: " + jsonData);

            // Convert JSON to ActuatorData
            ActuatorData newData =
                    DataUtil.getInstance().jsonToActuatorData(jsonData);

            // Update the local data
            if (onActuatorDataUpdate(newData)) {
                context.respond(ResponseCode.CHANGED,
                        "Actuator command updated: " + super.getName());
            } else {
                context.respond(ResponseCode.INTERNAL_SERVER_ERROR,
                        "Failed to update actuator command");
            }

        } catch (Exception e) {
            _Logger.warning("Error handling PUT request: " + e.getMessage());
            context.respond(ResponseCode.BAD_REQUEST,
                    "Invalid actuator data format");
        }
    }

    /**
     * Set initial actuator data if needed.
     * This can be called to provide initial data before any updates.
     *
     * @param data Initial ActuatorData
     */
    public void setActuatorData(ActuatorData data)
    {
        if (data != null) {
            this.actuatorData = data;
            _Logger.fine("Initial actuator data set for: " + super.getName());
        }
    }
}