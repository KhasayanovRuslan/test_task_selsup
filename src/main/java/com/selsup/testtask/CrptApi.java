package com.selsup.testtask;

import com.google.gson.Gson;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private int requestLimit;
    private long interval;
    private AtomicInteger currentCount;
    private long lastResetTime;
    private ReentrantLock lock;
    private HttpClient httpClient;
    private Gson gson;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.interval = timeUnit.toMillis(1);
        this.currentCount = new AtomicInteger(0);
        this.lastResetTime = System.currentTimeMillis();
        this.lock = new ReentrantLock();
        this.httpClient = HttpClients.createDefault();
        this.gson = new Gson();
    }

    public String createDocument(Document document, String signature, String token) {
        if (canMakeApiCall()) {
            String jsonDocument = gson.toJson(document);
            try {
                HttpPost request = new HttpPost(URL);
                request.setEntity(new StringEntity(jsonDocument, ContentType.APPLICATION_JSON));
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Signature", signature);
                request.setHeader("Bearer", token);

                HttpResponse response = httpClient.execute(request); // Выполнение HTTP запроса
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String line;
                    StringBuilder responseBody = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        responseBody.append(line);
                    }
                    System.out.println("Выполнен вызов к API для создания документа");
                    return responseBody.toString();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Достигнут лимит запросов, вызов к API заблокирован");
        }
        return null;
    }

    //Метод проверки превышения лимита к созданию документа
    private boolean canMakeApiCall() {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastResetTime > interval) {
                currentCount.set(0);
                lastResetTime = currentTime;
            }

            return currentCount.getAndIncrement() < requestLimit;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.MILLISECONDS, 5);
        System.out.println(api.createDocument(new Document(), "s", "1"));
    }

    @Data
    @RequiredArgsConstructor
    private static class Document {

        private String participantInn;

        private String docId;

        private String docStatus;

        private String docType;

        private String importRequest;

        private String ownerInn;

        private String producerInn;

        private String productionDate;

        private String productionType;

        private String regDate;

        private String regNumber;

        private List<Product> products;
    }

    @Data
    @RequiredArgsConstructor
    private static class Product {

        private String certificateDocument;

        private String certificateDocumentDate;

        private String certificateDocumentNumber;

        private String ownerInn;

        private String producerInn;

        private String productionDate;

        private String tnvedCode;

        private String uitCode;

        private String uituCode;
    }
}
