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

package programmingtheiot.gda.app;

import org.apache.commons.cli.*;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main GDA application.
 *
 * This class serves as the entry point for the Gateway Device Application (GDA).
 * It initializes and manages the lifecycle of the DeviceDataManager, which in turn
 * manages all connections and system monitoring tasks.
 */
public class GatewayDeviceApp
{
	// static

	private static final Logger _Logger =
			Logger.getLogger(GatewayDeviceApp.class.getName());

	public static final long DEFAULT_TEST_RUNTIME = 600000L; // 10 minutes
	public static final long DEFAULT_SLEEP_INTERVAL = 2000L; // 2 seconds

	// private var's

	private String configFile = ConfigConst.DEFAULT_CONFIG_FILE_NAME;

	private DeviceDataManager dataMgr = null;

	// constructors

	/**
	 * Default constructor.
	 * Initializes the GDA with command line arguments.
	 *
	 * @param args Command line arguments
	 */
	public GatewayDeviceApp(String[] args)
	{
		super();

		_Logger.info("Initializing GDA...");

		parseArgs(args);
	}


	// static

	/**
	 * Main application entry point.
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args)
	{
		GatewayDeviceApp gwApp = new GatewayDeviceApp(args);

		gwApp.startApp();

		// Check if we should run forever or for a limited time
		boolean runForever =
				ConfigUtil.getInstance().getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_RUN_FOREVER_KEY);

		if (runForever) {
			_Logger.info("GDA will run continuously. Press Ctrl+C to stop.");

			try {
				while (true) {
					Thread.sleep(DEFAULT_SLEEP_INTERVAL);
				}
			} catch (InterruptedException e) {
				_Logger.info("GDA interrupted. Shutting down...");
			}

			gwApp.stopApp(0);
		} else {
			try {
				Thread.sleep(DEFAULT_TEST_RUNTIME);
			} catch (InterruptedException e) {
				_Logger.info("GDA interrupted during test run. Shutting down...");
			}

			gwApp.stopApp(0);
		}
	}


	// public methods

	/**
	 * Initializes and starts the application.
	 *
	 */
	public void startApp()
	{
		_Logger.info("Starting GDA...");

		try {
			if (! ConfigUtil.getInstance().getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.TEST_EMPTY_APP_KEY)) {
				this.dataMgr = new DeviceDataManager();
			}

			if (this.dataMgr != null) {
				this.dataMgr.startManager();
			}

			_Logger.info("GDA started successfully.");
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start GDA. Exiting.", e);

			stopApp(-1);
		}
	}

	/**
	 * Stops the application.
	 * Cleanly shuts down the DeviceDataManager before exiting.
	 *
	 * @param code The exit code to pass to {@link System.exit()}
	 */
	public void stopApp(int code)
	{
		_Logger.info("Stopping GDA...");

		try {
			// Stop the DeviceDataManager if it exists
			if (this.dataMgr != null) {
				_Logger.info("Stopping DeviceDataManager...");
				this.dataMgr.stopManager();
				_Logger.info("DeviceDataManager stopped successfully.");
				this.dataMgr = null;
			}

			_Logger.log(Level.INFO, "GDA stopped successfully with exit code {0}.", code);

		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to cleanly stop GDA. Exiting.", e);
		}

		System.exit(code);
	}

	/**
	 * Gets the DeviceDataManager instance.
	 * This method is useful for testing and debugging.
	 *
	 * @return The DeviceDataManager instance, or null if not initialized
	 */
	public DeviceDataManager getDataManager()
	{
		return this.dataMgr;
	}


	// private methods

	/**
	 * Parse any arguments passed in on app startup.
	 * <p>
	 * This method should be written to check if any valid command line args are provided,
	 * including the name of the config file. Once parsed, call {@link #initConfig(String)}
	 * with the name of the config file, or null if the default should be used.
	 * <p>
	 * If any command line args conflict with the config file, the config file
	 * in-memory content should be overridden with the command line argument(s).
	 *
	 * @param args The non-null and non-empty args array.
	 */
	private void parseArgs(String[] args)
	{
		_Logger.info("Parsing command line args...");

		// store command line values in a map
		Map<String, String> argMap = new HashMap<String, String>();

		if (args != null && args.length > 0)  {
			// create the parser and options - only need one for now ("c" for config file)
			CommandLineParser parser = new DefaultParser();
			Options options = new Options();

			options.addOption("c", true, "The relative or absolute path of the config file.");

			try {
				CommandLine cmdLineArgs = parser.parse(options, args);

				if (cmdLineArgs.hasOption("c")) {
					argMap.put(ConfigConst.CONFIG_FILE_KEY, cmdLineArgs.getOptionValue("c"));
					System.setProperty(ConfigConst.CONFIG_FILE_KEY, cmdLineArgs.getOptionValue("c"));
					_Logger.info("Using custom config file: " + cmdLineArgs.getOptionValue("c"));
				} else {
					_Logger.info("No custom config file specified. Using default.");
				}
			} catch (ParseException e) {
				_Logger.warning("Failed to parse command line args. Ignoring - using defaults.");
			}
		}

		// call initConfig with the config file name or null
		String configFileName = argMap.get(ConfigConst.CONFIG_FILE_KEY);
		initConfig(configFileName);
	}

	/**
	 * Initialize the configuration.
	 *
	 * @param fileName The config file name, or null if default should be used.
	 */
	private void initConfig(String fileName)
	{
		_Logger.info("Initializing configuration with file: " + fileName);

		// Configuration initialization is handled by ConfigUtil singleton
		// This method is here for future enhancements if needed

		if (fileName != null && !fileName.isEmpty()) {
			// ConfigUtil will use the file specified via System property
			_Logger.info("Configuration file set via system property: " + fileName);
		} else {
			_Logger.info("Using default configuration file.");
		}
	}

}