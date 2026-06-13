package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.RetrofitClient
import com.example.data.*
import com.example.ui.GameViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppScreen {
    MainMenu,
    WorldCreator,
    WorldLibrary,
    SimulationRoom,
    GlobalSettings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.systemBars
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        WorldNavigationContainer()
                    }
                }
            }
        }
    }
}

@Composable
fun WorldNavigationContainer(viewModel: GameViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(AppScreen.MainMenu) }

    // Load last session on startup
    LaunchedEffect(Unit) {
        viewModel.loadLastSession()
    }

    when (currentScreen) {
        AppScreen.MainMenu -> {
            MainMenuScreen(
                viewModel = viewModel,
                onNavigateToCreator = { currentScreen = AppScreen.WorldCreator },
                onNavigateToLibrary = { currentScreen = AppScreen.WorldLibrary },
                onNavigateToSettings = { currentScreen = AppScreen.GlobalSettings },
                onNavigateToSimulation = { currentScreen = AppScreen.SimulationRoom }
            )
        }
        AppScreen.WorldCreator -> {
            WorldCreatorScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MainMenu },
                onWorldCreated = { worldId ->
                    viewModel.selectWorld(worldId)
                    currentScreen = AppScreen.SimulationRoom
                }
            )
        }
        AppScreen.WorldLibrary -> {
            WorldLibraryScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MainMenu },
                onWorldSelected = { worldId ->
                    viewModel.selectWorld(worldId)
                    currentScreen = AppScreen.SimulationRoom
                }
            )
        }
        AppScreen.SimulationRoom -> {
            SimulationRoomScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MainMenu }
            )
        }
        AppScreen.GlobalSettings -> {
            GlobalSettingsScreen(
                onBack = { currentScreen = AppScreen.MainMenu }
            )
        }
    }
}

