package com.rdwatch.androidtv.network

import com.rdwatch.androidtv.Movie
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    
    @GET("movies")
    suspend fun getMovies(): Response<List<Movie>>
    
    @GET("movies/{id}")
    suspend fun getMovie(@Path("id") movieId: String): Response<Movie>
}