package de.htw.ds.shop;

import de.sb.java.net.SocketAddress;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public ShopConnector getJdbcConnector() {
        return this.jdbcConnector;
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

    public void cancelOrder(String alias, String password, long orderIdentity) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException {
        synchronized(jdbcConnector){
            try{
                byte[] hash = getPasswordHash(password);
                this.jdbcConnector.deleteOrder(alias,hash,orderIdentity);
                this.jdbcConnector.getConnection().commit();
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public long createOrder(String alias, String password, Collection<OrderItem> items) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException {
        long orderNumber;
        synchronized(jdbcConnector){
            try{
                byte[] hash = getPasswordHash(password);
                orderNumber = this.jdbcConnector.insertOrder(alias,hash,this.taxRate,items);
                this.jdbcConnector.getConnection().commit();
                return orderNumber;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public Article queryArticle(long articleIdentity) throws SQLException{
        Article article;
        synchronized(jdbcConnector){
            try{
                article = this.jdbcConnector.queryArticle(articleIdentity);
                return article;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public SortedSet<Article> queryArticles() throws SQLException {
        SortedSet<Article> articles;
        synchronized(jdbcConnector){
            try{
                articles = this.jdbcConnector.queryArticles();
                return articles;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public Customer queryCustomer(String alias, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException {
        Customer customer;
        synchronized(jdbcConnector){
            try{
                byte[] hash = getPasswordHash(password);
                customer = this.jdbcConnector.queryCustomer(alias, hash);
                this.jdbcConnector.getConnection().commit();
                return customer;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public Order queryOrder(String alias, String password, long orderIdentity) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException {
        Order order;
        synchronized(jdbcConnector){
            try{
                byte[] hash = getPasswordHash(password);
                order = this.jdbcConnector.queryOrder(alias,hash,orderIdentity);
                this.jdbcConnector.getConnection().commit();
                return order;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public SortedSet<Order> queryOrders(String alias, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException {
        SortedSet<Order> orders;
        synchronized(jdbcConnector){
            try{
                byte[] hash = getPasswordHash(password);
                orders = this.jdbcConnector.queryOrders(alias,hash);
                this.jdbcConnector.getConnection().commit();
                return orders;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public long registerCustomer(Customer customer, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException {
        long customerNo;
        synchronized(jdbcConnector){
            try{
                byte[] hash = getPasswordHash(password);
                customerNo = this.jdbcConnector.insertCustomer(customer.getAlias(),hash,
                        customer.getGivenName(),customer.getFamilyName(),customer.getStreet(),customer.getPostcode(),
                        customer.getCity(),customer.getEmail(),customer.getPhone());
                this.jdbcConnector.getConnection().commit();
                return customerNo;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    public long unregisterCustomer(String alias, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException {
        long customerNo;
        synchronized(jdbcConnector){
            try{
                byte[] hash = getPasswordHash(password);
                customerNo = this.jdbcConnector.deleteCustomer(alias,hash);
                this.jdbcConnector.getConnection().commit();
                return customerNo;
            } catch(Exception se) {
                if (this.jdbcConnector.getConnection() != null) this.jdbcConnector.getConnection().rollback();
                throw se;
            }
        }
    }

    private byte[] getPasswordHash(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes("UTF-8"));
    }
}
