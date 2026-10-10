package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseLootParser
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.LootedItem
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.RngMeterParser
import io.github.tunnelvisionmod.tunnelvision.data.value.CorpseDropItem
import io.github.tunnelvisionmod.tunnelvision.data.value.CorpseDropNames
import io.github.tunnelvisionmod.tunnelvision.data.value.CorpseProfit
import io.github.tunnelvisionmod.tunnelvision.data.value.CorpseProfitBreakdown
import io.github.tunnelvisionmod.tunnelvision.data.value.CorpseValue
import io.github.tunnelvisionmod.tunnelvision.data.value.LootPrice
import io.github.tunnelvisionmod.tunnelvision.data.value.ProfitLine
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.removeFormatting
import io.github.tunnelvisionmod.tunnelvision.utils.Storage
import io.github.tunnelvisionmod.tunnelvision.utils.formatCoins
import io.github.tunnelvisionmod.tunnelvision.utils.formatPrice
import io.github.tunnelvisionmod.tunnelvision.utils.loreLines
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import java.util.Optional
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

/**
 * Replaces the corpse loot message with the same drops priced, and keeps the running profit.
 *
 * The server prints a loot block over several chat lines, so the lines are collected as they arrive
 * and the replacement is sent once the block has gone quiet. Nothing is printed from inside the chat
 * event itself - a message sent while chat is being filtered would be filtered in turn.
 *
 * The replacement keeps the shape of the block it stands in for: the server's own rules frame it,
 * each drop keeps the rarity colour the server gave it, and the prices line up in their own column
 * down the right. Crystals are the one exception to the rarity colour - they read in the crystal's
 * own colour, matching how they look everywhere else in the mod.
 *
 * The totals are kept whether or not the chat message is replaced, so the widget still fills up for
 * somebody who would rather keep Hypixel's own block.
 *
 * If no line in a block reads as an item, a replaced block is printed back unchanged and nothing is
 * recorded. That is the safety net for the block being reworded: a loot message we cannot price is
 * still worth seeing, and swallowing one would lose the only record of what the corpse paid.
 */
object CorpseTracker : Feature {
	/**
	 * Ticks a block has to go quiet before it is rendered. Two, not one, because a block can
	 * straddle a tick boundary when its lines arrive in separate packet batches.
	 */
	private const val FLUSH_AFTER_TICKS = 2

	/** A loot block is a handful of lines; many more means the wording changed and we should stop. */
	private const val MAX_BLOCK_LINES = 40

	/** Enough lore lines to recognise the wording, few enough not to flood the log. */
	private const val MENU_DEBUG_LINES = 40

	private const val INDENT = " "

	private val config get() = ConfigManager.config.mineshaft.corpses.corpseTracker
	private val priceType get() = ConfigManager.config.general.bazaarPrice

	private val totals = CorpseTotals()
	private val meter = RngMeter(CorpseValue.LOCKET_METER_XP)

	/** One corpse's loot block as it arrives: what we could read, and what the server actually sent. */
	private class Block(val type: CorpseType, val suppressed: Boolean) {
		val items = mutableListOf<LootedItem>()
		val original = mutableListOf<Component>()

		/** The rules the server drew around the block, reused so ours is framed the same way. */
		val rules = mutableListOf<Component>()
	}

	private var open: Block? = null
	private val ready = mutableListOf<Block>()
	private var idleTicks = 0

	/** So opening the meter menu reports once, not every tick it stays open. */
	private var menuLogged = false

	/**
	 * Whether [meter] matches the game. Only the RNG Meter menu can confirm it, and a Shattered
	 * Locket drop unsettles it again: it may be the payout or a drop of its own, and only the menu
	 * can tell which way the meter went.
	 */
	private var synced = false

	/**
	 * Whether the meter is set to the Shattered Locket, from the menu. A Locket that drops while it
	 * is not selected cannot be the payout and leaves the meter alone; null means we have not seen.
	 */
	private var locketSelected: Boolean? = null

	private val location = LocationTracker()
	private var remindOnEntry = false

	/** Set from the chat event, which must not print, so the warning goes out on the next tick. */
	private var sayTooEarly = false

	override fun init() {
		totals.load(Storage.data.corpseProfitCoins, Storage.data.corpseProfitCorpses)
		meter.set(Storage.data.corpseMeterXp)
		synced = Storage.data.corpseMeterSynced
		locketSelected = Storage.data.corpseMeterLocketSelected
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
		HudManager.register(Widget)
	}

