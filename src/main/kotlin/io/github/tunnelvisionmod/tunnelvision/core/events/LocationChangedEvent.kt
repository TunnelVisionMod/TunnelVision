package io.github.tunnelvisionmod.tunnelvision.core.events

/**
 * Where you are on Hypixel.
 *
 * [server] is the instance you are on, e.g. `mini123A`. Two mineshafts share the `mineshaft`
 * [island] but never the same [server], so it is what tells a party warp into someone else's
 * mineshaft apart from Hypixel repeating the location packet for the one you are already in.
 */
class LocationChangedEvent(val isOnSkyBlock: Boolean, val island: String?, val server: String?) : Event()
