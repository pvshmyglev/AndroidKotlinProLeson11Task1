package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

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