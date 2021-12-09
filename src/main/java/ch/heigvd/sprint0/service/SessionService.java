package ch.heigvd.sprint0.service;


import com.sun.istack.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
public class SessionService {

    private final String login = "/auth/login";
    private final String register = "/accounts/register";
    private FileReader apiLoginServerUrlJson;
    private JSONObject loginConfig;
    private String apiLoginServerUrl;


    // TODO DPO - Utilisez Autowired, c'est cool (je le faisais également avant), mais ceci implique un petit arrachage
    //  de cheveux si vous faites des tests d'intégration comparé à l'utilisation des constructeurs.
    //  Je vous conseille, à moins que vous soyez au clair là-dessus, d'utiliser les constructeurs.
    @Autowired
    private JWTService jwtService;


    private final String tokenName = "tokenJWT";

    public SessionService(){
        try {
            apiLoginServerUrlJson = new FileReader("./config.json", StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            System.out.println("Error, config(s) file(s) missing or cannot been read. Please check README.");
            System.exit(-1);
        }
        loginConfig = new JSONObject(new JSONTokener(new BufferedReader (apiLoginServerUrlJson)));
        apiLoginServerUrl = (String) loginConfig.get("apiAuthenticationServer");
    }

    public boolean setLogin(String username, String password, HttpServletResponse response) {
        String jwtToken = null;
        try {
            jwtToken = doLogin(username, password);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error with login service communication.");
        }

        if(jwtToken != null) {
            Cookie session = new Cookie(tokenName, jwtToken);
            response.addCookie(session);
            return true;
        } else {
            return false;
        }

    }

    private String doLogin(String username, String password) throws IOException {

        URL url = new URL(apiLoginServerUrl + login);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        JSONObject toServer = new JSONObject();
        toServer.put("username", username).put("password", password);

        OutputStream os = con.getOutputStream();
        os.write(toServer.toString().getBytes());

        if (con.getResponseCode() != 200) {
            return null;
        }
        InputStream responseStream = con.getInputStream();

        JSONObject response = new JSONObject(new JSONTokener(responseStream));
        return (String) response.get("token");

    }

    /**
     * Permet de vérifier si un user est connecté. Si oui, on récupère son rôle et username.
     * @param request
     * @return Un tableau de string contenant le rôle à l'indice 0 et le username à l'indice 1.
     *         Si il n'y a pas de login ou une erreur d'authenticité, renvoie null
     */
    public String[] checkLogin(HttpServletRequest request) {
        Objects.requireNonNull(request);

        String[] output = null;
        Cookie tokenLoginCookie = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(tokenName)) {
                    tokenLoginCookie = cookie;
                    break;
                }
            }
        }

        if (tokenLoginCookie != null) {     // Le token de login est présent, check de son authenticité
            output = jwtService.extractToken(tokenLoginCookie.getValue());
        }

        return output;
    }


    public String doRegister(String username, String password, HttpServletResponse response) throws IOException {

        URL url = new URL(apiLoginServerUrl + register);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        JSONObject toServer = new JSONObject();
        toServer.put("username", username).put("password", password);

        OutputStream os = con.getOutputStream();
        os.write(toServer.toString().getBytes());
        os.flush();

        int code = con.getResponseCode();

        InputStream responseStream = con.getErrorStream();
        JSONObject res = new JSONObject();
        if (responseStream != null) {
            res = new JSONObject(new JSONTokener(responseStream));
        }

        switch (code){
            case 201: return null;

            case 409: return (String) res.get("error");

            case 422:
                JSONArray arrayErrors = res.getJSONArray("errors");
                StringBuilder errorMessage = new StringBuilder();
                for (int i = 0; i < arrayErrors.length(); i++) {
                    JSONObject error = arrayErrors.getJSONObject(0);
                    errorMessage.append(error.getString("message"));
                }
                return errorMessage.toString();

            default: return "Erreur inconnue";
        }
    }

}
