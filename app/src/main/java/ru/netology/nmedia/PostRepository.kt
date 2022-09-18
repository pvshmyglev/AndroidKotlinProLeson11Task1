package ru.netology.nmedia

import kotlinx.coroutines.flow.Flow

interface PostRepository {

    val data : Flow<List<Post>>
    suspend fun getAll()
    fun getNeverCount() : Flow<Int>
    suspend fun likeById(id: Long)
    suspend fun shareById(id: Long)
    suspend fun removeById(id: Long)
    suspend fun save(post: Post)
    suspend fun getById(id: Long) : Post
    suspend fun readAllPosts()

}