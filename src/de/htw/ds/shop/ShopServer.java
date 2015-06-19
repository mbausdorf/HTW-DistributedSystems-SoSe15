package de.htw.ds.shop;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.net.URI;
import java.util.Collection;
import java.util.SortedSet;

/**
 * Created by Markus on 19.06.2015.
 */
@WebService(endpointInterface = "de.htw.ds.chat.SoapChatService", serviceName = "SoapChatService")
public class ShopServer implements ShopService, AutoCloseable {
    private final URI serviceUri;
    private final Endpoint endpoint;
    private final ShopConnector shopConnector;
    private final double taxRate;

    public ShopServer(int servicePort , string serviceName, double taxRate )
    {
        this.taxRate = taxRate;

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void cancelOrder(String alias, String password, long orderIdentity) {

    }

    @Override
    public long createOrder(String alias, String password, Collection<OrderItem> items) {
        return 0;
    }

    @Override
    public Article queryArticle(long articleIdentity) {
        return null;
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
