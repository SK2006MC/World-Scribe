package com.example.ui

import android.app.Application
import android.widget.Toast
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ClipboardManager
import android.content.ClipData
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class WorldExportModel(
    val world: SimulationWorld,
    val characters: List<WorldCharacter>,
    val places: List<WorldPlace>,
    val lores: List<WorldLore>,
    val messages: List<WorldMessage>
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GameDatabase.getDatabase(application)
    private val dao = database.simulationDao

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeWorld = MutableStateFlow<SimulationWorld?>(null)
    val activeWorld: StateFlow<SimulationWorld?> = _activeWorld.asStateFlow()

    private val _currentSimulationTurn = MutableStateFlow<SimulationTurnResponse?>(null)
    val currentSimulationTurn: StateFlow<SimulationTurnResponse?> = _currentSimulationTurn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Database Flows
    val allWorlds: StateFlow<List<SimulationWorld>> = dao.getAllWorlds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _worldMessages = MutableStateFlow<List<WorldMessage>>(emptyList())
    val worldMessages: StateFlow<List<WorldMessage>> = _worldMessages.asStateFlow()

    private val _worldCharacters = MutableStateFlow<List<WorldCharacter>>(emptyList())
    val worldCharacters: StateFlow<List<WorldCharacter>> = _worldCharacters.asStateFlow()

    private val _worldPlaces = MutableStateFlow<List<WorldPlace>>(emptyList())
    val worldPlaces: StateFlow<List<WorldPlace>> = _worldPlaces.asStateFlow()

    private val _worldLores = MutableStateFlow<List<WorldLore>>(emptyList())
    val worldLores: StateFlow<List<WorldLore>> = _worldLores.asStateFlow()

    private var activeCollectorJob: kotlinx.coroutines.Job? = null

    // Initialize/Load Active World
    fun selectWorld(worldId: Long) {
        activeCollectorJob?.cancel()
        activeCollectorJob = viewModelScope.launch {
            val world = dao.getWorldById(worldId)
            if (world != null) {
                _activeWorld.value = world
                _errorMessage.value = null

                // Launch collectors for individual world entities
                launch {
                    dao.getMessagesForWorld(worldId).collect { msgs ->
                        _worldMessages.value = msgs
                        // Parse last world response to keep simulation turn in memory
                        val lastWorldMsg = msgs.lastOrNull { it.sender == "WORLD" }
                        if (lastWorldMsg != null) {
                            try {
                                _currentSimulationTurn.value = RetrofitClient.simulationTurnAdapter.fromJson(lastWorldMsg.text)
                            } catch (e: Exception) {
                                Log.e("GameViewModel", "Error parsing saved turn: ${e.message}")
                            }
                        } else if (msgs.isEmpty()) {
                            // Brand new world, generate first introduction!
                            generateWorldIntroduction(world)
                        }
                    }
                }

                launch {
                    dao.getCharactersForWorld(worldId).collect { chars ->
                        _worldCharacters.value = chars
                    }
                }

                launch {
                    dao.getPlacesForWorld(worldId).collect { places ->
                        _worldPlaces.value = places
                    }
                }

                launch {
                    dao.getLoreForWorld(worldId).collect { lores ->
                        _worldLores.value = lores
                    }
                }
            }
        }
    }

    // Initialize Last Active Session on Startup
    fun loadLastSession() {
        viewModelScope.launch {
            val latest = dao.getLatestActiveWorld()
            if (latest != null) {
                selectWorld(latest.id)
            }
        }
    }

    // World & Elements Creators
    fun startNewWorld(title: String, premise: String, genre: String, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val world = SimulationWorld(
                title = title.ifBlank { "Uncharted Realm" },
                description = premise.ifBlank { "A mysterious dynamic story world beginning to unfold." },
                genre = genre
            )
            val worldId = dao.insertWorld(world)
            onStarted(worldId)
        }
    }

    fun deleteWorld(world: SimulationWorld) {
        viewModelScope.launch {
            dao.deleteWorld(world)
            if (_activeWorld.value?.id == world.id) {
                _activeWorld.value = null
                _currentSimulationTurn.value = null
                _worldMessages.value = emptyList()
                _worldCharacters.value = emptyList()
                _worldPlaces.value = emptyList()
                _worldLores.value = emptyList()
            }
        }
    }

    // Manual Aspects Introductions (Lego brick elements)
    fun addManualCharacter(name: String, role: String, description: String) {
        val world = _activeWorld.value ?: return
        viewModelScope.launch {
            val element = WorldCharacter(
                worldId = world.id,
                name = name,
                role = role,
                description = description
            )
            dao.insertCharacter(element)
        }
    }

    fun addManualPlace(name: String, description: String) {
        val world = _activeWorld.value ?: return
        viewModelScope.launch {
            val element = WorldPlace(
                worldId = world.id,
                name = name,
                description = description
            )
            dao.insertPlace(element)
        }
    }

    fun addManualLore(keyword: String, description: String) {
        val world = _activeWorld.value ?: return
        viewModelScope.launch {
            val element = WorldLore(
                worldId = world.id,
                keyword = keyword,
                description = description
            )
            dao.insertLore(element)
        }
    }

    // Toggle Character Status
    fun toggleCharacterStatus(character: WorldCharacter) {
        viewModelScope.launch {
            val newStatus = if (character.status == "ALIVE") "DEAD" else "ALIVE"
            val updated = character.copy(
                status = newStatus,
                historySummary = if (newStatus == "DEAD" && character.historySummary.isBlank()) {
                    "This character came to an end in the story line, leaving behind an indelible mark on history."
                } else character.historySummary
            )
            dao.updateCharacter(updated)
        }
    }

    // First Turn: World Introduction Creation
    private fun generateWorldIntroduction(world: SimulationWorld) {
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val sysPrompt = buildSystemPrompt(world, emptyList(), emptyList(), emptyList())
            val introPrompt = """
                The story begins.
                Configure initial opening narrative based on World: ${world.title} (${world.genre}).
                Premise: ${world.description}

                World Engine, generate the opening paragraph of our simulation. Establish the central setting, mood, and prompt 3 intriguing choices/paths to begin interacting.
            """.trimIndent()

            val contents = listOf(Content(role = "user", parts = listOf(Part(text = introPrompt))))
            makeWorldApiCall(sysPrompt, contents, world.id)
        }
    }

    // Player action execution turn
    fun executePlayerAction(actionText: String) {
        val world = _activeWorld.value ?: return
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // Save player's action
            val playerMsg = WorldMessage(
                worldId = world.id,
                sender = "PLAYER",
                text = actionText
            )
            dao.insertMessage(playerMsg)

            // Compile the whole message history to feed to Gemini
            val history = dao.getMessagesListForWorld(world.id)
            val reqContents = history.map { msg ->
                val role = if (msg.sender == "PLAYER") "user" else "model"
                Content(role = role, parts = listOf(Part(text = msg.text)))
            }

            // Read absolute latest state of universe entities
            val chars = dao.getCharactersListForWorld(world.id)
            val places = dao.getPlacesListForWorld(world.id)
            val lores = dao.getLoreListForWorld(world.id)

            val sysPrompt = buildSystemPrompt(world, chars, places, lores)
            makeWorldApiCall(sysPrompt, reqContents, world.id)
        }
    }

    private suspend fun makeWorldApiCall(systemPrompt: String, dialogueHistory: List<Content>, worldId: Long) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _isGenerating.value = false
            _errorMessage.value = "Gemini API key is not configured in Secrets! Please click the Secrets panel in AI Studio and add GEMINI_API_KEY."
            return
        }

        try {
            val request = GenerateContentRequest(
                contents = dialogueHistory,
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.85f
                )
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (jsonText != null) {
                // Parse the response turn
                val turn = RetrofitClient.simulationTurnAdapter.fromJson(jsonText)
                if (turn != null) {
                    _currentSimulationTurn.value = turn

                    var addedChars: MutableList<String> = mutableListOf()
                    var addedLocs: MutableList<String> = mutableListOf()
                    var addedConcepts: MutableList<String> = mutableListOf()

                    // Apply newly introduced or updated world components
                    turn.introducedCharacters?.forEach {
                        dao.insertCharacter(WorldCharacter(worldId = worldId, name = it.name, role = it.role, description = it.description))
                        addedChars.add(it.name)
                    }

                    turn.introducedPlaces?.forEach {
                        dao.insertPlace(WorldPlace(worldId = worldId, name = it.name, description = it.description))
                        addedLocs.add(it.name)
                    }

                    turn.introducedLores?.forEach {
                        dao.insertLore(WorldLore(worldId = worldId, keyword = it.keyword, description = it.description))
                        addedConcepts.add(it.keyword)
                    }

                    turn.characterStatusUpdates?.forEach { update ->
                        val existingChars = dao.getCharactersListForWorld(worldId)
                        val match = existingChars.firstOrNull { it.name.contains(update.name, ignoreCase = true) }
                        if (match != null) {
                            dao.updateCharacter(match.copy(
                                status = update.status,
                                historySummary = update.historySummary ?: match.historySummary
                            ))
                        }
                    }

                    // Save the WORLD narration response
                    val recordMsg = WorldMessage(
                        worldId = worldId,
                        sender = "WORLD",
                        text = jsonText,
                        addedCharacterNames = if (addedChars.isNotEmpty()) addedChars.joinToString(", ") else null,
                        addedPlaceNames = if (addedLocs.isNotEmpty()) addedLocs.joinToString(", ") else null,
                        addedLoreKeywords = if (addedConcepts.isNotEmpty()) addedConcepts.joinToString(", ") else null
                    )
                    dao.insertMessage(recordMsg)

                } else {
                    _errorMessage.value = "Failed to synchronize the structure of the world narration."
                }
            } else {
                _errorMessage.value = "The world remains static and silent. No response."
            }
        } catch (e: Exception) {
            _errorMessage.value = "World simulation connectivity lost: ${e.message}"
            Log.e("GameViewModel", "API Exception ", e)
        } finally {
            _isGenerating.value = false
        }
    }

    private fun buildSystemPrompt(
        world: SimulationWorld,
        characters: List<WorldCharacter>,
        places: List<WorldPlace>,
        lores: List<WorldLore>
    ): String {
        val aliveChars = characters.filter { it.status == "ALIVE" }
            .joinToString("\n") { "- ${it.name} (${it.role}): ${it.description}" }

        val deadChars = characters.filter { it.status == "DEAD" }
            .joinToString("\n") { "- ${it.name} (DEAD - History Summary: ${it.historySummary})" }

        val discoveredPlaces = places.joinToString("\n") { "- ${it.name}: ${it.description}" }

        val worldLoresList = lores.joinToString("\n") { "- Concept [${it.keyword}]: ${it.description}" }

        return """
            You are the "World Engine" - a dynamic, responsive interactive narrative director (AI Dungeon style).
            Current World Title: ${world.title}
            Genre: ${world.genre}
            World Premise & Background: ${world.description}

            === KNOWN CHARACTERS (ALIVE) ===
            ${if (aliveChars.isEmpty()) "None. You can introduce new characters." else aliveChars}

            === DEAD CHARACTERS (HISTORICAL MEMORY) ===
            ${if (deadChars.isEmpty()) "None." else deadChars}

            === DISCOVERED PLACES ===
            ${if (discoveredPlaces.isEmpty()) "None. You can introduce new places." else discoveredPlaces}

            === DEEP WORLD LORE & CONCEPTS ===
            ${if (worldLoresList.isEmpty()) "None." else worldLoresList}

            === INSTRUCTIONS ===
            1. Respond in character as the World. Directly narrate the player's choices/actions consequences with evocative, descriptive prose.
            2. Adapt your narrative based on the characters that are active (ALIVE), the places discovered, and the active lore.
            3. *Dynamic Universe Generation*: You can expand the world dynamically based on story progression:
               - To introduce new characters, add them to "introducedCharacters" (fill name, role: villain/heroine/friend/neutral, description).
               - To discover new places, add them to "introducedPlaces" (name, description).
               - To create new lore concepts or legends, add them to "introducedLores" (keyword, description).
               - To update a character's status (such as a villain dying or heroine's arc ending), add them to "characterStatusUpdates" with status "DEAD", and write a brief "historySummary" highlighting their legacy/death.
            4. Keep the narrative under 180 words. Propose exactly 3 highly interesting next suggestions/choices for the player.
            5. Ensure your JSON response strictly matches this schema (do NOT surround it with markdown, just direct JSON text):
            {
              "narrative": "Story text...",
              "suggestions": ["Option A...", "Option B...", "Option C..."],
              "activeCharacterName": "Name of character currently interacting with the player or central in this scene (or null if none)",
              "introducedCharacters": [  // optional, only if introducing new characters
                {"name": "...", "role": "villain/heroine/friend/neutral", "description": "..."}
              ],
              "introducedPlaces": [      // optional, only if discovering locations
                {"name": "...", "description": "..."}
              ],
              "introducedLores": [       // optional, only if introducing deep lore
                {"keyword": "...", "description": "..."}
              ],
              "characterStatusUpdates": [ // optional, to update character status (e.g., mark as DEAD)
                {"name": "...", "status": "DEAD", "historySummary": "..."}
              ]
            }
        """.trimIndent()
    }

    // --- Transfer / Share Capabilities ---

    fun exportWorld(worldId: Long, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val world = dao.getWorldById(worldId) ?: return@launch
                val characters = dao.getCharactersListForWorld(worldId)
                val places = dao.getPlacesListForWorld(worldId)
                val lores = dao.getLoreListForWorld(worldId)
                val messages = dao.getMessagesListForWorld(worldId)

                val exportData = WorldExportModel(
                    world = world,
                    characters = characters,
                    places = places,
                    lores = lores,
                    messages = messages
                )

                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(WorldExportModel::class.java)
                val jsonText = adapter.toJson(exportData)

                // Copy to Clipboard
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("World Export", jsonText)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, "World exported & copied to clipboard! Share it anywhere.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importWorld(jsonText: String, context: android.content.Context, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(WorldExportModel::class.java)
                val imported = adapter.fromJson(jsonText) ?: throw Exception("Invalid format")

                val oldWorld = imported.world
                val newWorld = oldWorld.copy(id = 0, title = "${oldWorld.title} (Imported)", createdAt = System.currentTimeMillis())
                val newWorldId = dao.insertWorld(newWorld)

                imported.characters.forEach {
                    dao.insertCharacter(it.copy(id = 0, worldId = newWorldId))
                }

                imported.places.forEach {
                    dao.insertPlace(it.copy(id = 0, worldId = newWorldId))
                }

                imported.lores.forEach {
                    dao.insertLore(it.copy(id = 0, worldId = newWorldId))
                }

                imported.messages.forEach {
                    dao.insertMessage(it.copy(id = 0, worldId = newWorldId))
                }

                onDone(newWorldId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "World imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: Invalid world code format", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
