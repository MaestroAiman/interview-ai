package com.pfa.interview.di

import com.pfa.interview.data.repository.AdminRepository
import com.pfa.interview.data.repository.AdminRepositoryImpl
import com.pfa.interview.data.repository.AuthRepository
import com.pfa.interview.data.repository.AuthRepositoryImpl
import com.pfa.interview.data.repository.CvRepository
import com.pfa.interview.data.repository.CvRepositoryImpl
import com.pfa.interview.data.repository.HealthRepository
import com.pfa.interview.data.repository.HealthRepositoryImpl
import com.pfa.interview.data.repository.HistoryRepository
import com.pfa.interview.data.repository.HistoryRepositoryImpl
import com.pfa.interview.data.repository.SessionRepository
import com.pfa.interview.data.repository.SessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository

    @Binds
    @Singleton
    abstract fun bindCvRepository(impl: CvRepositoryImpl): CvRepository
}
