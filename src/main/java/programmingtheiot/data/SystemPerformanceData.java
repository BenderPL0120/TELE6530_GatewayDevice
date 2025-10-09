/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */

package programmingtheiot.data;

import java.io.Serializable;

import programmingtheiot.common.ConfigConst;

/**
 * SystemPerformanceData implementation for system performance metrics.
 */
public class SystemPerformanceData extends BaseIotData implements Serializable
{
	// static

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 5555666677778888999L;

	// private var's

	private float cpuUtil  = ConfigConst.DEFAULT_VAL;
	private float diskUtil = ConfigConst.DEFAULT_VAL;
	private float memUtil  = ConfigConst.DEFAULT_VAL;

	// constructors

	/**
	 * Default constructor.
	 */
	public SystemPerformanceData()
	{
		super();

		// Set the name to system performance data identifier
		super.setName(ConfigConst.SYS_PERF_DATA);
	}


	// public methods

	public float getCpuUtilization()
	{
		return this.cpuUtil;
	}

	public float getDiskUtilization()
	{
		return this.diskUtil;
	}

	public float getMemoryUtilization()
	{
		return this.memUtil;
	}

	public void setCpuUtilization(float val)
	{
		super.updateTimeStamp();
		this.cpuUtil = val;
	}

	public void setDiskUtilization(float val)
	{
		super.updateTimeStamp();
		this.diskUtil = val;
	}

	public void setMemoryUtilization(float val)
	{
		super.updateTimeStamp();
		this.memUtil = val;
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
		sb.append(ConfigConst.CPU_UTIL_PROP).append('=').append(this.getCpuUtilization()).append(',');
		sb.append(ConfigConst.DISK_UTIL_PROP).append('=').append(this.getDiskUtilization()).append(',');
		sb.append(ConfigConst.MEM_UTIL_PROP).append('=').append(this.getMemoryUtilization());

		return sb.toString();
	}


	// protected methods

	/* (non-Javadoc)
	 * @see programmingtheiot.data.BaseIotData#handleUpdateData(programmingtheiot.data.BaseIotData)
	 */
	protected void handleUpdateData(BaseIotData data)
	{
		if (data instanceof SystemPerformanceData) {
			SystemPerformanceData spData = (SystemPerformanceData) data;
			this.setCpuUtilization(spData.getCpuUtilization());
			this.setDiskUtilization(spData.getDiskUtilization());
			this.setMemoryUtilization(spData.getMemoryUtilization());
		}
	}

}