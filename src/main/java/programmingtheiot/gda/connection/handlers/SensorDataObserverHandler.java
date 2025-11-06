package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;

/**
 * CoAP Observe handler for SensorData updates.
 */
public class SensorDataObserverHandler implements CoapHandler
{
    // static
    private static final Logger _Logger =
            Logger.getLogger(SensorDataObserverHandler.class.getName());

    // params
    private IDataMessageListener dataMsgListener = null;

    // constructors

    /**
     * Default constructor.
     */
    public SensorDataObserverHandler()
    {
        super();
    }

    // public methods

    /**
     * Sets the data message listener.
     * @param listener The listener instance.
     */
    public void setDataMessageListener(IDataMessageListener listener)
    {
        this.dataMsgListener = listener;
    }

    /**
     * Called when an error occurs.
     */
    @Override
    public void onError()
    {
        _Logger.warning("Handling CoAP error in SensorDataObserverHandler...");
    }

    /**
     * Called when a CoAP response (notification) is received.
     * @param response The CoAP response.
     */
    @Override
    public void onLoad(CoapResponse response)
    {
        _Logger.info("Received CoAP Observe notification for SensorData.");

        if (response != null) {
            String payload = response.getResponseText();
            _Logger.fine("Payload: " + payload);

            if (this.dataMsgListener != null) {
                try {
                    // Deserialize the JSON payload into SensorData
                    SensorData sensorData =
                            DataUtil.getInstance().jsonToSensorData(payload);

                    // Pass the data to the listener
                    this.dataMsgListener.handleSensorMessage(
                            ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData);

                } catch (Exception e) {
                    _Logger.warning(
                            "Failed to parse SensorData from CoAP response: " + e.getMessage());
                }
            } else {
                _Logger.warning("No data message listener set for SensorDataObserverHandler.");
            }
        }
    }
}