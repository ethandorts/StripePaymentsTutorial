package com.example.stripepaymentsnov2023;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String PUBLISH_KEY = "pk_test_51ODpyhBvKGTuqhDkDfcy4I91ABWLm4DglzHI4LxN9Lnmk4UeaKHlH8tjhmnw7Q6RA6H3V8r96cUiDFuZCF0KWGpt00zpi3vE64";
    String SECRET_KEY= "sk_test_51ODpyhBvKGTuqhDkBBwCBmEIA1vtbAL5aNqEOO5ZFvts5vjDrGqVeom3q5avvqeaAN9eeQxKJ8UNkORt5QAzUMTP00slGUqoDr";
    PaymentSheet paymentSheet;
    Button processButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        processButton = (Button) findViewById(R.id.button);
        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCustomerID();
            }
        });
        PaymentConfiguration.init(this, PUBLISH_KEY);
        paymentSheet = new PaymentSheet(this, paymentSheetResult -> {
            onPaymentResult(paymentSheetResult);
        });
    }

    public void getCustomerID() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/customers",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String CustomerID = jsonObject.getString("id");
                            System.out.println("Stripe CustomerID = " + CustomerID);
                            getEphericalKey(CustomerID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("customer = error");
            }
        }){
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError{
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + SECRET_KEY);
                return header;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }

    private void getEphericalKey(String customerID) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/ephemeral_keys",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String EphericalKey = jsonObject.getString("id");
                            System.out.println("Stripe EphericalKey = " + EphericalKey);
                            getClientSecret(customerID, EphericalKey);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }){
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError{
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + SECRET_KEY);
                header.put("Stripe-Version", "2020-08-27");
                return header;
            }
            @Override
            public Map<String, String> getParams() throws AuthFailureError{
                Map<String, String> params = new HashMap<>();
                params.put("customer", customerID);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }

    private void getClientSecret(String customerID, String EphericalKey) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/payment_intents",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String ClientSecret = jsonObject.getString("client_secret");
                            System.out.println("Stripe ClientSecret = " + ClientSecret);
                            paymentFlow(customerID, EphericalKey, ClientSecret);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + SECRET_KEY);
                return header;
            }
            @Override
            public Map<String,String> getParams() throws AuthFailureError{
                Map<String, String> params = new HashMap<>();
                params.put("customer", customerID);
                params.put("amount", "100" + "00");
                params.put("currency", "usd");
                params.put("automatic_payment_methods[enabled]", "true");
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }

    private void paymentFlow(String customerID, String EphericalKey, String ClientSecret) {
        paymentSheet.presentWithPaymentIntent(ClientSecret, new PaymentSheet.Configuration(
                "Ulster University", new PaymentSheet.CustomerConfiguration(
                        customerID,
                        EphericalKey
        )
        ));
    }

    private void onPaymentResult(PaymentSheetResult paymentSheetResult) {
        if(paymentSheetResult instanceof PaymentSheetResult.Completed){
            Toast.makeText(this, "Payment Successful", Toast.LENGTH_LONG).show();
        }
    }
}