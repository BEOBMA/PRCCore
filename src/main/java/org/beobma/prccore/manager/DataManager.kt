package org.beobma.prccore.manager

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.beobma.prccore.PrcCore
import org.beobma.prccore.data.AdvancementData
import org.beobma.prccore.data.GameData
import org.beobma.prccore.data.LocationListSerializer
import org.beobma.prccore.mine.Mine
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.util.Season
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DataManager {
    private val dataFolder = File(PrcCore.instance.dataFolder, "data")
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    private val secretKey: SecretKey = SecretKeySpec("SJKWP1245434321O".toByteArray(Charsets.UTF_8), "AES")
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16

    // 데이터
    var gameData: GameData = defaultGameMeta()
    var mines: MutableList<Mine> = mutableListOf()
    var plantList: MutableList<Plant> = mutableListOf()
    var interactionFarmlands: MutableList<Location> = mutableListOf()
    var playerList: MutableList<Player> = mutableListOf()
    var advancementList: MutableList<AdvancementData> = mutableListOf()

    var mineExitLocation: Location? = Location(Bukkit.getWorlds().first(), 0.0, 0.0, 0.0)

    /** 전체 저장 */
    fun saveAll() {
        saveEncrypted("data.dat", gameData)
        saveEncrypted("mines.dat", mines)
        saveEncrypted("plants.dat", plantList)
        saveEncrypted("advancements.dat", advancementList)
        saveEncrypted("interaction_locs.dat", interactionFarmlands, LocationListSerializer)
    }

    /** 전체 로드 */
    fun loadAll() {
        gameData = loadEncrypted("data.dat") ?: defaultGameMeta()
        mines = loadEncrypted("mines.dat") ?: mutableListOf()
        plantList = loadEncrypted("plants.dat") ?: mutableListOf()
        advancementList = loadEncrypted("advancements.dat") ?: mutableListOf()
        interactionFarmlands = (loadEncrypted("interaction_locs.dat", LocationListSerializer) ?: mutableListOf()).toMutableList()
    }

    /** 저장 */
    private inline fun <reified T> saveEncrypted(
        fileName: String,
        data: T,
        serializer: KSerializer<T>? = null
    ) {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        val file = File(dataFolder, fileName)

        val plain = if (serializer != null) json.encodeToString(serializer, data) else json.encodeToString(data)

        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        file.writeBytes(iv + encrypted)
        PrcCore.instance.loggerMessage("Encrypted save '$fileName' (${plain.toByteArray().size}→${encrypted.size} bytes)")
    }

    /** 로드 */
    private inline fun <reified T> loadEncrypted(
        fileName: String,
        serializer: KSerializer<T>? = null
    ): T? {
        val file = File(dataFolder, fileName)
        if (!file.exists()) return null

        val raw = file.readBytes()
        if (raw.size < IV_SIZE + 1) return null

        val iv = raw.copyOfRange(0, IV_SIZE)
        val encrypted = raw.copyOfRange(IV_SIZE, raw.size)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        }
        val plainBytes = cipher.doFinal(encrypted)
        val plainText = plainBytes.toString(Charsets.UTF_8)

        val result: T = if (serializer != null) json.decodeFromString(serializer, plainText) else json.decodeFromString(plainText)

        PrcCore.instance.loggerMessage("Decrypted load '$fileName' (${encrypted.size}→${plainBytes.size} bytes)")
        return result
    }

    /** 기본 메타 데이터 생성 */
    private fun defaultGameMeta() = GameData(6, 0, Season.Spring, 1)
}