// --- SCREEN 1: MAIN MENU ---
@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onNavigateToCreator: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSimulation: () -> Unit
) {
    val activeWorld by viewModel.activeWorld.collectAsState()
    val allWorlds by viewModel.allWorlds.collectAsState()
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Large Premium Title Branding
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "WORLDSCRIBE",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )
        Text(
            text = "Dynamic Live Story Universe Engine",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Classic Menu Buttons (Continue, Load, New, Import, Settings)
        
        // 1. CONTINUE
        val hasActiveWorld = activeWorld != null
        Button(
            onClick = { if (hasActiveWorld) onNavigateToSimulation() },
            enabled = hasActiveWorld,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("menu_continue_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "CONTINUE SIMULATION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (hasActiveWorld) {
                    Text(
                        text = "Active: ${activeWorld!!.title}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. CREATE NEW WORLD
        OutlinedButton(
            onClick = onNavigateToCreator,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("menu_create_button"),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "CONJURE NEW WORLD",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. LOAD STORY FROM LIBRARY
        OutlinedButton(
            onClick = onNavigateToLibrary,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("menu_library_button"),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "WORLD LIBRARY (${allWorlds.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. IMPORT TRANSFER
        OutlinedButton(
            onClick = { showImportDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("menu_import_button"),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Input, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "IMPORT WORLD CODE",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. SETTINGS
        OutlinedButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("menu_settings_button"),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ENGINE SETTINGS",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showImportDialog) {
        ImportWorldDialog(
            onDismiss = { showImportDialog = false },
            onImportSubmit = { rawCode ->
                viewModel.importWorld(rawCode, context) { newId ->
                    viewModel.selectWorld(newId)
                    onNavigateToSimulation()
                }
                showImportDialog = false
            }
        )
    }
}

// --- SCREEN 2: WORLD CREATOR ---
@Composable
fun WorldCreatorScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onWorldCreated: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var premise by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Fantasy") }

    val genres = listOf("Fantasy", "Sci-Fi", "Cyberpunk", "Gothic Horror", "Supernatural", "Post-Apocalyptic")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Conjure Story World",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Realm/World Title") },
            placeholder = { Text("e.g. Neo-Avalon, The Glass Wastes") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("world_creator_title"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select Universe Archetype (Genre)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGenre = genre },
                    label = { Text(genre) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = premise,
            onValueChange = { premise = it },
            label = { Text("Initialize Starting Premise & Aspect") },
            placeholder = { Text("Describe the starting state, who are you, any conflicts... e.g. A deep water colony where oxygen is traded as currency, and the pressure shields are beginning to groan.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("world_creator_premise"),
            maxLines = 8,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.startNewWorld(
                    title = title,
                    premise = premise,
                    genre = selectedGenre,
                    onStarted = onWorldCreated
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("world_creator_submit"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CONJURE THE UNIVERSE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// --- SCREEN 3: WORLD LIBRARY ---
@Composable
fun WorldLibraryScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onWorldSelected: (Long) -> Unit
) {
    val allWorlds by viewModel.allWorlds.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Saved Story Worlds",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (allWorlds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No dimensions created yet. Jump back to create yours!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allWorlds) { world ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorldSelected(world.id) }
                            .testTag("world_card_${world.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = world.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = world.genre,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = world.description,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                // EXPORT BUTTON (Share)
                                TextButton(
                                    onClick = { viewModel.exportWorld(world.id, context) }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EXPORT CODE", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // DELETE BUTTON
                                TextButton(
                                    onClick = { viewModel.deleteWorld(world) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("COLLAPSE", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 4: SIMULATION ROOM (Core Dialogue Interface) ---
@Composable
fun SimulationRoomScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val activeWorld by viewModel.activeWorld.collectAsState()
    val messages by viewModel.worldMessages.collectAsState()
    val turn by viewModel.currentSimulationTurn.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errors by viewModel.errorMessage.collectAsState()

    val characters by viewModel.worldCharacters.collectAsState()
    val places by viewModel.worldPlaces.collectAsState()
    val lores by viewModel.worldLores.collectAsState()

    var playerInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Active sheet modals
    var selectedTab by remember { mutableStateOf<String?>(null) } // "characters", "places", "lores", "add"

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeWorld == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. HEADER SECTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeWorld!!.title.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "DIMENSION: ${activeWorld!!.genre}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.exportWorld(activeWorld!!.id, context) }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Dimension", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // 2. UNIVERSE ELEMENTS ACCORDION NAVIGATION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InputChip(
                    selected = selectedTab == "characters",
                    onClick = { selectedTab = if (selectedTab == "characters") null else "characters" },
                    label = { Text("👥 Characters (${characters.filter { it.status == "ALIVE" }.size} A / ${characters.filter { it.status == "DEAD" }.size} D)") }
                )
                InputChip(
                    selected = selectedTab == "places",
                    onClick = { selectedTab = if (selectedTab == "places") null else "places" },
                    label = { Text("📍 Locations (${places.size})") }
                )
                InputChip(
                    selected = selectedTab == "lores",
                    onClick = { selectedTab = if (selectedTab == "lores") null else "lores" },
                    label = { Text("📜 Lore Keys (${lores.size})") }
                )
                InputChip(
                    selected = selectedTab == "add",
                    onClick = { selectedTab = if (selectedTab == "add") null else "add" },
                    label = { Text("➕ Scribe Element") }
                )
            }

            // 3. MAIN STORY TIMELINE
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    items(messages) { msg ->
                        StoryMessageRow(msg)
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "The Universe is reacting...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (errors != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errors!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. ACTION TRAY & SUGGESTIONS BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(8.dp)
            ) {
                // SUGGESTED OPTIONS (Only display if not loading and suggestions exist)
                if (!isGenerating && turn != null && turn!!.suggestions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(turn!!.suggestions) { suggest ->
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clickable {
                                        viewModel.executePlayerAction(suggest)
                                    }
                                    .testTag("suggestion_option"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = suggest,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // FREE-FORM TEXT FIELD TRADING
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = playerInput,
                        onValueChange = { playerInput = it },
                        placeholder = { Text("Interact with the world...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulation_room_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (playerInput.isNotBlank() && !isGenerating) {
                                viewModel.executePlayerAction(playerInput)
                                playerInput = ""
                            }
                        }),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (playerInput.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                            .clickable(enabled = playerInput.isNotBlank() && !isGenerating) {
                                viewModel.executePlayerAction(playerInput)
                                playerInput = ""
                            }
                            .testTag("simulation_room_send"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit action",
                            tint = if (playerInput.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }

        // 5. FLOATING ACCORDION DETAILS SHEET
        if (selectedTab != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { selectedTab = null }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(380.dp)
                        .clickable(enabled = false) {}, // Consume clicks
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val headerTitle = when (selectedTab) {
                                "characters" -> "👥 Dynamic Characters"
                                "places" -> "📍 Discovered Regions & Places"
                                "lores" -> "📜 Lore, Concepts & Legends"
                                else -> "➕ Scribe Narrative Aspect"
                            }
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { selectedTab = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                "characters" -> CharactersSheet(characters, onToggle = { viewModel.toggleCharacterStatus(it) })
                                "places" -> PlacesSheet(places)
                                "lores" -> LoresSheet(lores)
                                "add" -> ScribeElementSheet(onSubmit = { type, titleText, descText ->
                                    when (type) {
                                        "Character" -> viewModel.addManualCharacter(titleText, "neutral", descText)
                                        "Place" -> viewModel.addManualPlace(titleText, descText)
                                        "Lore" -> viewModel.addManualLore(titleText, descText)
                                    }
                                    selectedTab = null
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUBVIEWS: STORY MESSAGE ROW ---
@Composable
fun StoryMessageRow(message: WorldMessage) {
    val isPlayer = message.sender == "PLAYER"
    val alignment = if (isPlayer) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .testTag("message_bubble_${message.id}"),
            colors = CardDefaults.cardColors(
                containerColor = if (isPlayer) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ),
            border = if (!isPlayer) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isPlayer) 16.dp else 0.dp,
                bottomEnd = if (isPlayer) 0.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                var contentText = message.text
                var speakerName: String? = null

                // Parse if it was structural JSON from World (Gemini)
                if (!isPlayer && message.text.contains("narrative")) {
                    try {
                        val turnObj = RetrofitClient.simulationTurnAdapter.fromJson(message.text)
                        if (turnObj != null) {
                            contentText = turnObj.narrative
                            speakerName = turnObj.activeCharacterName
                        }
                    } catch (e: Exception) {
                        // fallback
                    }
                }

                if (speakerName != null) {
                    Text(
                        text = "SPEAKER: $speakerName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = contentText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                // Render dynamic world updates tags
                if (!isPlayer && (message.addedCharacterNames != null || message.addedPlaceNames != null || message.addedLoreKeywords != null)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.addedCharacterNames?.split(",")?.forEach {
                            WorldDeltaTag(icon = "👥", label = "Added: ${it.trim()}")
                        }
                        message.addedPlaceNames?.split(",")?.forEach {
                            WorldDeltaTag(icon = "📍", label = "Discovered: ${it.trim()}")
                        }
                        message.addedLoreKeywords?.split(",")?.forEach {
                            WorldDeltaTag(icon = "📜", label = "Unveiled: ${it.trim()}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldDeltaTag(icon: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = icon, fontSize = 9.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- TABS & SHEETS INTERNAL LAYOUTS ---

@Composable
fun CharactersSheet(characters: List<WorldCharacter>, onToggle: (WorldCharacter) -> Unit) {
    if (characters.isEmpty()) {
        Text("No characters added yet. The engine will discover them dynamically as the story progresses!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(characters) { char ->
                val isAlive = char.status == "ALIVE"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAlive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(char.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isAlive) Color(0xFFD4EDDA) else Color(0xFFF8D7DA),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = char.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isAlive) Color(0xFF155724) else Color(0xFF721C24)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = char.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isAlive && char.historySummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Legacy: ${char.historySummary}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        // Toggle switch for alive/dead status checking
                        IconButton(onClick = { onToggle(char) }) {
                            Icon(
                                imageVector = if (isAlive) Icons.Default.Cancel else Icons.Default.CheckCircle,
                                contentDescription = "Toggle status",
                                tint = if (isAlive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlacesSheet(places: List<WorldPlace>) {
    if (places.isEmpty()) {
        Text("No locations discovered yet. Explore options to unveil regional elements!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(places) { place ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(place.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(place.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun LoresSheet(lores: List<WorldLore>) {
    if (lores.isEmpty()) {
        Text("No world secrets revealed yet. Complete tasks to add deep elements!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lores) { lore ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Concept: [${lore.keyword}]", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(lore.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ScribeElementSheet(onSubmit: (type: String, title: String, desc: String) -> Unit) {
    var selectedType by remember { mutableStateOf("Character") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val types = listOf("Character", "Place", "Lore")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedType = type }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Element Name / Keyword") },
            placeholder = { Text("e.g. Malakar, Clockwork Core") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Aspect Description") },
            placeholder = { Text("Details about this element to help the core simulation stay consistent...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            maxLines = 4
        )

        Button(
            onClick = {
                if (title.isNotBlank() && desc.isNotBlank()) {
                    onSubmit(selectedType, title, desc)
                }
            },
            enabled = title.isNotBlank() && desc.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SCRIBE ASPECT")
        }
    }
}

// --- SUBVIEWS: DIALOGS AND CONFIGURATIONS ---

@Composable
fun ImportWorldDialog(
    onDismiss: () -> Unit,
    onImportSubmit: (String) -> Unit
) {
    var pasteValue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📥 Import Shared World Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Paste the copied World JSON string from your clipboard below. The simulation engine will recreate this dimension instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pasteValue,
                    onValueChange = { pasteValue = it },
                    placeholder = { Text("Paste Shared Code String...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 10
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onImportSubmit(pasteValue) },
                        enabled = pasteValue.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("IMPORT STORY")
                    }
                }
            }
        }
    }
}

// --- SCREEN 5: SETTINGS & SECRETS CONFIGURATION ---
@Composable
fun GlobalSettingsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Engine Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔑 Safe API Credentials",
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This application utilizes Google's flagship Gemini models to direct and simulate storylines. Enter your API key securely to unleash the engine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "How to configure your API key:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Click the 'Secrets' panel in the AI Studio sidebar.\n2. Add a new variable called 'GEMINI_API_KEY'.\n3. Copy your Gemini API Token from Google AI Studio and paste it in.\n4. Click save and run the program!",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🧠 Model Specs",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current Engine: gemini-3.5-flash\nTemperature: 0.85\nMax Tokens: Ambient",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
