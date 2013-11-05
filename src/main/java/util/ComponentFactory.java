package util;

import cli.Shell;
import client.ClientCli;
import client.IClientCli;
import proxy.IProxyCli;
import proxy.ProxyCli;
import server.FileServerCli;
import server.IFileServerCli;

/**
 * Provides methods for starting an arbitrary amount of various components.
 */
public class ComponentFactory {

	private ProxyCli proxyCli;
	private FileServerCli fsCli;
	private IClientCli clientCli;
	/**
	 * Creates and starts a new client instance using the provided {@link Config} and {@link Shell}.
	 *
	 * @param config the configuration containing parameters such as connection info
	 * @param shell  the {@code Shell} used for processing commands
	 * @return the created component after starting it successfully
	 * @throws Exception if an exception occurs
	 */
	public IClientCli startClient(Config config, Shell shell) throws Exception {
		if (clientCli == null) {
			clientCli = new ClientCli(config, shell);
		}
		return clientCli;
	}

	/**
	 * Creates and starts a new proxy instance using the provided {@link Config} and {@link Shell}.
	 *
	 * @param config the configuration containing parameters such as connection info
	 * @param shell  the {@code Shell} used for processing commands
	 * @return the created component after starting it successfully
	 * @throws Exception if an exception occurs
	 */
	public IProxyCli startProxy(Config config, Shell shell) throws Exception {
		if (proxyCli == null) {
			proxyCli = new ProxyCli(config, shell);
		}
		return proxyCli;
	}

	/**
	 * Creates and starts a new file server instance using the provided {@link Config} and {@link Shell}.
	 *
	 * @param config the configuration containing parameters such as connection info
	 * @param shell  the {@code Shell} used for processing commands
	 * @return the created component after starting it successfully
	 * @throws Exception if an exception occurs
	 */
	public IFileServerCli startFileServer(Config config, Shell shell) throws Exception {
		if (fsCli == null) {
			fsCli = new FileServerCli(config, shell);
		}
		return fsCli;
	}
}
