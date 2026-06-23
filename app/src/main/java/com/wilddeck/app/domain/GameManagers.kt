package com.wilddeck.app.domain

import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.Deck
import com.wilddeck.app.model.DeckScore
import com.wilddeck.app.model.MiniGameAnswer
import com.wilddeck.app.model.MiniGameSession
import com.wilddeck.app.model.RuleResult
import com.wilddeck.app.model.SymbiosisRelationship
import com.wilddeck.app.model.TriviaQuestion
import com.wilddeck.app.model.TriviaDifficulty
import kotlin.math.roundToInt
import kotlin.random.Random

class PlayerInventory(initialCardIds: Collection<String> = emptyList()) {
    private val ownedCardIds = initialCardIds.toMutableSet()

    fun addCard(cardId: String): Boolean = ownedCardIds.add(cardId)
    fun owns(cardId: String): Boolean = cardId in ownedCardIds
    fun allIds(): Set<String> = ownedCardIds.toSet()
    fun getAll(catalog: Map<String, AnimalCard>): List<AnimalCard> =
        ownedCardIds.mapNotNull(catalog::get)
}

class SymbiosisManager(relationships: List<SymbiosisRelationship>) {
    private val relationshipsByPair = relationships.associateBy {
        normalizedPair(it.animalAId, it.animalBId)
    }

    fun relationshipBetween(firstId: String, secondId: String): SymbiosisRelationship? =
        relationshipsByPair[normalizedPair(firstId, secondId)]

    fun score(cardIds: List<String>, catalog: Map<String, AnimalCard>): DeckScore {
        val base = cardIds.mapNotNull(catalog::get).sumOf { it.health + it.danger }
        val active = buildList {
            for (first in cardIds.indices) {
                for (second in first + 1 until cardIds.size) {
                    relationshipBetween(cardIds[first], cardIds[second])?.let(::add)
                }
            }
        }
        val multiplier = active.fold(1.0) { total, relationship ->
            total * relationship.type.multiplier
        }
        return DeckScore(base, multiplier, (base * multiplier).roundToInt(), active)
    }

    private fun normalizedPair(firstId: String, secondId: String): String =
        if (firstId <= secondId) "$firstId|$secondId" else "$secondId|$firstId"
}

class DeckManager(
    initialDecks: List<Deck>,
    private val inventory: PlayerInventory,
    private val catalog: Map<String, AnimalCard>,
    private val symbiosisManager: SymbiosisManager
) {
    private val decksById = initialDecks.associateByTo(linkedMapOf()) { it.id }

    fun allDecks(): List<Deck> = decksById.values.toList()

    fun createDeck(id: String, name: String): RuleResult {
        if (decksById.size >= MAX_DECKS) return RuleResult.Error("You can only have 5 decks.")
        decksById[id] = Deck(id, name.trim().ifBlank { "Untitled Deck" })
        return RuleResult.Success
    }

    fun deleteDeck(deckId: String): RuleResult =
        if (decksById.remove(deckId) != null) RuleResult.Success
        else RuleResult.Error("Deck not found.")

    fun renameDeck(deckId: String, name: String): RuleResult {
        val deck = decksById[deckId] ?: return RuleResult.Error("Deck not found.")
        decksById[deckId] = deck.copy(name = name.trim().ifBlank { deck.name })
        return RuleResult.Success
    }

    fun addCard(deckId: String, cardId: String): RuleResult {
        val deck = decksById[deckId] ?: return RuleResult.Error("Deck not found.")
        if (!inventory.owns(cardId)) return RuleResult.Error("You do not own this card yet.")
        if (cardId in deck.cardIds) return RuleResult.Error("This card is already in the deck.")
        if (deck.cardIds.size >= Deck.MAX_CARDS) return RuleResult.Error("Deck is full.")
        decksById[deckId] = withUpdatedScore(deck.copy(cardIds = deck.cardIds + cardId))
        return RuleResult.Success
    }

    fun removeCard(deckId: String, cardId: String): RuleResult {
        val deck = decksById[deckId] ?: return RuleResult.Error("Deck not found.")
        decksById[deckId] = withUpdatedScore(deck.copy(cardIds = deck.cardIds - cardId))
        return RuleResult.Success
    }

    fun refreshScores() {
        decksById.replaceAll { _, deck -> withUpdatedScore(deck) }
    }

    private fun withUpdatedScore(deck: Deck): Deck {
        val score = symbiosisManager.score(deck.cardIds, catalog)
        return deck.copy(score = score.finalScore, symbiosisMultiplier = score.multiplier)
    }

    companion object {
        const val MAX_DECKS = 5
    }
}

