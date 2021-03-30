package com.beomjo.whitenoise.ui.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import com.beomjo.compilation.util.LogUtil
import com.beomjo.whitenoise.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerServiceConnection @Inject constructor(
    context: Context,
    serviceComponent: ComponentName
) {

    val isConnected = MutableLiveData<Boolean>()
        .apply { postValue(false) }

    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }

    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        serviceComponent,
        mediaBrowserConnectionCallback, null
    ).apply { connect() }

    private lateinit var mediaController: MediaControllerCompat

    fun subscribe() {
        LogUtil.d("subscribe")
        mediaBrowser.subscribe("ff", object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String, children: List<MediaBrowserCompat.MediaItem>
            ) {
            }
        })
    }

    fun unsubscribe() {
        mediaBrowser.unsubscribe("ff")
    }

    fun prepareAndPlay(trackDownloadUri: Uri, track: Track) {
        if (mediaBrowser.isConnected) {
            val extra = Bundle().apply {
                putParcelable(PlayerService.KEY_PREPARE_TRACK, track)
            }
            mediaController.transportControls.prepareFromUri(trackDownloadUri, extra)
        }
    }

    fun pause() {
        if (mediaBrowser.isConnected) {
            mediaController.transportControls.pause()
        }
    }

    fun play() {
        if (mediaBrowser.isConnected) {
            mediaController.transportControls.play()
        }
    }

    fun setLoop(value: Boolean) {
        val repeatMode = if (value)
            PlaybackStateCompat.REPEAT_MODE_ONE
        else
            PlaybackStateCompat.REPEAT_MODE_NONE
        mediaController.transportControls.setRepeatMode(repeatMode)
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            isConnected.postValue(true)
            LogUtil.d("onConnected")
        }

        override fun onConnectionSuspended() {
            isConnected.postValue(false)
            LogUtil.d("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            isConnected.postValue(false)
            LogUtil.d("onConnectionFailed")
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
            LogUtil.d("onPlaybackStateChanged")
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            LogUtil.d("onMetadataChanged")
            nowPlaying.postValue(
                if (metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == null) {
                    NOTHING_PLAYING
                } else {
                    metadata
                }
            )
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            LogUtil.d("onQueueChanged")
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            LogUtil.d("onSessionEvent")
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
            LogUtil.d("onSessionDestroyed")
        }
    }
}

@Suppress("PropertyName")
val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()

@Suppress("PropertyName")
val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()