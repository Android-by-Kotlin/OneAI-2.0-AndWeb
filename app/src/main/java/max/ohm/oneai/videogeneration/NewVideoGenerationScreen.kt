package max.ohm.oneai.videogeneration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import max.ohm.oneai.utils.SetStatusBarColor
import max.ohm.oneai.utils.StatusBarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewVideoGenerationScreen(navController: NavController) {
    val viewModel: NewVideoGenerationViewModel = viewModel()
    val state = viewModel.state.collectAsState().value
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var prompt by remember { mutableStateOf("") }

    // Set status bar color for video generation screen
    SetStatusBarColor(StatusBarUtils.VideoGenerationStatusBarColor)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Enter Video Prompt") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dropdown for model selection
            var expanded by remember { mutableStateOf(false) }
            var selectedModel by remember { mutableStateOf("Select Model") }
            val models = listOf("Model A", "Model B", "Model C")

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { expanded = !expanded }) {
                    Text(selectedModel)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                selectedModel = model
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.generateVideo(prompt, selectedModel)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                Text("Generate Video")
            }

            if (state.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (state.videoUrl != null) {
                LaunchedEffect(state.videoUrl) {
                    // Navigate to video player screen with properly encoded URL
                    val encodedUrl = java.net.URLEncoder.encode(state.videoUrl, "UTF-8")
                    navController.navigate("videoPlayer?videoUrl=$encodedUrl")
                }
            }

            if (state.error != null) {
                LaunchedEffect(state.error) {
                    scope.launch {
                        snackbarHostState.showSnackbar(state.error)
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}
