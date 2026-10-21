package clinic.harbour.service;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Client for the clinic's locally hosted clinical language model (OpenAI-compatible chat completions). */
public class ChatLanguageModel {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String endpoint=System.getenv().getOrDefault("CLINIC_MODEL_URL","http://clinic-model.internal/v1/chat/completions");

    public String generate(String systemPrompt,String userMessage){
        JsonObject payload=Json.createObjectBuilder()
            .add("model","harbour-clinical-1")
            .add("messages",Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("role","system").add("content",systemPrompt))
                .add(Json.createObjectBuilder().add("role","user").add("content",userMessage)))
            .build();
        HttpRequest request=HttpRequest.newBuilder(URI.create(endpoint)).header("Content-Type","application/json").timeout(Duration.ofSeconds(20)).POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        try{
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            JsonObject body=Json.createReader(new StringReader(response.body())).readObject();
            return body.getJsonArray("choices").getJsonObject(0).getJsonObject("message").getString("content");
        }catch(Exception e){throw new IllegalStateException("Clinical assistant is unavailable",e);}
    }
}
