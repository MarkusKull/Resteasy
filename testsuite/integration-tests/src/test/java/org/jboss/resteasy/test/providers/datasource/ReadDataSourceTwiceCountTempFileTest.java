package org.jboss.resteasy.test.providers.datasource;

import java.io.ByteArrayOutputStream;
import java.io.File;


import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.resteasy.category.ExpectedFailing;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.test.providers.datasource.resource.ReadDataSourceTwiceCountTempFileResource;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * @tpSubChapter DataSource provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReadDataSourceTwiceCountTempFileTest {

    protected static final Logger logger = Logger.getLogger(ReadDataSourceTwiceCountTempFileResource.class.getName());

    static ResteasyClient client;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(ReadDataSourceTwiceCountTempFileResource.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, ReadDataSourceTwiceCountTempFileResource.class);
    }

    @Before
    public void init() {
        client = new ResteasyClientBuilder().build();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ReadDataSourceTwiceCountTempFileResource.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read the same stream twice, consuming content of whole stream
     * before reading the second and verifies that no temporary file left after stream is closed
     * @tpInfo RESTEASY-1182
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @Category({ExpectedFailing.class}) //[RESTEASY-1361] FIXME test failing on Travis CI but passing locally
    public void testFileNotFound() throws Exception {
        WebTarget target = client.target(generateURL("/post"));

        //Count files initially
        int beginning = countTempFiles();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        logger.info("The status of the response is " + response.getStatus());
        Assert.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-2847"), HttpResponseCodes.SC_OK, response.getStatus());
        int counter = response.readEntity(int.class);
        int updated = countTempFiles();
        logger.info("counter from beginning (before request): " + beginning);
        logger.info("counter from server: " + counter);
        logger.info("counter updated: " + countTempFiles());
        Assert.assertTrue("The number of temporary files for datasource before and after request is not the same",
                counter > updated);
    }

    /**
     * @tpTestDetails Tests DataSourceProviders ability to read the same stream twice, consuming content of whole stream
     * before reading the second and verifies that no temporary file left after stream is closed. The request is send multiple
     * times and then number of files is verified
     * @tpInfo RESTEASY-1182
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @Category({ExpectedFailing.class}) //[RESTEASY-1361] FIXME test failing on Travis CI but passing locally
    public void testFileNotFoundMultipleRequests() throws Exception {
        WebTarget target = client.target(generateURL("/post"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        logger.info("The status of the response is " + response.getStatus());
        Assert.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-2847"), HttpResponseCodes.SC_OK, response.getStatus());
        int counter = response.readEntity(int.class);

        response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        response.close();

        response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        response.close();

        response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        response.close();

        int updated = countTempFiles();
        logger.info("counter from server: " + counter);
        logger.info("counter updated: " + countTempFiles());
        Assert.assertTrue("The number of temporary files for datasource before and after request is not the same",
                counter > updated);
    }

    static int countTempFiles() throws Exception {
        String tmpdir = System.getProperty("java.io.tmpdir");
        File dir = new File(tmpdir);
        int counter = 0;
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith("resteasy-provider-datasource")) {
                counter++;
            }
        }
        return counter;
    }

    @AfterClass
    public static void afterclass() throws Exception {
        String tmpdir = System.getProperty("java.io.tmpdir");
        File dir = new File(tmpdir);
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith("resteasy-provider-datasource")) {
                logger.info(file.getName());
            }
        }
    }
}
