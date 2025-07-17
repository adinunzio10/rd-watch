package com.rdwatch.androidtv.repository

import com.rdwatch.androidtv.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getAllMovies(): Flow<List<Movie>>

    suspend fun getMovieById(movieId: Long): Movie?

    fun searchMovies(query: String): Flow<List<Movie>>

    suspend fun refreshMovies(): Result<Unit>

    suspend fun insertMovie(movie: Movie)

    suspend fun deleteMovie(movie: Movie)

    suspend fun deleteAllMovies()
}
