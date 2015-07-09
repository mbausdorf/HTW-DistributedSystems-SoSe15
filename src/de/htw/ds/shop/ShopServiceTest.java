package de.htw.ds.shop;

import de.sb.java.xml.Namespaces;

import org.junit.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import javax.xml.ws.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collection;
import java.util.SortedSet;

public class ShopServiceTest  {
    private static final int servicePort = 5555;
    private static final String serviceName = "ShopService";
    private static final double taxRate = 0.19;

    private static Service proxyFactory;
    private Savepoint sp;
    static ShopService serviceProxy;
    static ShopServer server;

    @BeforeClass
    public static void prepare() {
        server = new ShopServer(servicePort, serviceName, taxRate);

        try {
            final URL wsdlLocator = new URL(server.getServiceURI().toASCIIString() + "?wsdl");
            proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(ShopService.class));
            serviceProxy = proxyFactory.getPort(ShopService.class);
        }
        catch (MalformedURLException e) {
            throw new AssertionError("Server-URL ungültig");
        }
    }

    @Before
    public void beforeTest() {
        reloadData();
    }

    @AfterClass
    public static void reloadData() {
        try (BufferedReader f = new BufferedReader(new FileReader("src/de/htw/ds/shop/shop-mysql-data.ddl"))) {
            Statement stmt = server.getJdbcConnector().getConnection().createStatement();
            server.getJdbcConnector().getConnection().setAutoCommit(false);
            String line;
            String query = "";
            while ((line = f.readLine()) != null) {
                if (line.startsWith("--") || line.length() == 0)
                    continue;

                query += line;

                if (query.endsWith(";")) {
                    stmt.execute(query);
                    query = "";
                }
            }
            server.getJdbcConnector().getConnection().commit();
        }
        catch(SQLException e) {
            try {
                ShopConnector conn = server.getJdbcConnector();
                if (conn.getConnection() != null)
                    conn.getConnection().rollback();
            } catch (SQLException se2) {
                throw new AssertionError("Database error, could not perform rollback.");
            }
            throw new AssertionError(e.getMessage());
        }
        catch(IOException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    @Test
    public void testQueryArticle() {
        final Article article = serviceProxy.queryArticle(1);
        assertEquals("Beschreibung", "CARIOCA Fahrrad-Schlauch, 28x1.5 Zoll", article.getDescription());
    }

    @Test
    public void testQueryArticles() {
        final Collection<Article> articles = serviceProxy.queryArticles();
        assertEquals("anzahl artikel", 3, articles.size());
    }

    @Test
    public void testRegisterCustomer() {
        final Customer testCustomer = new Customer();
        testCustomer.setAlias("test");
        testCustomer.setEmail("example@example.org");
        testCustomer.setCity("1");
        testCustomer.setGivenName("2");
        testCustomer.setFamilyName("3");
        testCustomer.setPhone("4");
        testCustomer.setPostcode("5");
        testCustomer.setStreet("6");
        final long customerNum = serviceProxy.registerCustomer(testCustomer, "test");
        assertThat(customerNum, is(not(0l)));

        Customer foundCustomer = serviceProxy.queryCustomer("test", "test");

        assertThat(foundCustomer.getIdentity(), is(customerNum));
        assertThat(foundCustomer.getAlias(), is(testCustomer.getAlias()));
    }

    @Test
    public void testQueryCustomer() {
        Customer sascha = serviceProxy.queryCustomer("sascha", "sascha");
        assertThat(sascha.getAlias(), is("sascha"));
        assertThat(sascha.getFamilyName(), is("Baumeister"));
        assertThat(sascha.getPostcode(), is("10999"));
    }
    
    @Test
    public void testUnregisterCustomer() {
        final long customerNum = serviceProxy.unregisterCustomer("sascha", "sascha");
        assertThat(customerNum, is(0l));
        Customer foundCustomer = serviceProxy.queryCustomer("sascha", "sascha");
        assertThat(foundCustomer, is(not(nullValue())));
    }
    
    @Test 
    public void testCancelOrder() {
    	SortedSet<Order> orders = serviceProxy.queryOrders("sascha", "sascha");
    	serviceProxy.cancelOrder("sascha", "sascha", orders.first().getIdentity());
    	Order o = serviceProxy.queryOrder("sascha", "sascha", orders.first().getIdentity());
    	assertThat(o, is(nullValue()));
    }
}
