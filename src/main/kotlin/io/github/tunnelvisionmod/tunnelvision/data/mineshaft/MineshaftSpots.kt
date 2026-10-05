package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.tunnelvisionmod.tunnelvision.utils.render.Pos

class MineshaftSpots(private val corpses: Map<String, List<Pos>>, private val fossils: Map<String, Pos>) {
	companion object {
		private const val CRYSTAL_KEY = "CRYSTAL"
		private const val CRYSTAL_SUFFIX = "_C"
		private const val RESOURCE = "/assets/tunnelvision/mineshaft_spots.json"

		fun load(): MineshaftSpots = parse(MineshaftSpots::class.java.getResourceAsStream(RESOURCE)!!.reader().use { it.readText() })

		fun parse(json: String): MineshaftSpots {
			val root = JsonParser.parseString(json).asJsonObject
			val corpses = root.getAsJsonObject("corpses").entrySet().associate { (key, value) ->
				key to value.asJsonArray.map { it.asJsonArray.toPos() }
			}
			val fossils = root.getAsJsonObject("fossils").entrySet().associate { (key, value) -> key to value.asJsonArray.toPos() }
			return MineshaftSpots(corpses, fossils)
		}

		private fun JsonArray.toPos() = Pos(this[0].asInt, this[1].asInt, this[2].asInt)
	}

	fun corpseSpots(type: MineshaftType): List<Pos> =
		corpses[if (type.code.endsWith(CRYSTAL_SUFFIX)) CRYSTAL_KEY else type.code] ?: emptyList()

	fun fossil(type: MineshaftType): Pos? = fossils[type.code]
}
