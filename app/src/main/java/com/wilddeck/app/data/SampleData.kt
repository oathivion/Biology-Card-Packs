package com.wilddeck.app.data

import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.AnimalAbility
import com.wilddeck.app.model.AbilityType
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.CardRarity
import com.wilddeck.app.model.RelationshipType
import com.wilddeck.app.model.SymbiosisRelationship

object SampleData {
    private val abilities = mapOf(
        "lion" to AnimalAbility("pride_guard", "Pride Guard", AbilityType.TAUNT,
            "Lions defend their pride: becomes the enemy's preferred target and gains a shield.", 2),
        "elephant" to AnimalAbility("thick_hide", "Thick Hide", AbilityType.SHIELD,
            "Its thick hide absorbs extra damage after it attacks.", 3),
        "crocodile" to AnimalAbility("ambush", "River Ambush", AbilityType.AMBUSH,
            "An ambush strike deals bonus damage to an uninjured target.", 3),
        "wolf" to AnimalAbility("pack_hunt", "Pack Hunt", AbilityType.PACK,
            "Pack coordination adds damage for every other living ally.", 1),
        "rabbit" to AnimalAbility("burrow", "Sheltering Burrow", AbilityType.SHIELD,
            "Digs cover around an ally, granting a protective shield.", 3),
        "eagle" to AnimalAbility("diving_strike", "Diving Strike", AbilityType.STRIKE,
            "Exceptional eyesight guides a precise, high-damage strike.", 2),
        "shark" to AnimalAbility("feeding_frenzy", "Feeding Frenzy", AbilityType.STRIKE,
            "Deals bonus damage to an already injured target.", 2),
        "clownfish" to AnimalAbility("anemone_shelter", "Anemone Shelter", AbilityType.HEAL,
            "Protective mucus and shelter restore an ally's health.", 3),
        "anemone" to AnimalAbility("stinging_tentacles", "Stinging Tentacles", AbilityType.STUN,
            "Stinging cells briefly stun the target after dealing damage.", 1),
        "rhino" to AnimalAbility("charge", "Savanna Charge", AbilityType.TAUNT,
            "A massive charge deals bonus damage and draws enemy attention.", 2),
        "oxpecker" to AnimalAbility("tick_cleaner", "Tick Cleaner", AbilityType.HEAL,
            "Removes parasites and restores a large ally's health.", 3),
        "pistol_shrimp" to AnimalAbility("cavitation_snap", "Cavitation Snap", AbilityType.STUN,
            "Its shock wave damages and stuns an enemy.", 1),
        "goby" to AnimalAbility("watchman", "Watchman's Warning", AbilityType.DODGE,
            "Warns an ally of danger, granting a shield and drawing no attacks.", 2),
        "remora" to AnimalAbility("parasite_cleanup", "Parasite Cleanup", AbilityType.EMPOWER,
            "Cleans an ally and raises both its damage and remaining health.", 2)
    )

