package ru.netology.nmedia

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.lang.Exception


class PostRepositoryHTTPImpl(
    private val postDao: PostDao
) : PostRepository {

    override val data = postDao.getAll()
        .map(List<PostEntity>::toPost)
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {

        val result = PostsApi.retrofitService.getAll()

        if (!result.isSuccessful) {
            error("Response code: ${result.code()}")
        }

        val posts = result.body() ?: error("Body is null")
        postDao.insert(posts.toPostEntity())
        postDao.insertReadingPosts(posts.toReadedPostEntity())

    }

    override fun getNeverCount(): Flow<Int> = flow {
        while (true) {

            val result = PostsApi.retrofitService.getNewer(postDao.getMaxId())
            if (!result.isSuccessful) {
                error("Response code: ${result.code()}")
            }
            val posts = result.body() ?: error("Body is null")
            postDao.insert(posts.toPostEntity())

            val countResult = postDao.getNeverCount()

            emit(countResult)
            delay(10_000)
        }


    }
        .catch { e ->
            val err = AppError.from(e)
            throw CancellationException()
        }
        .flowOn(Dispatchers.Default)

    override suspend fun getById(id: Long): Post {

        val result = PostsApi.retrofitService.getById(id)

        if (!result.isSuccessful) {
            error("Response code: ${result.code()}")
        }

        val post = result.body() ?: error("Body is null")

        postDao.insertPost(post.toPostEntity())

        return post

    }

    override suspend fun readAllPosts() {
        postDao.insertReadingPosts(
            postDao.getUnreadedPosts().toReadedPostsEntity()
        )
        getNeverCount()
    }

    override suspend fun likeById(id: Long) {

        val postInBase = postDao.getById(id)
        val likedPost = postInBase.copy(
            likeByMe = !postInBase.likeByMe,
            countLikes = if (postInBase.likeByMe) {postInBase.countLikes - 1} else {postInBase.countLikes + 1}
        )

        postDao.insertPost(likedPost)

        val result =
            if (postInBase.likeByMe) { PostsApi.retrofitService.dislikeById(id) } else { PostsApi.retrofitService.likeById(id) }
        if (!result.isSuccessful) {
            error("Response code: ${result.code()}")
        }
    }

    override suspend fun shareById(id: Long) {

    }

    override suspend fun removeById(id: Long) {
        postDao.removeById(id)
        val result = PostsApi.retrofitService.removeById(id)
        if (!result.isSuccessful) {
            error("Response code: ${result.code()}")
        }

    }

    override suspend fun save(post: Post) {
        val result = PostsApi.retrofitService.save(post)
        if (!result.isSuccessful) {
            error("Response code: ${result.code()}")
        }
        val refreshPost = result.body() ?: error("Body is null")
        postDao.insertPost(refreshPost.toPostEntity())
        postDao.insertReadingPost(refreshPost.toReadedPostEntity())
    }




}