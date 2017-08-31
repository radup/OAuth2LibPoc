package ch.cern.poc.oauth2.test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.JerseyClientBuilder;

import ch.cern.poc.oauth2.Common;
import junit.framework.Assert;

public class MyTest {
	
	private URL url;
    private Client client = JerseyClientBuilder.newClient();
    
	public MyTest(URL url) {
		this.url = url;
	}
    
	public static void main(String args[]) throws Exception {
		
		MyTest myTest = new MyTest(new URL("http://localhost:8080/oauth2-poc/"));
		
		try {
            Response response = myTest.makeAuthCodeRequest();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            String authCode = myTest.getAuthCode(response);
            Assert.assertNotNull(authCode);

            OAuthAccessTokenResponse oauthResponse = myTest.makeTokenRequestWithAuthCode(authCode);
            String accessToken = oauthResponse.getAccessToken();

            URL restUrl = new URL(myTest.getUrl().toString() + "api/resource");
            WebTarget target = myTest.getClient().target(restUrl.toURI());
            String entity = target.request(MediaType.TEXT_HTML)
                    .header(Common.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                    .get(String.class);
            System.out.println("Response = " + entity);
        } catch (MalformedURLException | URISyntaxException | OAuthProblemException | OAuthSystemException | JSONException ex) {
            Logger.getLogger(AuthTest.class.getName()).log(Level.SEVERE, null, ex);
        }
		
	}
	
	private Response makeAuthCodeRequest() throws OAuthSystemException, URISyntaxException {
        OAuthClientRequest request = OAuthClientRequest
                .authorizationLocation(url.toString() + "api/authz")
                .setClientId(Common.CLIENT_ID)
                .setRedirectURI(url.toString() + "api/redirect")
                .setResponseType(ResponseType.CODE.toString())
                .setState("state")
                .buildQueryMessage();
        WebTarget target = client.target(new URI(request.getLocationUri()));
        return target.request(MediaType.TEXT_HTML).get();
    }

    private String getAuthCode(Response response) throws JSONException {
        JSONObject obj = new JSONObject(response.readEntity(String.class));
        JSONObject qp = obj.getJSONObject("queryParameters");
        String authCode = null;
        if (qp != null) {
            authCode = qp.getString("code");
        }

        return authCode;
    }

    private OAuthAccessTokenResponse makeTokenRequestWithAuthCode(String authCode) throws OAuthProblemException, OAuthSystemException {
        OAuthClientRequest request = OAuthClientRequest
                .tokenLocation(url.toString() + "api/token")
                .setClientId(Common.CLIENT_ID)
                .setClientSecret(Common.CLIENT_SECRET)
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setCode(authCode)
                .setRedirectURI(url.toString() + "api/redirect")
                .buildBodyMessage();
        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
        return oAuthClient.accessToken(request);
    }

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}
    
    
	

}
