package com.katza.calmind;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

public class TravelTimeHelper {

    public interface TravelTimeCallback {
        void onTimeRetrieved(int minutes);
        void onError(Exception e);
    }

    public static void getTravelTime(Context context, double startLat, double startLng,
                                     double endLat, double endLng, TravelTimeCallback callback) {

        // שימוש בשרת OSRM החינמי לחישוב מסלול נסיעה
        String url = String.format("https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                startLng, startLat, endLng, endLat);

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // OSRM מחזיר זמן בשניות, אנחנו נהפוך לדקות
                        double durationSeconds = response.getJSONArray("routes")
                                .getJSONObject(0).getDouble("duration");
                        int minutes = (int) Math.ceil(durationSeconds / 60);
                        callback.onTimeRetrieved(minutes);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                },
                error -> callback.onError(new Exception(error.getMessage()))
        );

        queue.add(request);
    }
}