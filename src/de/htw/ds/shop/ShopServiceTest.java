package de.htw.ds.shop;

import de.sb.java.xml.Namespaces;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Collection;

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
            throw new AssertionError("Server-URL ung�ltig");
        }
    }

    @Before
    public void preTest() {
        /*try {
            sp = server.getJdbcConnector().getConnection().setSavepoint("UnitTest");
        }
        catch(SQLException e) {
            throw new AssertionError("Konnte keinen Savepoint erstellen");
        }*/
    }

    @After
    public void postTest() {
        /*try {
            server.getJdbcConnector().getConnection().rollback(sp);
        }
        catch(SQLException e) {
            throw new AssertionError("Konnte den Zustand vor dem Test nicht wiederherstellen");
        }*/
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
        assertThat(customerNum, not(0));

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
}