	/** Clears the totals, on disk as well, for `/tv corpses reset`. */
	fun reset() {
		totals.reset()
		save()
	}

	val overall: CorpseTotal get() = totals.overall

	/** RNG meter progress towards a Shattered Locket, for `/tv corpses`. */
	val meterProgress: Double get() = meter.progress
	val meterNeeded: Double get() = CorpseValue.LOCKET_METER_XP

	fun setMeter(xp: Double) {
		meter.set(xp)
		synced = true
		save()
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		RngMeterParser.selectedFromChat(event.text)?.let { selected ->
			locketSelected = selected
			save()
			if (selected && synced && !meter.isFull) sayTooEarly = true
			return
		}
		if (!SkyBlock.isInMineshaft) return
		val text = event.text.trim()

		CorpseLootParser.corpseType(text)?.let { type ->
			open?.let { ready += it }
			// Whether to hide the block is decided once here, so toggling the setting mid-block
			// cannot leave half of it hidden.
			val block = Block(type, suppressed = config.replaceChat)
			open = block
			swallow(block, event)
			return
		}

		val block = open ?: return

		// The closing rule ends the block. Waiting for it, rather than stopping at the first line we
		// cannot read, is what keeps one odd line from cutting off every drop printed after it.
		if (CorpseLootParser.isRule(text) && block.items.isNotEmpty()) {
			block.rules += event.message
			swallow(block, event)
			ready += block
			open = null
			return
		}
		if (CorpseLootParser.isFrame(text)) {
			swallow(block, event)
			return
		}
		val item = CorpseLootParser.item(text)
		if (item == null) {
			// Not a drop we can read. Leave it on screen and keep going: showing one line we do not
			// understand beats dropping the rest of the loot, and the idle flush still closes us.
			Debug.log { "CorpseTracker: could not read \"$text\" inside a ${block.type} block" }
			return
		}
		// The server already colours the name by rarity and the symbol by gem, so both colours are
		// read off the line itself rather than kept in a table here.
		val parts = runs(event.message)
		block.items += item.copy(
			color = nameColor(parts),
			symbolColor = item.symbol?.let { symbolColor(parts, it) },
		)
		swallow(block, event)
	}

	/** Buffers the line, and hides it when this block is being replaced. */
	private fun swallow(block: Block, event: ChatReceivedEvent) {
		block.original += event.message
		idleTicks = 0
		if (block.suppressed) event.cancel()
		if (block.original.size >= MAX_BLOCK_LINES) {
			ready += block
			open = null
		}
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		readMeterMenu()
		if (sayTooEarly) {
			sayTooEarly = false
			sayTooEarly()
		}
		if (remindOnEntry && SkyBlock.isInMineshaft) {
			remindOnEntry = false
			when {
				!synced -> say("Open your RNG meter once so the Corpse Tracker knows your Frozen Corpse XP. ")
				meter.isFull && locketSelected != true -> sayFull()
				!meter.isFull && locketSelected == true -> sayTooEarly()
			}
		}
		open?.let { block ->
			if (++idleTicks >= FLUSH_AFTER_TICKS) {
				ready += block
				open = null
			}
		}
		if (ready.isEmpty()) return
		val blocks = ready.toList()
		ready.clear()
		blocks.forEach { render(it) }
	}

