package com.ruuvi.station.network.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.onboarding.ui.OnboardingSubTitle
import com.ruuvi.station.onboarding.ui.OnboardingText
import com.ruuvi.station.onboarding.ui.OnboardingTitle
import com.ruuvi.station.settings.ui.*
import com.ruuvi.station.util.extensions.navigate
import com.ruuvi.station.util.extensions.scaledSp
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

@OptIn(ExperimentalAnimationApi::class)
class SignInActivity: AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by closestKodein()

    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RuuviTheme() {
                val navController = rememberAnimatedNavController()
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val activity = LocalContext.current as Activity
                var title: String by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        title = SettingsRoutes.getTitleByRoute(
                            activity,
                            backStackEntry.destination.route ?: ""
                        )
                    }
                }

                GlideImage(
                    modifier = Modifier.fillMaxSize(),
                    model = rememberResourceUri(R.drawable.onboarding_bg_dark),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = Color.Transparent,
                    scaffoldState = scaffoldState
                ) { padding ->
                    AnimatedNavHost(navController = navController, startDestination = SignInRoutes.ENTER_EMAIL) {
                        composable(
                            SignInRoutes.ENTER_EMAIL,
                            enterTransition = { slideIntoContainer(towards = AnimatedContentScope.SlideDirection.Right, animationSpec = tween(600)) },
                            exitTransition = { slideOutOfContainer(towards = AnimatedContentScope.SlideDirection.Left, animationSpec = tween(600)) },
                        ) {
                            EnterEmailPage(
                                {},
                                { navController.navigate(UiEvent.Navigate(SignInRoutes.CLOUD_BENEFITS)) }
                            )
                        }
                        composable(
                            route = SignInRoutes.CLOUD_BENEFITS,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {

                        }
                        composable(
                            SignInRoutes.ENTER_CODE,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {

                        }
                    }
                }

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = false
                    )
                }
            }
        }
    }

    private val enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?) =
        { slideIntoContainer(
            towards = AnimatedContentScope.SlideDirection.Left,
            animationSpec = tween(600)
        ) }
    private val exitTransition:  (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?) =
        { slideOutOfContainer(
            towards = AnimatedContentScope.SlideDirection.Right,
            animationSpec = tween(600)
        ) }

    companion object {
        fun start(context: Context) {
            val settingsIntent = Intent(context, SignInActivity::class.java)
            context.startActivity(settingsIntent)
        }
    }
}

@Composable
fun EnterEmailPage(
    requestCode: (String) -> Unit,
    useWithoutAccount: () -> Unit
) {
    var email by remember{ mutableStateOf("") }


    Column(
        modifier = Modifier.systemBarsPadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        OnboardingTitle(
            text = stringResource(id = R.string.sign_in_or_create_free_account),
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.big))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingSubTitle(text = stringResource(id = R.string.to_use_all_app_features))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SignInTextFieldRuuvi(value = email, hint = stringResource(id = R.string.type_your_email), onValueChange = { email = it })
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        RuuviButton(text = stringResource(id = R.string.request_code)) {
        }
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        OnboardingText(text = stringResource(id = R.string.no_password_needed))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended).clickable { useWithoutAccount.invoke() },
                style = RuuviStationTheme.typography.onboardingSubtitle,
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.use_without_account),
                textDecoration = TextDecoration.Underline,
                fontSize = 16.scaledSp,
                lineHeight = 20.scaledSp
            )
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }
    }
}

@Composable
fun SignInTextFieldRuuvi(
    modifier: Modifier = Modifier,
    value: String,
    label: String? = null,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (String) -> Unit,
) {
    val labelFun: @Composable (() -> Unit)? = if (label != null) { { Paragraph(text = label) } } else null
    val hintFun:@Composable (() -> Unit)? = if (hint != null) { { Paragraph(text = hint) } } else null

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = labelFun,
        placeholder = hintFun,
        textStyle = RuuviStationTheme.typography.paragraph,
        colors = OnboardingTextFieldColors(),
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true
    )
}

@Composable
fun OnboardingTextFieldColors() = TextFieldDefaults.textFieldColors(
    backgroundColor = Color.Transparent,
    cursorColor = RuuviStationTheme.colors.accent,
    trailingIconColor = RuuviStationTheme.colors.accent,
    focusedIndicatorColor = RuuviStationTheme.colors.accent,
    unfocusedIndicatorColor = RuuviStationTheme.colors.trackColor
)