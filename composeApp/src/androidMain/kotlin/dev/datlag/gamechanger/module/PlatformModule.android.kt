package dev.datlag.gamechanger.module

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import dev.datlag.gamechanger.Sekret
import dev.datlag.gamechanger.getPackageName
import dev.datlag.gamechanger.other.FirebaseFactory
import dev.datlag.gamechanger.other.StateSaver
import dev.datlag.gamechanger.settings.ApplicationSettingsSerializer
import dev.datlag.gamechanger.settings.DataStoreAppSettings
import dev.datlag.gamechanger.settings.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.kodein.di.DI
import org.kodein.di.DirectDI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

actual object PlatformModule {

    private const val NAME = "AndroidPlatformModule"

    actual val di = DI.Module(NAME) {
        bindSingleton {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        }
        bindSingleton {
            val app: Context = instance()
            DataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = ApplicationSettingsSerializer,
                    producePath = {
                        app.filesDir.toOkioPath().resolve("datastore").resolve("app.settings")
                    }
                )
            )
        }
        bindSingleton<Settings.PlatformAppSettings> {
            DataStoreAppSettings(instance())
        }
        bindSingleton<HttpClient> {
            HttpClient(OkHttp) {
                engine {
                    config {
                        followRedirects(true)
                    }
                }
                install(ContentNegotiation) {
                    json(instance(), ContentType.Application.Json)
                    json(instance(), ContentType.Text.Plain)
                }
            }
        }
        bindSingleton("RAWG_API_KEY") {
            if (StateSaver.sekretLibraryLoaded) {
                Sekret.rawg(getPackageName()) ?: ""
            } else {
                ""
            }
        }

        bindSingleton<FirebaseFactory> {
            if (StateSaver.sekretLibraryLoaded) {
                FirebaseFactory.Initialized(
                    Firebase.initialize(
                        context = instance<Context>(),
                        options = FirebaseOptions(
                            projectId = Sekret.firebaseProject(getPackageName()),
                            applicationId = Sekret.firebaseAndroidApplication(getPackageName())!!,
                            apiKey = Sekret.firebaseAndroidApiKey(getPackageName())!!
                        )
                    )
                )
            } else {
                FirebaseFactory.Empty
            }
        }
    }

}