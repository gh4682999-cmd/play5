package com.example.melonvinyl

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat

/**
 * 안드로이드 오토에 '미디어 앱'으로 나타나는 서비스.
 * 폰에서 재생 중인 멜론의 곡 정보/상태를 그대로 복사(미러링)해서 오토에 보여주고,
 * 오토의 재생·일시정지·이전·다음 버튼을 멜론으로 전달합니다. (음원은 재생하지 않음)
 * 오토 화면 UI는 구글 템플릿이라 레코드 애니메이션은 표시할 수 없고,
 * 대신 앨범 이미지를 자켓+레코드+그라데이션 합성 이미지(CoverComposer)로 바꿔서 보여줍니다.
 */
class MelonMirrorService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat
    private var melon: MediaController? = null
    private var listening = false
    private val melonPkg = "com.iloen.melon"

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = mirror()
        override fun onPlaybackStateChanged(state: PlaybackState?) = mirror()
        override fun onSessionDestroyed() { melon = null; mirror() }
    }
    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, "MelonMirror").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { melon?.transportControls?.play() }
                override fun onPause() { melon?.transportControls?.pause() }
                override fun onSkipToNext() { melon?.transportControls?.skipToNext() }
                override fun onSkipToPrevious() { melon?.transportControls?.skipToPrevious() }
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    melon?.transportControls?.play()
                }
            })
            isActive = true
        }
        sessionToken = session.sessionToken
        mirror()
        connectMelon()
    }

    override fun onDestroy() {
        melon?.unregisterCallback(cb)
        if (listening) {
            getSystemService(MediaSessionManager::class.java)
                .removeOnActiveSessionsChangedListener(sessionsListener)
        }
        session.release()
        super.onDestroy()
    }

    private fun connectMelon() {
        try {
            val msm = getSystemService(MediaSessionManager::class.java)
            val cn = ComponentName(this, NowPlayingListener::class.java)
            msm.addOnActiveSessionsChangedListener(sessionsListener, cn)
            listening = true
            attach(msm.getActiveSessions(cn))
        } catch (e: SecurityException) {
            mirror() // 알림 접근 권한이 꺼져 있으면 안내 메시지를 표시
        }
    }

    private fun attach(list: List<MediaController>?) {
        val next = list?.firstOrNull { it.packageName == melonPkg }
        melon?.unregisterCallback(cb)
        melon = next
        next?.registerCallback(cb)
        mirror()
    }

    private var lastMetaKey: String? = null

    /** 곡이 바뀔 때만, 앨범 이미지를 '자켓+레코드+그라데이션' 합성 이미지로 바꿔서 오토에 전달 */
    private fun pushMetadata(md: MediaMetadata) {
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val key = "$title|$artist|${art != null}|${md.getLong(MediaMetadata.METADATA_KEY_DURATION)}"
        if (key == lastMetaKey) return
        lastMetaKey = key
        val b = MediaMetadataCompat.Builder(MediaMetadataCompat.fromMediaMetadata(md))
        if (art != null) {
            val cover = CoverComposer.compose(art)
            b.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, cover)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, cover)
        }
        session.setMetadata(b.build())
    }

    private fun mirror() {
        val c = melon
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        val md = c?.metadata
        if (md != null) pushMetadata(md)
        val st = c?.playbackState
        val state = if (c != null && st != null) {
            PlaybackStateCompat.Builder(PlaybackStateCompat.fromPlaybackState(st))
                .setActions(actions).build()
        } else {
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
                .setErrorMessage(
                    PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                    "폰에서 멜론을 재생하고, 앱의 알림 접근 권한을 켜 주세요"
                )
                .setActions(actions).build()
        }
        session.setPlaybackState(state)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot =
        BrowserRoot("root", null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val d = MediaDescriptionCompat.Builder()
            .setMediaId("melon")
            .setTitle("멜론 재생 정보 보기")
            .build()
        result.sendResult(
            mutableListOf(MediaBrowserCompat.MediaItem(d, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        )
    }
}
