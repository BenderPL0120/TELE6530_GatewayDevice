package programmingtheiot.gda.system;

import java.io.File;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;

/**
 * System disk utilization monitoring task.
 */
public class SystemDiskUtilTask extends BaseSystemUtilTask
{
    // static

    private static final Logger _Logger = Logger.getLogger(SystemDiskUtilTask.class.getName());

    // private var's

    private File diskFile = new File("/");  // 默认监控根目录


    // constructors

    /**
     * Default constructor - monitors root path.
     */
    public SystemDiskUtilTask()
    {
        super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
    }

    /**
     * Constructor with custom path.
     *
     * @param path The filesystem path to monitor
     */
    public SystemDiskUtilTask(String path)
    {
        super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);

        if (path != null && !path.isEmpty()) {
            File testFile = new File(path);
            if (testFile.exists()) {
                this.diskFile = testFile;
            } else {
                _Logger.warning("Path does not exist: " + path + ", using root path");
            }
        }
    }


    // public methods

    @Override
    public float getTelemetryValue()
    {
        long totalSpace = this.diskFile.getTotalSpace();
        long freeSpace = this.diskFile.getFreeSpace();

        if (totalSpace > 0) {
            long usedSpace = totalSpace - freeSpace;
            float diskUtil = ((float) usedSpace / totalSpace) * 100.0f;
            return diskUtil;
        }

        return 0.0f;
    }

}