class FrameManager(
    private val frames: Map<String, CardFrame>,
    unlockedFrameIds: Collection<String>,
    selectedFrames: Map<String, String>
) {
    private val unlockedIds = unlockedFrameIds.toMutableSet().apply { add("black") }
    private val selectedByCardId = selectedFrames.toMutableMap()

    fun allFrames(): List<CardFrame> = frames.values.toList()
    fun unlockedIds(): Set<String> = unlockedIds.toSet()
    fun selectedFrames(): Map<String, String> = selectedByCardId.toMap()
    fun selectedFrameId(cardId: String): String = selectedByCardId[cardId] ?: "black"
    fun isUnlocked(frameId: String): Boolean = frameId in unlockedIds
    fun unlock(frameId: String): RuleResult {
        if (frameId !in frames) return RuleResult.Error("Frame asset is missing.")
        unlockedIds += frameId
        return RuleResult.Success
    }

    fun applyFrame(cardId: String, frameId: String, inventory: PlayerInventory): RuleResult {
        if (!inventory.owns(cardId)) return RuleResult.Error("You do not own this card yet.")
        if (frameId !in frames) return RuleResult.Error("Frame asset is missing.")
        if (!isUnlocked(frameId)) return RuleResult.Error("This frame is locked.")
        selectedByCardId[cardId] = frameId
        return RuleResult.Success
    }

    fun resetFrame(cardId: String, inventory: PlayerInventory): RuleResult =
        applyFrame(cardId, "black", inventory)
}