	/**
	 * Resyncs the meter from the RNG Meter menu whenever it is open.
	 *
	 * Our own count only ever approximates the real one - corpses looted with the feature off, or on
	 * another account, are never seen - so the menu wins outright while it is readable, the same way
	 * the Heart of the Mountain menu wins over chat for crystals.
	 *
	 * The wording of the line was taken from another mod rather than seen first hand, so opening the
	 * menu in debug mode reports what was found, and what the lore said when nothing matched.
	 */
	private fun readMeterMenu() {
		val screen = Compat.screen as? AbstractContainerScreen<*> ?: run {
			menuLogged = false
			return
		}
		val title = screen.title.string.removeFormatting().trim()
		if (!RngMeterParser.isMeterMenu(title)) {
			menuLogged = false
			return
		}
		val items = screen.menu.slots.map { it.item }.filterNot { it.isEmpty }
		val reading = RngMeterParser.parseMenu(items.map { it.plainName() to it.loreLines() })

		if (!menuLogged) {
			menuLogged = true
			if (reading == null) {
				Debug.log { "CorpseTracker: \"$title\" has no Frozen Corpse line. Items: " + items.joinToString { it.plainName() } }
				items.asSequence()
					.flatMap { item -> item.loreLines().map { item.plainName() to it } }
					.filter { (_, line) -> line.contains("xp", ignoreCase = true) }
					.take(MENU_DEBUG_LINES)
					.forEach { (name, line) -> Debug.log { "CorpseTracker:   [$name] $line" } }
			} else {
				Debug.log { "CorpseTracker: \"$title\" says ${reading.progress} / ${reading.needed}, Locket selected: ${reading.locketSelected}, we had ${meter.progress}" }
				items.filter { RngMeterParser.isLocket(it.plainName()) || RngMeterParser.isFrozenCorpse(it.plainName(), it.loreLines()) }.forEach { item ->
					Debug.log { "CorpseTracker:   [${item.plainName()}] " + item.loreLines().joinToString(" | ") }
				}
				if (reading.needed != null && reading.needed != CorpseValue.LOCKET_METER_XP) {
					// Either no Shattered Locket row was found, or it does not cost what we assume.
					// Either way the per-corpse credit is measured against the wrong target.
					Debug.log {
						"CorpseTracker: that target is not a Shattered Locket's ${CorpseValue.LOCKET_METER_XP}" +
							" - no ${RngMeterParser.LOCKET_NAME} row found, or its cost changed"
					}
				}
			}
		}

		reading ?: return
		val selection = reading.locketSelected ?: locketSelected
		if (synced && reading.progress == meter.progress && selection == locketSelected) return
		meter.set(reading.progress)
		synced = true
		locketSelected = selection
		save()
	}

	private fun onLocationChanged(event: LocationChangedEvent) {
		open = null
		ready.clear()
		if (location.isNewLocation(event)) remindOnEntry = true
	}

	private fun render(block: Block) {
		if (block.items.isEmpty()) {
			// Nothing read, so the wording has changed. Put back whatever we hid and record nothing.
			Debug.log { "CorpseTracker: read no drops from a ${block.type} block, leaving it unchanged" }
			if (block.suppressed) block.original.forEach { ChatUtils.sendRaw(it) }
			return
		}
		// Credit this corpse's meter XP first, then see whether it is the corpse that filled it: the
		// corpse that completes the meter also earns its own slice before paying out. The meter is
		// tracked whether or not it is shown, so turning the line back on finds it where it really is.
		val wasFull = meter.isFull
		meter.gain(CorpseValue.meterXp(block.type))
		val droppedLocket = block.items.any { CorpseProfit.isMeterReward(CorpseDropNames.itemFor(it.name)) }
		// A Locket the meter was not set to dropped on its own and left the meter where it was. With
		// the selection unknown, a full meter is taken as the payout, as before.
		val meterAffected = droppedLocket && locketSelected != false
		val meterPayout = meterAffected && meter.claim() && config.showMeter
		val breakdown = CorpseProfit.of(
			block.type,
			block.items,
			priceType,
			includeMeter = config.showMeter,
			meterPayout = meterPayout,
		)
		Debug.log { "CorpseTracker: ${block.type} netted ${breakdown.net}, ${breakdown.unpriced} of ${block.items.size} drops unpriced" }
		breakdown.net?.let { totals.add(block.type, it) }
		if (meterAffected) synced = false
		save()
		if (block.suppressed) lines(breakdown, block.rules).forEach { ChatUtils.sendRaw(it) }
		when {
			meterAffected -> say("A Shattered Locket dropped. Open your RNG meter so the Corpse Tracker can re-read your Frozen Corpse XP. ")
			!wasFull && meter.isFull && locketSelected != true -> sayFull()
		}
	}

	private fun sayFull() = say(
		"Your Frozen Corpse RNG meter is full (${meterAmount(meter.progress)} / ${meterAmount(CorpseValue.LOCKET_METER_XP)}). " +
			"Set it to the Shattered Locket. ",
	)

	private fun sayTooEarly() = say(
		"Your Frozen Corpse RNG meter is set to the Shattered Locket at only " +
			"${meterAmount(meter.progress)} / ${meterAmount(CorpseValue.LOCKET_METER_XP)}. Reset it until it is full. ",
	)

