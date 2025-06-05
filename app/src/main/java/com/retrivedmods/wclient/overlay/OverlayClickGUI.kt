package com.retrivedmods.wclient.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrivedmods.wclient.R
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.ModuleContent
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.ui.component.NavigationRailX
import kotlinx.coroutines.launch
import kotlin.math.PI

class OverlayClickGUI : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            if (Build.VERSION.SDK_INT >= 31) {
                blurBehindRadius = 30
            }
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            dimAmount = 0.5f
            windowAnimations = android.R.style.Animation_Dialog
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private var selectedModuleCategory by mutableStateOf(ModuleCategory.Combat)

    @Composable
    override fun Content() {
        val context = LocalContext.current

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { OverlayManager.dismissOverlayWindow(this) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(0.95f)
                    .background(Color(0xFF1A1A1A), MaterialTheme.shapes.extraLarge)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), MaterialTheme.shapes.extraLarge)
                    .padding(16.dp)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color(0xFF202020), shape = MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_wclient),
                            contentDescription = "WClient Logo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        RainbowText("WClient", fontSize = 22f)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/N2Gejr8Fbp")))
                        }) {
                            Icon(painterResource(R.drawable.ic_discord), contentDescription = "Discord", tint = Color.White)
                        }
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wclient.neocities.org/")))
                        }) {
                            Icon(painterResource(R.drawable.ic_web), contentDescription = "Website", tint = Color.White)
                        }
                        IconButton(onClick = { /* Settings logic */ }) {
                            Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings", tint = Color.White)
                        }
                        IconButton(onClick = { OverlayManager.dismissOverlayWindow(this@OverlayClickGUI) }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Main GUI
                Row(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .background(Color(0xFF222222))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModuleCategory.entries.forEach { moduleCategory ->
                            val selected = selectedModuleCategory == moduleCategory
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.Red else Color.Transparent)
                                    .clickable { selectedModuleCategory = moduleCategory }
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(moduleCategory.iconResId),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    VerticalDivider(
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxHeight()
                    )

                    AnimatedContent(
                        targetState = selectedModuleCategory,
                        label = "AnimatedContent",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) { category ->
                        if (category == ModuleCategory.Config) {
                            SettingsPageContent()
                        } else {
                            ModuleContent(category)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RainbowText(text: String, fontSize: Float) {
        val transition = rememberInfiniteTransition()
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing))
        )

        val colors = List(7) { i ->
            val hue = (i * 360 / 7 + (phase * 180 / PI).toInt()) % 360
            Color.hsv(hue.toFloat(), 1f, 1f)
        }

        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                brush = Brush.horizontalGradient(colors)
            )
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsPageContent() {
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        var showFileNameDialog by remember { mutableStateOf(false) }
        var configFileName by remember { mutableStateOf("") }

        val filePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                if (ModuleManager.importConfigFromFile(context, it)) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("✅ Config imported successfully")
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("❌ Failed to import config")
                    }
                }
            }
        }

        Scaffold(
            containerColor = Color(0xFF151515),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Configuration", color = Color.White, fontSize = 22.sp)
                Text(
                    text = "Manage your WClient configurations with ease. Import your favorite setup or export your current state.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.elevatedCardElevation(8.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF202020))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ElevatedButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Import Config")
                        }

                        ElevatedButton(
                            onClick = { showFileNameDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Export Config")
                        }
                    }
                }
            }
        }

        if (showFileNameDialog) {
            AlertDialog(
                onDismissRequest = { showFileNameDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val filePath = if (ModuleManager.exportConfigToFile(context, configFileName)) {
                            context.getFileStreamPath(configFileName)?.absolutePath ?: "Unknown path"
                        } else null

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                filePath?.let { "✅ Exported to: $it" } ?: "❌ Failed to export config"
                            )
                        }

                        showFileNameDialog = false
                    }) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFileNameDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Export Config", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = configFileName,
                        onValueChange = { configFileName = it },
                        label = { Text("File name", color = Color.White) },
                        placeholder = { Text("e.g., my_config.json") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                textContentColor = Color.White
            )
        }
    }
}
