package com.sankofa.minipc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2201)
        }
        setContent { SankofaApp() }
    }
}

private data class PendingTool(
    val slug: String,
    val arguments: JsonObject,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SankofaApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = remember { DeviceProfiler.detect(context) }
    val nativeInfo = remember { NativeRuntime.info() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    var runtimeStatus by remember { mutableStateOf("Not checked") }
    var modelUrl by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("model.gguf") }
    var modelSha by remember { mutableStateOf("") }
    var modelSize by remember { mutableStateOf("") }
    var downloadStatus by remember { mutableStateOf("No download queued") }

    var gatewayUrl by remember { mutableStateOf("") }
    var gatewayToken by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var authConfigId by remember { mutableStateOf("") }
    var toolSlug by remember { mutableStateOf("GMAIL_GET_PROFILE") }
    var toolArguments by remember { mutableStateOf("{}") }
    var agentStatus by remember { mutableStateOf("Gateway not configured") }
    var pendingTool by remember { mutableStateOf<PendingTool?>(null) }

    fun gatewayClient(): GatewayClient = GatewayClient(
        baseUrl = gatewayUrl.trim(),
        bearerToken = gatewayToken,
    )

    fun executeTool(slug: String, arguments: JsonObject, approved: Boolean) {
        scope.launch {
            agentStatus = "Executing $slug…"
            runCatching {
                gatewayClient().executeTool(
                    userId = userId.trim(),
                    toolSlug = slug,
                    arguments = arguments,
                    approved = approved,
                )
            }.onSuccess { result ->
                agentStatus = "HTTP ${result.statusCode}: ${result.body.take(500)}"
            }.onFailure { error ->
                agentStatus = "Tool failed: ${error.message}"
            }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Sankofa Mini PC") }) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionCard("Phone profile") {
                    Text("Tier: ${profile.tier}")
                    Text("Android ${profile.androidVersion} · ${profile.architecture}")
                    Text("${profile.cpuCores} CPU cores · ${profile.totalRamMb} MB RAM")
                    Text("${profile.freeStorageMb} MB app storage available")
                    Text(nativeInfo)
                }

                SectionCard("Local runtime") {
                    Text(runtimeStatus)
                    Button(
                        onClick = {
                            scope.launch {
                                runtimeStatus = "Checking local Sankofa service…"
                                val (healthy, body) = RuntimeManager().localHealth()
                                runtimeStatus = if (healthy) "Healthy: $body" else "Offline: $body"
                            }
                        },
                    ) {
                        Text("Check local engine")
                    }
                }

                SectionCard("Verified model download") {
                    OutlinedTextField(
                        value = modelUrl,
                        onValueChange = { modelUrl = it },
                        label = { Text("HTTPS model URL") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("File name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = modelSha,
                        onValueChange = { modelSha = it },
                        label = { Text("SHA-256 (recommended)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = modelSize,
                        onValueChange = { modelSize = it.filter(Char::isDigit) },
                        label = { Text("Expected bytes (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            runCatching {
                                ModelDownloadWorker.enqueue(
                                    context = context,
                                    url = modelUrl.trim(),
                                    fileName = modelName.trim(),
                                    sha256 = modelSha.trim(),
                                    expectedSize = modelSize.toLongOrNull() ?: -1L,
                                )
                            }.onSuccess {
                                downloadStatus = "Download queued. It will resume after interruption."
                            }.onFailure {
                                downloadStatus = "Could not queue download: ${it.message}"
                            }
                        },
                    ) {
                        Text("Download model")
                    }
                    Text(downloadStatus)
                }

                SectionCard("Agent gateway") {
                    Text("Composio is optional. Local chat and local tools do not require it.")
                    OutlinedTextField(
                        value = gatewayUrl,
                        onValueChange = { gatewayUrl = it },
                        label = { Text("Gateway HTTPS URL") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = gatewayToken,
                        onValueChange = { gatewayToken = it },
                        label = { Text("Gateway access token") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text("Sankofa user ID") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = authConfigId,
                        onValueChange = { authConfigId = it },
                        label = { Text("Composio auth config ID") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                agentStatus = "Creating secure connection link…"
                                runCatching {
                                    gatewayClient().createConnectionLink(
                                        userId = userId.trim(),
                                        authConfigId = authConfigId.trim(),
                                    )
                                }.onSuccess { url ->
                                    agentStatus = "Connection link created"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure { error ->
                                    agentStatus = "Connection failed: ${error.message}"
                                }
                            }
                        },
                    ) {
                        Text("Connect account")
                    }

                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = toolSlug,
                        onValueChange = { toolSlug = it.uppercase() },
                        label = { Text("Composio tool slug") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = toolArguments,
                        onValueChange = { toolArguments = it },
                        label = { Text("JSON arguments") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                runCatching {
                                    json.parseToJsonElement(toolArguments).jsonObject
                                }.onSuccess { arguments ->
                                    val slug = toolSlug.trim().uppercase()
                                    when {
                                        AgentPolicy.isBlocked(slug) -> {
                                            agentStatus = "Blocked by default: destructive tool $slug"
                                        }
                                        AgentPolicy.needsApproval(slug) -> {
                                            pendingTool = PendingTool(slug, arguments)
                                        }
                                        else -> executeTool(slug, arguments, approved = false)
                                    }
                                }.onFailure {
                                    agentStatus = "Arguments must be a JSON object"
                                }
                            },
                        ) {
                            Text("Run tool")
                        }
                    }
                    Text(agentStatus)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    pendingTool?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingTool = null },
            title = { Text("Approve external action?") },
            text = {
                Text(
                    "${pending.slug} may change an external account. Review the tool and arguments before continuing."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingTool = null
                        executeTool(pending.slug, pending.arguments, approved = true)
                    },
                ) {
                    Text("Approve once")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTool = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
