package de.htw.ds.shop;

import de.sb.java.xml.Namespaces;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Savepoint;

public class ShopServiceTest  {
    private static final int servicePort = 5555;
    private static final String serviceName = "JaxShopService";
    private static final double taxRate = 0.19;
    private static final String host = "http://localhost:5555/";

    private static Service proxyFactory;
    private Savepoint sp;
    ShopService service;
    static ShopServer server;

    @BeforeClass
    public static void prepare() {
        server = new ShopServer(servicePort, serviceName, taxRate);

        try {
            final URL wsdlLocator = new URL(host + "?wsdl");
            proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(ShopService.class));
        }
        catch (MalformedURLException e) {
            throw new AssertionError("Server-URL ungültig");
        }
    }

    @Before
    public void preTest() {
        service = proxyFactory.getPort(ShopService.class);
        try {
            sp = server.getJdbcConnector().getConnection().setSavepoint("UnitTest");
        }
        catch(SQLException e) {
            throw new AssertionError("Konnte keinen Savepoint erstellen");
        }
    }

    @After
    public void postTest() {
        try {
            server.getJdbcConnector().getConnection().rollback(sp);
        }
        catch(SQLException e) {
            throw new AssertionError("Konnte den Zustand vor dem Test nicht wiederherstellen");
        }
    }

    @Test
    public void testQueryArticle() {
        final Article article = service.queryArticle(1);
        assertEquals("Beschreibung", "CARIOCA Fahrrad-Schlauch, 28x1.5 Zoll", article.getDescription());
    }
}
