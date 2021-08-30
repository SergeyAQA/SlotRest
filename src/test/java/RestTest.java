
import io.restassured.response.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.junit.jupiter.api.*;
import io.restassured.RestAssured;

import java.nio.charset.StandardCharsets;
import java.util.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestTest {

    protected JSONParser parser = new JSONParser();

    String baseUrl = "http://test-api.d6.dev.devcaz.com";
    String tokenUrl = "/v2/oauth2/token";
    String playerUrl = "/v2/players";

    static Random random = new Random();
    static int num = random.nextInt(10000);

    static String playerUserName = "testPlayer" + num;
    static String encodedPass = Base64.getEncoder().encodeToString((num + "@Pass#" + num).
            getBytes(StandardCharsets.UTF_8));

    static String bearerGuestToken;
    static String bearerPlayerToken;
    static Long userID;

    @Test()
    @Order(1)
    public void getGuestToken() throws ParseException {
        String urlToken = baseUrl + tokenUrl;
        Response response = RestAssured.given().auth().preemptive().
                basic("front_2d6b0a8391742f5d789d7d915755e09e", "").
                header("content-type", "application/json").
                body("""
                        {"grant_type":"client_credentials",
                        "scope":"guest:default"}
                        """).
                post(urlToken);
        Assertions.assertEquals(200, response.getStatusCode());
        bearerGuestToken = convertStringToJsonAndGetValue(response.getBody().asString(), "access_token");
    }

    @Test()
    @Order(2)
    public void createPlayer() throws ParseException {
        String createPlayer = baseUrl + playerUrl;
        Response response = RestAssured.given().
                header("Authorization", "Bearer " + bearerGuestToken).
                header("Content-Type", "application/json").
                body("{\"username\": \"" + playerUserName + "\",\n" +
                     "\"password_change\": \"" + encodedPass + "\",\n" +
                     "\"password_repeat\": \"" + encodedPass + "\",\n" +
                     "\"email\": \"" + playerUserName + "@gmail.com\"}\n").
                post(createPlayer);
        Assertions.assertEquals(201, response.getStatusCode());
        userID = convertLongToJsonAndGetValue(response.getBody().asString(), "id");
        System.out.println(response.getBody().asString());
    }

    @Test()
    @Order(3)
    public void authorizePlayer() throws ParseException {
        String urlToken = baseUrl + tokenUrl;
        Response response = RestAssured.given().auth().preemptive().
                basic("front_2d6b0a8391742f5d789d7d915755e09e", "").
                header("content-type", "application/json").
                body("{\"grant_type\":\"password\",\n" +
                     "\"username\":\"" + playerUserName + "\",\n" +
                     "\"password\":\"" + encodedPass + "\"}\n").
                post(urlToken);
        Assertions.assertEquals(200, response.getStatusCode());
        bearerPlayerToken = convertStringToJsonAndGetValue(response.getBody().asString(), "access_token");
    }

    @Test()
    @Order(4)
    public void getPlayersProfile() {
        String playerProfile = baseUrl + playerUrl + "/" + userID;
        Response response = RestAssured.given().
                header("Authorization", "Bearer " + bearerPlayerToken).
                header("Content-Type", "application/json").
                get(playerProfile);
        Assertions.assertEquals(200, response.getStatusCode());
        System.out.println(response.getBody().asString());
    }

    @Test()
    @Order(5)
    public void getAnotherPlayersProfile() {
        String playerProfile = baseUrl + playerUrl + "/" + (userID - 1);
        Response response = RestAssured.given().
                header("Authorization", "Bearer " + bearerPlayerToken).
                header("Content-Type", "application/json").
                get(playerProfile);
        Assertions.assertEquals(404, response.getStatusCode());
    }


    public String convertStringToJsonAndGetValue(String response, String nameOfValue) throws ParseException {
        JSONObject jsonResponse = (JSONObject) parser.parse(response);
        return (String) jsonResponse.get(nameOfValue);
    }

    public Long convertLongToJsonAndGetValue(String response, String nameOfValue) throws ParseException {
        JSONObject jsonResponse = (JSONObject) parser.parse(response);
        return (Long) jsonResponse.get(nameOfValue);
    }
}
