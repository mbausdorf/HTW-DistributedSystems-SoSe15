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
@WebService(endpointInterface = "de.htw.ds.shop.ShopService", serviceName = "ShopService")
public class ShopServer implements ShopService, AutoCloseable {
    private final URI serviceURI;
    private final Endpoint endpoint;
    private ShopConnector jdbcConnector;
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

    public void close() throws Exception {
        this.endpoint.stop();
    }

    public synchronized void cancelOrder(String alias, String password, long orderIdentity) {
        try{
            this.jdbcConnector.deleteOrder(alias,password.getBytes(),orderIdentity);
            this.jdbcConnector.getConnection().commit();
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
    }

    public synchronized long createOrder(String alias, String password, Collection<OrderItem> items) {
        long orderNumber;
        try{
            orderNumber = this.jdbcConnector.insertOrder(alias,password.getBytes(),this.taxRate,items);
            this.jdbcConnector.getConnection().commit();
            return orderNumber;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return 0;
    }

    public synchronized Article queryArticle(long articleIdentity) {
        Article article;
        try{
            article = this.jdbcConnector.queryArticle(articleIdentity);
            this.jdbcConnector.getConnection().commit();
            return article;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return null;
    }

    public synchronized SortedSet<Article> queryArticles() {
        SortedSet<Article> articles;
        try{
            articles = this.jdbcConnector.queryArticles();
            this.jdbcConnector.getConnection().commit();
            return articles;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return null;
    }

    public synchronized Customer queryCustomer(String alias, String password) {
        Customer customer;
        try{
            customer = this.jdbcConnector.queryCustomer(alias, password.getBytes());
            this.jdbcConnector.getConnection().commit();
            return customer;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return null;
    }

    public synchronized Order queryOrder(String alias, String password, long orderIdentity) {
        Order order;
        try{
            order = this.jdbcConnector.queryOrder(alias,password.getBytes(),orderIdentity);
            this.jdbcConnector.getConnection().commit();
            return order;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return null;
    }

    public synchronized SortedSet<Order> queryOrders(String alias, String password) {
        SortedSet<Order> orders;
        try{
            orders = this.jdbcConnector.queryOrders(alias,password.getBytes());
            this.jdbcConnector.getConnection().commit();
            return orders;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return null;
    }

    public synchronized long registerCustomer(Customer customer, String password) {
        long customerNo;
        try{
            customerNo = this.jdbcConnector.insertCustomer(customer.getAlias(),password.getBytes(),
                    customer.getGivenName(),customer.getFamilyName(),customer.getStreet(),customer.getPostcode(),
                    customer.getCity(),customer.getEmail(),customer.getPhone());
            this.jdbcConnector.getConnection().commit();
            return customerNo;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return 0;
    }

    public synchronized long unregisterCustomer(String alias, String password) {
        long customerNo;
        try{
            customerNo = this.jdbcConnector.deleteCustomer(alias,password.getBytes());
            this.jdbcConnector.getConnection().commit();
            return customerNo;
        } catch(Exception se) {
            try {
                if (this.jdbcConnector.getConnection() != null)
                    this.jdbcConnector.getConnection().rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            // according to task 3, we should throw an exception here, but we can't
        }
        return 0;
    }
}
