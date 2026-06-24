package com.wilddeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wilddeck.app.data.SampleData
import com.wilddeck.app.domain.MiniGameManager
import com.wilddeck.app.ui.screens.CardDetailScreen
import com.wilddeck.app.ui.screens.CollectionScreen
import com.wilddeck.app.ui.screens.DeckBuilderScreen
import com.wilddeck.app.ui.screens.FrameCustomizationScreen
import com.wilddeck.app.ui.screens.FrameStoreScreen
import com.wilddeck.app.ui.screens.HomeScreen
import com.wilddeck.app.ui.screens.MiniGameScreen
import com.wilddeck.app.ui.screens.CombatScreen
import com.wilddeck.app.ui.theme.WildDeckTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WildDeckTheme {
                WildDeckApp()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val COLLECTION = "collection"
    const val DECKS = "decks"
    const val GAME = "game"
    const val COMBAT = "combat"
    const val DETAIL = "detail/{cardId}"
    const val FRAMES = "frames?cardId={cardId}"
    const val FRAME_STORE = "frame_store"

    fun detail(cardId: String) = "detail/$cardId"
    fun frames(cardId: String? = null) = "frames?cardId=${cardId.orEmpty()}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WildDeckApp(viewModel: WildDeckViewModel = viewModel()) {
    val state = viewModel.uiState
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val isHome = currentRoute == Routes.HOME
    val framesById = state.frames.associateBy { it.id }
    val learningTriviaByCardId = remember(state.catalog) {
        val manager = MiniGameManager(state.catalog, Random(0))
        state.catalog.associate { card ->
            card.id to manager.createQuestions(card)
                .sortedWith(compareBy({ it.difficulty.ordinal }, { it.id }))
        }
    }

    Scaffold(
        topBar = {
            if (!isHome) {
                TopAppBar(
                    title = { Text(screenTitle(currentRoute)) },
                    navigationIcon = {
                        TextButton(onClick = { navController.popBackStack() }) { Text("← Back") }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                if (state.reducedMotion) EnterTransition.None else fadeIn() + slideInHorizontally { it / 5 }
            },
            exitTransition = {
                if (state.reducedMotion) ExitTransition.None else fadeOut() + slideOutHorizontally { -it / 5 }
            },
            popEnterTransition = {
                if (state.reducedMotion) EnterTransition.None else fadeIn() + slideInHorizontally { -it / 5 }
            },
            popExitTransition = {
                if (state.reducedMotion) ExitTransition.None else fadeOut() + slideOutHorizontally { it / 5 }
            }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    ownedCount = state.ownedCards.size,
                    lockedCount = state.catalog.size - state.ownedCards.size,
                    deckCount = state.decks.size,
                    progressionPoints = state.progressionPoints,
                    catalog = state.catalog,
                    ownedCards = state.ownedCards,
                    decks = state.decks,
                    frames = state.frames,
                    framesById = framesById,
                    unlockedFrameIds = state.unlockedFrameIds,
                    frameCost = viewModel::frameUnlockCost,
                    learningTriviaByCardId = learningTriviaByCardId,
                    humanRelationshipNotes = SampleData.humanRelationshipNotes,
                    onPlay = { navController.navigate(Routes.GAME) },
                    onCombat = { navController.navigate(Routes.COMBAT) },
                    onOpenCard = { navController.navigate(Routes.detail(it)) },
                    onCreateDeck = viewModel::createDeck,
                    onAddToDeck = viewModel::addCardToDeck,
                    onRemoveFromDeck = viewModel::removeCardFromDeck,
                    onBuyFrame = viewModel::unlockFrame,
                    onCustomizeFrames = { navController.navigate(Routes.frames()) }
                )
            }
            composable(Routes.COLLECTION) {
                CollectionScreen(
                    cards = state.ownedCards,
                    framesById = framesById,
                    decks = state.decks,
                    onOpenCard = { navController.navigate(Routes.detail(it)) },
                    onAddToDeck = viewModel::addCardToDeck,
                    onPlay = { navController.navigate(Routes.GAME) }
                )
            }
            composable(Routes.DECKS) {
                DeckBuilderScreen(
                    decks = state.decks,
                    ownedCards = state.ownedCards,
                    relationshipsFor = viewModel::relationshipsFor,
                    onCreate = viewModel::createDeck,
                    onRename = viewModel::renameDeck,
                    onDelete = viewModel::deleteDeck,
                    onAdd = viewModel::addCardToDeck,
                    onRemove = viewModel::removeCardFromDeck
                )
            }
            composable(Routes.GAME) {
                val session = state.miniGameSession
                MiniGameScreen(
                    session = session,
                    frame = session?.targetCard?.currentFrameId?.let(framesById::get),
                    feedback = state.miniGameFeedback,
                    points = state.progressionPoints,
                    entryCost = WildDeckViewModel.MINI_GAME_COST,
                    onStart = viewModel::startMiniGame,
                    onAnswer = viewModel::answerTrivia,
                    onCollection = { navController.navigate(Routes.COLLECTION) }
                )
            }
            composable(Routes.COMBAT) {
                CombatScreen(
                    session = state.combatSession,
                    effects = state.combatEffects,
                    effectSequence = state.combatEffectSequence,
                    points = state.progressionPoints,
                    decks = state.decks,
                    ownedCards = state.ownedCards,
                    lockedFrames = state.frames.filterNot { it.id in state.unlockedFrameIds },
                    reducedMotion = state.reducedMotion,
                    soundEnabled = state.soundEnabled,
                    hapticsEnabled = state.hapticsEnabled,
                    frameCost = viewModel::frameUnlockCost,
                    onStart = viewModel::startCombat,
                    onAction = viewModel::performCombatAction,
                    onNextRound = viewModel::nextCombatRound,
                    onEndRun = viewModel::endCombatRun,
                    onUnlockFrame = viewModel::unlockFrame,
                    onReducedMotion = viewModel::setReducedMotion,
                    onSound = viewModel::setSoundEnabled,
                    onHaptics = viewModel::setHapticsEnabled
                )
            }
            composable(Routes.FRAME_STORE) {
                FrameStoreScreen(
                    frames = state.frames,
                    unlockedFrameIds = state.unlockedFrameIds,
                    points = state.progressionPoints,
                    frameCost = viewModel::frameUnlockCost,
                    onBuy = viewModel::unlockFrame,
                    onCustomize = { navController.navigate(Routes.frames()) }
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("cardId") { type = NavType.StringType })
            ) { entry ->
                val cardId = entry.arguments?.getString("cardId")
                val card = viewModel.card(cardId)
                val relationships = card?.let {
                    state.catalog.mapNotNull { other ->
                        viewModel.relationshipsFor(listOf(it.id, other.id)).firstOrNull()
                    }.distinct()
                }.orEmpty()
                CardDetailScreen(
                    card = card,
                    frame = card?.currentFrameId?.let(framesById::get),
                    relationships = relationships,
                    isOwned = state.ownedCards.any { it.id == cardId },
                    onCustomize = { navController.navigate(Routes.frames(cardId)) }
                )
            }
            composable(
                route = Routes.FRAMES,
                arguments = listOf(navArgument("cardId") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                FrameCustomizationScreen(
                    ownedCards = state.ownedCards,
                    frames = state.frames,
                    unlockedFrameIds = state.unlockedFrameIds,
                    initialCardId = entry.arguments?.getString("cardId")?.ifBlank { null },
                    onApply = viewModel::applyFrame,
                    onReset = viewModel::resetFrame
                )
            }
        }
    }
}

private fun screenTitle(route: String?): String = when (route) {
    Routes.COLLECTION -> "Collection"
    Routes.DECKS -> "Deck Builder"
    Routes.GAME -> "Mini Game"
    Routes.COMBAT -> "Wild Run"
    Routes.DETAIL -> "Card Details"
    Routes.FRAMES -> "Frame Workshop"
    Routes.FRAME_STORE -> "Frame Store"
    else -> "WildDeck"
}
