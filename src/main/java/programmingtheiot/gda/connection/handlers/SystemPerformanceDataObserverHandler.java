package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SystemPerformanceData;

/**
 * CoAP Observe handler for SystemPerformanceData updates.
 */
public class SystemPerformanceDataObserverHandler implements CoapHandler
{
    // static
    private static final Logger _Logger =
            Logger.getLogger(SystemPerformanceDataObserverHandler.class.getName());

    // params
    private IDataMessageListener dataMsgListener = null;

    // constructors

    /**
     * Default constructor.
     */
    public SystemPerformanceDataObserverHandler()
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
        _Logger.warning("Handling CoAP error in SystemPerformanceDataObserverHandler...");
    }

    /**
     * Called when a CoAP response (notification) is received.
     * @param response The CoAP response.
     */
    @Override
    public void onLoad(CoapResponse response)
    {
        _Logger.info("Received CoAP Observe notification for SystemPerformanceData.");

        if (response != null) {
            String payload = response.getResponseText();
            _Logger.fine("Payload: " + payload);

            if (this.dataMsgListener != null) {
                try {
                    // Deserialize the JSON payload into SystemPerformanceData
                    SystemPerformanceData sysPerfData =
                            DataUtil.getInstance().jsonToSystemPerformanceData(payload);

                    // Pass the data to the listener
                    this.dataMsgListener.handleSystemPerformanceMessage(
                            ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData);

                } catch (Exception e) {
                    _Logger.warning(
                            "Failed to parse SystemPerformanceData from CoAP response: " + e.getMessage());
                }
            } else {
                _Logger.warning("No data message listener set for SystemPerformanceDataObserverHandler.");
            }
        }
    }
}