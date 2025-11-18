package programmingtheiot.integration.app;

import static org.junit.Assert.*;

import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.app.DeviceDataManager;

/**
 * Integration test for DeviceDataManager's internal analysis logic.
 *
 */
public class DeviceDataManagerSimpleCdaActuationTest
{
    // static
    private static final Logger _Logger =
            Logger.getLogger(DeviceDataManagerSimpleCdaActuationTest.class.getName());

    // local var's
    private DeviceDataManager devDataMgr = null;
    private float nominalVal = 40.0f;
    private float lowVal = 30.0f;
    private float highVal = 50.0f;
    private int delay = 10; // Should match PiotConfig.props 'humidityMaxTimePastThreshold' (应与PiotConfig.props中的'humidityMaxTimePastThreshold'匹配)

    // test setup methods

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.devDataMgr = new DeviceDataManager();

        ConfigUtil cfgUtil = ConfigUtil.getInstance();

        // Load values from config to ensure test aligns with config
        this.nominalVal = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, ConfigConst.NOMINAL_HUMIDITY_SETTING_KEY);
        this.lowVal     = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_HUMIDIFIER_FLOOR_KEY);
        this.highVal    = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_HUMIDIFIER_CEILING_KEY);
        this.delay      = cfgUtil.getInteger(ConfigConst.GATEWAY_DEVICE, ConfigConst.HUMIDITY_MAX_TIME_PAST_THRESHOLD_KEY);

        _Logger.info("Testing humidity thresholds [Nominal=" + this.nominalVal + ", Low=" + this.lowVal + ", Delay=" + this.delay + "s]");

        // Start the manager - this will load the config
        this.devDataMgr.startManager();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        // Stop the manager
        this.devDataMgr.stopManager();
    }

    // test methods

    /**
     * Test method for running the DeviceDataManager.
     *
     */
    @Test
    public void testSendActuationEventsToCda()
    {
        _Logger.info("Beginning test sequence...");

        SensorData sd = new SensorData();
        sd.setName("My Test Humidity Sensor");
        sd.setLocationID("constraineddevice001");
        sd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);

        // --- 1. Send nominal values (should NOT trigger) ---
        _Logger.info("Sending nominal value (1)...");
        sd.setValue(this.nominalVal);
        this.devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(2); // Wait 2s

        _Logger.info("Sending nominal value (2)...");
        sd.setValue(this.nominalVal);
        this.devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(2); // Wait 2s

        // --- 2. Send low value (starts timer) ---
        _Logger.info("Sending LOW value (1)... This should start the timer.");
        sd.setValue(this.lowVal - 2); // e.g., 28.0
        this.devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);

        // Wait for timer to expire (e.g., 10s + 1s buffer)
        _Logger.info("Waiting for delay + 1 seconds...");
        waitForSeconds(this.delay + 1);

        // --- 3. Send low value again (should trigger ON) ---
        _Logger.info("Sending LOW value (2)... This should trigger ACTUATOR ON.");
        sd.setValue(this.lowVal - 1); // e.g., 29.0
        this.devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);

        // Wait for timer to expire again
        _Logger.info("Waiting for delay + 1 seconds...");
        waitForSeconds(this.delay + 1);

        // --- 4. Send low value again (should NOT trigger new command) ---
        _Logger.info("Sending LOW value (3)... This should NOT trigger a new event (already ON).");
        sd.setValue(this.lowVal - 1); // e.g., 29.0
        this.devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);

        // Wait for timer to expire again
        _Logger.info("Waiting for delay + 1 seconds...");
        waitForSeconds(this.delay + 1);

        // --- 5. Send nominal value (should trigger OFF) ---
        _Logger.info("Sending NOMINAL value (3)... This should trigger ACTUATOR OFF.");
        sd.setValue(this.nominalVal);
        this.devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);

        _Logger.info("Waiting for 2 seconds...");
        waitForSeconds(2);

        _Logger.info("Test sequence complete.");
    }

    /**
     * Helper method to pause execution for a given number of seconds.
     * @param seconds
     */
    private void waitForSeconds(int seconds)
    {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}