	private fun say(text: String) {
		ChatUtils.send(
			Component.literal(text).withStyle(ChatFormatting.YELLOW).append(
				Component.literal("[Open RNG Meter]").withStyle { style ->
					style.withColor(ChatFormatting.AQUA).withBold(true)
						.withClickEvent(ClickEvent.RunCommand("/rngmeter"))
						.withHoverEvent(HoverEvent.ShowText(Component.literal("Run /rngmeter")))
				},
			),
		)
	}

	private fun save() {
		totals.saveInto(Storage.data.corpseProfitCoins, Storage.data.corpseProfitCorpses)
		Storage.data.corpseMeterXp = meter.progress
		Storage.data.corpseMeterSynced = synced
		Storage.data.corpseMeterLocketSelected = locketSelected
		Storage.save()
	}

	/**
	 * The replacement block: a centred header, a divider, every drop priced, the meter, the key and
	 * - under a second divider - the result.
	 *
	 * The server draws its own rule above the block, and that line arrives before the header, so the
	 * block is not open yet and it is neither captured nor hidden. It therefore stays on screen and
	 * becomes our top frame; drawing another one here is what put two rules above the message. Only
	 * the closing rule is ours, reused from [rules] so it matches.
	 *
	 * Everything is laid out against the width of that rule: the header centres over it and the
	 * prices right-align to its far edge.
	 */
	fun lines(breakdown: CorpseProfitBreakdown, rules: List<Component> = emptyList()): List<Component> {
		val rule = rules.lastOrNull() ?: fallbackRule()
		val width = mc.font.width(rule)

		val rows = mutableListOf<Pair<Component, Component>>()
		breakdown.lines.forEach { rows += label(it) to priceText(it.price, it.fromMeter) }
		if (breakdown.meterCoins > 0) {
			val meterXp = CorpseValue.meterXp(breakdown.type)
			rows += Component.literal(INDENT + "RNG Meter +" + formatPrice(meterXp)).withStyle(ChatFormatting.LIGHT_PURPLE) to
				Component.literal(formatCoins(breakdown.meterCoins)).withStyle(ChatFormatting.GOLD)
		}
		breakdown.type.keyName?.let { key ->
			val cost = breakdown.keyCoins
			rows += Component.literal(INDENT + key).withStyle(ChatFormatting.RED) to
				Component.literal(if (cost == null) "?" else "-" + formatCoins(cost)).withStyle(ChatFormatting.RED)
		}

		val lines = mutableListOf<Component>()
		lines += centred(header(breakdown.type), width)
		lines += divider(width)
		rows.forEach { (label, price) -> lines += row(label, price, width) }
		lines += divider(width)
		lines += row(resultLabel(breakdown), signed(breakdown.net), width)
		lines += rule
		return lines
	}

