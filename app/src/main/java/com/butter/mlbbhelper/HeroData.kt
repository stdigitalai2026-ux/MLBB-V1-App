package com.butter.mlbbhelper

object HeroData {
    
    data class Hero(
        val name: String,
        val role: String,
        val threatType: String,
        val coreBuild: List<String>,
        val counterBuild: Map<String, String>,
        val emblem: String
    )

    val allHeroes = listOf(
        Hero("Miya", "Marksman", "Poke",
            listOf("Swift Boots", "Windtalker", "Berserker's Fury", "Haas' Claws", "Malefic Roar"),
            mapOf("Tanky" to "Demon Hunter Sword", "Heal" to "Sea Halberd", "Burst" to "Wind of Nature"),
            "Marksman: Weakness Finder, Fatal, Weapons Master"
        ),
        Hero("Saber", "Assassin", "Burst",
            listOf("Warrior Boots", "Blade of Heptaseas", "Hunter Strike", "Blade of Despair", "Malefic Roar"),
            mapOf("Tanky" to "Malefic Roar", "Burst" to "Wind of Nature", "CC" to "Tough Boots"),
            "Assassin: High and Dry, Master Assassin, Killing Spree"
        ),
        Hero("Gusion", "Assassin", "Burst",
            listOf("Arcane Boots", "Calamity Reaper", "Holy Crystal", "Genius Wand", "Blood Wings"),
            mapOf("Tanky" to "Divine Glaive", "Heal" to "Necklace of Durance", "Burst" to "Winter Truncheon"),
            "Mage: Impure Rage, Fatal, Weapons Master"
        ),
        Hero("Franco", "Tank", "CC",
            listOf("Tough Boots", "Antique Cuirass", "Athena's Shield", "Dominance Ice", "Immortality"),
            mapOf("Magic" to "Athena's Shield", "Physical" to "Blade Armor", "Heal" to "Sea Halberd"),
            "Tank: Vitality, Pull Yourself Together, Brave Smite"
        ),
        Hero("Esmeralda", "Mage/Tank", "Heal",
            listOf("Warrior Boots", "Enchanted Talisman", "Holy Crystal", "Concentrated Energy", "Immortality"),
            mapOf("Heal" to "Necklace of Durance", "Tanky" to "Divine Glaive"),
            "Mage: Impure Rage, Fatal, Tenacity"
        )
    )

    fun cariHero(namaTeks: String): Hero? {
        val teks = namaTeks.lowercase().trim()
        return allHeroes.find { hero -> 
            teks.contains(hero.name.lowercase()) || hero.name.lowercase().contains(teks) 
        }
    }

    fun analisaMatchup(heroKita: Hero, listLawan: List<Hero>): String {
        val sb = StringBuilder()
        sb.append("=== ${heroKita.name.uppercase()} ===\n")
        sb.append("Emblem: ${heroKita.emblem}\n\n")
        sb.append("🔥 BUILD MEMATIKAN (CORE):\n")
        heroKita.coreBuild.forEachIndexed { i, item -> sb.append("${i+1}. $item\n") }
        sb.append("\n")

        if (listLawan.isNotEmpty()) {
            sb.append("🎯 ANALISIS KELEMAHAN LAWAN:\n")
            var butuhAntiTank = false
            var butuhAntiHeal = false
            var butuhAntiBurst = false
            var butuhAntiCC = false

            listLawan.forEach { lawan ->
                sb.append("- ${lawan.name} (${lawan.threatType})\n")
                when (lawan.threatType) {
                    "Tanky" -> butuhAntiTank = true
                    "Heal" -> butuhAntiHeal = true
                    "Burst" -> butuhAntiBurst = true
                    "CC" -> butuhAntiCC = true
                }
            }
            sb.append("\n🛡️ ITEM COUNTER:\n")
            if (butuhAntiTank) sb.append("• Lawan Tank -> ${heroKita.counterBuild["Tanky"] ?: "Malefic Roar"}\n")
            if (butuhAntiHeal) sb.append("• Lawan Heal -> ${heroKita.counterBuild["Heal"] ?: "Sea Halberd"}\n")
            if (butuhAntiBurst) sb.append("• Lawan Burst -> ${heroKita.counterBuild["Burst"] ?: "Wind of Nature"}\n")
            if (butuhAntiCC) sb.append("• Lawan CC -> ${heroKita.counterBuild["CC"] ?: "Tough Boots"}\n")
        }
        return sb.toString()
    }
}