class MiniGameManager(
    private val cards: List<AnimalCard>,
    private val random: Random = Random.Default
) {
    fun startSession(excludedCardIds: Set<String> = emptySet()): MiniGameSession? {
        val availableCards = cards.filterNot { it.id in excludedCardIds }
        if (availableCards.isEmpty()) return null
        val target = availableCards[random.nextInt(availableCards.size)]
        return MiniGameSession(
            targetCard = target,
            questions = createQuestions(target).shuffled(random)
        )
    }

    fun answer(session: MiniGameSession, selectedAnswer: String): Pair<MiniGameSession, MiniGameAnswer> {
        if (session.isRewarded) {
            return session to MiniGameAnswer(false, "Card already awarded.")
        }
        val isCorrect = selectedAnswer == session.currentQuestion.correctAnswer
        val newCount = if (isCorrect) {
            session.matchCount + 1
        } else {
            (session.matchCount - 1).coerceAtLeast(0)
        }
        val won = newCount >= session.requiredMatchCount
        val nextQuestionIndex = (session.questionIndex + 1) % session.questions.size
        val updated = session.copy(
            questionIndex = nextQuestionIndex,
            matchCount = newCount,
            isRewarded = won
        )
        val answer = if (won && isCorrect) {
            MiniGameAnswer(true, "Card added to inventory.", session.targetCard)
        } else if (isCorrect) {
            MiniGameAnswer(true, "Correct! Next question.")
        } else {
            MiniGameAnswer(false, "Incorrect. Progress decreased by 1.")
        }
        return updated to answer
    }

    fun createQuestions(target: AnimalCard): List<TriviaQuestion> =
        requireNotNull(funFactBank[target.id]) { "Missing trivia facts for ${target.id}." }
            .mapIndexed { index, fact ->
                TriviaQuestion(
                    id = "${target.id}_${fact.difficulty.name.lowercase()}_$index",
                    prompt = fact.prompt,
                    options = (fact.distractors + fact.answer).shuffled(random),
                    correctAnswer = fact.answer,
                    difficulty = fact.difficulty
                )
            }

    private data class Fact(
        val difficulty: TriviaDifficulty,
        val prompt: String,
        val answer: String,
        val distractors: List<String>
    )

    private fun easy(prompt: String, answer: String, vararg distractors: String) =
        Fact(TriviaDifficulty.EASY, prompt, answer, distractors.toList())

    private fun medium(prompt: String, answer: String, vararg distractors: String) =
        Fact(TriviaDifficulty.MEDIUM, prompt, answer, distractors.toList())

    private fun hard(prompt: String, answer: String, vararg distractors: String) =
        Fact(TriviaDifficulty.HARD, prompt, answer, distractors.toList())

    private val funFactBank = mapOf(
        "lion" to listOf(
            easy("Which lion fact is true?", "Lions are the only cats that regularly live in social groups called prides.", "Lions breathe through gills.", "Lions build dams from branches.", "Lions hatch from leathery eggs."),
            easy("What is a male lion best known for?", "Adult males often grow a thick mane around the head and neck.", "Adult males grow antlers each spring.", "Adult males carry young in a pouch.", "Adult males have a turtle-like shell."),
            easy("Who usually does much of the hunting in a lion pride?", "Female lions often cooperate to hunt prey.", "Newborn cubs hunt alone.", "Only old males ever hunt.", "Lions farm grass for food."),
            medium("Why do lions roar?", "Roars help advertise territory and keep pride members in contact.", "Roars are used to echolocate insects underground.", "Roars cool the body like sweating.", "Roars replace the need to drink water."),
            medium("What is unusual about lion social life compared with most cats?", "They form lasting groups with related females.", "They migrate across oceans every winter.", "They live permanently inside caves.", "They change color to match flowers."),
            medium("What does a darker mane often signal?", "A darker mane can signal maturity and strong condition.", "A darker mane means the lion is always female.", "A darker mane means the lion is a cub.", "A darker mane proves the lion cannot roar."),
            medium("What happens when lions hunt as a team?", "They can surround or flush prey more effectively.", "They weave nets from grass.", "They stun prey with electric organs.", "They bury prey under snow."),
            hard("How far can a lion's roar travel in good conditions?", "About 8 kilometers, or roughly 5 miles.", "About 80 meters, or roughly one city block.", "About 800 kilometers, across countries.", "Only a few centimeters."),
            hard("Why do lions spend so much time resting?", "Resting saves energy between intense hunts and hot daytime periods.", "Resting lets them photosynthesize.", "Resting hardens their bones into armor.", "Resting makes their mane waterproof."),
            hard("What is a lion's tail tuft useful for?", "It can help pride members follow signals in tall grass.", "It works as a venomous stinger.", "It stores drinking water.", "It acts like a second paw.")
        ),
        "elephant" to listOf(
            easy("Which elephant fact is true?", "African elephants are the largest living land animals.", "Elephants are tiny burrowing reptiles.", "Elephants breathe only through their ears.", "Elephants have feathers instead of hair."),
            easy("What is an elephant trunk?", "A flexible nose and upper lip used for smelling, breathing, drinking, and grasping.", "A hard shell that covers the back.", "A wing used for gliding.", "A root that anchors the animal."),
            easy("What do elephants use their tusks for?", "Digging, stripping bark, moving objects, and defense.", "Catching fish with suction cups.", "Spinning webs.", "Filtering plankton like baleen."),
            medium("Why are elephants called ecosystem engineers?", "They knock down trees, dig for water, and create paths that other animals use.", "They build metal bridges.", "They pollinate only underwater plants.", "They freeze ponds during summer."),
            medium("How do elephants communicate over long distances?", "They can use low-frequency rumbles that travel through air and ground.", "They flash colored lights from their tusks.", "They sing ultrasonic bird songs only.", "They drum on hollow shells with claws."),
            medium("What is special about elephant family groups?", "Related females and calves often stay together under an experienced matriarch.", "Only males raise every calf.", "Calves leave the group within hours forever.", "Adults never recognize relatives."),
            medium("What do elephants do with mud?", "Mud baths help cool their skin and protect against sun and insects.", "Mud lets them breathe underwater.", "Mud turns into feathers.", "Mud is their only food."),
            hard("How many muscles does an elephant trunk contain?", "Tens of thousands of muscle units.", "Exactly seven muscles.", "No muscles, only bone.", "One single spring-shaped muscle."),
            hard("Why can elephants hear some sounds humans cannot?", "They detect very low-frequency infrasound.", "They hear only radio broadcasts.", "They hear with their tusks alone.", "They cannot hear at all."),
            hard("What is a little-known elephant foot adaptation?", "A fatty pad helps cushion steps and sense ground vibrations.", "A wheel-like bone rolls under each foot.", "A claw injects venom.", "A hoof opens like a flower.")
        ),
        "crocodile" to listOf(
            easy("Which crocodile fact is true?", "Nile crocodiles are powerful ambush predators.", "Crocodiles are warm-blooded birds.", "Crocodiles eat only nectar.", "Crocodiles have no teeth."),
            easy("Where do Nile crocodiles often hunt?", "Near water edges where animals come to drink.", "High in alpine snowfields.", "Inside desert cactus flowers.", "In the open sky."),
            easy("What are crocodile eyes and nostrils adapted for?", "They sit high on the head so the animal can watch and breathe while mostly submerged.", "They glow to attract pollinating bees.", "They are used as extra legs.", "They close forever after hatching."),
            medium("What is a crocodile's death roll?", "A spinning motion used to tear or subdue prey.", "A courtship dance done on tree branches.", "A way to shed feathers.", "A method of digging snow caves."),
            medium("Why do crocodiles bask in the sun?", "They use external heat to regulate body temperature.", "They recharge solar panels in their scales.", "They turn plants into sugar through photosynthesis.", "They dry out their gills."),
            medium("What do crocodiles do for some hatchlings?", "Mothers may carry babies gently in their mouths to water.", "Fathers weave nests from silk.", "Adults nurse babies with milk.", "Babies are raised by fish."),
            medium("What makes crocodile jaws famous?", "They can close with tremendous force.", "They chew sideways like cows.", "They contain no nerves.", "They are made of flexible cartilage only."),
            hard("What is unusual about crocodile hearts?", "They have a four-chambered heart and can redirect blood flow while diving.", "They have no heart.", "Their heart is in the tail tip.", "Their heart pumps seawater instead of blood."),
            hard("How can crocodiles stay underwater so long?", "They slow activity and conserve oxygen while submerged.", "They breathe water through fur.", "They store air in hollow tusks.", "They stop needing oxygen permanently."),
            hard("What determines crocodile hatchling sex in many species?", "Nest temperature during incubation.", "The color of nearby rocks.", "The father's roar volume.", "The number of fish in the river.")
        ),
        "wolf" to listOf(
            easy("Which wolf fact is true?", "Gray wolves live and hunt in family-based packs.", "Wolves are insects with six legs.", "Wolves hatch from eggs in ponds.", "Wolves eat only bamboo."),
            easy("What is a wolf howl used for?", "Keeping contact with pack members and advertising territory.", "Turning prey into stone.", "Cooling the paws.", "Building nests from ice."),
            easy("What do wolves mainly eat?", "Large and medium mammals such as deer, elk, or smaller prey depending on location.", "Only flower pollen.", "Only coral algae.", "Only tree bark."),
            medium("Why do wolves hunt cooperatively?", "Cooperation helps them pursue and test large prey.", "Cooperation lets them fly in formation.", "Cooperation makes them invisible.", "Cooperation replaces the need for teeth."),
            medium("What is a wolf pack usually centered around?", "A breeding pair and their offspring.", "A random swarm with no family bonds.", "Only unrelated adult males.", "A queen wolf like a bee colony."),
            medium("How do wolves mark territory?", "They use scent marks, tracks, and howls.", "They paint rocks with feathers.", "They plant flags made of leaves.", "They carve words with antlers."),
            medium("Why are wolves important to ecosystems?", "They can influence prey behavior and help balance food webs.", "They produce most forest oxygen.", "They build coral reefs.", "They create rainfall with howls."),
            hard("What is a wolf's sense of smell useful for?", "Tracking prey, identifying pack members, and reading scent marks.", "Detecting magnetic fields only.", "Finding buried treasure maps.", "Turning sound into light."),
            hard("How far can wolves travel in a day?", "They can cover many kilometers while patrolling or hunting.", "They cannot walk more than one meter.", "They only move by swimming backward.", "They teleport between dens."),
            hard("What happened after wolves returned to Yellowstone?", "Their return helped restore predator-prey interactions and affected elk behavior.", "All rivers instantly froze.", "Elk became extinct overnight.", "Trees stopped growing completely.")
        ),
        "rabbit" to listOf(
            easy("Which rabbit fact is true?", "Rabbits are herbivores with powerful hind legs.", "Rabbits are fish with gills.", "Rabbits are venomous snakes.", "Rabbits are single-celled animals."),
            easy("What are rabbit ears useful for?", "Hearing predators and releasing body heat.", "Flying long distances.", "Stinging prey.", "Digging with metal claws."),
            easy("What do wild rabbits often eat?", "Grasses, herbs, leaves, and other plant material.", "Large sharks.", "Only stones.", "Electrical wires as their main diet."),
            medium("What is a rabbit warren?", "A connected system of burrows used for shelter.", "A floating nest on the ocean.", "A pile of bones used for display.", "A tree hollow used only by eagles."),
            medium("Why are rabbit eyes placed on the sides of the head?", "They provide a wide field of view for spotting predators.", "They help chew grass.", "They produce venom.", "They replace the nose."),
            medium("What are cecotropes?", "Nutrient-rich droppings rabbits re-eat to absorb more nutrients.", "Baby rabbits before birth.", "A type of rabbit antler.", "A waterproof feather coat."),
            medium("Why do rabbits thump the ground?", "A thump can warn nearby rabbits of danger.", "It starts photosynthesis.", "It calls fish to the surface.", "It sharpens their teeth."),
            hard("How do rabbit teeth grow?", "Their incisors grow continuously and are worn down by chewing.", "Their teeth fall out every night.", "Their teeth are replaced by beaks.", "Their teeth stop growing before birth."),
            hard("What is a rabbit's main anti-predator strategy?", "Detect danger early, sprint, zigzag, and dive into cover.", "Fight bears head-on.", "Spray ink like an octopus.", "Hide inside a shell."),
            hard("Why can rabbits reproduce quickly?", "Short pregnancies and large litters help prey populations recover.", "They clone by splitting in half.", "They lay thousands of eggs in water.", "They only reproduce once per century.")
        ),
        "eagle" to listOf(
            easy("Which eagle fact is true?", "Golden eagles are powerful birds of prey.", "Eagles are marine jellyfish.", "Eagles have no eyes.", "Eagles are herbivorous worms."),
            easy("What are eagle talons used for?", "Grabbing and killing prey.", "Filtering plankton.", "Digging coral tunnels.", "Spinning silk webs."),
            easy("What sense is especially sharp in eagles?", "Vision.", "Taste through the tail.", "Smell through feathers only.", "Hearing underwater whale songs only."),
            medium("Why do golden eagles soar?", "Soaring saves energy while searching large areas for prey.", "Soaring cools eggs before hatching.", "Soaring grows new bones.", "Soaring lets them breathe through feet."),
            medium("Where do golden eagles often nest?", "On cliffs, large trees, or high rugged sites.", "Deep in coral polyps.", "Inside termite mounds only.", "Under glacier ice."),
            medium("What do golden eagles commonly hunt?", "Mammals and birds such as rabbits, marmots, and grouse.", "Only tree sap.", "Only microscopic algae.", "Only fallen fruit."),
            medium("What is a stoop?", "A fast hunting dive made by a bird of prey.", "A nest made of ice.", "A type of fish scale.", "A sound used by rabbits."),
            hard("Why do eagles have a hooked beak?", "It tears flesh into manageable pieces.", "It filters mud from water.", "It drills through rock for minerals.", "It stores eggs."),
            hard("How do eagles reduce energy cost in flight?", "They ride thermals and updrafts.", "They flap only once per day.", "They use jet engines in feathers.", "They float on helium sacs."),
            hard("What makes eagle eyesight so detailed?", "Large eyes and dense light-sensitive cells support high visual acuity.", "Their eyes are made of mirrors.", "Their pupils work like sonar.", "Their eyelids magnify sound.")
        ),
        "shark" to listOf(
            easy("Which reef shark fact is true?", "Reef sharks help balance coral reef food webs.", "Reef sharks are land mammals.", "Reef sharks eat only pine needles.", "Reef sharks have feathers."),
            easy("What are shark teeth known for?", "Many sharks continually replace teeth through life.", "Shark teeth are made of wood.", "Sharks have no teeth.", "Shark teeth are used for photosynthesis."),
            easy("Where do reef sharks live?", "Around tropical coral reefs and nearby waters.", "Only in desert caves.", "Only in mountaintop snow.", "Only inside beehives."),
            medium("What is the lateral line?", "A sensory system that detects movement and vibrations in water.", "A stripe used to count age rings.", "A rope sharks use to climb.", "A feather row on the wing."),
            medium("Why are sharks important predators?", "They often remove weak or abundant animals and influence prey behavior.", "They produce coral skeletons.", "They pollinate flowers on land.", "They make ocean tides."),
            medium("What does a shark's cartilaginous skeleton mean?", "Its skeleton is made mostly of cartilage instead of bone.", "It has no skeleton at all.", "Its skeleton is made of glass.", "Its bones are hollow like birds."),
            medium("How do many sharks smell prey?", "Sensitive nostrils detect dissolved chemicals in water.", "They smell with their fins only.", "They cannot detect chemicals.", "They smell through coral branches."),
            hard("What are ampullae of Lorenzini?", "Electroreceptor organs that detect tiny electric fields.", "Extra stomachs for digesting coral.", "Baby teeth stored in the tail.", "Air sacs used for singing."),
            hard("Why must some sharks keep moving?", "Water flow over gills helps them get oxygen.", "Stopping turns them into coral.", "Movement charges batteries in their teeth.", "They cannot sleep in any form."),
            hard("What is countershading?", "A darker back and lighter belly help camouflage the shark from above and below.", "A way sharks build shade with fins.", "A temperature control organ.", "A mating call.")
        ),
        "clownfish" to listOf(
            easy("Which clownfish fact is true?", "Clownfish live among sea anemone tentacles.", "Clownfish live in desert sand dunes.", "Clownfish are flying mammals.", "Clownfish have antlers."),
            easy("Why are clownfish safe near anemones?", "A protective mucus coating helps them avoid being stung.", "They wear shells made of stone.", "They have thick fur armor.", "They turn invisible permanently."),
            easy("What do clownfish often eat?", "Algae, plankton, and small food scraps.", "Large elephants.", "Only dry leaves.", "Only metal pieces."),
            medium("How do clownfish help anemones?", "They can bring nutrients, clean, and help chase away some predators.", "They build wooden houses for anemones.", "They teach anemones to swim.", "They turn anemones into coral instantly."),
            medium("What is special about clownfish sex change?", "Groups can change sex, with the dominant fish becoming female.", "All clownfish are born male lions.", "They change into birds each winter.", "They have no reproductive roles."),
            medium("Where do clownfish lay eggs?", "On a cleaned surface near their host anemone.", "In nests high in pine trees.", "Inside mammal burrows.", "Loose in dry desert air."),
            medium("Why do clownfish rarely travel far from anemones?", "The anemone provides protection from many predators.", "They cannot swim at all.", "They need tree bark to breathe.", "They are attached by roots."),
            hard("What is acclimation in clownfish-anemone life?", "Clownfish gradually adjust their mucus and behavior around tentacles.", "They grow a hard turtle shell.", "They learn to fly before entering water.", "They become venomous snakes."),
            hard("What social structure do clownfish groups often have?", "A size-based hierarchy with a breeding female and breeding male.", "A queen like an ant colony with workers only.", "A random group with no ranking.", "Only solitary adults, never groups."),
            hard("Why can clownfish waste benefit anemones?", "Nitrogen-rich waste can fertilize symbiotic algae in the anemone.", "Waste turns tentacles into bones.", "Waste creates freshwater bubbles.", "Waste stops the anemone from eating.")
        ),
        "anemone" to listOf(
            easy("Which sea anemone fact is true?", "Sea anemones are animals related to corals and jellyfish.", "Sea anemones are flowering land plants.", "Sea anemones are birds.", "Sea anemones are mammals with fur."),
            easy("What do anemone tentacles contain?", "Stinging cells called nematocysts.", "Tiny feathers for flight.", "Wooden teeth.", "Milk glands."),
            easy("Where can many sea anemones live?", "Rocky shores, tide pools, and coral reef habitats.", "Only dry treetops.", "Only desert air.", "Only inside volcano smoke."),
            medium("How do sea anemones capture prey?", "Tentacles sting and hold small animals that drift or swim close.", "They chase prey with legs.", "They shoot arrows made of bone.", "They trap prey in spider silk."),
            medium("What do many anemones share with photosynthetic algae?", "Some host algae that provide food from sunlight.", "All anemones grow leaves.", "Algae become anemone bones.", "Algae make anemones warm-blooded."),
            medium("How do anemones stay attached?", "A pedal disc grips rock or other surfaces.", "They use bird claws.", "They drill with tusks.", "They glue themselves with ice."),
            medium("What can anemones do when disturbed?", "They can contract their bodies and pull tentacles inward.", "They run away on hooves.", "They launch into the air.", "They shed a shell."),
            hard("What is a nematocyst?", "A microscopic stinging capsule that fires a tiny harpoon-like thread.", "A fish egg carried in fins.", "A mammal hair follicle.", "A bird lung chamber."),
            hard("How can some anemones reproduce without mates?", "They can clone by splitting or budding.", "They lay hard-shelled bird eggs.", "They produce seeds like grasses.", "They split into insects."),
            hard("Why do clownfish and anemones form mutualism?", "Fish gain shelter while anemones can gain nutrients and defense.", "Both animals become plants.", "The anemone teaches fish to breathe air.", "The fish turns tentacles into coral.")
        ),
        "rhino" to listOf(
            easy("Which white rhinoceros fact is true?", "White rhinoceroses are large grazing mammals.", "Rhinos are tiny insects.", "Rhinos live only in coral reefs.", "Rhinos have wings."),
            easy("What do white rhinos mainly eat?", "Grass.", "Only meat.", "Only plankton.", "Only tree sap."),
            easy("What are rhino horns made of?", "Keratin, the same protein found in hair and fingernails.", "Ivory like elephant tusks.", "Solid bone attached to the skull.", "Metal."),
            medium("Why is the white rhino's mouth broad?", "Its wide square lip is adapted for grazing grass.", "It filters krill like a whale.", "It drills holes in trees.", "It catches insects in flight."),
            medium("What role do rhinos play in grasslands?", "Grazing helps shape plant communities and open habitat.", "They build coral reefs.", "They pollinate orchids with feathers.", "They create snowfields."),
            medium("Why do rhinos use mud wallows?", "Mud cools the body and helps protect skin from insects and sun.", "Mud lets them breathe underwater.", "Mud becomes their horn.", "Mud is used to hatch eggs."),
            medium("How do rhinos often communicate?", "Through scent marks, dung piles, sounds, and body posture.", "By changing feather colors.", "By flashing bioluminescent horns.", "By singing underwater."),
            hard("What is a midden in rhino behavior?", "A shared dung pile used for scent communication.", "A nest of twigs in a tree.", "A coral cave.", "A feather display."),
            hard("Why are white rhinos threatened?", "Poaching for horns and habitat pressure have harmed populations.", "They cannot eat grass anymore.", "They migrate to space.", "They have no predators as adults, so they vanish."),
            hard("What is unusual about white rhino conservation history?", "Southern white rhinos recovered from very low numbers through protection.", "They were domesticated as dairy animals.", "They were discovered only in Antarctica.", "They naturally live in every ocean.")
        ),
        "oxpecker" to listOf(
            easy("Which oxpecker fact is true?", "Oxpeckers perch on large mammals and eat ticks and insects.", "Oxpeckers are sharks.", "Oxpeckers are underground mushrooms.", "Oxpeckers are reptiles with shells."),
            easy("Where are red-billed oxpeckers found?", "In parts of sub-Saharan Africa.", "Only in Arctic sea ice.", "Only in Australian coral lagoons.", "Only in South American glaciers."),
            easy("What animal might an oxpecker ride on?", "A rhinoceros, buffalo, giraffe, or antelope.", "A jellyfish bell.", "A cactus flower.", "A snowflake."),
            medium("Why do oxpeckers pick at hosts?", "They feed on ticks, insects, blood, and wound tissue.", "They mine gold from skin.", "They plant seeds in fur.", "They weave host hair into nests while flying."),
            medium("How can oxpeckers warn hosts?", "Alarm calls may alert mammals to danger.", "They flash electric lights.", "They spray ink.", "They drum with tusks."),
            medium("Why is the oxpecker relationship complicated?", "They remove parasites but may also keep wounds open to feed.", "They turn mammals into birds.", "They are always harmful and never helpful.", "They only visit fish."),
            medium("What feature helps oxpeckers cling to hosts?", "Strong feet and claws help them grip moving animals.", "Suction cups like an octopus.", "Hooves on their wings.", "A turtle shell."),
            hard("What does a red-billed oxpecker's bill help it do?", "Comb through hair and pick parasites from skin.", "Filter plankton from seawater.", "Crack open coconuts underwater.", "Drill tunnels through stone."),
            hard("Why might hosts tolerate oxpeckers?", "Hosts may benefit from parasite removal and alarm calls.", "Hosts use oxpeckers as gills.", "Oxpeckers provide milk.", "Oxpeckers carry hosts across rivers."),
            hard("What is one conservation issue for oxpeckers?", "Their numbers can be affected by pesticide use on livestock and wildlife.", "They are harmed by deep-sea fishing nets only.", "They require polar ice to nest.", "They cannot survive near mammals.")
        ),
        "pistol_shrimp" to listOf(
            easy("Which pistol shrimp fact is true?", "Pistol shrimp can snap a specialized claw extremely fast.", "Pistol shrimp are large flying birds.", "Pistol shrimp eat only grass on land.", "Pistol shrimp have elephant trunks."),
            easy("What does a pistol shrimp snap create?", "A fast bubble and loud shock wave.", "A feather plume.", "A spider web.", "A flower seed."),
            easy("Where do many pistol shrimp live?", "In shallow tropical marine habitats, often in burrows.", "On dry mountain peaks.", "Inside mammal fur.", "In freshwater clouds."),
            medium("What is cavitation?", "A vapor bubble forms and collapses because of rapid pressure change.", "A type of bird migration.", "A mammal chewing motion.", "A plant root disease only."),
            medium("How do pistol shrimp use the snap?", "To stun prey, defend territory, and communicate.", "To photosynthesize.", "To grow feathers.", "To make fresh water."),
            medium("What animal can share a burrow with pistol shrimp?", "A watchman goby.", "A lion.", "A golden eagle.", "A rhinoceros."),
            medium("Why is the snapping claw asymmetrical?", "One claw is enlarged and specialized for rapid snapping.", "Both claws become wings.", "The large claw stores eggs only.", "The claw is actually a horn."),
            hard("How hot can the collapsing bubble briefly become?", "It can momentarily reach temperatures comparable to the surface of the sun.", "It stays colder than ice.", "It becomes room temperature only.", "It freezes seawater instantly."),
            hard("Why does the snap make sound?", "The collapsing cavitation bubble produces a sharp pop.", "The shrimp sings with vocal cords.", "The claw hits a tiny drum.", "The tail whistles like a bird."),
            hard("What is special about goby-shrimp teamwork?", "The shrimp digs while the goby watches for predators.", "The goby carries the shrimp through air.", "The shrimp teaches the goby to roar.", "Both animals build coral skeletons.")
        ),
        "goby" to listOf(
            easy("Which watchman goby fact is true?", "Watchman gobies can share burrows with pistol shrimp.", "Gobies are elephants.", "Gobies live only in tree nests.", "Gobies have feathers."),
            easy("What does the goby do for its shrimp partner?", "It watches for danger near the burrow entrance.", "It builds wooden dams.", "It hunts antelope.", "It produces milk."),
            easy("Where do watchman gobies live?", "Sandy tropical reef areas.", "Dry deserts far from water.", "Polar ice caps only.", "High forest canopies only."),
            medium("Why does the pistol shrimp need a lookout?", "Its eyesight is poor compared with the goby's.", "It has no claws.", "It cannot move sand.", "It is asleep all day forever."),
            medium("How do gobies signal danger to shrimp?", "They use body movements and contact cues near the burrow.", "They write messages in ink.", "They howl like wolves.", "They flash antlers."),
            medium("What do many gobies eat?", "Small crustaceans, worms, and tiny reef animals.", "Only grass.", "Only elephant tusks.", "Only large mammals."),
            medium("Why is this partnership mutualism?", "Both species benefit: shelter work from shrimp and warning from goby.", "Only one species exists.", "Both turn into plants.", "Neither animal benefits."),
            hard("How does the shrimp keep contact with the goby?", "It often touches the goby with an antenna while working.", "It ties a rope to the goby.", "It uses a radio transmitter.", "It holds the goby's tail with teeth."),
            hard("What happens when the goby signals danger?", "The shrimp and goby can retreat into the burrow.", "The shrimp flies away.", "The goby becomes invisible forever.", "The burrow turns into coral instantly."),
            hard("Why is the burrow important for reef life?", "It provides shelter in open sand where hiding places are limited.", "It creates mountain snow.", "It filters all ocean water.", "It grows leaves for photosynthesis.")
        ),
        "remora" to listOf(
            easy("Which remora fact is true?", "Remoras use a suction disc to attach to larger marine animals.", "Remoras are land rabbits.", "Remoras fly with feathers.", "Remoras grow antlers."),
            easy("Where is a remora's suction disc?", "On the top of its head.", "At the end of a long trunk.", "Inside a shell.", "On a pair of wings."),
            easy("What hosts can remoras ride?", "Sharks, rays, turtles, whales, and large fish.", "Desert cactus stems.", "Tree branches only.", "Clouds."),
            medium("What do remoras gain from hitchhiking?", "Transport, protection, and access to food scraps or parasites.", "The ability to breathe air forever.", "A hard rhino horn.", "A bird nest."),
            medium("How did the suction disc evolve?", "It is a modified dorsal fin.", "It is a second mouth.", "It is a turtle shell.", "It is a plant root."),
            medium("What do remoras eat?", "Food scraps, small animals, and parasites depending on the situation.", "Only grass.", "Only elephant leaves.", "Only rocks."),
            medium("Why is the relationship often called commensalism?", "The remora benefits while the host is usually little affected.", "Both animals always die.", "The host becomes a remora.", "The remora pollinates flowers."),
            hard("How does the remora disc stick?", "Sliding plates create friction and suction against the host.", "It uses glue made of silk.", "It bites permanently into bone.", "It magnetizes seawater."),
            hard("Why can attaching save energy?", "The remora rides currents created by a larger swimming animal.", "It stops needing oxygen.", "It turns into a leaf.", "It stores sunlight in scales."),
            hard("What is one reason remoras switch hosts?", "They may move to find better feeding or travel opportunities.", "They must change species every hour.", "They cannot attach twice.", "They are chased by flowers.")
        )
    )
}
