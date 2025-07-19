package com.rdwatch.androidtv.repository

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.data.MovieDao
import com.rdwatch.androidtv.network.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl
    @Inject
    constructor(
        private val apiService: ApiService,
        private val movieDao: MovieDao,
    ) : MovieRepository {
        override fun getAllMovies(): Flow<List<Movie>> {
            return movieDao.getAllMovies()
        }

        override suspend fun getMovieById(movieId: Long): Movie? {
            return movieDao.getMovieById(movieId)
        }

        override fun searchMovies(query: String): Flow<List<Movie>> {
            return movieDao.searchMovies(query)
        }

        override suspend fun refreshMovies(): Result<Unit> {
            return try {
                val response = apiService.getMovies()
                if (response.isSuccessful) {
                    response.body()?.let { movies ->
                        movieDao.deleteAllMovies()
                        movieDao.insertMovies(movies)
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to fetch movies: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun insertMovie(movie: Movie) {
            movieDao.insertMovie(movie)
        }

        override suspend fun deleteMovie(movie: Movie) {
            movieDao.deleteMovie(movie)
        }

        override suspend fun deleteAllMovies() {
            movieDao.deleteAllMovies()
        }
    }
