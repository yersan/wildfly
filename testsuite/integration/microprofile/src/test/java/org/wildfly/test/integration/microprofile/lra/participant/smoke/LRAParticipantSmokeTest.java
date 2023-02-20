/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.lra.participant.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.lra.EnableLRAExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel.HotelParticipant;
import org.wildfly.test.integration.microprofile.lra.participant.smoke.model.Booking;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableLRAExtensionsSetupTask.class)
public class LRAParticipantSmokeTest {

    private static final String LRA_COORDINATOR_URL_KEY = "lra.coordinator.url";
    private static final String CLOSE_PATH = "/close";

    private static final long CLIENT_TIMEOUT = Long.getLong("lra.internal.client.timeout", 10);
    private static final long END_TIMEOUT = Long.getLong("lra.internal.client.end.timeout", CLIENT_TIMEOUT);

    @ArquillianResource
    public URL baseURL;

    public Client client;

    @Before
    public void before() {
        System.setProperty(LRA_COORDINATOR_URL_KEY, "http://" +
            (System.getProperties().containsKey("ipv6") ? "[::1]" : "127.0.0.1") + ":8080/lra-coordinator/lra-coordinator");
        client = ClientBuilder.newClient();
    }

    @After
    public void after() {
        if (client != null) {
            client.close();
        }
    }

    @Deployment
    public static WebArchive getDeployment() {

        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "lra-participant-test.war")
            .addPackages(true,
                "org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel",
                "org.wildfly.test.integration.microprofile.lra.participant.smoke.model")
            .addClasses(LRAParticipantSmokeTest.class,
                EnableLRAExtensionsSetupTask.class,
                CLIServerSetupTask.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return webArchive;
    }

    @Test
    public void hotelParticipantCompleteBookingTest() throws Exception {
        final URI lraId = startBooking();
        String id = getLRAUid(lraId.toString());
        closeLRA(id);
        validateBooking(id, true);
    }

    @Test
    public void hotelParticipantCompensateBookingTest() throws Exception {
        final URI lraId = startBooking();
        String id = getLRAUid(lraId.toString());
        compensateBooking(id);
        validateBooking(id, false);
    }

    private URI startBooking() throws Exception {
        Booking b = bookHotel("Paris-hotel");
        return new URI(b.getId());
    }

    private String completeBooking(String lraId) throws Exception {
        Response response = null;
        String responseEntity = null;
        try {
            response = client.target(baseURL.toURI())
                .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                .path(HotelParticipant.TRANSACTION_COMPLETE)
                .request()
                .header(LRA_HTTP_CONTEXT_HEADER, lraId)
                .put(Entity.text(""));

            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

    private String compensateBooking(String lraId) throws Exception {
        Response response = null;
        String responseEntity = null;
        try {
            response = client.target(baseURL.toURI())
                .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                .path(HotelParticipant.TRANSACTION_COMPENSATE)
                .request()
                .header(LRA_HTTP_CONTEXT_HEADER, lraId)
                .put(Entity.text(""));

            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

    private static final Pattern UID_REGEXP_EXTRACT_MATCHER = Pattern.compile(".*/([^/?]+).*");

    public String getLRAUid(String lraId) {
        return lraId == null ? null : UID_REGEXP_EXTRACT_MATCHER.matcher(lraId).replaceFirst("$1");
    }

    private void validateBooking(String lraId, boolean isEntryPresent) throws Exception {
        Response response = null;
        try {
            response = client.target(baseURL.toURI())
                .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                .request()
                .get();

            String result = response.readEntity(String.class);
            if (isEntryPresent) {
                Assert.assertTrue(
                    "Booking confirmed", result.contains("CONFIRMED"));
            } else {
                Assert.assertTrue(
                    "Booking cancelled", result.contains("CANCELLED"));
            }
        } catch (URISyntaxException e) {
            throw new Exception("Response: " + String.valueOf(response) + ", Exception Message: " + e.getMessage());
        }
    }


    private Booking bookHotel(String name) throws Exception {
        Response response = null;
        try {
            response = client.target(baseURL.toURI())
                .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                .queryParam("hotelName", name)
                .request()
                .post(Entity.text(""));

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new Exception("hotel booking problem; response status = " + response.getStatus());
            } else if (response.hasEntity()) {
                String result = response.readEntity(String.class);
                ObjectMapper obj = new ObjectMapper();
                return obj.readValue(result, Booking.class);
            } else {
                throw new Exception("hotel booking problem; no entity");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private String closeLRA(String lraId) throws Exception {
        Response response = null;
        String responseEntity = null;
        try {
            URI coordinatorURI = new URI(System.getProperty(LRA_COORDINATOR_URL_KEY));
            response = client.target(coordinatorURI)
                .path(lraId.concat(CLOSE_PATH))
                .request()
                .header("Narayana-LRA-API-version", "1.0")
                .async()
                .put(Entity.text(""))
                .get(END_TIMEOUT, TimeUnit.SECONDS);
            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

}