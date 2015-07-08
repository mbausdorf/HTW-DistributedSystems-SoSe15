package de.htw.ds.shop;

import de.sb.java.net.SocketAddress;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.SortedSet;

/**
 * Created by Markus on 19.06.2015.
 */
@WebService(endpointInterface = "de.htw.ds.chat.SoapChatService", serviceName = "SoapChatService")
public class ShopServer implements ShopService, AutoCloseable {
    private final URI serviceURI;
    private final Endpoint endpoint;
    private final ShopConnector jdbcConnector;
    private final double taxRate;

    public ShopServer(int servicePort , String serviceName, double taxRate )
    {
        if(taxRate < 0 || taxRate > 1) throw new IllegalArgumentException();
        this.taxRate = taxRate;

        try{
            this.jdbcConnector = new ShopConnector();
            this.jdbcConnector.getConnection().setAutoCommit(false);
        } catch (SQLException exception)
        {
            System.out.printf("Error in SQL connection: " + exception.getMessage());
        }

        if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

        try {
            this.serviceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
        } catch (final URISyntaxException exception) {
            throw new IllegalArgumentException();
        }

        // non-standard SOAP1.2 binding: "http://java.sun.com/xml/ns/jaxws/2003/05/soap/bindings/HTTP/"
        this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
        this.endpoint.publish(this.serviceURI.toASCIIString());

    }

    static public void main (final String[] args) throws WebServiceException, IOException {
        final long timestamp = System.currentTimeMillis();
        final int servicePort = Integer.parseInt(args[0]);
        final String serviceName = args[1];
        double taxRate = Double.parseDouble(args[2]);

        try {
            try (ShopServer server = new ShopServer(servicePort, serviceName, taxRate)) {
                System.out.format("Dynamic (bottom-up) JAX-WS shop server running, enter \"quit\" to stop.\n");
                System.out.format("Service URI is \"%s\".\n", server.getServiceURI().toASCIIString());
                System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

                final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
                while (!"quit".equals(charSource.readLine()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the service URI.
     * @return the service URI
     */
    public URI getServiceURI () {
        return this.serviceURI;
    }

    @Override
    public void close() throws Exception {
        this.endpoint.stop();
    }

    @Override
    public synchronized void cancelOrder(String alias, String password, long orderIdentity) {

    }

    @Override
    public long createOrder(String alias, String password, Collection<OrderItem> items) {
        long orderNumber;
        try{
            orderNumber = this.jdbcConnector.insertOrder(alias,password.getBytes(),this.taxRate,items);
        } catch (Exception exception){
            this.jdbcConnector.getConnection().rollback();
            throw exception;
        }

        return orderNumber;
    }

    @Override
    public synchronized Article queryArticle(long articleIdentity) {
        Article article;
        try{
            article = this.jdbcConnector.queryArticle(articleIdentity);
        } catch (Exception exception){
            this.jdbcConnector.getConnection().rollback();
            throw exception;
        }

        return article;
    }

    @Override
    public SortedSet<Article> queryArticles() {
        return null;
    }

    @Override
    public Customer queryCustomer(String alias, String password) {
        return null;
    }

    @Override
    public Order queryOrder(String alias, String password, long orderIdentity) {
        return null;
    }

    @Override
    public SortedSet<Order> queryOrders(String alias, String password) {
        return null;
    }

    @Override
    public long registerCustomer(Customer customer, String password) {
        return 0;
    }

    @Override
    public long unregisterCustomer(String alias, String password) {
        return 0;
    }
}
