package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ScreenState
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuizDatabaseScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.QuizViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: QuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme()
            MyApplicationTheme(darkTheme = isDark) {
                MainApp(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val state = viewModel.uiState.value

        when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                if (state.screenState == ScreenState.QUIZ) {
                    viewModel.speakCurrentQuestion()
                    return true
                }
            }
            KeyEvent.KEYCODE_1 -> {
                if (state.screenState == ScreenState.QUIZ && !state.isLocked) {
                    viewModel.checkAnswer(0)
                    return true
                }
            }
            KeyEvent.KEYCODE_2 -> {
                if (state.screenState == ScreenState.QUIZ && !state.isLocked) {
                    viewModel.checkAnswer(1)
                    return true
                }
            }
            KeyEvent.KEYCODE_3 -> {
                if (state.screenState == ScreenState.QUIZ && !state.isLocked) {
                    viewModel.checkAnswer(2)
                    return true
                }
            }
            KeyEvent.KEYCODE_4 -> {
                if (state.screenState == ScreenState.QUIZ && !state.isLocked) {
                    viewModel.checkAnswer(3)
                    return true
                }
            }
            KeyEvent.KEYCODE_N -> {
                if (state.screenState == ScreenState.QUIZ && !state.isLocked) {
                    viewModel.timeUp()
                    return true
                }
            }
            KeyEvent.KEYCODE_P -> {
                if (state.screenState == ScreenState.QUIZ) {
                    viewModel.toggleTimer()
                    return true
                }
            }
            KeyEvent.KEYCODE_S -> {
                if (state.screenState == ScreenState.QUIZ) {
                    viewModel.startQuiz()
                    return true
                }
            }
            KeyEvent.KEYCODE_H -> {
                if (state.screenState == ScreenState.RESULT) {
                    viewModel.goHome()
                    return true
                }
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                if (state.screenState != ScreenState.HOME) {
                    viewModel.goHome()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun MainApp(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when (uiState.screenState) {
            ScreenState.HOME -> {
                HomeScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenState.DATABASE -> {
                QuizDatabaseScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenState.QUIZ -> {
                QuizScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenState.RESULT -> {
                ResultScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
