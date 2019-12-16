package com.example.socialmediaapp.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAYu8NDTA:APA91bFbgAPYIWzBBGzIfxhaut3Qq2rSvtHyf7DhJExtW89ZHNQpBNH5QSaN6jStsCjCgmpc294gstMeTvhkd8sK3p3rB2mmvN40oaLeBWL_Fv2aml2-ISyDcR1JDR3xFAqyMtXpRUBw"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
