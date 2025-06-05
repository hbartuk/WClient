package com.retrivedmods.wclient.router.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retrivedmods.wclient.R
import com.retrivedmods.wclient.service.Services
import com.retrivedmods.wclient.util.LocalSnackbarHostState
import com.retrivedmods.wclient.util.MinecraftUtils
import com.retrivedmods.wclient.util.SnackbarHostStateScope
import com.retrivedmods.wclient.viewmodel.MainScreenViewModel
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageContent() {
    SnackbarHostStateScope {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current
        val mainScreenViewModel: MainScreenViewModel = viewModel()
        val onPostPermissionResult: (Boolean) -> Unit = block@{ isGranted: Boolean ->
            if (!isGranted) {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.notification_permission_denied)
                    )
                }
                return@block
            }

            if (mainScreenViewModel.selectedGame.value === null) {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.select_game_first)
                    )
                }
                return@block
            }

            Services.toggle(context, mainScreenViewModel.captureModeModel.value)
        }
        val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted -> onPostPermissionResult(isGranted) }
        val overlayPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (!Settings.canDrawOverlays(context)) {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.overlay_permission_denied)
                    )
                }
                return@rememberLauncherForActivityResult
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@rememberLauncherForActivityResult
            }
            onPostPermissionResult(true)
        }
        var isActiveBefore by rememberSaveable { mutableStateOf(Services.isActive) }
        var showConnectionDialog by remember { mutableStateOf(false) }
        LaunchedEffect(Services.isActive) {
            if (Services.isActive == isActiveBefore) {
                return@LaunchedEffect
            }

            isActiveBefore = Services.isActive
            if (Services.isActive) {
                showConnectionDialog = true
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.backend_connected),
                    actionLabel = context.getString(R.string.start_game)
                )
                val selectedGame = mainScreenViewModel.selectedGame.value
                if (result == SnackbarResult.ActionPerformed && selectedGame != null) {
                    val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.failed_to_launch_game),
                        )
                    }
                }
                return@LaunchedEffect
            }

            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.backend_disconnected)
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.home))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                    )
                )
            },
            bottomBar = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier
                        .animateContentSize()
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(
                Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BackendCard()
                    IntroductionCard()
                    GameCard()
                }
                FloatingActionButton(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            Toast.makeText(
                                context,
                                R.string.request_overlay_permission,
                                Toast.LENGTH_SHORT
                            ).show()

                            overlayPermissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package: ${context.packageName}")
                                )
                            )
                            return@FloatingActionButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@FloatingActionButton
                        }

                        onPostPermissionResult(true)
                    },
                    modifier = Modifier
                        .padding(15.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    AnimatedContent(Services.isActive, label = "") { isActive ->
                        if (!isActive) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Pause,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }

        if (showConnectionDialog) {
            val ipAddress = remember {
                runCatching {
                    NetworkInterface.getNetworkInterfaces().asSequence()
                        .flatMap { it.inetAddresses.asSequence() }
                        .filterIsInstance<Inet4Address>()
                        .firstOrNull { !it.isLoopbackAddress }
                        ?.hostAddress
                }.getOrNull() ?: "127.0.0.1"
            }

            AlertDialog(
                onDismissRequest = { showConnectionDialog = false },
                title = {
                    Text(
                        "How to Connect",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("To join, go to Minecraft's Friends tab and join through LAN. If LAN doesn't show up, you can add a new server in the Servers tab by entering the IP address and port provided below, then press Play.")

                        Text(
                            "IP Address:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            ipAddress,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            "Port:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            "19132",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showConnectionDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}


@Composable
private fun BackendCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        onClick = {

        }
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Icon(
                Icons.Rounded.Whatshot,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                    .scale(0.8f)
                    .size(20.dp)
            )
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "WClient : Ultimate Cheats For MCPE",
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
                )
                Text(
                    "Seamlessly connect and manage your games with WClient's robust backend.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

@Composable
private fun IntroductionCard() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                "Welcome to WClient",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            Text(
                "Experience the ultimate gaming management solution with WClient. Optimize your gaming experience effortlessly.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Changelogs",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            Text(
                """
        - Added Criticals
        - Added Tutorials
        - Added Reach
        - Added SmartAura
        - Fixed JavaAura
        - Improved EnemyHunter
        - Improved OpFightBot
        - Improved PlayerTP
        - Added FastBreak
        - Added FastEat
        - Added Jesus
        - Improved InfiniteAura
        - Added FakeLag
        - Simplified Categories
        - Fixed Crash
        - Optimized Client
        - Use Below 1.21.60
        """.trimIndent(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun GameCard() {
    val context = LocalContext.current
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsStateWithLifecycle()
    var showGameSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var serverHostName by rememberSaveable(showGameSettingsDialog) { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by rememberSaveable(showGameSettingsDialog) { mutableStateOf(captureModeModel.serverPort.toString()) }
    var showGameSelectorDialog by remember { mutableStateOf(false) }
    val packageInfos by mainScreenViewModel.packageInfos.collectAsStateWithLifecycle()
    val packageInfoState by mainScreenViewModel.packageInfoState.collectAsStateWithLifecycle()
    val selectedGame by mainScreenViewModel.selectedGame.collectAsStateWithLifecycle()

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = {
            showGameSettingsDialog = true
        }
    ) {
        Row(
            Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.mipmap.minecraft_icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Minecraft",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(
                        R.string.recommended_version,
                        MinecraftUtils.RECOMMENDED_VERSION
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                Icons.Rounded.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .scale(0.8f)
                    .size(20.dp)
            )
        }
    }

    if (showGameSelectorDialog) {
        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            mainScreenViewModel.fetchPackageInfos()
        }

        BasicAlertDialog(
            onDismissRequest = {
                showGameSelectorDialog = false
            },
            modifier = Modifier
                .padding(vertical = 24.dp),
            content = {
                Surface(
                    shape = AlertDialogDefaults.shape,
                    tonalElevation = AlertDialogDefaults.TonalElevation
                ) {
                    Column(
                        Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Select Your Game",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        LazyColumn(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            item {
                                if (packageInfoState === MainScreenViewModel.PackageInfoState.Loading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .padding(vertical = 16.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                            items(packageInfos.size) {
                                val packageInfo = packageInfos[it]
                                val applicationInfo = packageInfo.applicationInfo!!
                                val packageManager = context.packageManager
                                val icon = remember {
                                    applicationInfo.loadIcon(packageManager).toBitmap()
                                        .asImageBitmap()
                                }
                                val name = remember {
                                    applicationInfo.loadLabel(packageManager).toString()
                                }
                                val packageName = packageInfo.packageName
                                val versionName = packageInfo.versionName ?: "0.0.0"
                                Card(
                                    onClick = {
                                        mainScreenViewModel.selectGame(packageName)
                                        showGameSelectorDialog = false
                                    },
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            bitmap = icon,
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier
                                                .size(24.dp)
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                versionName,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showGameSettingsDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                showGameSettingsDialog = false
            },
            modifier = Modifier
                .padding(vertical = 24.dp),
            content = {
                Surface(
                    shape = AlertDialogDefaults.shape,
                    tonalElevation = AlertDialogDefaults.TonalElevation
                ) {
                    Column(
                        Modifier
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Game Settings",
                            modifier = Modifier
                                .align(Alignment.Start),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            if (isPressed) {
                                SideEffect {
                                    showGameSelectorDialog = true
                                }
                            }
                            TextField(
                                value = selectedGame ?: "",
                                onValueChange = {},
                                readOnly = true,
                                maxLines = 1,
                                label = {
                                    Text("Select Game")
                                },
                                placeholder = {
                                    Text("No game selected")
                                },
                                interactionSource = interactionSource,
                                enabled = !Services.isActive
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TextField(
                                    value = serverHostName,
                                    label = {
                                        Text("Server Host Name")
                                    },
                                    onValueChange = {
                                        serverHostName = it

                                        if (it.isEmpty()) return@TextField
                                        mainScreenViewModel.selectCaptureModeModel(
                                            captureModeModel.copy(serverHostName = it)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    enabled = !Services.isActive
                                )
                                TextField(
                                    value = serverPort,
                                    label = {
                                        Text("Server Port")
                                    },
                                    onValueChange = {
                                        serverPort = it

                                        if (it.isEmpty()) return@TextField
                                        val port = it.toIntOrNull() ?: return@TextField
                                        if (port < 0 || port > 65535) return@TextField

                                        mainScreenViewModel.selectCaptureModeModel(
                                            captureModeModel.copy(serverPort = port)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    singleLine = true,
                                    enabled = !Services.isActive
                                )
                            }
                            if (Services.isActive) {
                                Card(
                                    modifier = Modifier
                                        .width(TextFieldDefaults.MinWidth),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                "Tips",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                "Change game settings while the service is inactive.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
