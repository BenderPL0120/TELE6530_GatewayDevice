/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */

package programmingtheiot.data;

import java.io.Serializable;

import programmingtheiot.common.ConfigConst;

/**
 * SensorData implementation for IoT sensor devices.
 */
public class SensorData extends BaseIotData implements Serializable
{
	// static

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 1234567890123456789L;

	// private var's

	private float value = ConfigConst.DEFAULT_VAL;

	// constructors

	/**
	 * Default constructor.
	 */
	public SensorData()
	{
		super();
	}

	/**
	 * Constructor with sensor type.
	 *
	 * @param sensorType The type ID for this sensor
	 */
	public SensorData(int sensorType)
	{
		super();
		super.setTypeID(sensorType);
	}


	// public methods

	public float getValue()
	{
		return this.value;
	}

	public void setValue(float val)
	{
		super.updateTimeStamp();
		this.value = val;
	}

	/**
	 * Returns a string representation of this instance. This will invoke the base class
	 * {@link #toString()} method, then append the output from this call.
	 *
	 * @return String The string representing this instance, returned in CSV 'key=value' format.
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder(super.toString());

		sb.append(',');
		sb.append(ConfigConst.VALUE_PROP).append('=').append(this.getValue());

		return sb.toString();
	}


	// protected methods

	/* (non-Javadoc)
	 * @see programmingtheiot.data.BaseIotData#handleUpdateData(programmingtheiot.data.BaseIotData)
	 */
	protected void handleUpdateData(BaseIotData data)
	{
		if (data instanceof SensorData) {
			SensorData sData = (SensorData) data;
			this.setValue(sData.getValue());
		}
	}

}