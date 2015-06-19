package de.htw.ds.shop;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.xml.ws.Service;
import de.sb.java.TypeMetadata;
import de.sb.java.xml.Namespaces;


/**
 * Swing based GUI-client utilizing the JAX-WS shop service. Note that this class is declared final
 * because it provides an application entry point, and therefore not supposed to be extended.
 * @see <a
 *      href="http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Freference%2Fref-icons.htm">The
 *      original source of the Eclipse icons, available under EPL license</a>
 */
@TypeMetadata(copyright = "2012-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class ShopClient3 {

	/**
	 * Application entry point. The given runtime parameters must be a SOAP service URI.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URI is malformed
	 * @throws IOException if there is a problem reading the frame icon
	 * @throws UnsupportedLookAndFeelException if the VM does not support Nimbus look-and-feel
	 */
	static public void main (final String[] args) throws URISyntaxException, IOException, UnsupportedLookAndFeelException {
		final URL wsdlLocator = new URL(args[0] + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(ShopService.class));
		final ShopService serviceProxy = proxyFactory.getPort(ShopService.class);

		UIManager.setLookAndFeel(new NimbusLookAndFeel());
		final JComponent contentPane = new ShopPanel(serviceProxy);
		contentPane.setBorder(new EmptyBorder(2, 2, 2, 2));

		final JFrame frame = new JFrame("Dynamic (bottom-up) JAX-WS Shop Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(contentPane);
		frame.setIconImage(ImageIO.read(ShopClient3.class.getResourceAsStream("shopping-cart.png")));
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
}