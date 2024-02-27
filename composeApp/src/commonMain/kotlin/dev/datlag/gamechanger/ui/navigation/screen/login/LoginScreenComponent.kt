package dev.datlag.gamechanger.ui.navigation.screen.login

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import dev.datlag.gamechanger.common.nullableFirebaseInstance
import dev.datlag.gamechanger.common.onRender
import dev.datlag.gamechanger.settings.Settings
import dev.datlag.tooling.async.suspendCatching
import dev.datlag.tooling.compose.withMainContext
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.kodein.di.DI
import org.kodein.di.instance

class LoginScreenComponent(
    componentContext: ComponentContext,
    override val di: DI,
    private val onFinish: () -> Unit
) : LoginComponent, ComponentContext by componentContext {

    private val appSettings: Settings.PlatformAppSettings by di.instance()
    override val emailRegex = "(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])".toRegex(RegexOption.IGNORE_CASE)

    private val firebaseApp by lazy {
        di.nullableFirebaseInstance()
    }
    override val passwordReset = MutableStateFlow(false)
    override val loggingIn = MutableStateFlow(false)

    init {
        if (firebaseApp == null) {
            onFinish()
        }

        launchIO {
            appSettings.setWelcomeCompleted(true)
        }
    }

    @Composable
    override fun render() {
        onRender {
            LoginScreen(this)
        }
    }

    override fun login(email: String, pass: String) {
        launchIO {
            firebaseApp?.let {
                loggingIn.emit(true)

                val auth = Firebase.auth(it)
                val result = suspendCatching {
                    auth.signInWithEmailAndPassword(email, pass)
                }.getOrNull()

                if (result?.user == null) {
                    suspendCatching {
                        auth.createUserWithEmailAndPassword(email, pass)
                    }.getOrNull()

                    withMainContext {
                        onFinish()
                    }
                } else {
                    withMainContext {
                        onFinish()
                    }
                }
            }
        }
    }

    override fun skip() {
        onFinish()
    }

    override fun resetPassword(email: String) {
        launchIO {
            firebaseApp?.let {
                passwordReset.emit(true)

                suspendCatching {
                    Firebase.auth(it).sendPasswordResetEmail(email = email)
                }.getOrNull()
            }
        }
    }
}