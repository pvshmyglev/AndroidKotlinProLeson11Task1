package ru.netology.nmedia

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import okhttp3.internal.notifyAll

class PostViewModel (application: Application) : AndroidViewModel(application), PostInteractionCommands{

    private val repository : PostRepository = PostRepositoryHTTPImpl(
        AppDb.getInstance(application).postDao()
    )

    val data : LiveData<FeedModel> = repository.data.map(::FeedModel)
        .catch { e ->
           e.printStackTrace()
        }
        .asLiveData(Dispatchers.Default)

    private val _state = MutableLiveData(FeedModelState())
    val state : LiveData<FeedModelState>
        get() = _state

    val never: LiveData<Int> = data.switchMap {
        repository.getNeverCount().asLiveData(Dispatchers.Default)
    }

    private val emptyPost = Post(
        0L,
        "",
        "",
        "",
        "",
        "",
        false,
        0,
        0,
        0
    )

    val editedPost = MutableLiveData(emptyPost)
    val openedPost = MutableLiveData(emptyPost)

    private val _postUpdated = SingleLiveEvent<Post>()
    val postUpdated: LiveData<Post>
        get() = _postUpdated

    private val _needScrolling = SingleLiveEvent<Boolean>()
    val needScrolling: LiveData<Boolean>
        get() = _needScrolling

    private val _errorMessage = SingleLiveEvent<String>()
    val errorMessage: MutableLiveData<String>
        get() = _errorMessage

    fun updatedPost(post: Post) {

        viewModelScope.launch {

            try {
                _state.value = FeedModelState(refreshing = true)
                repository.save(post)
                _state.value = FeedModelState()
            } catch (e: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }

    }

    fun loadPosts() {

       viewModelScope.launch {

           try {
               _state.value = FeedModelState(loading = true)
               repository.getAll()
               _state.value = FeedModelState()
           } catch (e: Exception) {
               _state.value = FeedModelState(error = true)
           }
       }
    }

    fun refreshPosts() {

        viewModelScope.launch {

            try {
                _state.value = FeedModelState(refreshing = true)
                repository.getAll()
                _state.value = FeedModelState()
            } catch (e: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    private fun setObserveEditOpenPost(id: Long) {

        if (editedPost.value?.id != 0L && editedPost.value?.id == id) {
            data.value?.posts?.map { post ->
                if (post.id == editedPost.value?.id) { editedPost.value = post }
            }
        }

        if (openedPost.value?.id != 0L && openedPost.value?.id == id) {
            data.value?.posts?.map { post ->
                if (post.id == openedPost.value?.id) { openedPost.value = post }
            }
        }
    }

    override fun onLike(post: Post) {

        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.likeById(post.id)
                _state.value = FeedModelState()
            } catch (e: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }



    override fun onShare(post: Post) {
        //TODO
    }

    override fun onRemove(post: Post) {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.removeById(post.id)
                _state.value = FeedModelState()
            } catch (e: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    override fun onSaveContent(newContent: String) {
        viewModelScope.launch {
            val text = newContent.trim()
            editedPost.value?.let { thisEditedPost ->
                if (thisEditedPost.content != text) {
                    val postForSaved = thisEditedPost.copy(content = text)
                    repository.save(postForSaved)
                }
                editedPost.value = emptyPost
                setObserveEditOpenPost(thisEditedPost.id)
            }
        }
    }

    override fun readNeverPosts() {
        viewModelScope.launch {
            repository.readAllPosts()
            _needScrolling.postValue(true)
        }

    }

    override fun onEditPost(post: Post) {
        editedPost.value = post
    }

    override fun onCancelEdit() {
        editedPost.value = emptyPost
    }

    override fun onOpenPost(post: Post) {
        openedPost.value = post
    }

    override fun onCancelOpen() {
        openedPost.value = emptyPost
    }



}