    val animalCards = listOf(
        AnimalCard("lion", "Lion", "Big cat", 8, 9, "🦁",
            "Lions are social big cats that live in prides. Females usually coordinate hunts while males help defend territory.",
            "Meat", "African savanna and grassland", CardRarity.RARE,
            "A large, muscular predator can endure substantial injury.",
            "Powerful jaws, claws, speed, and coordinated hunting make close contact extremely dangerous."),
        AnimalCard("elephant", "African Elephant", "Mammal", 10, 9, "🐘",
            "The African elephant is the largest living land animal and an important ecosystem engineer that reshapes vegetation and water access.",
            "Leaves", "Savanna, forest, and wetland", CardRarity.LEGENDARY,
            "Its immense body, thick hide, and strong skeleton make it extraordinarily durable.",
            "Its size, tusks, and strength can be lethal when it feels threatened."),
        AnimalCard("crocodile", "Nile Crocodile", "Reptile", 9, 10, "🐊",
            "Nile crocodiles are ambush predators that use powerful tails and jaws to capture prey near the water's edge.",
            "Fish", "Rivers, lakes, and marshes", CardRarity.RARE,
            "Armored skin and a robust body protect it from many injuries.",
            "A crushing bite, explosive speed, and drowning behavior create extreme danger."),
        AnimalCard("wolf", "Gray Wolf", "Canid", 7, 7, "🐺",
            "Gray wolves are intelligent, social predators whose packs communicate, raise young, and defend territories together.",
            "Meat", "Forest, tundra, and grassland", CardRarity.UNCOMMON,
            "A sturdy frame and high endurance support long-distance travel and hunting.",
            "Sharp teeth and pack coordination are dangerous, though wolves usually avoid people."),
        AnimalCard("rabbit", "European Rabbit", "Mammal", 2, 1, "🐇",
            "Rabbits are burrowing herbivores with powerful hind legs. They are an important prey species in many ecosystems.",
            "Grass", "Meadow, scrubland, and woodland edge", CardRarity.COMMON,
            "Its small, lightweight body is vulnerable to injury.",
            "Rabbits pose little direct danger to an average person."),
        AnimalCard("eagle", "Golden Eagle", "Bird of prey", 4, 5, "🦅",
            "Golden eagles use exceptional eyesight and fast dives to hunt mammals and birds across open country.",
            "Small mammals", "Mountains, cliffs, and open plains", CardRarity.UNCOMMON,
            "Hollow bones and moderate size limit how much trauma it can withstand.",
            "Large talons and a hooked beak can cause serious wounds when handled."),
        AnimalCard("shark", "Reef Shark", "Fish", 7, 7, "🦈",
            "Reef sharks patrol coral ecosystems and help keep marine food webs balanced by preying on weak or abundant animals.",
            "Fish", "Tropical coral reefs", CardRarity.RARE,
            "A streamlined, muscular body is resilient in its aquatic environment.",
            "Rows of sharp teeth and rapid movement are hazardous, although attacks are uncommon."),
        AnimalCard("clownfish", "Clownfish", "Reef fish", 2, 1, "🐠",
            "Clownfish live among sea anemone tentacles. Protective mucus helps them avoid being stung by their host.",
            "Algae", "Warm coral reefs", CardRarity.UNCOMMON,
            "Its small body is fragile outside the shelter of an anemone.",
            "Clownfish are not dangerous to people."),
        AnimalCard("anemone", "Sea Anemone", "Cnidarian", 3, 2, "🪸",
            "Sea anemones are stationary predators related to corals and jellyfish. Their tentacles contain stinging cells.",
            "Plankton", "Rocky shores and coral reefs", CardRarity.UNCOMMON,
            "Its soft body is delicate but can contract and cling tightly to rock.",
            "Most species cause only mild irritation to humans."),
        AnimalCard("rhino", "White Rhinoceros", "Mammal", 10, 9, "🦏",
            "White rhinoceroses are massive grazing mammals. Despite their name, both African rhino species are gray.",
            "Grass", "African grassland and savanna", CardRarity.LEGENDARY,
            "Enormous size and very thick skin provide exceptional durability.",
            "A fast charge from a multi-ton animal can be fatal."),
        AnimalCard("oxpecker", "Red-billed Oxpecker", "Bird", 2, 1, "🐦",
            "Oxpeckers ride on large African mammals and feed on ticks, insects, and sometimes blood from wounds.",
            "Ticks", "African savanna", CardRarity.UNCOMMON,
            "A small bird has limited resistance to physical injury.",
            "It poses almost no danger to an average person."),
        AnimalCard("pistol_shrimp", "Pistol Shrimp", "Crustacean", 2, 3, "🦐",
            "Pistol shrimp snap one oversized claw so quickly that it creates a cavitation bubble and a sharp shock wave.",
            "Tiny crustaceans", "Shallow tropical seabed", CardRarity.RARE,
            "Its small exoskeleton offers modest protection.",
            "The snapping claw is startling and powerful at close range but is not a major human threat."),
        AnimalCard("goby", "Watchman Goby", "Fish", 2, 1, "🐟",
            "Watchman gobies share burrows with nearly blind pistol shrimp and warn their partners when predators approach.",
            "Tiny crustaceans", "Sandy tropical reefs", CardRarity.UNCOMMON,
            "This small reef fish is physically fragile.",
            "It has no meaningful ability to harm an average person."),
        AnimalCard("remora", "Remora", "Fish", 3, 1, "🐟",
            "Remoras use a suction disc to travel on sharks and other large marine animals while feeding on scraps and parasites.",
            "Food scraps", "Warm open ocean and reefs", CardRarity.UNCOMMON,
            "A flexible body and moderate size provide limited durability.",
            "Remoras are not aggressive and present little danger to people.")
    ).map { card -> card.copy(ability = abilities.getValue(card.id)) }

    val frames = listOf(
        CardFrame("black", "Black Border", "Classic", 0xFF171717, true),
        CardFrame("forest", "Forest Frame", "Leaf-carved", 0xFF285943, true),
        CardFrame("ocean", "Ocean Frame", "Wave-carved", 0xFF176B87, true),
        CardFrame("desert", "Desert Frame", "Sandstone", 0xFFC47A32, false),
        CardFrame("arctic", "Arctic Frame", "Frosted", 0xFF87BFD1, false),
        CardFrame("gold", "Legendary Gold", "Ornate", 0xFFD5A928, false, CardRarity.LEGENDARY)
    )

    val relationships = listOf(
        SymbiosisRelationship("clownfish", "anemone", RelationshipType.MUTUALISM,
            "The anemone shelters the clownfish; the fish brings nutrients, cleans, and helps defend its host."),
        SymbiosisRelationship("oxpecker", "rhino", RelationshipType.MUTUALISM,
            "Oxpeckers eat ticks and raise alarm calls while rhinoceroses provide food and a safe perch."),
        SymbiosisRelationship("goby", "pistol_shrimp", RelationshipType.MUTUALISM,
            "The shrimp maintains a shared burrow while the sharp-eyed goby watches for danger."),
        SymbiosisRelationship("remora", "shark", RelationshipType.COMMENSALISM,
            "The remora gains transport and food scraps while the shark is usually little affected.")
    )
}
