package com.hypernode.ledger.webService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hypernode.ledger.ErrorHandling;
//import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

//@Slf4j
public class WebServiceCaller {
    private static final ObjectMapper objectmapper = new ObjectMapper();

    static {
        objectmapper.registerModule(new JavaTimeModule());
    }
    public static <T> T callServerMethod(String connectionString, String endpointName, Object bodyObject, TypeReference<T> typeRef) throws IOException, InterruptedException {
        try
        {
            return callServerMethodThrows(connectionString, endpointName, bodyObject, typeRef);
        }
        catch (RuntimeException e)
        {
            ErrorHandling.logEvent("error webservicecaller.callServerMethod",false,e);
            //log.info("Swallowed exception ", e);
        }
        return null;
    }

    public static <T> T callServerMethodThrows(String connectionString, String endpointName, Object bodyObject, TypeReference<T> typeRef) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("Connection string is empty");
        }
        try {
            // Create an HttpClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // Build the request
            if (!connectionString.endsWith("/")) {
                connectionString = connectionString + "/";
            }

            if (!connectionString.startsWith("https://")
            && !connectionString.startsWith("http://")) {
                connectionString = "http://" + connectionString;
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(connectionString + endpointName))
                    .header("Content-Type", "application/json");
            HttpRequest request;
            if (bodyObject == null) {
                request = builder.GET().build();
            } else {

                String body = objectmapper.writeValueAsString(bodyObject);
                request = builder
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
            }

            // Send the request and get the response
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // Process the response
            System.out.println("Status code: " + response.statusCode());
            System.out.println("Body: " + response.body());
            if (typeRef.getType().equals(String.class)) {
                return (T) response.body();
            }
            return objectmapper.readValue(response.body(), typeRef);
        }
        catch (IOException | InterruptedException e)
        {
            if (e instanceof JsonProcessingException)
            {
                //log.error("Unexpected response type [expected {}] when calling {}", typeRef, connectionString + endpointName, e);
            }
            else
            {
                //log.error("Unexpected error when calling {}", connectionString + endpointName, e);
            }
            ErrorHandling.logEvent("Error webservicecaller.callServerMethodThrows",true,e);
            //throw new RuntimeException(e);
            return null;
        }
    }
}