	private fun header(type: CorpseType): Component =
		Component.literal(type.tabName.uppercase() + " ").withStyle(type.color, ChatFormatting.BOLD)
			.append(Component.literal("CORPSE LOOT!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))

	private fun fallbackRule(): Component =
		Component.literal("▬".repeat(38)).withStyle(ChatFormatting.GREEN)

	/**
	 * A thin rule. Struck-through spaces, because that is the only way to draw a continuous
	 * hairline in chat - a row of dashes reads as dashes.
	 */
	private fun divider(width: Int): Component {
		val space = max(1, mc.font.width(" "))
		return Component.literal(" ".repeat(max(1, width / space))).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH)
	}

	/** Pads [text] on the left so it sits in the middle of [width]. */
	private fun centred(text: Component, width: Int): Component =
		spacer((width - mc.font.width(text)) / 2).append(text)

	/** One row with the price right-aligned to [width], so every price shares a right edge. */
	private fun row(label: Component, price: Component, width: Int): Component =
		Component.empty().append(label)
			.append(spacer(width - mc.font.width(label) - mc.font.width(price)))
			.append(price)

	/**
	 * Blank text exactly [pixels] wide.
	 *
	 * A space is 4px and a bold space 5px, and every width from 12px up is some combination of the
	 * two, so mixing them lands on the exact pixel instead of rounding to the nearest 4. That is
	 * what makes the price column line up rather than merely sit near each other.
	 */
	private fun spacer(pixels: Int): MutableComponent {
		val narrow = max(1, mc.font.width(" "))
		val wide = max(narrow + 1, mc.font.width(Component.literal(" ").withStyle(ChatFormatting.BOLD)))
		val (plain, bold) = spacing(pixels, narrow, wide)
		return Component.literal(" ".repeat(plain))
			.append(Component.literal(" ".repeat(bold)).withStyle(ChatFormatting.BOLD))
	}

	/**
	 * How many [narrow] and [wide] spaces add up to exactly [pixels], using the fewest characters.
	 * Falls back to the closest run of narrow spaces for the few widths that cannot be hit exactly.
	 */
	fun spacing(pixels: Int, narrow: Int, wide: Int): Pair<Int, Int> {
		if (pixels <= 0 || narrow <= 0 || wide <= narrow) return max(0, pixels / max(1, narrow)) to 0
		var best: Pair<Int, Int>? = null
		for (bold in 0..pixels / wide) {
			val rest = pixels - bold * wide
			if (rest % narrow != 0) continue
			val option = rest / narrow to bold
			if (best == null || option.first + option.second < best.first + best.second) best = option
		}
		return best ?: (pixels / narrow to 0)
	}

	/** `  ✦ Fine Peridot Gemstone ×12`, each part in the colour the server gave it. */
	private fun label(line: ProfitLine): Component {
		val text = Component.literal(INDENT)
		line.item.symbol?.let { symbol ->
			val part = Component.literal("$symbol ")
			line.item.symbolColor?.let { part.setStyle(Style.EMPTY.withColor(it)) }
			text.append(part)
		}
		text.append(name(line))
		if (line.fromMeter) text.append(Component.literal(" (meter)").withStyle(ChatFormatting.DARK_GRAY))
		if (line.item.amount > 1) {
			text.append(Component.literal(" ×" + formatPrice(line.item.amount.toDouble())).withStyle(ChatFormatting.GRAY))
		}
		return text
	}

	/** A crystal reads in its own colour; everything else keeps the rarity colour off the message. */
	private fun name(line: ProfitLine): MutableComponent {
		val text = Component.literal(line.item.name)
		val crystal = (line.drop as? CorpseDropItem.Crystal)?.crystal
		return when {
			crystal != null -> text.withStyle(crystal.color)
			line.item.color != null -> text.setStyle(Style.EMPTY.withColor(line.item.color!!))
			else -> text.withStyle(ChatFormatting.WHITE)
		}
	}

	/** The coloured runs of a chat line, as (colour, text) in the order they were written. */
	private fun runs(message: Component): List<Pair<Int?, String>> {
		val parts = mutableListOf<Pair<Int?, String>>()
		message.visit(
			FormattedText.StyledContentConsumer<Unit> { style, text ->
				parts += style.color?.value to text
				Optional.empty()
			},
			Style.EMPTY,
		)
		return parts
	}

	/** The colour of the longest run of letters in the line, which is the item name. */
	private fun nameColor(parts: List<Pair<Int?, String>>): Int? =
		parts.filter { it.first != null && it.second.any(Char::isLetter) }
			.maxByOrNull { part -> part.second.count(Char::isLetter) }
			?.first

	/** The colour of the run the gem symbol sits in, so it keeps the gem's own colour. */
	private fun symbolColor(parts: List<Pair<Int?, String>>, symbol: String): Int? =
		parts.firstOrNull { it.first != null && it.second.contains(symbol) }?.first

	/**
	 * A meter payout is struck through: it is what the corpse handed over, but the coins were
	 * already counted across every corpse that filled the meter, so it adds nothing here.
	 */
	private fun priceText(price: LootPrice, fromMeter: Boolean = false): Component = when (price) {
		is LootPrice.Coins -> {
			val text = Component.literal(formatCoins(price.total))
			if (fromMeter) text.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH) else text.withStyle(ChatFormatting.GOLD)
		}
		LootPrice.Unknown -> Component.literal("?").withStyle(ChatFormatting.DARK_GRAY)
	}

	private fun resultLabel(breakdown: CorpseProfitBreakdown): Component {
		val net = breakdown.net
		val word = if (net != null && net < 0) "Loss" else "Profit"
		val text = Component.literal(INDENT + word).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
		if (breakdown.unpriced > 0) {
			text.append(Component.literal(" (" + breakdown.unpriced + " unpriced)").withStyle(ChatFormatting.DARK_GRAY))
		}
		return text
	}

