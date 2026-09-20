package com.wapo.flagship.features.newsletter.repo.remote

import com.wapo.flagship.features.newsletter.repo.remote.model.NewsletterEnrollResponse
import com.wapo.flagship.features.newsletter.repo.remote.model.NewsletterUnenrollResponse
import com.wapo.flagship.features.newsletter.repo.remote.model.NewslettersRequestBody
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface NewslettersService {
    @POST("api/v1/user/enroll/")
    suspend fun enrollUser(
        @HeaderMap headers: HashMap<String, String>,
        @Body body: NewslettersRequestBody,
    ): APIResult<NewsletterEnrollResponse>

    @POST("api/v1/user/unenroll/")
    suspend fun unenrollUser(
        @HeaderMap headers: HashMap<String, String>,
        @Body body: NewslettersRequestBody,
    ): APIResult<NewsletterUnenrollResponse>

    @GET("api/v1/user/enrollments/")
    suspend fun getEnrollments(
        @HeaderMap headers: HashMap<String, String>,
    ): APIResult<List<String>>
}
