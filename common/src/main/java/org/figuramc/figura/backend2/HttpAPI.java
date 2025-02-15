package org.figuramc.figura.backend2;

import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;

public class HttpAPI {

    private final String token;
    private final String ModName = "Figura";
    public HttpAPI(String token) {
        this.token = token;
    }


    // -- builders -- // 


    protected static URI getUri(String url) {
        return URI.create(getBackendAddress() + "/" + url);
    }

    public static String getBackendAddress() {
        if(Configs.BLOCK_CLOUD.value) return "http://127.0.0.1:9/api";
        if(Configs.VANILLA_CLOUD.value){
            return "https://" + ServerAddress.parseString(Configs.SERVER_IP.defaultValue).getHost() + "/api";
        }
        String backendIP = Configs.USE_MC_HOST_RESOLVER.value ? ServerAddress.parseString(Configs.SERVER_IP.value).getHost() : Configs.SERVER_IP.value;
        if(Configs.USE_SECURE_CLOUD.value) return "https://" + backendIP + "/api";
        return "http://" + backendIP + "/api";
    }

    protected HttpRequest.Builder header(String url) {
        return HttpRequest
                .newBuilder(getUri(url))
                .header("user-agent", ModName+"/" + FiguraMod.VERSION)
                .header("token", token);
    }


    // -- runners -- // 


    protected static void runString(HttpRequest request, BiConsumer<Integer, String> consumer) {
        try {
            requestDebug(request);
            HttpResponse<String> response = NetworkStuff.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code == 401) NetworkStuff.reAuth();
            consumer.accept(code, response.body());
        } catch (Exception e) {
            if (!e.getMessage().contains("GOAWAY received"))
                FiguraMod.LOGGER.error("", e);
        }
    }

    protected static void runStream(HttpRequest request, BiConsumer<Integer, InputStream> consumer) {
        try {
            requestDebug(request);
            HttpResponse<InputStream> response = NetworkStuff.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int code = response.statusCode();
            if (code == 401) NetworkStuff.reAuth();
            consumer.accept(code, response.body());
        } catch (Exception e) {
            if (!e.getMessage().contains("GOAWAY received"))
                FiguraMod.LOGGER.error("", e);
        }
    }


    // -- feedback -- // 


    private static void requestDebug(HttpRequest msg) {
        if (NetworkStuff.debug)
            FiguraMod.debug( "Sent Http request:\n\t" + msg.uri().toString() + "\n\t" + msg.headers().map().toString());
    }


    // -- accessors -- // 


    // will return 200 OK if token is valid
    public HttpRequest checkAuth() {
        return header("").build();
    }

    public HttpRequest getUser(UUID id) {
        return header(id.toString()).build();
    }

    public HttpRequest getLimits() {
        return header("limits").build();
    }

    public HttpRequest getVersion() {
        return header("version").build();
    }

    public HttpRequest getMotd() {
        return header("motd").build();
    }

    public HttpRequest getAvatar(UUID owner, String id) {
        return header(owner.toString() + '/' + id).build();
    }

    public HttpRequest uploadAvatar(String id, byte[] bytes) {
        return header(id).PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).header("Content-Type", "application/octet-stream").build();
    }

    public HttpRequest deleteAvatar(String id) {
        return header(id).DELETE().build();
    }

    public HttpRequest setEquipped(String json) {
        return header("equip").POST(HttpRequest.BodyPublishers.ofString(json)).header("Content-Type", "application/json").build();
    }
    public HttpRequest setBadge(Integer badgeId) {
        String badge = badgeId.toString();
        return header("temp_badges").POST(HttpRequest.BodyPublishers.ofString(badge))
                .header("Content-Type", "application/json").build();
    }

    public HttpRequest clearBadge() {
        return header("temp_badges").POST(HttpRequest.BodyPublishers.ofString("null"))
                .header("Content-Type", "application/json").build();
    }
}