	private fun signed(coins: Double?): Component {
		if (coins == null) return Component.literal("?").withStyle(ChatFormatting.DARK_GRAY)
		val color = if (coins < 0) ChatFormatting.RED else ChatFormatting.GREEN
		val sign = if (coins > 0) "+" else ""
		return Component.literal(sign + formatCoins(coins)).withStyle(color, ChatFormatting.BOLD)
	}

	private val widgetHeader: Component = Component.literal("Corpse Profit").withStyle(ChatFormatting.GOLD)

	/** Clear space between the widest widget label and its value. */
	private const val WIDGET_GAP = 6

	private fun widgetLabel(name: String, color: ChatFormatting, total: CorpseTotal): Component =
		Component.literal(name).withStyle(color)
			.append(Component.literal(" ×" + total.corpses).withStyle(ChatFormatting.DARK_GRAY))

	/** Lays the widget out like the message: values right-aligned, here to the widest row. */
	private fun widgetLines(totals: List<Pair<String, Pair<ChatFormatting, CorpseTotal>>>, meterLine: Component): List<Component> {
		val rows = totals.map { (name, rest) -> widgetLabel(name, rest.first, rest.second) to signed(rest.second.coins) }
		val width = (rows.maxOfOrNull { mc.font.width(it.first) + mc.font.width(it.second) } ?: 0) + WIDGET_GAP
		return listOf(widgetHeader) + rows.map { (label, value) -> row(label, value, width) } + meterLine
	}

	/** `1.7M`, rounded down so a meter 1,000 XP short of full never reads as full. */
	fun meterAmount(xp: Double): String = when {
		xp >= 1_000_000 -> String.format(Locale.ROOT, "%.1fM", floor(xp / 100_000) / 10)
		xp >= 1_000 -> String.format(Locale.ROOT, "%.0fk", floor(xp / 1_000))
		else -> String.format(Locale.ROOT, "%.0f", floor(xp))
	}

	/**
	 * The meter's progress, with what to do about it when something needs doing: open the menu while
	 * our count is unconfirmed, select the Locket once it is full. A guess is marked with `~`.
	 */
	private fun meterLine(progress: Double, synced: Boolean, locketSelected: Boolean?): Component {
		val needed = CorpseValue.LOCKET_METER_XP
		val amount = (if (synced) "" else "~") + meterAmount(progress) + " / " + meterAmount(needed)
		val text = Component.literal("RNG Meter ").withStyle(ChatFormatting.GRAY)
		return when {
			!synced -> text.append(Component.literal("$amount · open /rngmeter").withStyle(ChatFormatting.YELLOW))
			progress >= needed && locketSelected == true -> text.append(Component.literal("$amount · Locket selected").withStyle(ChatFormatting.LIGHT_PURPLE))
			locketSelected == true -> text.append(Component.literal("$amount · reset until full").withStyle(ChatFormatting.RED))
			progress >= needed -> text.append(Component.literal("$amount · set Shattered Locket").withStyle(ChatFormatting.LIGHT_PURPLE))
			else -> text.append(Component.literal(amount + " (" + (progress / needed * 100).toInt() + "%)").withStyle(ChatFormatting.DARK_GRAY))
		}
	}

	object Widget : HudWidget("corpse_tracker", "Corpse Profit", HudPosition(0.02f, 0.3f)) {
		override val isEnabled get() = config.enabled && config.widget

		override fun getLines(): List<Component> {
			if (!SkyBlock.isOnMiningIsland) return emptyList()
			val perType = totals.perType
			val rows = if (perType.isEmpty()) {
				emptyList()
			} else {
				perType.map { (type, total) -> type.tabName to (type.color to total) } +
					("Overall" to (ChatFormatting.WHITE to totals.overall))
			}
			return widgetLines(rows, meterLine(meter.progress, synced, locketSelected))
		}

		override fun getExampleLines() = widgetLines(
			listOf(
				"Lapis" to (CorpseType.LAPIS.color to CorpseTotal(42, 1_240_000.0)),
				"Vanguard" to (CorpseType.VANGUARD.color to CorpseTotal(2, -29_400_000.0)),
				"Overall" to (ChatFormatting.WHITE to CorpseTotal(44, -28_160_000.0)),
			),
			meterLine(1_200_000.0, synced = true, locketSelected = false),
		)
	}
}
