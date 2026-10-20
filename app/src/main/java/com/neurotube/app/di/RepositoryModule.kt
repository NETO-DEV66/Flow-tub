package com.neurotube.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.neurotube.app.data.local.PlayerPreferences
import com.neurotube.app.data.repository.YouTubeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideYouTubeRepository(playerPreferences: PlayerPreferences): YouTubeRepository = YouTubeRepository.getInstance(playerPreferences)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        @ApplicationContext context: Context,
    ): com.neurotube.app.data.local.SubscriptionRepository =
        com.neurotube.app.data.local.SubscriptionRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideLikedVideosRepository(
        @ApplicationContext context: Context,
    ): com.neurotube.app.data.local.LikedVideosRepository =
        com.neurotube.app.data.local.LikedVideosRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideViewHistory(
        @ApplicationContext context: Context,
    ): com.neurotube.app.data.local.ViewHistory =
        com.neurotube.app.data.local.ViewHistory
            .getInstance(context)

    @Provides
    @Singleton
    fun provideHomeFeedCacheRepository(
        @ApplicationContext context: Context,
    ): com.neurotube.app.data.local.HomeFeedCacheRepository =
        com.neurotube.app.data.local
            .HomeFeedCacheRepository(context)

    @Provides
    @Singleton
    fun provideMusicPlaylistRepository(
        @ApplicationContext context: Context,
    ): com.neurotube.app.data.music.PlaylistRepository =
        com.neurotube.app.data.music
            .PlaylistRepository(context)

    // VideoDownloadManager is now @Singleton @Inject — Hilt provides it automatically
    @Provides
    @Singleton
    fun providePlayerPreferences(
        @ApplicationContext context: Context,
    ): com.neurotube.app.data.local.PlayerPreferences =
        com.neurotube.app.data.local
            .PlayerPreferences(context)

    /**
     * Transitional: [com.neurotube.app.data.local.BackupRepository] still builds its own
     * collaborators, so it is provided here rather than injected through its constructor. One
     * instance serves the whole app.
     */
    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
    ): com.neurotube.app.data.local.BackupRepository =
        com.neurotube.app.data.local
            .BackupRepository(context)
}
